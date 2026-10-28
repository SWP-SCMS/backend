# Railway Email OTP Operations

## Configuration

- Set `SPRING_PROFILES_ACTIVE=prod` in production and `SPRING_PROFILES_ACTIVE=staging` in staging.
- Set `TRUST_RAILWAY_PROXY_HEADERS=true` only when Railway is the trusted ingress proxy. The backend uses only `X-Real-IP` in that mode; `X-Forwarded-For` is never consulted. Otherwise IP rate limits use the servlet remote address.
- Set `SMTP_FROM` to the same mailbox as `SMTP_USERNAME`. Set `SMTP_ALLOW_CUSTOM_FROM=true` only when the SMTP provider explicitly permits a different sender.
- Staging must set `SMTP_RECIPIENT_OVERRIDE` to the approved test mailbox. Every newly queued OTP email is redirected there; production rejects a configured override and never applies the staging redirect.
- Keep SMTP credentials and OTP keys in Railway's secret configuration. Do not print secret values in logs or incident records.

## Monitor

Every five minutes, the application emits one aggregate `email_outbox_health` event with counts only; it contains no recipient, account, OTP, or payload data.

- `WARN`: any `FAILED` job, `PENDING` older than 2 minutes, or `PROCESSING` locked longer than 5 minutes.
- `ERROR`: any `TERMINAL_FAILED` updated in the last 24 hours, any active (`PENDING`, `FAILED`, or `PROCESSING`) job created more than 10 minutes ago, or 40 or more `SENT` jobs in the last 24 hours.

On an alert, inspect Railway application logs and SMTP provider status. Check SMTP configuration without exposing its values, then use staging and the recipient override to verify delivery before production changes. Do not edit or delete outbox rows as an operational shortcut; terminal failures are retained for audit/history.
