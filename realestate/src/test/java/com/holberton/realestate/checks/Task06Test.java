package com.holberton.realestate.checks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Bodies;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Response;
import com.holberton.realestate.checks.support.Schema;

import tools.jackson.databind.JsonNode;

/** Task 6: validation. */
class Task06Test extends ApiCheck {

    private static final String VALID_ADDRESS =
            "\"address\": {\"street\": \"1 Rue des Tests\", \"zipCode\": \"75001\", \"city\": \"Paris\"}";

    @Order(1)
    @Test
    @DisplayName("a reference that is not P- followed by three digits is refused with 400")
    void invalidReference() {
        refused("{\"reference\": \"P-1\", " + VALID_ADDRESS
                + ", \"surface\": 30, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
        refused("{\"reference\": \"HOUSE-001\", " + VALID_ADDRESS
                + ", \"surface\": 30, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
        refused("{" + VALID_ADDRESS + ", \"surface\": 30, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
    }

    @Order(2)
    @Test
    @DisplayName("a surface of 0 or less is refused with 400")
    void invalidSurface() {
        refused("{\"reference\": \"P-945\", " + VALID_ADDRESS
                + ", \"surface\": 0, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
        refused("{\"reference\": \"P-945\", " + VALID_ADDRESS
                + ", \"surface\": -10, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
    }

    @Order(3)
    @Test
    @DisplayName("a negative monthly rent is refused with 400")
    void invalidRent() {
        refused("{\"reference\": \"P-945\", " + VALID_ADDRESS
                + ", \"surface\": 30, \"monthlyRent\": -1, \"energyClass\": \"D\"}");
    }

    @Order(4)
    @Test
    @DisplayName("a zip code that is not five digits is refused with 400")
    void invalidZipCode() {
        refused("{\"reference\": \"P-945\", \"address\": {\"street\": \"1 Rue des Tests\","
                + " \"zipCode\": \"750\", \"city\": \"Paris\"},"
                + " \"surface\": 30, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
        refused("{\"reference\": \"P-945\", \"address\": {\"street\": \"1 Rue des Tests\","
                + " \"zipCode\": \"ABCDE\", \"city\": \"Paris\"},"
                + " \"surface\": 30, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
    }

    @Order(5)
    @Test
    @DisplayName("a blank street or a blank city is refused with 400")
    void blankAddressFields() {
        refused("{\"reference\": \"P-945\", \"address\": {\"street\": \"   \","
                + " \"zipCode\": \"75001\", \"city\": \"Paris\"},"
                + " \"surface\": 30, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
        refused("{\"reference\": \"P-945\", \"address\": {\"street\": \"1 Rue des Tests\","
                + " \"zipCode\": \"75001\", \"city\": \"\"},"
                + " \"surface\": 30, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
    }

    @Order(6)
    @Test
    @DisplayName("a missing address and a missing energy class are refused with 400")
    void missingFields() {
        refused("{\"reference\": \"P-945\", \"surface\": 30, \"monthlyRent\": 700,"
                + " \"energyClass\": \"D\"}");
        refused("{\"reference\": \"P-945\", " + VALID_ADDRESS
                + ", \"surface\": 30, \"monthlyRent\": 700}");
    }

    @Order(7)
    @Test
    @DisplayName("a refused creation saves nothing")
    void refusedCreationSavesNothing() {
        refused("{\"reference\": \"P-947\", " + VALID_ADDRESS
                + ", \"surface\": 0, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
        Expect.status(get("/api/properties/P-947"), 404);
        Expect.isTrue(schema().countWhere("properties", "reference", "P-947") == 0,
                "expected no row with REFERENCE = 'P-947' after a refused creation");
    }

    @Order(8)
    @Test
    @DisplayName("a refused update changes nothing")
    void refusedUpdateChangesNothing() {
        Expect.status(post("/api/properties", Bodies.property("P-948", "Metz", 35, 600, "D")), 201);
        try {
            Response response = put("/api/properties/P-948", "{\"reference\": \"P-948\", " + VALID_ADDRESS
                    + ", \"surface\": -5, \"monthlyRent\": 9999, \"energyClass\": \"D\"}");
            Expect.status(response, 400);
            JsonNode body = Expect.object(get("/api/properties/P-948"));
            Expect.number(body, "surface", 35, "GET /api/properties/P-948 after a refused PUT");
            Expect.number(body, "monthlyRent", 600, "GET /api/properties/P-948 after a refused PUT");
        } finally {
            delete("/api/properties/P-948");
        }
    }

    @Order(9)
    @Test
    @DisplayName("a valid creation is still accepted")
    void validCreationStillWorks() {
        Response response = post("/api/properties", Bodies.property("P-949", "Metz", 35, 0, "A"));
        try {
            Expect.status(response, 201);
            Expect.number(Expect.object(response), "monthlyRent", 0,
                    response.request() + " (a rent of 0 is valid)");
        } finally {
            delete("/api/properties/P-949");
        }
    }

    @Order(10)
    @Test
    @DisplayName("every column of the table PROPERTIES is NOT NULL")
    void columnsAreNotNull() {
        for (String column : new String[] {"reference", "street", "zip_code", "city", "surface",
                "monthly_rent", "energy_class"}) {
            Schema.Column found = schema().column("properties", column);
            Expect.isTrue(!found.nullable(),
                    "expected the column " + found.name() + " of PROPERTIES to be NOT NULL"
                            + " (@Column(nullable = false)) but it accepts null");
        }
    }

    private void refused(String body) {
        Response response = post("/api/properties", body);
        if (response.status() == 201) {
            String reference = Expect.object(response).path("reference").asString("");
            if (!reference.isEmpty()) {
                delete("/api/properties/" + reference);
            }
        }
        Expect.status(response, 400);
    }
}
