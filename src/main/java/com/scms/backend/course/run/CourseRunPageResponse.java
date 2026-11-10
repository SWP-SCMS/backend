package com.scms.backend.course.run;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.scms.backend.course.CourseRun;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseRunPageResponse(List<CourseRunResponse> content, int page, int size, long totalElements, int totalPages) {

	public static CourseRunPageResponse from(org.springframework.data.domain.Page<CourseRun> page) {
		return new CourseRunPageResponse(page.getContent().stream().map(CourseRunResponse::from).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
