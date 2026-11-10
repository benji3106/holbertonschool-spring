package com.holberton.realestate.checks;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Bodies;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Refs;
import com.holberton.realestate.checks.support.Response;
import com.holberton.realestate.checks.support.Schema;

import tools.jackson.databind.JsonNode;

/** Task 11: the leases and the business rules. */
class Task11Test extends ApiCheck {

    private static final Refs REFS = Refs.block(960, 979);

    @Order(1)
    @Test
    @DisplayName("POST /api/leases returns 201 with the lease and its Location")
    void signLease() {
        String reference = property(1000, "D");
        long tenant = tenant(3000);
        Response response = post("/api/leases", Bodies.lease(reference, tenant, 24));
        Expect.status(response, 201);
        JsonNode body = Expect.object(response);
        String what = response.request();
        Expect.text(body, "propertyReference", reference, what);
        Expect.number(body, "tenantId", (int) tenant, what);
        Expect.number(body, "durationMonths", 24, what);
        Expect.bool(body, "active", true, what);
        long id = Expect.number(body, "id", what);
        String location = response.header("Location").orElse("");
        Expect.isTrue(location.endsWith("/api/leases/" + id),
                what + ": expected the header Location to end with /api/leases/" + id + " but got "
                        + (location.isEmpty() ? "no Location header" : location));
    }

    @Order(2)
    @Test
    @DisplayName("the lease computes monthlyRent, totalRent and a deposit of two months")
    void leaseAmounts() {
        String reference = property(1000, "D");
        Response response = post("/api/leases", Bodies.lease(reference, tenant(5000), 24));
        Expect.status(response, 201);
        JsonNode body = Expect.object(response);
        String what = response.request() + " (rent 1000, duration 24)";
        Expect.number(body, "monthlyRent", 1000, what);
        Expect.number(body, "totalRent", 24000, what);
        Expect.number(body, "deposit", 2000, what);
    }

    @Order(3)
    @Test
    @DisplayName("the lease keeps the rent of the signature even if the property changes later")
    void leaseKeepsItsRent() {
        String reference = property(1000, "D");
        Response signed = post("/api/leases", Bodies.lease(reference, tenant(5000), 12));
        Expect.status(signed, 201);
        long id = Expect.number(Expect.object(signed), "id", signed.request());
        Expect.status(put("/api/properties/" + reference,
                Bodies.property(reference, "Angers", 45, 1500, "D")), 200);
        Response response = get("/api/leases/" + id);
        Expect.status(response, 200);
        Expect.number(Expect.object(response), "monthlyRent", 1000,
                response.request() + " after the rent of the property changed to 1500");
    }

    @Order(4)
    @Test
    @DisplayName("a duration out of 12 to 36 months is refused with 400, 12 and 36 are accepted")
    void durationRule() {
        long tenant = tenant(9000);
        Expect.status(post("/api/leases", Bodies.lease(property(1000, "D"), tenant, 11)), 400);
        Expect.status(post("/api/leases", Bodies.lease(property(1000, "D"), tenant, 37)), 400);
        Expect.status(post("/api/leases", Bodies.lease(property(1000, "D"), tenant, 12)), 201);
        Expect.status(post("/api/leases", Bodies.lease(property(1000, "D"), tenant, 36)), 201);
    }

    @Order(5)
    @Test
    @DisplayName("a lease without property reference or without tenant is refused with 400")
    void missingFieldsRule() {
        long tenant = tenant(9000);
        Expect.status(post("/api/leases", "{\"tenantId\": " + tenant + ", \"durationMonths\": 24}"), 400);
        Expect.status(post("/api/leases",
                "{\"propertyReference\": \"P-960\", \"durationMonths\": 24}"), 400);
    }

    @Order(6)
    @Test
    @DisplayName("an unknown property reference returns 404")
    void unknownPropertyRule() {
        Response response = post("/api/leases", Bodies.lease("P-999", tenant(9000), 24));
        Expect.status(response, 404);
        Expect.number(Expect.object(response), "status", 404, response.request());
    }

    @Order(7)
    @Test
    @DisplayName("an unknown tenant id returns 404")
    void unknownTenantRule() {
        Response response = post("/api/leases", Bodies.lease(property(1000, "D"), 999999L, 24));
        Expect.status(response, 404);
        Expect.number(Expect.object(response), "status", 404, response.request());
    }

