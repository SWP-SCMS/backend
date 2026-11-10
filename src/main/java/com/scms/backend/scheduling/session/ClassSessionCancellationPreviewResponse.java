package com.scms.backend.scheduling.session;

import java.util.List;
import java.util.UUID;

public record ClassSessionCancellationPreviewResponse(int count, List<UUID> sessionIds) {
}
