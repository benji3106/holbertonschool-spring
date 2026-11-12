package com.holberton.realestate.controller;

import com.holberton.realestate.dto.TenantRequest;
import com.holberton.realestate.dto.TenantResponse;
import com.holberton.realestate.service.TenantService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/{id}")
    public TenantResponse findById(@PathVariable("id") Long id) {
        return tenantService.findById(id);
    }

    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody TenantRequest request) {
        TenantResponse created = tenantService.create(request);
        URI location = URI.create("/api/tenants/" + created.id());
        return ResponseEntity.created(location).body(created);
    }
}