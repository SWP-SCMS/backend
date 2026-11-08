package com.scms.backend.course;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.scms.backend.account.*;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.Room;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

class CourseLeaveCancellationTests {
    @Test
    void rejectsEnrolledSessionAndAllowsEmptySession() {
        var db = mock(JdbcTemplate.class);
        var runs = mock(CourseRunRepository.class);
        var sessions = mock(CourseSessionRepository.class);
        var schedules = mock(CourseSessionService.class);
        var accounts = mock(AccountRepository.class);
        var audits = mock(AuditEventRepository.class);
        var notifications = mock(NotificationWriter.class);
        var service = new CourseSessionDisruptionService(db, runs, sessions, schedules, accounts, audits,
            notifications, Clock.fixed(Instant.parse("2030-11-08T00:00:00Z"), ZoneOffset.UTC));
        UUID manager = UUID.randomUUID(), runId = UUID.randomUUID(), sessionId = UUID.randomUUID(), member = UUID.randomUUID();
        var run = mock(CourseRun.class);
        when(run.getId()).thenReturn(runId);
        when(run.getStatus()).thenReturn(CourseRunStatus.CONFIRMED);
        var session = new CourseSession(sessionId, run, 1, mock(Room.class), mock(Account.class),
            Instant.parse("2030-11-10T01:00:00Z"), Instant.parse("2030-11-10T02:00:00Z"), BigInteger.TEN);
        ReflectionTestUtils.setField(session, "version", 2L);
        when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
        when(runs.findByIdForUpdate(runId)).thenReturn(Optional.of(run));
        when(sessions.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
        when(db.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<UUID>>any(), eq(sessionId))).thenReturn(List.of(member));

        assertThatThrownBy(() -> service.cancelFromEndpoint(manager, runId, sessionId, 1L)).isInstanceOf(CourseException.class);
        assertThat(session.getStatus()).isEqualTo(CourseSessionStatus.SCHEDULED);
        assertThatThrownBy(() -> service.cancelFromEndpoint(manager, runId, sessionId, 2L))
            .isInstanceOf(CourseException.class).hasFieldOrPropertyWithValue("code", "SESSION_HAS_MEMBERS");
        assertThat(session.getStatus()).isEqualTo(CourseSessionStatus.SCHEDULED);
        verifyNoInteractions(notifications, audits, schedules);
        verify(sessions, never()).saveAndFlush(any());
        when(db.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<UUID>>any(), eq(sessionId))).thenReturn(List.of());
        service.cancelFromEndpoint(manager, runId, sessionId, 2L);
        assertThat(session.getStatus()).isEqualTo(CourseSessionStatus.CANCELLED_PENDING_MAKEUP);
        verify(sessions).saveAndFlush(session);
        verify(audits).save(any());
        verifyNoInteractions(notifications, schedules);
    }
}
