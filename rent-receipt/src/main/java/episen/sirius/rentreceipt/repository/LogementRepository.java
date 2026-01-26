package episen.sirius.rentreceipt.repository;

import episen.sirius.rentreceipt.model.Logement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogementRepository extends JpaRepository<Logement, Long> {
}
