package com.holberton.realestate.checks;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Bodies;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Reflect;
import com.holberton.realestate.checks.support.Refs;
import com.holberton.realestate.checks.support.Response;

import tools.jackson.databind.JsonNode;

/** Task 14 (optional): the three kinds of property and their charges. */
class Task14Test extends ApiCheck {

    private static final Refs REFS = Refs.block(900, 909);

    @Order(1)
    @Test
    @DisplayName("P-001 is an apartment on the 3rd floor with an elevator")
    void sampleApartment() {
        Response response = get("/api/properties/P-001");
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        String what = response.request();
        Expect.text(body, "type", "APARTMENT", what);
        Expect.number(body, "floor", 3, what);
        Expect.bool(body, "hasElevator", true, what);
        Expect.number(body, "monthlyCharges", 90, what + " (25 m2 x 2 + 40 for the elevator)");
    }

    @Order(2)
    @Test
    @DisplayName("P-002 is a house with 400 m2 of land")
    void sampleHouse() {
        Response response = get("/api/properties/P-002");
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        String what = response.request();
        Expect.text(body, "type", "HOUSE", what);
        Expect.number(body, "landSurface", 400, what);
        Expect.number(body, "monthlyCharges", 260, what + " (120 m2 x 2 + 400 / 20)");
    }

    @Order(3)
    @Test
    @DisplayName("P-003 is a commercial space whose activity is bakery")
    void sampleCommercialSpace() {
        Response response = get("/api/properties/P-003");
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        String what = response.request();
        Expect.text(body, "type", "COMMERCIAL", what);
        Expect.text(body, "activity", "bakery", what);
        Expect.number(body, "monthlyCharges", 320, what + " (80 m2 x 4)");
    }

    @Order(4)
    @Test
    @DisplayName("the fields that do not belong to the type of the property are null")
    void fieldsOfTheOtherTypesAreNull() {
        JsonNode house = Expect.object(get("/api/properties/P-002"));
        Expect.isNull(house, "floor", "GET /api/properties/P-002 (a house has no floor)");
        Expect.isNull(house, "hasElevator", "GET /api/properties/P-002");
        Expect.isNull(house, "activity", "GET /api/properties/P-002");
        JsonNode commercial = Expect.object(get("/api/properties/P-003"));
        Expect.isNull(commercial, "landSurface", "GET /api/properties/P-003");
        Expect.isNull(commercial, "floor", "GET /api/properties/P-003");
    }

    @Order(5)
    @Test
    @DisplayName("creating an apartment computes 2 euros per m2, plus 40 with an elevator")
    void createApartment() {
        String reference = REFS.next();
        Response response = post("/api/properties", Bodies.typedProperty(reference, "Tours", 50, 900, "C",
                "APARTMENT", "\"floor\": 2, \"hasElevator\": false"));
        try {
            Expect.status(response, 201);
            JsonNode body = Expect.object(response);
            Expect.text(body, "type", "APARTMENT", response.request());
            Expect.number(body, "floor", 2, response.request());
            Expect.bool(body, "hasElevator", false, response.request());
            Expect.number(body, "monthlyCharges", 100, response.request() + " (50 m2 x 2, no elevator)");
        } finally {
            delete("/api/properties/" + reference);
        }
    }

    @Order(6)
    @Test
    @DisplayName("creating a house adds 1 euro per 20 m2 of land")
    void createHouse() {
        String reference = REFS.next();
        Response response = post("/api/properties", Bodies.typedProperty(reference, "Tours", 50, 900, "C",
                "HOUSE", "\"landSurface\": 100"));
        try {
            Expect.status(response, 201);
            JsonNode body = Expect.object(response);
            Expect.text(body, "type", "HOUSE", response.request());
            Expect.number(body, "landSurface", 100, response.request());
            Expect.number(body, "monthlyCharges", 105, response.request() + " (50 m2 x 2 + 100 / 20)");
        } finally {
            delete("/api/properties/" + reference);
        }
    }

    @Order(7)
    @Test
    @DisplayName("creating a commercial space computes 4 euros per m2")
    void createCommercialSpace() {
        String reference = REFS.next();
        Response response = post("/api/properties", Bodies.typedProperty(reference, "Tours", 50, 900, "C",
                "COMMERCIAL", "\"activity\": \"restaurant\""));
        try {
            Expect.status(response, 201);
            JsonNode body = Expect.object(response);
            Expect.text(body, "type", "COMMERCIAL", response.request());
            Expect.text(body, "activity", "restaurant", response.request());
            Expect.number(body, "monthlyCharges", 200, response.request() + " (50 m2 x 4)");
        } finally {
            delete("/api/properties/" + reference);
        }
    }

    @Order(8)
    @Test
    @DisplayName("a request without type still creates an apartment")
    void typeIsOptional() {
        String reference = REFS.next();
        Response response = post("/api/properties", Bodies.property(reference, "Tours", 30, 600, "D"));
        try {
            Expect.status(response, 201);
            JsonNode body = Expect.object(response);
            Expect.text(body, "type", "APARTMENT", response.request() + " (type is optional)");
            Expect.number(body, "floor", 0, response.request() + " (floor defaults to 0)");
            Expect.bool(body, "hasElevator", false, response.request()
                    + " (hasElevator defaults to false)");
            Expect.number(body, "monthlyCharges", 60, response.request() + " (30 m2 x 2)");
        } finally {
            delete("/api/properties/" + reference);
        }
    }

