package com.holberton.realestate.checks;

import java.util.List;
import java.util.Set;

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

/** Task 10: the tenants. */
class Task10Test extends ApiCheck {

    @Order(1)
    @Test
    @DisplayName("POST /api/tenants returns 201 with the tenant, its id and its income")
    void createTenant() {
        String email = Bodies.email("tenant-create");
        Response response = post("/api/tenants", Bodies.tenant("Olive Locataire", email, 2400));
        Expect.status(response, 201);
        JsonNode body = Expect.object(response);
        String what = response.request();
        Expect.text(body, "name", "Olive Locataire", what);
        Expect.text(body, "email", email, what);
        Expect.number(body, "monthlyIncome", 2400, what);
        Expect.isTrue(Expect.number(body, "id", what) > 0,
                what + ": expected a generated numeric id but got " + body);
    }

    @Order(2)
    @Test
    @DisplayName("POST /api/tenants sets the Location header to /api/tenants/{id}")
    void createTenantSetsLocation() {
        Response response = post("/api/tenants",
                Bodies.tenant("Paul Locataire", Bodies.email("tenant-location"), 3000));
        Expect.status(response, 201);
        long id = Expect.number(Expect.object(response), "id", response.request());
        String location = response.header("Location").orElse("");
        Expect.isTrue(location.endsWith("/api/tenants/" + id),
                response.request() + ": expected the header Location to end with /api/tenants/" + id
                        + " but got " + (location.isEmpty() ? "no Location header" : location));
    }

    @Order(3)
    @Test
    @DisplayName("GET /api/tenants/{id} returns this tenant")
    void readTenant() {
        long id = createTenant("Rosa Locataire", 2000);
        Response response = get("/api/tenants/" + id);
        Expect.status(response, 200);
        Expect.number(Expect.object(response), "monthlyIncome", 2000, response.request());
    }

    @Order(4)
    @Test
    @DisplayName("POST /api/tenants with an email already used returns 409")
    void duplicateEmail() {
        String email = Bodies.email("tenant-duplicate");
        Expect.status(post("/api/tenants", Bodies.tenant("Sam First", email, 2000)), 201);
        Response response = post("/api/tenants", Bodies.tenant("Sam Second", email, 2000));
        Expect.status(response, 409);
        Expect.number(Expect.object(response), "status", 409, response.request());
    }

    @Order(5)
    @Test
    @DisplayName("an invalid tenant is refused with 400 and the field in errors")
    void invalidTenant() {
        Response response = post("/api/tenants",
                Bodies.tenant("Tom Locataire", "not-an-email", 2000));
        Expect.status(response, 400);
        JsonNode errors = Expect.at(Expect.object(response), "errors", response.request());
        Expect.isTrue(errors.has("email"),
                response.request() + ": expected the key \"email\" in \"errors\" but got " + errors);
        Expect.status(post("/api/tenants", Bodies.tenant("  ", Bodies.email("tenant-blank"), 2000)), 400);
        Expect.status(post("/api/tenants", Bodies.tenant("Una Locataire",
                Bodies.email("tenant-income"), -1)), 400);
    }

    @Order(6)
    @Test
    @DisplayName("a refused tenant creation saves nothing")
    void refusedCreationSavesNothing() {
        String email = Bodies.email("tenant-refused");
        Expect.status(post("/api/tenants", Bodies.tenant("", email, 2000)), 400);
        Expect.isTrue(schema().countWhere("tenants", "email", email) == 0,
                "expected no row with EMAIL = '" + email + "' in TENANTS after a refused creation");
    }

    @Order(7)
    @Test
    @DisplayName("GET /api/tenants/{id} with an unknown id returns a 404 problem")
    void unknownTenant() {
        Response response = get("/api/tenants/999999");
        Expect.status(response, 404);
        Expect.number(Expect.object(response), "status", 404, response.request());
    }

    @Order(8)
    @Test
    @DisplayName("the table TENANTS has a unique email and no column that accepts null")
    void tenantsTable() {
        Expect.isTrue(schema().hasTable("tenants"),
                "expected a table TENANTS in the database but found " + schema().tables());
        Set<String> unique = schema().uniqueColumns("tenants");
        Expect.isTrue(unique.contains("EMAIL"),
                "expected a UNIQUE constraint on the column EMAIL of TENANTS but the unique columns are "
                        + unique);
        for (String column : new String[] {"name", "email", "monthly_income"}) {
            Schema.Column found = schema().column("tenants", column);
            Expect.isTrue(!found.nullable(),
                    "expected the column " + found.name() + " of TENANTS to be NOT NULL");
        }
    }

    @Order(9)
    @Test
    @DisplayName("Tenant is a separate entity: it does not extend Owner")
    void tenantIsNotAnOwner() {
        Class<?> tenant = Reflect.require(Reflect.PACKAGE + ".model.Tenant");
        Reflect.requireAnnotation(tenant, "jakarta.persistence.Entity");
        Class<?> owner = Reflect.find(Reflect.PACKAGE + ".model.Owner").orElse(null);
        Expect.isTrue(owner == null
                        || (!owner.isAssignableFrom(tenant) && !tenant.isAssignableFrom(owner)),
                "expected Tenant and Owner to be two separate entities, but one extends the other");
    }

    private long createTenant(String name, int income) {
        Response response = post("/api/tenants", Bodies.tenant(name, Bodies.email("tenant-task12"), income));
        Expect.status(response, 201);
        return Expect.number(Expect.object(response), "id", response.request());
    }
}
