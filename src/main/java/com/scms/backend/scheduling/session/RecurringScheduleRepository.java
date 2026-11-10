package com.scms.backend.scheduling.session;

import com.scms.backend.scheduling.RecurringSchedule;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringScheduleRepository extends JpaRepository<RecurringSchedule, UUID> {
}
