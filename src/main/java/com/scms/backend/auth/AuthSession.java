package com.scms.backend.auth;

record AuthSession(AuthResponse response, String refreshToken) {
}
