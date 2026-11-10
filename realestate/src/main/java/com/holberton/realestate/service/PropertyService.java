package com.holberton.realestate.service;

import com.holberton.realestate.dto.PropertyRequest;
import com.holberton.realestate.model.Property;
import com.holberton.realestate.repository.PropertyRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PropertyService {

    private static final Sort BY_REFERENCE = Sort.by("reference");

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    public List<Property> findAll() {
        return propertyRepository.findAll(BY_REFERENCE);
    }

    /** Both parameters are optional: null means "no filter". */
    public List<Property> search(String city, Integer maxRent) {
        if (city != null && maxRent != null) {
            return propertyRepository.findByAddressCityIgnoreCaseAndMonthlyRentLessThanEqual(
                    city, maxRent, BY_REFERENCE);
        }
        if (city != null) {
            return propertyRepository.findByAddressCityIgnoreCase(city, BY_REFERENCE);
        }
        if (maxRent != null) {
            return propertyRepository.findByMonthlyRentLessThanEqual(maxRent, BY_REFERENCE);
        }
        return findAll();
    }

    public Optional<Property> findByReference(String reference) {
        return propertyRepository.findByReference(reference);
    }

    /** Returns an empty Optional if the reference is already used. */
    public Optional<Property> create(PropertyRequest request) {
        if (propertyRepository.existsByReference(request.reference())) {
            return Optional.empty();
        }
        Property property = new Property(request.reference(), request.address(),
                request.surface(), request.monthlyRent(), request.energyClass());
        return Optional.of(propertyRepository.save(property));
    }

    /** Returns an empty Optional if no property has this reference. */
    public Optional<Property> update(String reference, PropertyRequest request) {
        Optional<Property> existing = propertyRepository.findByReference(reference);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        Property property = existing.get();
        property.setAddress(request.address());
        property.setSurface(request.surface());
        property.setMonthlyRent(request.monthlyRent());
        property.setEnergyClass(request.energyClass());
        return Optional.of(propertyRepository.save(property));
    }

    /** Returns false if no property has this reference. */
    public boolean delete(String reference) {
        Optional<Property> existing = propertyRepository.findByReference(reference);
        if (existing.isEmpty()) {
            return false;
        }
        propertyRepository.delete(existing.get());
        return true;
    }
}