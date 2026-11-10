package com.scms.backend.course.catalog;

import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.CourseStatus;
import com.scms.backend.course.management.CourseRepository;
import com.scms.backend.course.run.CourseRunRepository;
import com.scms.backend.course.session.CourseSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseCatalogService {

	private final CourseRepository courses;

	private final CourseRunRepository runs;

	private final CourseSessionRepository sessions;

	private final Clock clock;

	CourseCatalogService(CourseRepository courses, CourseRunRepository runs, CourseSessionRepository sessions, Clock clock) {
		this.courses = courses;
		this.runs = runs;
		this.sessions = sessions;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	CatalogCoursePageResponse listCourses(Pageable pageable) {
		return CatalogCoursePageResponse.from(courses.findCatalogByStatus(CourseStatus.PUBLISHED, pageable));
	}

	@Transactional(readOnly = true)
	CatalogCourseResponse getCourse(UUID courseId) {
		return courses.findCatalogByIdAndStatus(courseId, CourseStatus.PUBLISHED).map(CatalogCourseResponse::from).orElseThrow(CourseException::notFound);
	}

	@Transactional(readOnly = true)
	CatalogCourseRunPageResponse listRuns(Pageable pageable) {
		Instant now = clock.instant();
		return CatalogCourseRunPageResponse.from(runs.findCatalogByStatusAndCourseStatus(CourseRunStatus.OPEN_FOR_REGISTRATION, CourseStatus.PUBLISHED, pageable), now);
	}

	@Transactional(readOnly = true)
	CatalogCourseRunDetailResponse getRun(UUID runId) {
		CourseRun run = runs.findCatalogByIdAndStatusAndCourseStatus(runId, CourseRunStatus.OPEN_FOR_REGISTRATION, CourseStatus.PUBLISHED).orElseThrow(CourseException::runNotFound);
		return new CatalogCourseRunDetailResponse(CatalogCourseRunResponse.from(run, clock.instant()), sessions.findByCourseRunIdOrderByStartAtAscIdAsc(runId).stream().map(CatalogCourseSessionResponse::from).toList());
	}
}
