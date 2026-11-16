package com.scms.backend.qa;

import com.scms.backend.account.AccountRole;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Synthetic fixtures for owned disposable databases only, never production migrations.
 */
public final class CourseQaFixtures {

	public static final String PASSWORD = "Qa-only-password-123!";

	private final JdbcTemplate db;

	public CourseQaFixtures(JdbcTemplate db) {
		this.db = db;
	}

	public UUID membership(UUID memberId, UUID actorId, String planCode, String offerName, Instant now) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		db.update("""
			insert into membership_plans (plan_code, display_name, supports_booking, supports_personal_coaching)
			values (?, ?, false, false)
			on conflict (plan_code) do nothing
			""", planCode, offerName);
		db.update("""
			insert into membership_offers (id, plan_code, name, description, price_amount, duration_days,
				status, created_by_account_id)
			values (?, ?, ?, 'Center access', 12000, 30, 'ACTIVE', ?)
			""", offerId, planCode, offerName, actorId);
		db.update("""
			insert into membership_orders (id, order_number, member_account_id, created_by_account_id, offer_id,
				offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot,
				duration_days_snapshot, payment_method, status, expires_at, paid_at)
			values (?, ?, ?, ?, ?, ?, ?, 12000, 'VND', 30, 'CASH', 'PAID', ?, ?)
			""", orderId, "ORD-" + orderId, memberId, actorId, offerId, offerName, planCode, java.sql.Timestamp.from(now.plusSeconds(3600)), java.sql.Timestamp.from(now));
		db.update("""
			insert into payments (id, order_id, method, status, amount, currency_code,
				processed_by_account_id, paid_at)
			values (?, ?, 'CASH', 'PAID', 12000, 'VND', ?, ?)
			""", paymentId, orderId, actorId, java.sql.Timestamp.from(now));
		db.update("""
			insert into memberships (id, member_account_id, order_id, offer_id, plan_code_snapshot,
				offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
				status, starts_at, ends_at)
			values (?, ?, ?, ?, ?, ?, 12000, 'VND', 30, 'ACTIVE', ?, ?)
			""", membershipId, memberId, orderId, offerId, planCode, offerName, java.sql.Timestamp.from(now.minusSeconds(60)), java.sql.Timestamp.from(now.plusSeconds(3600)));
		return membershipId;
	}

	public UUID account(String name, AccountRole role) {
		String email = name + "@course-qa.invalid";
		var existing = db.queryForList("select id from accounts where email=?", UUID.class, email);
		if (!existing.isEmpty())
			return existing.getFirst();
		var id = UUID.randomUUID();
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) values(?,?,'ACTIVE',?,?,?,?,?)", id, role.name(), "QA " + name, "09" + String.format("%08d", Math.floorMod(name.hashCode(), 100000000)), email, java.sql.Date.valueOf("1990-01-01"), new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(PASSWORD));
		if (role == AccountRole.MEMBER)
			db.update("insert into member_profiles(account_id) values(?)", id);
		return id;
	}

	public record Payment(UUID paymentId, UUID orderId, String reference, UUID runId, UUID memberId) {
	}

	public Payment payment(UUID member, Instant deadline, boolean createPayment, int totalUnits) {
		UUID coach = account("coach", AccountRole.COACH);
		UUID discipline = UUID.randomUUID();
		UUID course = UUID.randomUUID();
		UUID run = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID reservation = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		UUID room = UUID.randomUUID();
		String reference = "COR-" + order.toString().replace("-", "").toUpperCase();
		Instant createdAt = deadline.minusSeconds(60);
		db.update("insert into disciplines(id,name,status) values(?,?,'ACTIVE')", discipline, "D-" + discipline);
		db.update("insert into courses(id,code,name,discipline_id,status) values(?,?,?,?,'PUBLISHED')", course, "C-" + course, "Course", discipline);
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values(?,?,2,'EXCLUSIVE','ACTIVE')", room, "Room " + room);
		db.update("""
			insert into course_runs(id,run_code,course_id,primary_coach_account_id,tuition_vnd,capacity,
				minimum_enrollment,registration_opens_at,registration_closes_at,planned_start_at,
				planned_end_at,total_units,named_coach_commitment,policy_version_snapshot,status)
			values(?,?,?,?,500000,2,1,?,?,?,?,?,true,'P1','OPEN_FOR_REGISTRATION')
			""", run, "R-" + run, course, coach, java.sql.Timestamp.from(createdAt.minusSeconds(3600)), java.sql.Timestamp.from(deadline.plusSeconds(3600)), java.sql.Timestamp.from(deadline.plusSeconds(7200)), java.sql.Timestamp.from(deadline.plusSeconds(3600L * (totalUnits + 2))), totalUnits);
		db.update("""
			insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,
				tuition_vnd_snapshot,currency_code,status,idempotency_key,created_at,updated_at)
			values(?,?,?,?,?,500000,'VND','PENDING_PAYMENT',?,?,?)
			""", order, reference, member, member, run, "key-" + order, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(createdAt));
		db.update("""
			insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at,
				created_at,updated_at) values(?,?,?,?,'HELD',?,?,?)
			""", reservation, order, member, run, java.sql.Timestamp.from(deadline), java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(createdAt));
		if (createPayment)
			db.update("""
				insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,
					actor_account_id,payment_reference,amount,currency_code,status,deadline_at,bank_code_snapshot,
					bank_account_number_snapshot,bank_account_name_snapshot,created_at)
				values(?,?,?,?,?,?,500000,'VND','PENDING',?,'MB','123456789','SCMS GYM',?)
				""", payment, order, reservation, member, member, reference, java.sql.Timestamp.from(deadline), java.sql.Timestamp.from(createdAt));
		for (int unit = 1; unit <= totalUnits; unit++) {
			Instant startAt = deadline.plusSeconds(7200L + (unit - 1L) * 3600L);
			Instant endAt = startAt.plusSeconds(3600);
			db.update("""
				insert into course_sessions(id,course_run_id,unit_number,room_id,coach_account_id,start_at,end_at,
					original_start_at,original_end_at,allocation_vnd,status)
				values(?,?,?,?,?,?,?,?,?,500000,'SCHEDULED')
				""", UUID.randomUUID(), run, unit, room, coach, java.sql.Timestamp.from(startAt), java.sql.Timestamp.from(endAt), java.sql.Timestamp.from(startAt), java.sql.Timestamp.from(endAt));
		}
		return new Payment(createPayment ? payment : null, order, reference, run, member);
	}
}
