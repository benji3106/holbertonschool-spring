package com.holberton.realestate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.holberton.realestate.dto.PropertyRequest;
import com.holberton.realestate.dto.PropertyResponse;
import com.holberton.realestate.exception.DuplicateReferenceException;
import com.holberton.realestate.exception.PropertyNotFoundException;
import com.holberton.realestate.model.Address;
import com.holberton.realestate.model.EnergyClass;
import com.holberton.realestate.model.Property;
import com.holberton.realestate.repository.LeaseRepository;
import com.holberton.realestate.repository.OwnerRepository;
import com.holberton.realestate.repository.PropertyRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private LeaseRepository leaseRepository;

    private PropertyService propertyService;

    @BeforeEach
    void setUp() {
        propertyService = new PropertyService(propertyRepository, ownerRepository,
                leaseRepository);
    }

    private PropertyRequest request(String reference) {
        return new PropertyRequest(reference, new Address("3 Rue Lepic", "75018", "Paris"),
                40, 800, EnergyClass.E);
    }

    @Test
    @DisplayName("an unknown reference throws PropertyNotFoundException")
    void unknownReferenceThrowsNotFound() {
        when(propertyRepository.findByReference("P-999")).thenReturn(Optional.empty());

        assertThrows(PropertyNotFoundException.class,
                () -> propertyService.findByReference("P-999"));
    }

    @Test
    @DisplayName("a duplicate reference throws DuplicateReferenceException and saves nothing")
    void duplicateReferenceThrowsAndSavesNothing() {
        when(propertyRepository.existsByReference("P-001")).thenReturn(true);

        assertThrows(DuplicateReferenceException.class,
                () -> propertyService.create(request("P-001")));
        verify(propertyRepository, never()).save(any(Property.class));
    }

    @Test
    @DisplayName("a valid creation calls save and returns the new property")
    void validCreationCallsSave() {
        when(propertyRepository.existsByReference("P-004")).thenReturn(false);
        when(propertyRepository.save(any(Property.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PropertyResponse response = propertyService.create(request("P-004"));

        verify(propertyRepository).save(any(Property.class));
        assertEquals("P-004", response.reference());
        assertEquals(9600, response.annualRent());
    }
}