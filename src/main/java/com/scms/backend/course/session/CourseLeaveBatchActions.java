package com.scms.backend.course.session;

import com.scms.backend.course.management.CourseManagementService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseLeaveBatchActions {

	private final CourseManagementService management;

	private final CourseSessionDisruptionService disruptions;

	public CourseLeaveBatchActions(CourseManagementService management, CourseSessionDisruptionService disruptions) {
		this.management = management;
		this.disruptions = disruptions;
	}

	@Transactional
	public void reassign(UUID manager, UUID run, UUID session, long version, UUID room, UUID coach, Instant start, Instant end) {
		management.patchSession(manager, run, session, new CourseSessionPatchRequest(version, room, coach, start, end));
	}

	@Transactional
	public void cancel(UUID manager, UUID run, UUID session, long version) {
		disruptions.cancelFromEndpoint(manager, run, session, version);
	}
}
