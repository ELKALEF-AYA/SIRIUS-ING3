package com.jsahome.quittance.repository;

import com.jsahome.quittance.model.Locataire;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocataireRepository extends JpaRepository<Locataire, Long> {
}
