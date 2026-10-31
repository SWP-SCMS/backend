alter table course_refund_obligations
    drop constraint chk_course_refund_obligations_source;

alter table course_refund_obligations
    add constraint chk_course_refund_obligations_source
    check (source_type in ('UNRESOLVED_SESSIONS', 'COURSE_RUN_CANCELLED'));
