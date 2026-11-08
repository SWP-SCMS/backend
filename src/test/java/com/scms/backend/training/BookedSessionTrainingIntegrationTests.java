package com.scms.backend.training;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import com.scms.backend.account.AccountRole;
import com.scms.backend.qa.CourseQaFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class BookedSessionTrainingIntegrationTests {
    @Container @ServiceConnection static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
    @Autowired JdbcTemplate db;
    @Autowired BookedSessionTrainingService service;
    @Autowired MemberTrainingResultService memberResults;
    @Autowired MemberTrainingFeedbackService memberFeedback;
    @MockitoBean Clock clock;
    static final Instant NOW = Instant.parse("2035-01-01T10:00:00Z");

    record Fixture(UUID coach, UUID member, UUID session) {}

    Fixture fixture() {
        when(clock.instant()).thenReturn(NOW);
        CourseQaFixtures fixtures = new CourseQaFixtures(db);
        UUID member = fixtures.account("training-" + UUID.randomUUID(), AccountRole.MEMBER);
        UUID coach = fixtures.account("coach", AccountRole.COACH);
        var payment = fixtures.payment(member, NOW.minusSeconds(10800), true, 1);
        db.update("update course_runs set status='IN_PROGRESS' where id=?", payment.runId());
        UUID enrollment = UUID.randomUUID();
        db.update("""
            insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,
                member_account_id,actor_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at)
            select ?,p.course_order_id,p.course_reservation_id,p.id,p.member_account_id,p.actor_account_id,
                o.course_run_id,p.amount,1,? from course_payments p join course_orders o on o.id=p.course_order_id where p.id=?
            """, enrollment, Timestamp.from(NOW.minusSeconds(14400)), payment.paymentId());
        UUID session = db.queryForObject("select id from course_sessions where course_run_id=?", UUID.class, payment.runId());
        db.update("""
            insert into course_enrollment_sessions(id,course_enrollment_id,course_session_id,course_run_id,unit_number,allocation_vnd,disposition)
            values(?,?,?,?,1,500000,'ENROLLED')
            """, UUID.randomUUID(), enrollment, session, payment.runId());
        return new Fixture(coach, member, session);
    }

    @Test void migrationAndHistorySupportCurrentCourseSessionsAndRetries() {
        Fixture f = fixture();
        var created = service.save(f.coach(), f.session(), f.member(), true, null, "Done", "Good");
        var edited = service.save(f.coach(), f.session(), f.member(), true, null, "More", "Better");
        assertThat(edited.result().id()).isEqualTo(created.result().id());
        assertThat(edited.feedback().id()).isEqualTo(created.feedback().id());
        assertThat(edited.result().activityAt()).isEqualTo(NOW.minusSeconds(3600));
        var history = memberResults.viewOwnHistory(f.member(), PageRequest.of(0, 20)).content();
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().courseSessionId()).isEqualTo(f.session());
        assertThat(history.getFirst().sessionName()).isEqualTo("Course");
        var feedbackHistory = memberFeedback.viewOwnHistory(f.member(), PageRequest.of(0, 20)).content();
        assertThat(feedbackHistory).hasSize(1);
        assertThat(feedbackHistory.getFirst().courseSessionId()).isEqualTo(f.session());
        assertThat(feedbackHistory.getFirst().content()).isEqualTo("Better");
        assertThat(service.get(f.coach(), f.session(), f.member(), true).result().id()).isEqualTo(created.result().id());
    }

    @Test void feedbackFailureRollsBackResultAndAuditTogether() {
        Fixture f = fixture();
        assertThatThrownBy(() -> service.save(f.coach(), f.session(), f.member(), true, null, "Done", " "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(db.queryForObject("select count(*) from training_results where course_session_id=?", Long.class, f.session())).isZero();
        assertThat(memberResults.viewOwnHistory(f.member())).isEmpty();
    }
}
