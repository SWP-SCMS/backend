package com.scms.backend.scheduling;

import java.util.List;

import org.springframework.http.HttpStatus;

public class PtSessionAssignmentException extends RuntimeException {

	private final List<PtSessionAffectedBooking> affectedBookings;
	private final List<PtSessionAffectedWaitlist> affectedWaitlists;

	private PtSessionAssignmentException(List<PtSessionAffectedBooking> affectedBookings,
			List<PtSessionAffectedWaitlist> affectedWaitlists) {
		super("The selected Coach is not assigned to every Member booked or waitlisted for this PT Session.");
		this.affectedBookings = List.copyOf(affectedBookings);
		this.affectedWaitlists = List.copyOf(affectedWaitlists);
	}

	static PtSessionAssignmentException incompatibleMembers(List<PtSessionAffectedBooking> affectedBookings,
			List<PtSessionAffectedWaitlist> affectedWaitlists) {
		return new PtSessionAssignmentException(affectedBookings, affectedWaitlists);
	}

	public HttpStatus getStatus() { return HttpStatus.CONFLICT; }
	public String getCode() { return "PT_SESSION_ASSIGNMENT_HAS_INCOMPATIBLE_MEMBERS"; }
	public List<PtSessionAffectedBooking> getAffectedBookings() { return affectedBookings; }
	public List<PtSessionAffectedWaitlist> getAffectedWaitlists() { return affectedWaitlists; }
}
