package com.holberton.realestate.service;

import com.holberton.realestate.dto.TenantMapper;
import com.holberton.realestate.dto.TenantRequest;
import com.holberton.realestate.dto.TenantResponse;
import com.holberton.realestate.exception.DuplicateEmailException;
import com.holberton.realestate.exception.TenantNotFoundException;
import com.holberton.realestate.model.Tenant;
import com.holberton.realestate.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public TenantResponse findById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        return TenantMapper.toResponse(tenant);
    }

    @Transactional
    public TenantResponse create(TenantRequest request) {
        if (tenantRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        Tenant saved = tenantRepository.save(TenantMapper.toEntity(request));
        return TenantMapper.toResponse(saved);
    }
}