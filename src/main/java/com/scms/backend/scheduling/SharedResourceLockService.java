package com.scms.backend.scheduling;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharedResourceLockService {
	private final AccountRepository accounts;
	private final RoomRepository rooms;
	private final SharedResourceLockRendezvous rendezvous;

	SharedResourceLockService(AccountRepository accounts, RoomRepository rooms, SharedResourceLockRendezvous rendezvous) {
		this.accounts = accounts;
		this.rooms = rooms;
		this.rendezvous = rendezvous;
	}

	@Transactional
	public void lock(Collection<UUID> coachIds, Collection<UUID> roomIds) {
		rendezvous.beforeLock();
		Set<UUID> ids = new TreeSet<>(Comparator.comparing(UUID::toString));
		if (coachIds != null) ids.addAll(coachIds.stream().filter(java.util.Objects::nonNull).toList());
		if (roomIds != null) ids.addAll(roomIds.stream().filter(java.util.Objects::nonNull).toList());
		for (UUID id : ids) {
			if (coachIds != null && coachIds.contains(id)) accounts.findByIdForUpdate(id);
			if (roomIds != null && roomIds.contains(id)) rooms.findByIdForUpdate(id);
		}
		rendezvous.afterLock();
	}
}
