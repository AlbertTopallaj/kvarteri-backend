package se.kvarteri.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import se.kvarteri.models.Resident;
import se.kvarteri.models.Warning;

import java.util.List;
import java.util.UUID;

public interface WarningRepository extends JpaRepository<Warning, UUID> {

    @Query("select w from Warning w join fetch w.issuedBy where w.resident = ?1 order by w.createdAt desc")
    List<Warning> findByResident(Resident resident);

    @Modifying
    @Query("delete from Warning w where w.resident = ?1")
    void deleteByResident(Resident resident);
}
