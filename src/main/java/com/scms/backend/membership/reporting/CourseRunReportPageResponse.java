package com.scms.backend.membership.reporting;

import java.util.List;

record CourseRunReportPageResponse(List<CourseRunReportItem> content, int page, int size, long totalElements, int totalPages) {
}
