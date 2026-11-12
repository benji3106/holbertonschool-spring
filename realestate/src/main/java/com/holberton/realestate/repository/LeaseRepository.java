package com.holberton.realestate.repository;

import com.holberton.realestate.model.Lease;
import com.holberton.realestate.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseRepository extends JpaRepository<Lease, Long> {

    boolean existsByPropertyAndActiveTrue(Property property);
}