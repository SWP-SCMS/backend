package com.scms.backend.course.session;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.course.CourseException;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.room.RoomRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/course-runs")
class CourseResourceAvailabilityController {

	private final AccountRepository accounts;

	private final RoomRepository rooms;

	private final CourseSessionRepository sessions;

	private final JdbcTemplate db;

	CourseResourceAvailabilityController(AccountRepository accounts, RoomRepository rooms, CourseSessionRepository sessions, JdbcTemplate db) {
		this.accounts = accounts;
		this.rooms = rooms;
		this.sessions = sessions;
		this.db = db;
	}

	record Window(@NotNull Instant startAt, @NotNull Instant endAt) {
	}

	record Request(@NotEmpty @Size(max = 366) List<@NotNull @Valid Window> windows, @Positive int capacity, UUID excludeSessionId) {
	}

	record CoachOption(UUID accountId, String fullName) {
	}

	record RoomOption(UUID id, String name, int capacity) {
	}

	record Response(List<CoachOption> coaches, List<RoomOption> rooms) {
	}

	record Occupied(UUID coachId, UUID roomId, Instant startAt, Instant endAt) {
	}

	@PostMapping("/resource-availability")
	@Transactional(readOnly = true)
	Response available(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Request request) {
		if (!accounts.existsByIdAndRoleAndStatus(UUID.fromString(jwt.getSubject()), AccountRole.MANAGER, AccountStatus.ACTIVE))
			throw new com.scms.backend.auth.InvalidAuthenticatedAccountException();
		if (request.windows().stream().anyMatch(w -> !w.startAt().isBefore(w.endAt())))
			throw CourseException.validation("windows", "Giờ kết thúc phải sau giờ bắt đầu.");
		if (request.excludeSessionId() != null && !sessions.existsById(request.excludeSessionId()))
			throw CourseException.invalidSession();
		Instant start = request.windows().stream().map(Window::startAt).min(Comparator.naturalOrder()).orElseThrow();
		Instant end = request.windows().stream().map(Window::endAt).max(Comparator.naturalOrder()).orElseThrow();
		// Fetch occupied intervals once; only intervals overlapping actual requested windows block resources.
		List<Occupied> occupied = db.query("""
			select coach_id, room_id, start_at, end_at from (
			    select teaching_coach_account_id coach_id, room_id, start_time start_at, end_time end_at
			    from class_sessions where status in ('SCHEDULED','IN_PROGRESS')
			    union all
			    select coach_account_id, room_id, start_at, end_at from course_sessions
			    where status in ('SCHEDULED','MAKEUP_SCHEDULED')
			      and id <> coalesce(cast(? as uuid), '00000000-0000-0000-0000-000000000000'::uuid)
			    union all
			    select coach_account_id, null::uuid, start_time, end_time from coach_leave_requests where status='APPROVED'
			    union all
			    select null::uuid, room_id, start_time, end_time from room_suspensions
			) busy where start_at < ? and end_at > ?
			""", (rs, row) -> new Occupied(rs.getObject("coach_id", UUID.class), rs.getObject("room_id", UUID.class), rs.getTimestamp("start_at").toInstant(), rs.getTimestamp("end_at").toInstant()), request.excludeSessionId(), Timestamp.from(end), Timestamp.from(start));
		Set<UUID> busyCoaches = new HashSet<>();
		Set<UUID> busyRooms = new HashSet<>();
		for (Occupied item : occupied) {
			if (request.windows().stream().anyMatch(w -> item.startAt().isBefore(w.endAt()) && item.endAt().isAfter(w.startAt()))) {
				if (item.coachId() != null)
					busyCoaches.add(item.coachId());
				if (item.roomId() != null)
					busyRooms.add(item.roomId());
			}
		}
		var coachOptions = accounts.findByRoleAndStatusOrderById(AccountRole.COACH, AccountStatus.ACTIVE).stream().filter(c -> !busyCoaches.contains(c.getId())).map(c -> new CoachOption(c.getId(), c.getFullName())).toList();
		var roomOptions = rooms.findAll().stream().filter(r -> r.getStatus() == RoomStatus.ACTIVE && r.getCapacity() >= request.capacity() && !busyRooms.contains(r.getId())).sorted(Comparator.comparing(Room::getName)).map(r -> new RoomOption(r.getId(), r.getName(), r.getCapacity())).toList();
		return new Response(coachOptions, roomOptions);
	}
}
