package com.scms.backend.scheduling.sportclass;

import com.scms.backend.scheduling.SportClassType;
import java.util.UUID;

public record SportClassCreateRequest(UUID disciplineId, String name, SportClassType classType, String description) {
}
