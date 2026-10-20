package com.scms.backend.emailverification;

import com.scms.backend.account.AccountStatus;
public record VerifyEmailResponse(boolean verified, AccountStatus status) { }
