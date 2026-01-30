package episen.sirius.rentreceipt.repository;

import episen.sirius.rentreceipt.model.RentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentReceiptRepository extends JpaRepository<RentReceipt, Long> {
}
