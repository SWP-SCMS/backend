package com.scms.backend.emailverification.verification;

import com.scms.backend.account.AccountStatus;

public record VerifyEmailResponse(boolean verified, AccountStatus status) {
}
