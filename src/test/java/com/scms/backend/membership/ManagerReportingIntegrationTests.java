package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ManagerReportingIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired JdbcTemplate db;
	@Autowired ManagerReportingService reports;

	@Test
	void reportsUseConfirmedSourcesHalfOpenRangesAndExcludeTestMembersByDefault() {
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant from = now.minus(10, ChronoUnit.HOURS);
		Instant to = now.minus(2, ChronoUnit.HOURS);
		UUID manager = account("MANAGER", false);
		UUID coach = account("COACH", false);
		UUID member = account("MEMBER", false);
		UUID testMember = account("MEMBER", true);
		UUID offer = offer(manager);
		UUID membership = membership(member, manager, offer, false, now);
		UUID testMembership = membership(testMember, manager, offer, true, now);
		UUID sportClass = sportClass();
		UUID room = room();
		UUID session = session(sportClass, coach, room, manager, now.minus(4, ChronoUnit.HOURS), 4);
		session(sportClass, coach, room, manager, now.minus(6, ChronoUnit.HOURS), 6);
		session(sportClass, coach, room, manager, to, 20);
		UUID normalBooking = booking(session, member, membership, manager, "BOOKED");
		booking(session, member, membership, manager, "CANCELLED");
		UUID testBooking = booking(session, testMember, testMembership, manager, "BOOKED");
		UUID visit = visit(member, membership, manager, now.minus(5, ChronoUnit.HOURS), now.minus(3, ChronoUnit.HOURS));
		attendance(normalBooking, "PRESENT", visit);
		attendance(testBooking, "ABSENT", null);

		ManagerDashboardResponse dashboard = reports.dashboard(manager, from, to, false);

		assertThat(dashboard.members().totalMembers()).isOne();
		assertThat(dashboard.members().activeMemberships()).isOne();
		assertThat(dashboard.membershipRevenue().paidPayments()).isOne();
		assertThat(dashboard.membershipRevenue().paidRevenue()).isEqualTo(BigInteger.valueOf(10_000));
		assertThat(dashboard.bookings().totalBookings()).isEqualTo(2);
		assertThat(dashboard.bookings().bookedBookings()).isOne();
		assertThat(dashboard.bookings().cancelledBookings()).isOne();
		assertThat(dashboard.attendance().reportableSessions()).isEqualTo(2);
		assertThat(dashboard.attendance().present()).isOne();
		assertThat(dashboard.attendance().absent()).isZero();
		assertThat(dashboard.centerVisits().totalVisits()).isOne();
		assertThat(dashboard.classUtilization().totalSessions()).isEqualTo(2);
		assertThat(dashboard.classUtilization().reportableSessions()).isEqualTo(2);
		assertThat(dashboard.classUtilization().totalCapacity()).isEqualTo(10);
		assertThat(dashboard.classUtilization().utilizationRate()).isEqualByComparingTo(new BigDecimal("0.1000"));

		assertThat(reports.members(manager, from, to, true).totalMembers()).isEqualTo(2);
		assertThat(reports.bookings(manager, from, to, true).totalBookings()).isEqualTo(3);
		assertThat(reports.attendance(manager, from, to, true).absent()).isOne();
	}

	private UUID account(String role, boolean testData) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash,is_test_data,created_at)
			values (?,?, 'ACTIVE', ?, ?, ?, date '1990-01-01', '{noop}password', ?, ?)
			""", id, role, role + " Report User", id.toString().substring(0, 12), id + "@report.test",
			testData, Timestamp.from(Instant.now().minus(8, ChronoUnit.HOURS)));
		return id;
	}

	private UUID offer(UUID manager) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into membership_offers(id,plan_code,name,description,price_amount,duration_days,status,created_by_account_id)
			values (?,'BASIC',?,'Reporting fixture',10000,30,'ACTIVE',?)
			""", id, "Report Offer " + id, manager);
		return id;
	}

	private UUID membership(UUID member, UUID manager, UUID offer, boolean testData, Instant now) {
		UUID order = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		UUID membership = UUID.randomUUID();
		Instant created = now.minus(7, ChronoUnit.HOURS);
		db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
			offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,
			duration_days_snapshot,payment_method,status,expires_at,paid_at,created_at,is_test_data)
			values (?,?,?,?,?,'Report Offer','BASIC',10000,'VND',30,'CASH','PAID',?,?,?,?,?)
			""", order, "ORD-" + order, member, manager, offer, Timestamp.from(now.plus(1, ChronoUnit.DAYS)),
			Timestamp.from(created), Timestamp.from(created), testData);
		db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,processed_by_account_id,paid_at,created_at)
			values (?,?,'CASH','PAID',10000,'VND',?,?,?)
			""", payment, order, manager, Timestamp.from(created), Timestamp.from(created));
		db.update("""
			insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,
			price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at,created_at)
			values (?,?,?,?,'BASIC','Report Offer',10000,'VND',30,'ACTIVE',?,?,?)
			""", membership, member, order, offer, Timestamp.from(now.minus(1, ChronoUnit.DAYS)),
			Timestamp.from(now.plus(29, ChronoUnit.DAYS)), Timestamp.from(created));
		return membership;
	}

	private UUID sportClass() {
		UUID discipline = UUID.randomUUID();
		UUID sportClass = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values (?,?, 'ACTIVE')", discipline,
			"Report Discipline " + discipline);
		db.update("insert into sport_classes(id,discipline_id,name,class_type,status) values (?,?,?,'GROUP','ACTIVE')",
			sportClass, discipline, "Report Class " + sportClass);
		return sportClass;
	}

	private UUID room() {
		UUID id = UUID.randomUUID();
		db.update("insert into rooms(id,name,capacity,status) values (?,?,30,'ACTIVE')", id, "Report Room " + id);
		return id;
	}

	private UUID session(UUID sportClass, UUID coach, UUID room, UUID manager, Instant start, int capacity) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into class_sessions(id,sport_class_id,teaching_coach_account_id,room_id,start_time,end_time,
			capacity,status,created_by_account_id,created_at)
			values (?,?,?,?,?,?,?,'COMPLETED',?,?)
			""", id, sportClass, coach, room, Timestamp.from(start), Timestamp.from(start.plus(1, ChronoUnit.HOURS)),
			capacity, manager, Timestamp.from(start.minus(1, ChronoUnit.DAYS)));
		return id;
	}

	private UUID booking(UUID session, UUID member, UUID membership, UUID actor, String status) {
		UUID id = UUID.randomUUID();
		boolean cancelled = "CANCELLED".equals(status);
		db.update("""
			insert into bookings(id,class_session_id,member_account_id,membership_id,status,booked_by_account_id,
			cancelled_by_account_id,cancellation_source,cancelled_at,created_at)
			values (?,?,?,?,?,?,?,?,?,?)
			""", id, session, member, membership, status, actor, cancelled ? actor : null,
			cancelled ? "MEMBER" : null, cancelled ? Timestamp.from(Instant.now()) : null,
			Timestamp.from(Instant.now().minus(3, ChronoUnit.HOURS)));
		return id;
	}

	private UUID visit(UUID member, UUID membership, UUID actor, Instant in, Instant out) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into center_visits(id,member_account_id,membership_id,checked_in_by_account_id,checked_in_at,
			checked_out_at,checked_out_by_account_id,checkout_source)
			values (?,?,?,?,?,?,?,'RECEPTIONIST')
			""", id, member, membership, actor, Timestamp.from(in), Timestamp.from(out), actor);
		return id;
	}

	private void attendance(UUID booking, String status, UUID visit) {
		db.update("""
			insert into attendance(id,booking_id,status,center_visit_id,recording_source,recorded_at)
			values (?,?,?,?, 'SYSTEM', ?)
			""", UUID.randomUUID(), booking, status, visit, Timestamp.from(Instant.now().minus(3, ChronoUnit.HOURS)));
	}
}
