package com.scms.backend.scheduling.session;

import java.util.List;

public record MemberClassSessionPageResponse(List<MemberClassSessionResponse> content, int page, int size, long totalElements, int totalPages) {
}
