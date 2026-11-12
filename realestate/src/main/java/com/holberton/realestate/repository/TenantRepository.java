package com.holberton.realestate.repository;

import com.holberton.realestate.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    boolean existsByEmail(String email);
}