package com.holberton.realestate.checks;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Bodies;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Reflect;
import com.holberton.realestate.checks.support.Refs;
import com.holberton.realestate.checks.support.Response;
import com.holberton.realestate.checks.support.Schema;

import tools.jackson.databind.JsonNode;

/** Task 12: transactions, ending a lease and the rented flag. */
class Task12Test extends ApiCheck {

    private static final Refs REFS = Refs.block(980, 989);
    private static final String TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";

    @Order(1)
    @Test
    @DisplayName("a property without lease is sent back with rented equal to false")
    void rentedIsFalseByDefault() {
        String reference = property(1000);
        Response response = get("/api/properties/" + reference);
        Expect.status(response, 200);
        Expect.bool(Expect.object(response), "rented", false, response.request());
    }

    @Order(2)
    @Test
    @DisplayName("signing a lease marks the property as rented")
    void signingMarksTheProperty() {
        String reference = property(1000);
        Expect.status(post("/api/leases", Bodies.lease(reference, tenant(9000), 24)), 201);
        Response response = get("/api/properties/" + reference);
        Expect.status(response, 200);
        Expect.bool(Expect.object(response), "rented", true,
                response.request() + " after the lease was signed");
        Expect.isTrue(schema().number("select count(*) from properties where reference = '" + reference
                        + "' and rented = true") == 1,
                "expected the column RENTED of the row " + reference + " to be true in the database");
    }

    @Order(3)
    @Test
    @DisplayName("POST /api/leases/{id}/end returns 200 and the lease is no longer active")
    void endLease() {
        long lease = lease(property(1000));
        Response response = post("/api/leases/" + lease + "/end", null);
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        Expect.bool(body, "active", false, response.request());
        Expect.number(body, "id", (int) lease, response.request());
        Expect.bool(Expect.object(get("/api/leases/" + lease)), "active", false,
                "GET /api/leases/" + lease + " after the lease was ended");
    }

    @Order(4)
    @Test
    @DisplayName("ending a lease frees the property")
    void endLeaseFreesTheProperty() {
        String reference = property(1000);
        long lease = lease(reference);
        Expect.status(post("/api/leases/" + lease + "/end", null), 200);
        Response response = get("/api/properties/" + reference);
        Expect.status(response, 200);
        Expect.bool(Expect.object(response), "rented", false,
                response.request() + " after the lease was ended");
    }

    @Order(5)
    @Test
    @DisplayName("a property can be rented again once its lease is ended")
    void rentAgain() {
        String reference = property(1000);
        long lease = lease(reference);
        Expect.status(post("/api/leases/" + lease + "/end", null), 200);
        Response response = post("/api/leases", Bodies.lease(reference, tenant(9000), 12));
        Expect.status(response, 201);
        Expect.bool(Expect.object(response), "active", true, response.request());
    }

    @Order(6)
    @Test
    @DisplayName("ending a lease that is already ended returns 409")
    void endTwice() {
        long lease = lease(property(1000));
        Expect.status(post("/api/leases/" + lease + "/end", null), 200);
        Response response = post("/api/leases/" + lease + "/end", null);
        Expect.status(response, 409);
    }

    @Order(7)
    @Test
    @DisplayName("ending a lease that does not exist returns 404")
    void endUnknown() {
        Expect.status(post("/api/leases/999999/end", null), 404);
    }

    @Order(8)
    @Test
    @DisplayName("DELETE /api/properties/{reference} returns 409 when the property has a lease")
    void deleteRentedProperty() {
        String reference = property(1000);
        long lease = lease(reference);
        Response response = delete("/api/properties/" + reference);
        Expect.status(response, 409);
        Expect.status(get("/api/properties/" + reference), 200);

        Expect.status(post("/api/leases/" + lease + "/end", null), 200);
        Response afterEnd = delete("/api/properties/" + reference);
        Expect.status(afterEnd, 409);
        Expect.status(get("/api/properties/" + reference), 200);
    }

    @Order(9)
    @Test
    @DisplayName("the column RENTED of PROPERTIES is NOT NULL")
    void rentedColumn() {
        Schema.Column column = schema().column("properties", "rented");
        Expect.isTrue(!column.nullable(),
                "expected the column RENTED of PROPERTIES to be NOT NULL");
    }

    @Order(10)
    @Test
    @DisplayName("LeaseService is transactional and no controller is")
    void transactions() {
        Class<?> service = Reflect.require(Reflect.PACKAGE + ".service.LeaseService");
        boolean transactional = Reflect.hasAnnotation(service, TRANSACTIONAL);
        for (Method method : service.getDeclaredMethods()) {
            if (Reflect.hasAnnotation(method, TRANSACTIONAL)) {
                transactional = true;
            }
        }
        Expect.isTrue(transactional,
                "expected @Transactional on LeaseService or on the methods that change several rows");
        for (Class<?> controller : Reflect.restControllers(context())) {
            Expect.isTrue(!Reflect.hasAnnotation(controller, TRANSACTIONAL),
                    "expected no @Transactional on " + controller.getSimpleName()
                            + ": transactions belong to the services");
            for (Method method : controller.getDeclaredMethods()) {
                Expect.isTrue(!Reflect.hasAnnotation(method, TRANSACTIONAL),
                        "expected no @Transactional on " + controller.getSimpleName() + "."
                                + method.getName() + ": transactions belong to the services");
            }
        }
    }

    private String property(int monthlyRent) {
        String reference = REFS.next();
        Expect.status(post("/api/properties", Bodies.property(reference, "Angers", 45, monthlyRent, "D")),
                201);
        return reference;
    }

    private long tenant(int monthlyIncome) {
        Response response = post("/api/tenants",
                Bodies.tenant("Tenant Task14", Bodies.email("tenant-task14"), monthlyIncome));
        Expect.status(response, 201);
        return Expect.number(Expect.object(response), "id", response.request());
    }

    private long lease(String reference) {
        Response response = post("/api/leases", Bodies.lease(reference, tenant(9000), 24));
        Expect.status(response, 201);
        return Expect.number(Expect.object(response), "id", response.request());
    }
}
