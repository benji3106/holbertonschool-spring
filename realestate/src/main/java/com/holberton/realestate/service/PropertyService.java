package com.holberton.realestate.service;

import com.holberton.realestate.dto.PropertyMapper;
import com.holberton.realestate.dto.PropertyRequest;
import com.holberton.realestate.dto.PropertyResponse;
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

    public List<PropertyResponse> findAll() {
        return toResponses(propertyRepository.findAll(BY_REFERENCE));
    }

    /** Both parameters are optional: null means "no filter". */
    public List<PropertyResponse> search(String city, Integer maxRent) {
        if (city != null && maxRent != null) {
            return toResponses(propertyRepository
                    .findByAddressCityIgnoreCaseAndMonthlyRentLessThanEqual(city, maxRent, BY_REFERENCE));
        }
        if (city != null) {
            return toResponses(propertyRepository.findByAddressCityIgnoreCase(city, BY_REFERENCE));
        }
        if (maxRent != null) {
            return toResponses(propertyRepository.findByMonthlyRentLessThanEqual(maxRent, BY_REFERENCE));
        }
        return findAll();
    }

    public Optional<PropertyResponse> findByReference(String reference) {
        return propertyRepository.findByReference(reference).map(PropertyMapper::toResponse);
    }

    /** Returns an empty Optional if the reference is already used. */
    public Optional<PropertyResponse> create(PropertyRequest request) {
        if (propertyRepository.existsByReference(request.reference())) {
            return Optional.empty();
        }
        Property saved = propertyRepository.save(PropertyMapper.toEntity(request));
        return Optional.of(PropertyMapper.toResponse(saved));
    }

    /** Returns an empty Optional if no property has this reference. */
    public Optional<PropertyResponse> update(String reference, PropertyRequest request) {
        Optional<Property> existing = propertyRepository.findByReference(reference);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        Property property = existing.get();
        property.setAddress(request.address());
        property.setSurface(request.surface());
        property.setMonthlyRent(request.monthlyRent());
        property.setEnergyClass(request.energyClass());
        return Optional.of(PropertyMapper.toResponse(propertyRepository.save(property)));
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

    private List<PropertyResponse> toResponses(List<Property> properties) {
        return properties.stream().map(PropertyMapper::toResponse).toList();
    }
}