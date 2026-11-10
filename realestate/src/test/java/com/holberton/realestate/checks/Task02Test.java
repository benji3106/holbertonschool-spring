package com.holberton.realestate.checks;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
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

/** Task 2: a service, dependency injection and the full CRUD of the properties, in memory.
 *
 * <p>The checks that create a property use their own references, in the P-91x range.</p>
 */
class Task02Test extends ApiCheck {

    @Order(1)
    @Test
    @DisplayName("GET /api/properties returns 200 with a JSON array")
    void listIsAnArray() {
        Response response = get("/api/properties");
        Expect.status(response, 200);
        Expect.jsonContentType(response);
        Expect.array(response);
    }

    @Order(2)
    @Test
    @DisplayName("GET /api/properties contains the three sample properties")
    void listContainsTheSamples() {
        Response response = get("/api/properties");
        Expect.status(response, 200);
        List<String> references = Expect.texts(Expect.array(response), "reference", response.request());
        Expect.contains(references, "P-001", response.request());
        Expect.contains(references, "P-002", response.request());
        Expect.contains(references, "P-003", response.request());
    }

    @Order(3)
    @Test
    @DisplayName("GET /api/properties returns the properties sorted by reference")
    void listIsSorted() {
        Response response = get("/api/properties");
        Expect.status(response, 200);
        Expect.sorted(Expect.texts(Expect.array(response), "reference", response.request()),
                response.request());
    }

    @Order(4)
    @Test
    @DisplayName("a property is sent back with its reference, surface, rent and energy class")
    void propertyFields() {
        Response response = get("/api/properties/P-001");
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        String what = response.request();
        Expect.text(body, "reference", "P-001", what);
        Expect.number(body, "surface", 25, what);
        Expect.number(body, "monthlyRent", 950, what);
        Expect.text(body, "energyClass", "D", what);
    }

    @Order(5)
    @Test
    @DisplayName("the address is a nested object with street, zipCode and city")
    void addressIsNested() {
        Response response = get("/api/properties/P-002");
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        String what = response.request();
        Expect.isTrue(Expect.at(body, "address", what).isObject(),
                what + ": expected \"address\" to be a JSON object");
        Expect.text(body, "address.street", "8 Chemin des Vignes", what);
        Expect.text(body, "address.zipCode", "33000", what);
        Expect.text(body, "address.city", "Bordeaux", what);
    }

    @Order(6)
    @Test
    @DisplayName("GET /api/properties/P-003 returns the third sample property")
    void oneProperty() {
        Response response = get("/api/properties/P-003");
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        String what = response.request();
        Expect.text(body, "reference", "P-003", what);
        Expect.text(body, "address.city", "Lyon", what);
        Expect.number(body, "surface", 80, what);
        Expect.number(body, "monthlyRent", 2500, what);
        Expect.text(body, "energyClass", "C", what);
    }

    @Order(7)
    @Test
    @DisplayName("GET /api/properties with an unknown reference returns 404")
    void unknownReference() {
        Response response = get("/api/properties/P-999");
        Expect.status(response, 404);
    }

