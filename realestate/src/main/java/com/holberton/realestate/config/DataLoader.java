package com.holberton.realestate.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.holberton.realestate.model.Address;
import com.holberton.realestate.model.EnergyClass;
import com.holberton.realestate.model.Property;
import com.holberton.realestate.repository.PropertyRepository;

@Component
public class DataLoader implements CommandLineRunner {

    private final PropertyRepository properties;

    public DataLoader(PropertyRepository properties) {
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (properties.count() > 0) {
            return;
        }
        properties.save(new Property("P-001",
                new Address("12 Rue de la Paix", "75002", "Paris"), 25, 950, EnergyClass.D));
        properties.save(new Property("P-002",
                new Address("8 Chemin des Vignes", "33000", "Bordeaux"), 120, 1800, EnergyClass.B));
        properties.save(new Property("P-003",
                new Address("1 Place Bellecour", "69002", "Lyon"), 80, 2500, EnergyClass.C));
    }
}