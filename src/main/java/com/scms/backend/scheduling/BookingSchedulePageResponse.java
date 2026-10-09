package com.scms.backend.scheduling;

import java.util.List;

public record BookingSchedulePageResponse(List<BookingScheduleResponse> content, int page, int size,
		long totalElements, int totalPages) {
}