    @Order(9)
    @Test
    @DisplayName("a commercial space without activity is refused with 400 and saves nothing")
    void commercialSpaceNeedsAnActivity() {
        String reference = REFS.next();
        Response response = post("/api/properties",
                Bodies.typedProperty(reference, "Tours", 50, 900, "C", "COMMERCIAL", ""));
        if (response.status() == 201) {
            delete("/api/properties/" + reference);
        }
        Expect.status(response, 400);
        Expect.status(get("/api/properties/" + reference), 404);
        Response blank = post("/api/properties", Bodies.typedProperty(reference, "Tours", 50, 900, "C",
                "COMMERCIAL", "\"activity\": \"   \""));
        if (blank.status() == 201) {
            delete("/api/properties/" + reference);
        }
        Expect.status(blank, 400);
    }

    @Order(10)
    @Test
    @DisplayName("a negative floor or a negative land surface is refused with 400")
    void negativeSpecificFields() {
        String reference = REFS.next();
        Response floor = post("/api/properties", Bodies.typedProperty(reference, "Tours", 50, 900, "C",
                "APARTMENT", "\"floor\": -1"));
        if (floor.status() == 201) {
            delete("/api/properties/" + reference);
        }
        Expect.status(floor, 400);
        Response land = post("/api/properties", Bodies.typedProperty(reference, "Tours", 50, 900, "C",
                "HOUSE", "\"landSurface\": -20"));
        if (land.status() == 201) {
            delete("/api/properties/" + reference);
        }
        Expect.status(land, 400);
    }

    @Order(11)
    @Test
    @DisplayName("a PUT keeps the type of the property and updates its specific fields")
    void updateKeepsTheType() {
        String reference = REFS.next();
        Expect.status(post("/api/properties", Bodies.typedProperty(reference, "Tours", 50, 900, "C",
                "HOUSE", "\"landSurface\": 100")), 201);
        try {
            Response response = put("/api/properties/" + reference,
                    Bodies.typedProperty(reference, "Tours", 50, 900, "C", "APARTMENT",
                            "\"landSurface\": 500, \"floor\": 7"));
            Expect.status(response, 200);
            JsonNode body = Expect.object(response);
            Expect.text(body, "type", "HOUSE", response.request() + " (a PUT ignores the type)");
            Expect.number(body, "landSurface", 500, response.request());
            Expect.number(body, "monthlyCharges", 125, response.request() + " (50 m2 x 2 + 500 / 20)");
        } finally {
            delete("/api/properties/" + reference);
        }
    }

    @Order(12)
    @Test
    @DisplayName("the table PROPERTIES has the discriminator column PROPERTY_TYPE")
    void discriminatorColumn() {
        schema().column("properties", "property_type");
        Class<?> property = Reflect.require(Reflect.PACKAGE + ".model.Property");
        String strategy = Reflect.attribute(property, "jakarta.persistence.Inheritance", "strategy")
                .orElse("");
        Expect.isTrue(strategy.contains("SINGLE_TABLE"),
                "expected @Inheritance(strategy = InheritanceType.SINGLE_TABLE) on Property but got "
                        + (strategy.isEmpty() ? "no @Inheritance" : strategy));
        String column = Reflect.attribute(property, "jakarta.persistence.DiscriminatorColumn", "name")
                .orElse("");
        Expect.isTrue("property_type".equalsIgnoreCase(column),
                "expected @DiscriminatorColumn(name = \"property_type\") on Property but got "
                        + (column.isEmpty() ? "no @DiscriminatorColumn" : "\"" + column + "\""));
    }

    @Order(13)
    @Test
    @DisplayName("Property is abstract and the three kinds override getMonthlyCharges")
    void inheritanceHierarchy() {
        Class<?> property = Reflect.require(Reflect.PACKAGE + ".model.Property");
        Expect.isTrue(Modifier.isAbstract(property.getModifiers()),
                "expected the class Property to be abstract");
        String[][] kinds = {
            {"Apartment", "APARTMENT"},
            {"House", "HOUSE"},
            {"CommercialSpace", "COMMERCIAL"}
        };
        for (String[] kind : kinds) {
            Class<?> type = Reflect.require(Reflect.PACKAGE + ".model." + kind[0]);
            Expect.isTrue(property.isAssignableFrom(type),
                    "expected " + kind[0] + " to extend Property");
            Reflect.requireAnnotation(type, "jakarta.persistence.Entity");
            String value = Reflect.attribute(type, "jakarta.persistence.DiscriminatorValue", "value")
                    .orElse("");
            Expect.isTrue(kind[1].equals(value),
                    "expected @DiscriminatorValue(\"" + kind[1] + "\") on " + kind[0] + " but got "
                            + (value.isEmpty() ? "no @DiscriminatorValue" : "\"" + value + "\""));
            boolean overridden = false;
            for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
                if (method.getName().equals("getMonthlyCharges")) {
                    overridden = true;
                }
            }
            Expect.isTrue(overridden,
                    "expected " + kind[0] + " to override getMonthlyCharges(): polymorphism must choose"
                            + " the right computation, not an if on the type");
        }
    }
}
