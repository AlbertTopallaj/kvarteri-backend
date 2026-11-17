package se.kvarteri.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.kvarteri.exception.ConflictException;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Role;
import se.kvarteri.repositories.*;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ResidentService {

    private final ResidentRepository residents;
    private final ParkingSpotRepository spots;
    private final SpotQueueEntryRepository queue;
    private final BookingRepository bookings;
    private final NotificationRepository notifications;
    private final PasswordEncoder encoder;

    /**
     * Removes an account: frees rented spots, drops queue entries, bookings and notifications, and anonymizes
     * the row. The row is kept (tickets and messages reference it) but can no longer log in and the email is freed.
     */
    public void deactivate(Resident r) {
        if (r.getRole() == Role.ADMIN && residents.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new ConflictException("Cannot remove the last administrator");
        }
        spots.findByTenant(r).forEach(s -> s.setTenant(null));
        queue.deleteByResident(r);
        bookings.deleteByResident(r);
        notifications.deleteByRecipient(r);
        r.setEnabled(false);
        r.setName("Borttagen användare");
        r.setApartmentNumber(null);
        r.setEmail("deleted-" + r.getId() + "@deleted.invalid");
        r.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
        residents.save(r);
    }
}
