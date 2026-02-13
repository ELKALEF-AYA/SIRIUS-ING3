package episen.sirius.rentreceipt.repository;
import java.util.List;


import episen.sirius.rentreceipt.model.RentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentReceiptRepository extends JpaRepository<RentReceipt, Long> {
    List<RentReceipt> findByLocataire_Id(Long locataireId);
}

