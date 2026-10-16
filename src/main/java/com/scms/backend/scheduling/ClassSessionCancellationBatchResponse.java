package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

public record ClassSessionCancellationBatchResponse(int cancelledCount, List<UUID> sessionIds) {
}
