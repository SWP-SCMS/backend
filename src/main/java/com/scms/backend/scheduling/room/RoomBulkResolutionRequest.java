package com.scms.backend.scheduling.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record RoomBulkResolutionRequest(@NotBlank @Schema(allowableValues = { "MOVE", "CANCEL" }) String action, UUID targetRoomId, String reason) {
}
