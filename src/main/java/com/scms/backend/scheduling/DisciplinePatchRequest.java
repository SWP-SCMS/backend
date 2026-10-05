package com.scms.backend.scheduling;

public record DisciplinePatchRequest(String name, String description, DisciplineStatus status) {
}
