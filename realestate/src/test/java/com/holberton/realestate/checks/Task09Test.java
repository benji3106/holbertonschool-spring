package com.holberton.realestate.checks;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Bodies;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Reflect;
import com.holberton.realestate.checks.support.Response;
import com.holberton.realestate.checks.support.Schema;

import tools.jackson.databind.JsonNode;

/** Task 9: the relation between a property and its owner. */
class Task09Test extends ApiCheck {

    @Order(1)
    @Test
    @DisplayName("PUT /api/properties/{reference}/owner assigns the owner and returns 200")
    void assignOwner() {
        long owner = createOwner("Gina Owner");
        createProperty("P-955");
        try {
            Response response = put("/api/properties/P-955/owner", Bodies.ownerAssignment(owner));
            Expect.status(response, 200);
            JsonNode body = Expect.object(response);
            Expect.text(body, "reference", "P-955", response.request());
            Expect.number(body, "ownerId", (int) owner, response.request());
            Expect.number(Expect.object(get("/api/properties/P-955")), "ownerId", (int) owner,
                    "GET /api/properties/P-955 after the assignment");
        } finally {
            delete("/api/properties/P-955");
            delete("/api/owners/" + owner);
        }
    }

    @Order(2)
    @Test
    @DisplayName("a property without owner has an ownerId equal to null")
    void ownerIdIsNullWithoutOwner() {
        createProperty("P-956");
        try {
            Response response = get("/api/properties/P-956");
            Expect.status(response, 200);
            Expect.isNull(Expect.object(response), "ownerId", response.request());
        } finally {
            delete("/api/properties/P-956");
        }
    }

    @Order(3)
    @Test
    @DisplayName("a second assignment replaces the owner of the property")
    void replaceOwner() {
        long first = createOwner("Hugo Owner");
        long second = createOwner("Iris Owner");
        createProperty("P-957");
        try {
            Expect.status(put("/api/properties/P-957/owner", Bodies.ownerAssignment(first)), 200);
            Response response = put("/api/properties/P-957/owner", Bodies.ownerAssignment(second));
            Expect.status(response, 200);
            Expect.number(Expect.object(response), "ownerId", (int) second, response.request());
            Expect.size(Expect.array(get("/api/owners/" + first + "/properties")), 0,
                    "GET /api/owners/" + first + "/properties after the property changed owner");
        } finally {
            delete("/api/properties/P-957");
            delete("/api/owners/" + first);
            delete("/api/owners/" + second);
        }
    }

    @Order(4)
    @Test
    @DisplayName("an assignment with an unknown property or an unknown owner returns 404")
    void assignUnknown() {
        long owner = createOwner("Jules Owner");
        createProperty("P-958");
        try {
            Expect.status(put("/api/properties/P-999/owner", Bodies.ownerAssignment(owner)), 404);
            Response response = put("/api/properties/P-958/owner", Bodies.ownerAssignment(999999L));
            Expect.status(response, 404);
            Expect.isNull(Expect.object(get("/api/properties/P-958")), "ownerId",
                    "GET /api/properties/P-958 after a refused assignment");
        } finally {
            delete("/api/properties/P-958");
            delete("/api/owners/" + owner);
        }
    }

    @Order(5)
    @Test
    @DisplayName("an assignment without ownerId returns 400")
    void assignWithoutOwnerId() {
        createProperty("P-959");
        try {
            Expect.status(put("/api/properties/P-959/owner", "{}"), 400);
            Expect.status(put("/api/properties/P-959/owner", "{\"ownerId\": null}"), 400);
        } finally {
            delete("/api/properties/P-959");
        }
    }

    @Order(6)
    @Test
    @DisplayName("GET /api/owners/{id}/properties returns the properties of this owner, sorted")
    void propertiesOfAnOwner() {
        long owner = createOwner("Karl Owner");
        createProperty("P-955");
        createProperty("P-956");
        try {
            Expect.status(put("/api/properties/P-956/owner", Bodies.ownerAssignment(owner)), 200);
            Expect.status(put("/api/properties/P-955/owner", Bodies.ownerAssignment(owner)), 200);
            Response response = get("/api/owners/" + owner + "/properties");
            Expect.status(response, 200);
            List<String> references = Expect.texts(Expect.array(response), "reference",
                    response.request());
            Expect.isTrue(references.equals(List.of("P-955", "P-956")),
                    response.request() + ": expected exactly [P-955, P-956] sorted by reference but got "
                            + references);
        } finally {
            delete("/api/properties/P-955");
            delete("/api/properties/P-956");
            delete("/api/owners/" + owner);
        }
    }

