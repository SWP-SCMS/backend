package com.scms.backend.scheduling.sportclass;

import com.scms.backend.scheduling.SportClassStatus;
import com.scms.backend.scheduling.SportClassType;

public record SportClassPatchRequest(String name, SportClassType classType, String description, SportClassStatus status) {
}
