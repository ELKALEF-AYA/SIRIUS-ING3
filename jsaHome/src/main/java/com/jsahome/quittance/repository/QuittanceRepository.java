package com.jsahome.quittance.repository;

import com.jsahome.quittance.model.Quittance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuittanceRepository extends JpaRepository<Quittance, Long> {
}
