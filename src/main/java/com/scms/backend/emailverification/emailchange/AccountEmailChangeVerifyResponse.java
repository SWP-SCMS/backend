package com.scms.backend.emailverification.emailchange;

public record AccountEmailChangeVerifyResponse(boolean verified, String email) {
}
