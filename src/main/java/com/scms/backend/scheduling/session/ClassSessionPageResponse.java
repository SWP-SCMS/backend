package com.scms.backend.scheduling.session;

import java.util.List;

public record ClassSessionPageResponse(List<ClassSessionResponse> content, int page, int size, long totalElements, int totalPages) {
}
