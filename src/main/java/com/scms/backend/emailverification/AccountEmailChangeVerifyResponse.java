package com.scms.backend.emailverification;

public record AccountEmailChangeVerifyResponse(boolean verified, String email) {
}
