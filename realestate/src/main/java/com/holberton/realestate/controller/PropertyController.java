package com.holberton.realestate.controller;

import com.holberton.realestate.dto.PropertyRequest;
import com.holberton.realestate.dto.PropertyResponse;
import com.holberton.realestate.service.PropertyService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public List<PropertyResponse> findAll(
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "maxRent", required = false) Integer maxRent) {
        return propertyService.search(city, maxRent);
    }

    @GetMapping("/{reference}")
    public PropertyResponse findByReference(@PathVariable("reference") String reference) {
        return propertyService.findByReference(reference);
    }

    @PostMapping
    public ResponseEntity<PropertyResponse> create(@Valid @RequestBody PropertyRequest request) {
        PropertyResponse created = propertyService.create(request);
        URI location = URI.create("/api/properties/" + created.reference());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{reference}")
    public PropertyResponse update(@PathVariable("reference") String reference,
                                   @Valid @RequestBody PropertyRequest request) {
        return propertyService.update(reference, request);
    }

    @DeleteMapping("/{reference}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("reference") String reference) {
        propertyService.delete(reference);
    }
}