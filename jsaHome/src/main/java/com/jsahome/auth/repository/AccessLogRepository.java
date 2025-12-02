package com.jsahome.auth.repository;

import com.jsahome.auth.model.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    List<AccessLog> findByAccessGrantedTrue();
    List<AccessLog> findByAccessGrantedFalse();
}