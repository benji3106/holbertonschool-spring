package com.holberton.realestate.repository;

import com.holberton.realestate.model.Property;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    Optional<Property> findByReference(String reference);

    boolean existsByReference(String reference);

    List<Property> findByAddressCityIgnoreCase(String city, Sort sort);

    List<Property> findByMonthlyRentLessThanEqual(int maxRent, Sort sort);

    List<Property> findByAddressCityIgnoreCaseAndMonthlyRentLessThanEqual(String city, int maxRent,
                                                                          Sort sort);

    List<Property> findByOwnerIdOrderByReference(Long ownerId);

    boolean existsByOwnerId(Long ownerId);
}