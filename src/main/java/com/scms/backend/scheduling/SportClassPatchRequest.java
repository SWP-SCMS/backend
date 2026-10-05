package com.scms.backend.scheduling;

public record SportClassPatchRequest(String name, SportClassType classType, String description,
		SportClassStatus status) {
}
