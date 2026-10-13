package com.scms.backend.training;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "training_plan_versions")
public class TrainingPlanVersion {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "training_plan_id", nullable = false, updatable = false)
	private UUID trainingPlanId;

	@Column(name = "version_number", nullable = false, updatable = false)
	private Integer versionNumber;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb", updatable = false)
	private Map<String, Object> content;

	@Column(name = "created_by_account_id", nullable = false, updatable = false)
	private UUID createdByAccountId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected TrainingPlanVersion() {
	}

	public TrainingPlanVersion(UUID id, UUID trainingPlanId, int versionNumber, Map<String, Object> content,
			UUID createdByAccountId, Instant createdAt) {
		this.id = Objects.requireNonNull(id);
		this.trainingPlanId = Objects.requireNonNull(trainingPlanId);
		this.versionNumber = versionNumber;
		this.content = new LinkedHashMap<>(Objects.requireNonNull(content));
		this.createdByAccountId = Objects.requireNonNull(createdByAccountId);
		this.createdAt = Objects.requireNonNull(createdAt);
	}

	public UUID getId() { return id; }
	public UUID getTrainingPlanId() { return trainingPlanId; }
	public Integer getVersionNumber() { return versionNumber; }
	public Map<String, Object> getContent() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(content));
	}
	public UUID getCreatedByAccountId() { return createdByAccountId; }
	public Instant getCreatedAt() { return createdAt; }
}
