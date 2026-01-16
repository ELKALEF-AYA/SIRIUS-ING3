package com.jsahome.quittance.repository;

import com.jsahome.quittance.model.Logement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogementRepository extends JpaRepository<Logement, Long> {
}
