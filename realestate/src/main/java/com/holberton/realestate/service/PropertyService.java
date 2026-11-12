package com.holberton.realestate.service;

import com.holberton.realestate.dto.PropertyMapper;
import com.holberton.realestate.dto.PropertyRequest;
import com.holberton.realestate.dto.PropertyResponse;
import com.holberton.realestate.exception.DuplicateReferenceException;
import com.holberton.realestate.exception.OwnerNotFoundException;
import com.holberton.realestate.exception.PropertyHasLeasesException;
import com.holberton.realestate.exception.PropertyNotFoundException;
import com.holberton.realestate.model.Owner;
import com.holberton.realestate.model.Property;
import com.holberton.realestate.repository.LeaseRepository;
import com.holberton.realestate.repository.OwnerRepository;
import com.holberton.realestate.repository.PropertyRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PropertyService {

    private static final Sort BY_REFERENCE = Sort.by("reference");

    private final PropertyRepository propertyRepository;
    private final OwnerRepository ownerRepository;
    private final LeaseRepository leaseRepository;

    public PropertyService(PropertyRepository propertyRepository,
                           OwnerRepository ownerRepository,
                           LeaseRepository leaseRepository) {
        this.propertyRepository = propertyRepository;
        this.ownerRepository = ownerRepository;
        this.leaseRepository = leaseRepository;
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

    public PropertyResponse findByReference(String reference) {
        return PropertyMapper.toResponse(findEntity(reference));
    }

    @Transactional
    public PropertyResponse create(PropertyRequest request) {
        if (propertyRepository.existsByReference(request.reference())) {
            throw new DuplicateReferenceException(request.reference());
        }
        Property saved = propertyRepository.save(PropertyMapper.toEntity(request));
        return PropertyMapper.toResponse(saved);
    }

    @Transactional
    public PropertyResponse update(String reference, PropertyRequest request) {
        Property property = findEntity(reference);
        property.setAddress(request.address());
        property.setSurface(request.surface());
        property.setMonthlyRent(request.monthlyRent());
        property.setEnergyClass(request.energyClass());
        return PropertyMapper.toResponse(propertyRepository.save(property));
    }

    @Transactional
    public void delete(String reference) {
        Property property = findEntity(reference);
        if (leaseRepository.existsByProperty(property)) {
            throw new PropertyHasLeasesException(reference);
        }
        propertyRepository.delete(property);
    }

    @Transactional
    public PropertyResponse assignOwner(String reference, Long ownerId) {
        Property property = findEntity(reference);
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new OwnerNotFoundException(ownerId));
        property.setOwner(owner);
        return PropertyMapper.toResponse(propertyRepository.save(property));
    }

    private Property findEntity(String reference) {
        return propertyRepository.findByReference(reference)
                .orElseThrow(() -> new PropertyNotFoundException(reference));
    }

    private List<PropertyResponse> toResponses(List<Property> properties) {
        return properties.stream().map(PropertyMapper::toResponse).toList();
    }
}