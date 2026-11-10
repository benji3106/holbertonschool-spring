package com.holberton.realestate.controller;

import com.holberton.realestate.dto.PropertyRequest;
import com.holberton.realestate.model.Property;
import com.holberton.realestate.service.PropertyService;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public List<Property> findAll(@RequestParam(name = "city", required = false) String city,
                                  @RequestParam(name = "maxRent", required = false) Integer maxRent) {
        return propertyService.search(city, maxRent);
    }

    @GetMapping("/{reference}")
    public ResponseEntity<Property> findByReference(@PathVariable("reference") String reference) {
        Optional<Property> property = propertyService.findByReference(reference);
        if (property.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(property.get());
    }

    @PostMapping
    public ResponseEntity<Property> create(@RequestBody PropertyRequest request) {
        Optional<Property> created = propertyService.create(request);
        if (created.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        URI location = URI.create("/api/properties/" + created.get().getReference());
        return ResponseEntity.created(location).body(created.get());
    }

    @PutMapping("/{reference}")
    public ResponseEntity<Property> update(@PathVariable("reference") String reference,
                                           @RequestBody PropertyRequest request) {
        Optional<Property> updated = propertyService.update(reference, request);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated.get());
    }

    @DeleteMapping("/{reference}")
    public ResponseEntity<Void> delete(@PathVariable("reference") String reference) {
        if (!propertyService.delete(reference)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}