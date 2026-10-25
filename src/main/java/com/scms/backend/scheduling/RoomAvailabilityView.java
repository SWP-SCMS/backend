package com.scms.backend.scheduling;

import java.util.UUID;

interface RoomAvailabilityView {

	UUID getRoomId();
	String getName();
	String getUsageMode();
	int getCapacity();
	long getOccupiedCapacity();
	boolean getSuspended();
}
