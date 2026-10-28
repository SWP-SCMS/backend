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

## Incident handling

- **SMTP authentication failure:** verify the Gmail mailbox still permits SMTP and that `SMTP_USERNAME` is the mailbox address. Rotate the Google App Password if needed, update only the Railway secret, and redeploy. Never use the Google account password or place the App Password in a ticket or log.
- **Quota or provider throttling:** leave retry scheduling to the outbox (`30s`, `120s`, then `300s`). Check the daily sent count and provider quota. Do not repeatedly resend OTPs manually; expired delivery windows become terminal and the user can request a fresh OTP.
- **Retry exhaustion:** inspect the aggregate failure counts and provider-side status, correct the configuration/outage, then use the supported resend flow to enqueue a new OTP. Do not reset outbox state directly in SQL.
- **Manual activation:** use the existing Receptionist request and Manager approval workflow only after confirming the user through the documented operational process. Record the reason/evidence through that workflow; never activate an account by directly changing its database status.
