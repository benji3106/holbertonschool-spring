package com.holberton.realestate.service;

import com.holberton.realestate.dto.LeaseMapper;
import com.holberton.realestate.dto.LeaseRequest;
import com.holberton.realestate.dto.LeaseResponse;
import com.holberton.realestate.exception.InsufficientIncomeException;
import com.holberton.realestate.exception.LeaseAlreadyEndedException;
import com.holberton.realestate.exception.LeaseNotFoundException;
import com.holberton.realestate.exception.PropertyNotFoundException;
import com.holberton.realestate.exception.PropertyUnavailableException;
import com.holberton.realestate.exception.TenantNotFoundException;
import com.holberton.realestate.model.Lease;
import com.holberton.realestate.model.Property;
import com.holberton.realestate.model.Tenant;
import com.holberton.realestate.repository.LeaseRepository;
import com.holberton.realestate.repository.PropertyRepository;
import com.holberton.realestate.repository.TenantRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LeaseService {

    private static final Sort BY_ID = Sort.by("id");

    private final LeaseRepository leaseRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;

    public LeaseService(LeaseRepository leaseRepository, PropertyRepository propertyRepository,
                        TenantRepository tenantRepository) {
        this.leaseRepository = leaseRepository;
        this.propertyRepository = propertyRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<LeaseResponse> findAll() {
        return leaseRepository.findAll(BY_ID).stream()
                .map(LeaseMapper::toResponse)
                .toList();
    }

    public LeaseResponse findById(Long id) {
        return LeaseMapper.toResponse(findEntity(id));
    }

    /** Checks the rules in order: the first broken rule decides the error. */
    @Transactional
    public LeaseResponse sign(LeaseRequest request) {
        // rule 2: the property exists
        Property property = propertyRepository.findByReference(request.propertyReference())
                .orElseThrow(() -> new PropertyNotFoundException(request.propertyReference()));

        // rule 3: the tenant exists
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(request.tenantId()));

        // rule 4: the property is available
        if (property.isRented()) {
            throw new PropertyUnavailableException(property.getReference());
        }

        // rule 5: the tenant earns enough
        int requiredIncome = property.getMonthlyRent() * Lease.INCOME_TO_RENT_RATIO;
        if (tenant.getMonthlyIncome() < requiredIncome) {
            throw new InsufficientIncomeException(tenant.getMonthlyIncome(), requiredIncome);
        }

        // rule 6: everything is fine, two rows change in the same transaction
        Lease lease = leaseRepository.save(new Lease(property, tenant,
                request.durationMonths(), property.getMonthlyRent()));
        property.setRented(true);
        return LeaseMapper.toResponse(lease);
    }

    @Transactional
    public LeaseResponse end(Long id) {
        Lease lease = findEntity(id);
        if (!lease.isActive()) {
            throw new LeaseAlreadyEndedException(id);
        }
        lease.end();
        lease.getProperty().setRented(false);
        return LeaseMapper.toResponse(lease);
    }

    private Lease findEntity(Long id) {
        return leaseRepository.findById(id)
                .orElseThrow(() -> new LeaseNotFoundException(id));
    }
}