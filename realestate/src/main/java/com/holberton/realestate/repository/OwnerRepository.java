package com.holberton.realestate.repository;

import com.holberton.realestate.model.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerRepository extends JpaRepository<Owner, Long> {

    boolean existsByEmail(String email);
}