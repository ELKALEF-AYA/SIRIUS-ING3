package episen.sirius.rentreceipt.repository;

import episen.sirius.rentreceipt.model.Locataire;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocataireRepository extends JpaRepository<Locataire, Long> {
}