    @Order(8)
    @Test
    @DisplayName("Address is a record and EnergyClass an enum with the classes A to G")
    void modelTypes() {
        Class<?> address = Reflect.require(Reflect.PACKAGE + ".model.Address");
        Expect.isTrue(address.isRecord(), "expected Address to be a record");
        List<String> components = Arrays.stream(address.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList();
        Expect.isTrue(components.equals(List.of("street", "zipCode", "city")),
                "expected Address(String street, String zipCode, String city) but got " + components);

        Class<?> energyClass = Reflect.require(Reflect.PACKAGE + ".model.EnergyClass");
        Expect.isTrue(energyClass.isEnum(), "expected EnergyClass to be an enum");
        List<String> constants = Arrays.stream(energyClass.getEnumConstants())
                .map(constant -> ((Enum<?>) constant).name())
                .toList();
        Expect.isTrue(constants.equals(List.of("A", "B", "C", "D", "E", "F", "G")),
                "expected the constants A, B, C, D, E, F, G but got " + constants);
    }

    @Order(9)
    @Test
    @DisplayName("PropertyService is a @Service injected into the controller through its constructor")
    void constructorInjection() {
        Class<?> service = Reflect.require(Reflect.PACKAGE + ".service.PropertyService");
        Reflect.requireAnnotation(service, "org.springframework.stereotype.Service");

        Class<?> controller = Reflect.require(Reflect.PACKAGE + ".controller.PropertyController");
        Reflect.requireAnnotation(controller, "org.springframework.web.bind.annotation.RestController");
        Expect.isTrue(Reflect.constructorTakes(controller, Reflect.PACKAGE + ".service.PropertyService"),
                "expected PropertyController to receive a PropertyService in its constructor");
        Expect.isTrue(Reflect.fieldsAnnotatedWith(controller,
                        "org.springframework.beans.factory.annotation.Autowired").isEmpty(),
                "expected no @Autowired field in PropertyController: inject through the constructor");
    }

    @Order(10)
    @Test
    @DisplayName("PropertyController does not store the properties itself")
    void controllerHasNoData() {
        Class<?> controller = Reflect.require(Reflect.PACKAGE + ".controller.PropertyController");
        for (Field field : controller.getDeclaredFields()) {
            Expect.isTrue(!Collection.class.isAssignableFrom(field.getType())
                            && !java.util.Map.class.isAssignableFrom(field.getType()),
                    "expected PropertyController to hold no list of properties but it declares the field "
                            + field.getName() + " of type " + field.getType().getSimpleName());
        }
    }

    @Order(11)
    @Test
    @DisplayName("POST /api/properties returns 201 with the created property")
    void createReturns201() {
        Response response = post("/api/properties", Bodies.property("P-910", "Nantes", 40, 800, "E"));
        try {
            Expect.status(response, 201);
            JsonNode body = Expect.object(response);
            String what = response.request();
            Expect.text(body, "reference", "P-910", what);
            Expect.text(body, "address.city", "Nantes", what);
            Expect.number(body, "surface", 40, what);
            Expect.number(body, "monthlyRent", 800, what);
            Expect.text(body, "energyClass", "E", what);
        } finally {
            delete("/api/properties/P-910");
        }
    }

    @Order(12)
    @Test
    @DisplayName("POST /api/properties sets the Location header of the new property")
    void createSetsLocation() {
        Response response = post("/api/properties", Bodies.property("P-911", "Nantes", 40, 800, "E"));
        try {
            Expect.status(response, 201);
            String location = response.header("Location").orElse("");
            Expect.isTrue(location.endsWith("/api/properties/P-911"),
                    response.request() + ": expected the header Location to end with /api/properties/P-911"
                            + " but got " + (location.isEmpty() ? "no Location header" : location));
        } finally {
            delete("/api/properties/P-911");
        }
    }

    @Order(13)
    @Test
    @DisplayName("a created property can be read back with GET")
    void createdPropertyIsReadable() {
        Expect.status(post("/api/properties", Bodies.property("P-912", "Lille", 55, 1200, "A")), 201);
        try {
            Response response = get("/api/properties/P-912");
            Expect.status(response, 200);
            JsonNode body = Expect.object(response);
            Expect.text(body, "reference", "P-912", response.request());
            Expect.number(body, "surface", 55, response.request());
        } finally {
            delete("/api/properties/P-912");
        }
    }

    @Order(14)
    @Test
    @DisplayName("POST /api/properties with a reference already used returns 409")
    void duplicateReferenceReturns409() {
        Expect.status(post("/api/properties", Bodies.property("P-913", "Lille", 55, 1200, "A")), 201);
        try {
            Response response = post("/api/properties", Bodies.property("P-913", "Lille", 55, 1200, "A"));
            Expect.status(response, 409);
        } finally {
            delete("/api/properties/P-913");
        }
    }

    @Order(15)
    @Test
    @DisplayName("a refused creation does not change the existing property")
    void refusedCreationChangesNothing() {
        Expect.status(post("/api/properties", Bodies.property("P-914", "Lille", 55, 1200, "A")), 201);
        try {
            post("/api/properties", Bodies.property("P-914", "Tours", 999, 9999, "G"));
            Response response = get("/api/properties/P-914");
            Expect.status(response, 200);
            JsonNode body = Expect.object(response);
            String what = "after a refused POST on P-914";
            Expect.number(body, "surface", 55, what);
            Expect.number(body, "monthlyRent", 1200, what);
            Expect.text(body, "address.city", "Lille", what);
        } finally {
            delete("/api/properties/P-914");
        }
    }

    @Order(16)
    @Test
    @DisplayName("PUT /api/properties/{reference} updates the property and returns 200")
    void updateReturns200() {
        Expect.status(post("/api/properties", Bodies.property("P-915", "Lille", 55, 1200, "A")), 201);
        try {
            Response response = put("/api/properties/P-915",
                    Bodies.property("P-915", "9 Rue Neuve", "59000", "Tourcoing", 60, 1300, "B"));
            Expect.status(response, 200);
            JsonNode body = Expect.object(response);
            String what = response.request();
            Expect.text(body, "reference", "P-915", what);
            Expect.text(body, "address.city", "Tourcoing", what);
            Expect.text(body, "address.street", "9 Rue Neuve", what);
            Expect.number(body, "surface", 60, what);
            Expect.number(body, "monthlyRent", 1300, what);
            Expect.text(body, "energyClass", "B", what);

            JsonNode reread = Expect.object(get("/api/properties/P-915"));
            Expect.number(reread, "monthlyRent", 1300, "GET /api/properties/P-915 after the PUT");
        } finally {
            delete("/api/properties/P-915");
        }
    }

    @Order(17)
    @Test
    @DisplayName("a PUT ignores the reference of the body: the reference never changes")
    void updateIgnoresTheReferenceOfTheBody() {
        Expect.status(post("/api/properties", Bodies.property("P-916", "Lille", 55, 1200, "A")), 201);
        try {
            Response response = put("/api/properties/P-916", Bodies.property("P-919", "Reims", 70, 1400, "C"));
            Expect.status(response, 200);
            Expect.text(Expect.object(response), "reference", "P-916", response.request());
            Expect.status(get("/api/properties/P-919"), 404);
            Expect.number(Expect.object(get("/api/properties/P-916")), "surface", 70,
                    "GET /api/properties/P-916 after the PUT");
        } finally {
            delete("/api/properties/P-916");
            delete("/api/properties/P-919");
        }
    }

    @Order(18)
    @Test
    @DisplayName("PUT /api/properties/{reference} with an unknown reference returns 404")
    void updateUnknownReturns404() {
        Expect.status(put("/api/properties/P-999", Bodies.property("P-999", "Reims", 70, 1400, "C")), 404);
    }

    @Order(19)
    @Test
    @DisplayName("DELETE /api/properties/{reference} returns 204 and the property is gone")
    void deleteReturns204() {
        Expect.status(post("/api/properties", Bodies.property("P-917", "Brest", 45, 650, "D")), 201);
        Response response = delete("/api/properties/P-917");
        Expect.status(response, 204);
        Expect.emptyBody(response);
        Expect.status(get("/api/properties/P-917"), 404);
    }

    @Order(20)
    @Test
    @DisplayName("DELETE /api/properties/{reference} with an unknown reference returns 404")
    void deleteUnknownReturns404() {
        Expect.status(delete("/api/properties/P-999"), 404);
    }

    @Order(21)
    @Test
    @DisplayName("PropertyRequest is a record and Property has no setReference method")
    void requestIsARecordAndReferenceIsFinal() {
        Class<?> request = Reflect.require(Reflect.PACKAGE + ".dto.PropertyRequest");
        Expect.isTrue(request.isRecord(), "expected PropertyRequest to be a record");
        Class<?> property = Reflect.require(Reflect.PACKAGE + ".model.Property");
        Expect.isTrue(!Reflect.hasMethod(property, "setReference"),
                "expected Property to have no setReference method: a reference never changes");
    }
}
