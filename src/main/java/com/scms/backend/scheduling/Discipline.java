package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "disciplines")
public class Discipline {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DisciplineStatus status;

	@ElementCollection
	@CollectionTable(name = "discipline_required_benefits",
		joinColumns = @JoinColumn(name = "discipline_id"))
	@Column(name = "benefit_code", nullable = false, length = 50)
	private Set<String> requiredBenefitCodes = new HashSet<>();

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	protected Discipline() {
	}

	Discipline(UUID id, String name, String description, DisciplineStatus status) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.status = status;
	}

	void update(String name, String description, DisciplineStatus status) {
		this.name = name;
		this.description = description;
		this.status = status;
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	String getDescription() {
		return description;
	}

	public DisciplineStatus getStatus() {
		return status;
	}

	List<String> getRequiredBenefitCodes() {
		return requiredBenefitCodes.stream().sorted().toList();
	}

	List<String> getEffectiveRequiredBenefitCodes() {
		return java.util.stream.Stream.concat(java.util.stream.Stream.of("CLASS_BOOKING"),
			requiredBenefitCodes.stream()).distinct().sorted().toList();
	}

	void replaceRequiredBenefitCodes(Collection<String> benefitCodes) {
		requiredBenefitCodes.clear();
		requiredBenefitCodes.addAll(benefitCodes);
	}
}
