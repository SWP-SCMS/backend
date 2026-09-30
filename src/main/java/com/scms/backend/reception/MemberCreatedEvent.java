package com.scms.backend.reception;

import java.util.UUID;

public record MemberCreatedEvent(UUID accountId, String memberId) {
}
