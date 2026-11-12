package com.holberton.realestate.controller;

import com.holberton.realestate.dto.LeaseRequest;
import com.holberton.realestate.dto.LeaseResponse;
import com.holberton.realestate.service.LeaseService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leases")
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @GetMapping
    public List<LeaseResponse> findAll() {
        return leaseService.findAll();
    }

    @GetMapping("/{id}")
    public LeaseResponse findById(@PathVariable("id") Long id) {
        return leaseService.findById(id);
    }

    @PostMapping
    public ResponseEntity<LeaseResponse> sign(@Valid @RequestBody LeaseRequest request) {
        LeaseResponse created = leaseService.sign(request);
        URI location = URI.create("/api/leases/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/{id}/end")
    public LeaseResponse end(@PathVariable("id") Long id) {
        return leaseService.end(id);
    }
}