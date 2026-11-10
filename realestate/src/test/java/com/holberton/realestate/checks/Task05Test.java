package com.holberton.realestate.checks;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Bodies;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Reflect;
import com.holberton.realestate.checks.support.Response;

import tools.jackson.databind.JsonNode;

/** Task 5: the DTOs. */
class Task05Test extends ApiCheck {

    private static final List<String> ENTITIES = List.of(
            Reflect.PACKAGE + ".model.Property",
            Reflect.PACKAGE + ".model.Owner",
            Reflect.PACKAGE + ".model.Tenant",
            Reflect.PACKAGE + ".model.Lease",
            Reflect.PACKAGE + ".model.Apartment",
            Reflect.PACKAGE + ".model.House",
            Reflect.PACKAGE + ".model.CommercialSpace");

    @Order(1)
    @Test
    @DisplayName("the JSON of a property contains annualRent, 12 times the monthly rent")
    void annualRent() {
        Response response = get("/api/properties/P-001");
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        Expect.number(body, "monthlyRent", 950, response.request());
        Expect.number(body, "annualRent", 11400, response.request());
    }

    @Order(2)
    @Test
    @DisplayName("the JSON of a property no longer contains the technical id")
    void noIdInTheJson() {
        Response response = get("/api/properties/P-002");
        Expect.status(response, 200);
        Expect.absent(Expect.object(response), "id", response.request());
    }

    @Order(3)
    @Test
    @DisplayName("every property of the list is a DTO: annualRent, no id")
    void theListSendsDtos() {
        Response response = get("/api/properties");
        Expect.status(response, 200);
        JsonNode properties = Expect.array(response);
        Expect.isTrue(properties.size() > 0, response.request() + ": expected at least one property");
        for (JsonNode property : properties) {
            String reference = property.path("reference").asString();
            Expect.absent(property, "id", response.request() + " (" + reference + ")");
            Expect.number(property, "annualRent",
                    Expect.number(property, "monthlyRent", response.request()) * 12,
                    response.request() + " (" + reference + ")");
        }
    }

    @Order(4)
    @Test
    @DisplayName("a created or updated property is sent back as a DTO too")
    void createAndUpdateSendDtos() {
        Response created = post("/api/properties", Bodies.property("P-940", "Dijon", 50, 700, "C"));
        try {
            Expect.status(created, 201);
            JsonNode body = Expect.object(created);
            Expect.number(body, "annualRent", 8400, created.request());
            Expect.absent(body, "id", created.request());

            Response updated = put("/api/properties/P-940", Bodies.property("P-940", "Dijon", 50, 800, "C"));
            Expect.status(updated, 200);
            JsonNode updatedBody = Expect.object(updated);
            Expect.number(updatedBody, "annualRent", 9600, updated.request());
            Expect.absent(updatedBody, "id", updated.request());
        } finally {
            delete("/api/properties/P-940");
        }
    }

    @Order(5)
    @Test
    @DisplayName("PropertyResponse is a record with the components of the task")
    void propertyResponseIsARecord() {
        Class<?> type = Reflect.require(Reflect.PACKAGE + ".dto.PropertyResponse");
        Expect.isTrue(type.isRecord(), "expected PropertyResponse to be a record");
        List<String> components = Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
        for (String expected : List.of("reference", "address", "surface", "monthlyRent", "energyClass",
                "annualRent")) {
            Expect.isTrue(components.contains(expected),
                    "expected a component \"" + expected + "\" in PropertyResponse but got " + components);
        }
    }

    @Order(6)
    @Test
    @DisplayName("PropertyMapper converts an entity into a response and a request into an entity")
    void propertyMapperExists() {
        Class<?> mapper = Reflect.require(Reflect.PACKAGE + ".dto.PropertyMapper");
        Expect.isTrue(Reflect.hasMethod(mapper, "toResponse"),
                "expected a method toResponse(Property) in PropertyMapper but got "
                        + Reflect.methodNames(mapper));
        Expect.isTrue(Reflect.hasMethod(mapper, "toEntity"),
                "expected a method toEntity(PropertyRequest) in PropertyMapper but got "
                        + Reflect.methodNames(mapper));
    }

    @Order(7)
    @Test
    @DisplayName("no method of a controller receives or returns an entity")
    void controllersNeverExposeEntities() {
        List<String> mistakes = new ArrayList<>();
        for (Class<?> controller : Reflect.restControllers(context())) {
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                for (String entity : ENTITIES) {
                    if (Reflect.mentions(method.getGenericReturnType(), entity)) {
                        mistakes.add(controller.getSimpleName() + "." + method.getName() + " returns "
                                + Reflect.simpleName(entity));
                    }
                    for (java.lang.reflect.Type parameter : method.getGenericParameterTypes()) {
                        if (Reflect.mentions(parameter, entity)) {
                            mistakes.add(controller.getSimpleName() + "." + method.getName() + " receives "
                                    + Reflect.simpleName(entity));
                        }
                    }
                }
            }
        }
        Expect.isTrue(mistakes.isEmpty(),
                "expected the controllers to use DTOs only, but " + mistakes);
    }
}