    @Order(7)
    @Test
    @DisplayName("GET /api/owners/{id}/properties returns an empty list for an owner without property")
    void ownerWithoutProperty() {
        long owner = createOwner("Lena Owner");
        try {
            Response response = get("/api/owners/" + owner + "/properties");
            Expect.status(response, 200);
            Expect.size(Expect.array(response), 0, response.request());
        } finally {
            delete("/api/owners/" + owner);
        }
    }

    @Order(8)
    @Test
    @DisplayName("GET /api/owners/{id}/properties with an unknown owner returns 404")
    void propertiesOfAnUnknownOwner() {
        Expect.status(get("/api/owners/999999/properties"), 404);
    }

    @Order(9)
    @Test
    @DisplayName("DELETE /api/owners/{id} returns 204 for an owner without property, 404 if unknown")
    void deleteOwner() {
        long owner = createOwner("Marc Owner");
        Response response = delete("/api/owners/" + owner);
        Expect.status(response, 204);
        Expect.status(get("/api/owners/" + owner), 404);
        Expect.status(delete("/api/owners/999999"), 404);
    }

    @Order(10)
    @Test
    @DisplayName("DELETE /api/owners/{id} returns 409 when the owner still owns a property")
    void deleteOwnerWithProperties() {
        long owner = createOwner("Nina Owner");
        createProperty("P-955");
        try {
            Expect.status(put("/api/properties/P-955/owner", Bodies.ownerAssignment(owner)), 200);
            Response response = delete("/api/owners/" + owner);
            Expect.status(response, 409);
            Expect.status(get("/api/owners/" + owner), 200);
        } finally {
            delete("/api/properties/P-955");
            delete("/api/owners/" + owner);
        }
    }

    @Order(11)
    @Test
    @DisplayName("the column OWNER_ID of PROPERTIES is a nullable foreign key to OWNERS")
    void ownerIdColumn() {
        Schema.Column column = schema().column("properties", "owner_id");
        Expect.isTrue(column.nullable(),
                "expected the column OWNER_ID of PROPERTIES to accept null: a property may have no owner");
        List<String> keys = schema().foreignKeys("properties");
        Expect.isTrue(keys.contains("OWNERID -> OWNERS"),
                "expected the column OWNER_ID of PROPERTIES to be a foreign key to the table OWNERS but"
                        + " the foreign keys of PROPERTIES are " + keys);
    }

    @Order(12)
    @Test
    @DisplayName("Property.owner is a @ManyToOne loaded lazily")
    void ownerIsLazyManyToOne() {
        Class<?> property = Reflect.require(Reflect.PACKAGE + ".model.Property");
        Field owner = null;
        for (Field field : property.getDeclaredFields()) {
            if (Reflect.hasAnnotation(field, "jakarta.persistence.ManyToOne")) {
                owner = field;
            }
        }
        Expect.isTrue(owner != null,
                "expected a field annotated with @ManyToOne in Property for its owner");
        String fetch = Reflect.attribute(owner, "jakarta.persistence.ManyToOne", "fetch").orElse("");
        Expect.isTrue(fetch.contains("LAZY"),
                "expected @ManyToOne(fetch = FetchType.LAZY) on Property." + owner.getName()
                        + " but the fetch type is " + (fetch.isEmpty() ? "not readable" : fetch));
    }

    private long createOwner(String name) {
        Response response = post("/api/owners", Bodies.owner(name, Bodies.email("owner-task11")));
        Expect.status(response, 201);
        return Expect.number(Expect.object(response), "id", response.request());
    }

    private void createProperty(String reference) {
        delete("/api/properties/" + reference);
        Expect.status(post("/api/properties", Bodies.property(reference, "Angers", 45, 700, "D")), 201);
    }
}
