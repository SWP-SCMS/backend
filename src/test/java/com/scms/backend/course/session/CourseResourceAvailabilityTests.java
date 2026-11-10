package com.scms.backend.course.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.scheduling.Room;
import com.scms.backend.scheduling.RoomStatus;
import com.scms.backend.scheduling.room.RoomRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.jwt.Jwt;

class CourseResourceAvailabilityTests {

	AccountRepository accounts = mock(AccountRepository.class);

	RoomRepository rooms = mock(RoomRepository.class);

	CourseSessionRepository sessions = mock(CourseSessionRepository.class);

	JdbcTemplate db = mock(JdbcTemplate.class);

	CourseResourceAvailabilityController controller = new CourseResourceAvailabilityController(accounts, rooms, sessions, db);

	UUID manager = UUID.randomUUID(), coachId = UUID.randomUUID(), roomId = UUID.randomUUID();

	Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject(manager.toString()).build();

	Instant start = Instant.parse("2030-01-01T03:00:00Z");

	void setup(List<CourseResourceAvailabilityController.Occupied> busy) {
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		Account coach = mock(Account.class);
		when(coach.getId()).thenReturn(coachId);
		when(coach.getFullName()).thenReturn("Coach");
		when(accounts.findByRoleAndStatusOrderById(AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(List.of(coach));
		Room room = mock(Room.class);
		when(room.getId()).thenReturn(roomId);
		when(room.getName()).thenReturn("Room");
		when(room.getCapacity()).thenReturn(20);
		when(room.getStatus()).thenReturn(RoomStatus.ACTIVE);
		when(rooms.findAll()).thenReturn(List.of(room));
		when(db.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<CourseResourceAvailabilityController.Occupied>>any(), any(Object[].class))).thenReturn(busy);
	}

	CourseResourceAvailabilityController.Request request(int capacity, UUID exclude) {
		return new CourseResourceAvailabilityController.Request(List.of(new CourseResourceAvailabilityController.Window(start, start.plusSeconds(3600)), new CourseResourceAvailabilityController.Window(start.plusSeconds(86400), start.plusSeconds(90000))), capacity, exclude);
	}

	@Test
	void excludesResourcesBlockedOnlyOnSecondDay() {
		setup(List.of(new CourseResourceAvailabilityController.Occupied(coachId, roomId, start.plusSeconds(87000), start.plusSeconds(88000))));
		var result = controller.available(jwt, request(10, null));
		assertThat(result.coaches()).isEmpty();
		assertThat(result.rooms()).isEmpty();
	}

	@Test
	void allowsAdjacentAndGapIntervalsButFiltersCapacity() {
		setup(List.of(new CourseResourceAvailabilityController.Occupied(coachId, roomId, start.plusSeconds(3600), start.plusSeconds(7200))));
		assertThat(controller.available(jwt, request(10, null)).rooms()).hasSize(1);
		var result = controller.available(jwt, request(21, null));
		assertThat(result.coaches()).hasSize(1);
		assertThat(result.rooms()).isEmpty();
	}

	@Test
	void requiresActiveManager() {
		assertThatThrownBy(() -> controller.available(jwt, request(10, null))).isInstanceOf(com.scms.backend.auth.InvalidAuthenticatedAccountException.class);
		verifyNoInteractions(db);
	}

	@Test
	void editExclusionIsPassedToQuery() {
		setup(List.of());
		UUID excluded = UUID.randomUUID();
		when(sessions.existsById(excluded)).thenReturn(true);
		assertThat(controller.available(jwt, request(10, excluded)).rooms()).hasSize(1);
		verify(db).query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<CourseResourceAvailabilityController.Occupied>>any(), eq(excluded), any(), any());
	}
}
