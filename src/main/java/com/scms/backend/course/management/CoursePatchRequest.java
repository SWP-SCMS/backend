package com.scms.backend.course.management;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.scms.backend.course.CourseFieldLimits;
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

final class CoursePatchRequest {

	@NotNull
	@PositiveOrZero
	@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
	private Long expectedVersion;

	private boolean namePresent;

	@Pattern(regexp = ".*\\S.*", message = "must not be blank")
	@Size(max = 200)
	private String name;

	private boolean descriptionPresent;

	@Size(max = CourseFieldLimits.DESCRIPTION)
	@Schema(types = { "string", "null" })
	private String description;

	private boolean imageUrlPresent;

	@Size(max = CourseFieldLimits.IMAGE_URL)
	@Schema(types = { "string", "null" })
	private String imageUrl;

	private boolean defaultMinimumEnrollmentPresent;

	@Positive
	@Schema(types = { "integer", "null" })
	private Integer defaultMinimumEnrollment;

	@JsonSetter("expectedVersion")
	public void setExpectedVersion(Long expectedVersion) {
		this.expectedVersion = expectedVersion;
	}

	@JsonSetter("name")
	public void setName(String name) {
		this.namePresent = true;
		this.name = name;
	}

	@JsonSetter("description")
	public void setDescription(String description) {
		this.descriptionPresent = true;
		this.description = description;
	}

	@JsonSetter("imageUrl")
	public void setImageUrl(String imageUrl) {
		this.imageUrlPresent = true;
		this.imageUrl = imageUrl;
	}

	@JsonSetter("defaultMinimumEnrollment")
	public void setDefaultMinimumEnrollment(Integer defaultMinimumEnrollment) {
		this.defaultMinimumEnrollmentPresent = true;
		this.defaultMinimumEnrollment = defaultMinimumEnrollment;
	}

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown course patch field: " + fieldName);
	}

	Long expectedVersion() {
		return expectedVersion;
	}

	boolean hasName() {
		return namePresent;
	}

	String name() {
		return name;
	}

	boolean hasDescription() {
		return descriptionPresent;
	}

	String description() {
		return description;
	}

	boolean hasImageUrl() {
		return imageUrlPresent;
	}

	String imageUrl() {
		return imageUrl;
	}

	boolean hasDefaultMinimumEnrollment() {
		return defaultMinimumEnrollmentPresent;
	}

	Integer defaultMinimumEnrollment() {
		return defaultMinimumEnrollment;
	}

	@Schema(maxLength = CourseFieldLimits.DESCRIPTION, types = { "string", "null" })
	public String getDescription() {
		return description;
	}

	@Schema(maxLength = CourseFieldLimits.IMAGE_URL, types = { "string", "null" })
	public String getImageUrl() {
		return imageUrl;
	}

	boolean hasMutableField() {
		return namePresent || descriptionPresent || imageUrlPresent || defaultMinimumEnrollmentPresent;
	}
}
