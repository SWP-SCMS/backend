package com.scms.backend.scheduling;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringScheduleRepository extends JpaRepository<RecurringSchedule, UUID> {
}
