package com.scms.backend.scheduling;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RoomBulkResolutionRequest(
		@NotBlank @Schema(allowableValues = {"MOVE", "CANCEL"}) String action,
		UUID targetRoomId, String reason) {
}
