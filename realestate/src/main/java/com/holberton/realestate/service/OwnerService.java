package com.holberton.realestate.service;

import com.holberton.realestate.dto.OwnerMapper;
import com.holberton.realestate.dto.OwnerRequest;
import com.holberton.realestate.dto.OwnerResponse;
import com.holberton.realestate.exception.DuplicateEmailException;
import com.holberton.realestate.exception.OwnerNotFoundException;
import com.holberton.realestate.model.Owner;
import com.holberton.realestate.repository.OwnerRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class OwnerService {

    private static final Sort BY_NAME = Sort.by("name");

    private final OwnerRepository ownerRepository;

    public OwnerService(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    public List<OwnerResponse> findAll() {
        return ownerRepository.findAll(BY_NAME).stream()
                .map(OwnerMapper::toResponse)
                .toList();
    }

    public OwnerResponse findById(Long id) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new OwnerNotFoundException(id));
        return OwnerMapper.toResponse(owner);
    }

    public OwnerResponse create(OwnerRequest request) {
        if (ownerRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        Owner saved = ownerRepository.save(OwnerMapper.toEntity(request));
        return OwnerMapper.toResponse(saved);
    }
}