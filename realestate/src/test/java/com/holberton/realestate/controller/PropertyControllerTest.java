package com.holberton.realestate.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.holberton.realestate.dto.PropertyRequest;
import com.holberton.realestate.dto.PropertyResponse;
import com.holberton.realestate.exception.PropertyNotFoundException;
import com.holberton.realestate.model.Address;
import com.holberton.realestate.model.EnergyClass;
import com.holberton.realestate.service.PropertyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PropertyController.class)
class PropertyControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private PropertyService propertyService;

    PropertyControllerTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("GET of a known property answers 200 with its JSON")
    void getKnownPropertyAnswers200() throws Exception {
        PropertyResponse response = new PropertyResponse("P-001",
                new Address("12 Rue de la Paix", "75002", "Paris"), 25, 950, EnergyClass.D,
                11400, null, false);
        when(propertyService.findByReference("P-001")).thenReturn(response);

        mockMvc.perform(get("/api/properties/P-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("P-001"))
                .andExpect(jsonPath("$.address.city").value("Paris"))
                .andExpect(jsonPath("$.annualRent").value(11400));
    }

    @Test
    @DisplayName("GET of an unknown property answers 404 with a problem")
    void getUnknownPropertyAnswers404() throws Exception {
        when(propertyService.findByReference("P-999"))
                .thenThrow(new PropertyNotFoundException("P-999"));

        mockMvc.perform(get("/api/properties/P-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("an invalid POST answers 400 and never calls the service")
    void invalidPostAnswers400() throws Exception {
        String invalidBody = """
                {"reference": "P-1",
                 "address": {"street": "3 Rue Lepic", "zipCode": "75018", "city": "Paris"},
                 "surface": 0, "monthlyRent": 800, "energyClass": "E"}
                """;

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reference").exists())
                .andExpect(jsonPath("$.errors.surface").exists());

        verify(propertyService, never()).create(any(PropertyRequest.class));
    }
}