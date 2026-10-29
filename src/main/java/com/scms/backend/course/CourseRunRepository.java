package com.scms.backend.course;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRunRepository extends JpaRepository<CourseRun, UUID> {
}
