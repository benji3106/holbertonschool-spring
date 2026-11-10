package com.holberton.realestate.service;

import com.holberton.realestate.dto.PropertyRequest;
import com.holberton.realestate.model.Address;
import com.holberton.realestate.model.EnergyClass;
import com.holberton.realestate.model.Property;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PropertyService {

    private final List<Property> properties = new ArrayList<>();

    public PropertyService() {
        properties.add(new Property("P-001",
                new Address("12 Rue de la Paix", "75002", "Paris"), 25, 950, EnergyClass.D));
        properties.add(new Property("P-002",
                new Address("8 Chemin des Vignes", "33000", "Bordeaux"), 120, 1800, EnergyClass.B));
        properties.add(new Property("P-003",
                new Address("1 Place Bellecour", "69002", "Lyon"), 80, 2500, EnergyClass.C));
    }

    public List<Property> findAll() {
        return properties.stream()
                .sorted(Comparator.comparing(Property::getReference))
                .toList();
    }

    public Optional<Property> findByReference(String reference) {
        return properties.stream()
                .filter(property -> property.getReference().equals(reference))
                .findFirst();
    }

    /** Returns an empty Optional if the reference is already used. */
    public Optional<Property> create(PropertyRequest request) {
        if (findByReference(request.reference()).isPresent()) {
            return Optional.empty();
        }
        Property property = new Property(request.reference(), request.address(),
                request.surface(), request.monthlyRent(), request.energyClass());
        properties.add(property);
        return Optional.of(property);
    }

    /** Returns an empty Optional if no property has this reference. */
    public Optional<Property> update(String reference, PropertyRequest request) {
        Optional<Property> existing = findByReference(reference);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        Property property = existing.get();
        property.setAddress(request.address());
        property.setSurface(request.surface());
        property.setMonthlyRent(request.monthlyRent());
        property.setEnergyClass(request.energyClass());
        return Optional.of(property);
    }

    /** Returns false if no property has this reference. */
    public boolean delete(String reference) {
        return properties.removeIf(property -> property.getReference().equals(reference));
    }
}