    @Order(8)
    @Test
    @DisplayName("a property that already has an active lease cannot be rented again: 409")
    void alreadyRentedRule() {
        String reference = property(1000, "D");
        Expect.status(post("/api/leases", Bodies.lease(reference, tenant(9000), 24)), 201);
        Response response = post("/api/leases", Bodies.lease(reference, tenant(9000), 24));
        Expect.status(response, 409);
    }

    @Order(9)
    @Test
    @DisplayName("a tenant who earns less than three times the rent is refused with 422")
    void incomeRule() {
        Response response = post("/api/leases", Bodies.lease(property(1000, "D"), tenant(2999), 24));
        Expect.status(response, 422);
        Expect.number(Expect.object(response), "status", 422, response.request());
    }

    @Order(10)
    @Test
    @DisplayName("an income of exactly three times the rent is accepted")
    void incomeLimitIsIncluded() {
        Response response = post("/api/leases", Bodies.lease(property(1000, "D"), tenant(3000), 24));
        Expect.status(response, 201);
    }

    @Order(11)
    @Test
    @DisplayName("the rules are checked in order: the state of the property comes before the income")
    void rulesOrder() {
        String reference = property(1000, "D");
        Expect.status(post("/api/leases", Bodies.lease(reference, tenant(9000), 24)), 201);
        Response response = post("/api/leases", Bodies.lease(reference, tenant(100), 24));
        Expect.statusIn(response, "(the rule on the property comes before the rule on the income)", 409);
    }

    @Order(12)
    @Test
    @DisplayName("a refused lease saves nothing")
    void refusedLeaseSavesNothing() {
        long before = schema().count("leases");
        Expect.status(post("/api/leases", Bodies.lease(property(1000, "D"), tenant(100), 24)), 422);
        Expect.status(post("/api/leases", Bodies.lease("P-999", tenant(9000), 24)), 404);
        long after = schema().count("leases");
        Expect.isTrue(before == after,
                "expected no new row in the table LEASES after two refused leases but the number of rows"
                        + " went from " + before + " to " + after);
    }

    @Order(13)
    @Test
    @DisplayName("GET /api/leases returns the leases sorted by id")
    void listLeases() {
        Expect.status(post("/api/leases", Bodies.lease(property(1000, "D"), tenant(9000), 24)), 201);
        Response response = get("/api/leases");
        Expect.status(response, 200);
        JsonNode leases = Expect.array(response);
        Expect.isTrue(leases.size() > 0, response.request() + ": expected at least one lease");
        Expect.ascending(Expect.numbers(leases, "id", response.request()), response.request());
    }

    @Order(14)
    @Test
    @DisplayName("GET /api/leases/{id} returns the lease, and 404 when the id is unknown")
    void readLease() {
        Response signed = post("/api/leases",
                Bodies.lease(property(1000, "D"), tenant(9000), 18));
        Expect.status(signed, 201);
        long id = Expect.number(Expect.object(signed), "id", signed.request());
        Response response = get("/api/leases/" + id);
        Expect.status(response, 200);
        Expect.number(Expect.object(response), "durationMonths", 18, response.request());
        Expect.status(get("/api/leases/999999"), 404);
    }

    @Order(15)
    @Test
    @DisplayName("the table LEASES points to a property and to a tenant")
    void leasesTable() {
        Expect.isTrue(schema().hasTable("leases"),
                "expected a table LEASES in the database but found " + schema().tables());
        List<String> keys = schema().foreignKeys("leases");
        Expect.isTrue(keys.contains("PROPERTYID -> PROPERTIES"),
                "expected a foreign key from LEASES to PROPERTIES but got " + keys);
        Expect.isTrue(keys.contains("TENANTID -> TENANTS"),
                "expected a foreign key from LEASES to TENANTS but got " + keys);
        for (String column : new String[] {"property_id", "tenant_id"}) {
            Schema.Column found = schema().column("leases", column);
            Expect.isTrue(!found.nullable(),
                    "expected the column " + found.name() + " of LEASES to be NOT NULL: a lease always"
                            + " links a property and a tenant");
        }
    }

    /** A new property, with a reference nobody else uses and no lease yet. */
    private String property(int monthlyRent, String energyClass) {
        String reference = REFS.next();
        Response created = post("/api/properties",
                Bodies.property(reference, "Angers", 45, monthlyRent, energyClass));
        Expect.status(created, 201);
        return reference;
    }

    private long tenant(int monthlyIncome) {
        Response response = post("/api/tenants",
                Bodies.tenant("Tenant Task13", Bodies.email("tenant-task13"), monthlyIncome));
        Expect.status(response, 201);
        return Expect.number(Expect.object(response), "id", response.request());
    }
}
