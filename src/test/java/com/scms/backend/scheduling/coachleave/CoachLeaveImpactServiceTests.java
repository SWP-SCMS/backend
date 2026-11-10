package com.scms.backend.scheduling.coachleave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.scms.backend.scheduling.CoachLeaveException;
import com.scms.backend.scheduling.CoachLeaveStatus;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class CoachLeaveImpactServiceTests {

	final UUID manager = UUID.randomUUID(), leave = UUID.randomUUID(), coach = UUID.randomUUID(), session = UUID.randomUUID();

	final Instant now = Instant.parse("2030-11-08T01:00:00Z"), start = now.plusSeconds(172800), end = start.plusSeconds(3600);

	final CoachLeaveService leaves = mock(CoachLeaveService.class);

	final JdbcTemplate db = mock(JdbcTemplate.class);

	final CoachLeaveImpactService service = new CoachLeaveImpactService(leaves, db, Clock.fixed(now, ZoneOffset.UTC));

	CoachLeaveResponse request(List<UUID> ids, CoachLeaveStatus status) {
		return new CoachLeaveResponse(leave, coach, "Coach", "0900000000", start.minusSeconds(3600), end.plusSeconds(3600), "Leave", status, null, null, null, now, ids);
	}

	@SuppressWarnings("unchecked")
	void row(String type, String status, Instant starts) throws Exception {
		Map<String, Object> values = new HashMap<>();
		values.put("id", session);
		values.put("type", type);
		values.put("name", "Aerobic");
		values.put("status", status);
		values.put("start_time", Timestamp.from(starts));
		values.put("end_time", Timestamp.from(starts.plusSeconds(3600)));
		values.put("coach_id", coach);
		values.put("coach_name", "Coach");
		values.put("room_name", "F1-020");
		values.put("run_status", "CONFIRMED");
		values.put("version", 2L);
		values.put("member_count", 3L);
		values.put("capacity", 15);
		if (type.equals("COURSE")) {
			values.put("course_run_id", UUID.randomUUID());
			values.put("run_code", "AERO-01");
			values.put("unit_number", 1);
		}
		ResultSet rs = mock(ResultSet.class, call -> {
			String method = call.getMethod().getName();
			if (call.getArguments().length > 0 && call.getArgument(0) instanceof String key) {
				Object value = values.get(key);
				if (method.equals("getString"))
					return value == null ? null : value.toString();
				if (method.equals("getLong"))
					return value == null ? 0L : ((Number) value).longValue();
				if (method.equals("getInt"))
					return value == null ? 0 : ((Number) value).intValue();
				if (method.equals("getBoolean"))
					return Boolean.TRUE.equals(value);
				if (method.equals("getObject") || method.equals("getTimestamp"))
					return value;
			}
			return RETURNS_DEFAULTS.answer(call);
		});
		when(db.query(anyString(), any(RowMapper.class), any(Object[].class))).thenAnswer(call -> List.of(((RowMapper<?>) call.getArgument(1)).mapRow(rs, 0)));
	}

	@Test
	void emptyImpactUsesAuthoritativeApprovalIdsWithoutQueryingAllSchedules() {
		when(leaves.managerDetail(manager, leave)).thenReturn(request(List.of(), CoachLeaveStatus.PENDING));
		assertThat(service.detail(manager, leave).sessions()).isEmpty();
		verifyNoInteractions(db);
	}

	@Test
	void classDetailsRetainIdentityAndActionPermissions() throws Exception {
		when(leaves.managerDetail(manager, leave)).thenReturn(request(List.of(session), CoachLeaveStatus.PENDING));
		row("CLASS", "SCHEDULED", start);
		var result = service.detail(manager, leave);
		assertThat(result.sessions()).hasSize(1);
		var item = result.sessions().getFirst();
		assertThat(item.id()).isEqualTo(session);
		assertThat(item.type()).isEqualTo("CLASS");
		assertThat(item.memberCount()).isEqualTo(3);
		assertThat(item.canReassign()).isTrue();
		assertThat(item.canReschedule()).isTrue();
		verify(db).query(contains("order by start_time,id,type"), any(RowMapper.class), eq(session), eq(session));
	}

	@Test
	void courseDetailsIncludeRunUnitAndUseMakeupRules() throws Exception {
		when(leaves.managerDetail(manager, leave)).thenReturn(request(List.of(session), CoachLeaveStatus.PENDING));
		row("COURSE", "SCHEDULED", start);
		var item = service.detail(manager, leave).sessions().getFirst();
		assertThat(item.runCode()).isEqualTo("AERO-01");
		assertThat(item.unitNumber()).isEqualTo(1);
		assertThat(item.courseRunId()).isNotNull();
		assertThat(item.canCancel()).isFalse();
	}

	@Test
	void rescheduleCutoffDoesNotHideTheAffectedSession() throws Exception {
		when(leaves.managerDetail(manager, leave)).thenReturn(request(List.of(session), CoachLeaveStatus.PENDING));
		row("PT", "SCHEDULED", now.plusSeconds(3600));
		var item = service.detail(manager, leave).sessions().getFirst();
		assertThat(item.canReschedule()).isFalse();
		assertThat(item.canReassign()).isTrue();
		assertThat(item.canCancel()).isFalse();
	}

	@Test
	void expiredRequestRetainsContextButDisablesAllActions() throws Exception {
		when(leaves.managerDetail(manager, leave)).thenReturn(request(List.of(session), CoachLeaveStatus.EXPIRED));
		row("CLASS", "SCHEDULED", start);
		var item = service.detail(manager, leave).sessions().getFirst();
		assertThat(item.canReassign()).isFalse();
		assertThat(item.canReschedule()).isFalse();
		assertThat(item.canCancel()).isFalse();
	}

	@Test
	void candidatesRejectSessionsOutsideTheExactRequestAndType() throws Exception {
		when(leaves.managerDetail(manager, leave)).thenReturn(request(List.of(session), CoachLeaveStatus.PENDING));
		row("CLASS", "SCHEDULED", start);
		assertThatThrownBy(() -> service.available(manager, leave, "COURSE", session)).isInstanceOf(CoachLeaveException.class);
		assertThatThrownBy(() -> service.available(manager, leave, "CLASS", UUID.randomUUID())).isInstanceOf(CoachLeaveException.class);
	}
}
