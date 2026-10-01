-- BR-PAY-10: the 24-hour payment window starts only after a
-- bank-transfer Payment/QR request is created successfully.
alter table membership_orders
    alter column expires_at drop not null;
