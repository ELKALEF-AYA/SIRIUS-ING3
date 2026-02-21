package episen.sirius.rentreceipt.controller;

import episen.sirius.rentreceipt.model.Locataire;
import episen.sirius.rentreceipt.repository.LocataireRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/locataires")
@CrossOrigin(origins = "http://localhost:5173")
public class LocataireController {

    private final LocataireRepository locataireRepository;

    public LocataireController(LocataireRepository locataireRepository) {
        this.locataireRepository = locataireRepository;
    }

    @GetMapping
    public List<Locataire> getAllLocataires() {
        return locataireRepository.findAll();
    }
}
