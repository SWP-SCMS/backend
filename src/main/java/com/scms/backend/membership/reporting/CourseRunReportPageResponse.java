package com.scms.backend.membership.reporting;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CourseRunReportPageResponse(List<CourseRunReportItem> content, int page, int size, long totalElements, int totalPages) {
}
