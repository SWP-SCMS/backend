package com.scms.backend.scheduling.session;

import java.util.List;

public record CoachClassSessionPageResponse(List<CoachClassSessionResponse> content, int page, int size, long totalElements, int totalPages) {
}
