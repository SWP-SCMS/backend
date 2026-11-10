package com.scms.backend.course.run;

import com.scms.backend.course.CourseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Calendar calculation only; resource availability is checked again when saving.
 */
final class CourseWeeklySchedule {

	static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

	private CourseWeeklySchedule() {
	}

	static CourseWeeklyPreview generate(CourseWeeklyScheduleRequest request) {
		if (request == null || request.openingDate() == null || request.startTime() == null || request.endTime() == null)
			throw CourseException.validation("schedule", "Vui lòng nhập ngày khai giảng và giờ học.");
		if (request.totalUnits() < 1 || request.totalUnits() > 366)
			throw CourseException.validation("totalUnits", "Số buổi phải từ 1 đến 366.");
		if (!request.startTime().isBefore(request.endTime()))
			throw CourseException.validation("endTime", "Giờ kết thúc phải sau giờ bắt đầu trong cùng ngày.");
		var days = request.weekdays();
		if (days == null || days.isEmpty() || days.size() > 7 || days.stream().anyMatch(d -> d == null || d < 1 || d > 7) || new HashSet<>(days).size() != days.size())
			throw CourseException.validation("weekdays", "Chọn các thứ không trùng, từ 1 (thứ Hai) đến 7 (Chủ nhật).");
		if (!days.contains(request.openingDate().getDayOfWeek().getValue()))
			throw CourseException.validation("openingDate", "Ngày khai giảng phải thuộc một trong các thứ học đã chọn.");
		// Restrict to a calendar range supported consistently by JSON, PostgreSQL and clients.
		if (request.openingDate().getYear() < 2000 || request.openingDate().getYear() > 9990)
			throw CourseException.validation("openingDate", "Ngày khai giảng nằm ngoài khoảng hỗ trợ.");
		List<CourseWeeklySlot> slots = new ArrayList<>();
		LocalDate date = request.openingDate();
		while (slots.size() < request.totalUnits()) {
			if (days.contains(date.getDayOfWeek().getValue())) {
				slots.add(new CourseWeeklySlot(slots.size() + 1, date.atTime(request.startTime()).atZone(ZONE).toInstant(), date.atTime(request.endTime()).atZone(ZONE).toInstant()));
			}
			date = date.plusDays(1);
		}
		return new CourseWeeklyPreview(ZONE.getId(), slots.getFirst().startAt(), slots.getLast().endAt(), List.copyOf(slots));
	}
}
