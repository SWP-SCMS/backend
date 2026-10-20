package com.scms.backend.auth;

record RegistrationOutcome(RegistrationResponse response, String pendingSessionToken) { }
