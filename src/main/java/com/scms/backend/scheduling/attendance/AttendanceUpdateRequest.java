package com.scms.backend.scheduling.attendance;

import com.scms.backend.scheduling.AttendanceStatus;

public record AttendanceUpdateRequest(AttendanceStatus status) {
}
