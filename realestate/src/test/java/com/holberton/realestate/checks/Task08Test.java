package com.holberton.realestate.checks;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Bodies;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Response;
import com.holberton.realestate.checks.support.Schema;

import tools.jackson.databind.JsonNode;

/** Task 8: the owners. */
class Task08Test extends ApiCheck {

    @Order(1)
    @Test
    @DisplayName("POST /api/owners returns 201 with the owner and its generated id")
    void createOwner() {
        String email = Bodies.email("owner-create");
        Response response = post("/api/owners", Bodies.owner("Alice Martin", email));
        Expect.status(response, 201);
        JsonNode body = Expect.object(response);
        String what = response.request();
        Expect.text(body, "name", "Alice Martin", what);
        Expect.text(body, "email", email, what);
        Expect.isTrue(Expect.at(body, "id", what).isNumber() && Expect.number(body, "id", what) > 0,
                what + ": expected a generated numeric id in the response but got " + body);
    }

    @Order(2)
    @Test
    @DisplayName("POST /api/owners sets the Location header to /api/owners/{id}")
    void createOwnerSetsLocation() {
        Response response = post("/api/owners", Bodies.owner("Bob Dupont", Bodies.email("owner-location")));
        Expect.status(response, 201);
        long id = Expect.number(Expect.object(response), "id", response.request());
        String location = response.header("Location").orElse("");
        Expect.isTrue(location.endsWith("/api/owners/" + id),
                response.request() + ": expected the header Location to end with /api/owners/" + id
                        + " but got " + (location.isEmpty() ? "no Location header" : location));
    }

    @Order(3)
    @Test
    @DisplayName("GET /api/owners/{id} returns this owner")
    void readOwner() {
        String email = Bodies.email("owner-read");
        long id = createOwner("Carla Nunez", email);
        Response response = get("/api/owners/" + id);
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        Expect.number(body, "id", (int) id, response.request());
        Expect.text(body, "name", "Carla Nunez", response.request());
        Expect.text(body, "email", email, response.request());
    }

    @Order(4)
    @Test
    @DisplayName("GET /api/owners returns the owners sorted by name")
    void listOwners() {
        long second = createOwner("Zoe Bbb", Bodies.email("owner-list-2"));
        long first = createOwner("Zoe Aaa", Bodies.email("owner-list-1"));
        Response response = get("/api/owners");
        Expect.status(response, 200);
        JsonNode owners = Expect.array(response);
        List<String> names = Expect.texts(owners, "name", response.request());
        Expect.contains(names, "Zoe Aaa", response.request());
        Expect.contains(names, "Zoe Bbb", response.request());
        Expect.sorted(names, response.request());
        Expect.isTrue(names.indexOf("Zoe Aaa") < names.indexOf("Zoe Bbb"),
                response.request() + ": expected the owner " + first + " before the owner " + second
                        + " (sorted by name) but got " + names);
    }

    @Order(5)
    @Test
    @DisplayName("POST /api/owners with an email already used returns 409")
    void duplicateEmail() {
        String email = Bodies.email("owner-duplicate");
        createOwner("Dan First", email);
        Response response = post("/api/owners", Bodies.owner("Dan Second", email));
        Expect.status(response, 409);
        Expect.number(Expect.object(response), "status", 409, response.request());
    }

    @Order(6)
    @Test
    @DisplayName("POST /api/owners with an invalid email returns 400 with errors")
    void invalidEmail() {
        Response response = post("/api/owners", Bodies.owner("Eve Blanc", "not-an-email"));
        Expect.status(response, 400);
        JsonNode errors = Expect.at(Expect.object(response), "errors", response.request());
        Expect.isTrue(errors.has("email"),
                response.request() + ": expected the key \"email\" in \"errors\" but got " + errors);
    }

    @Order(7)
    @Test
    @DisplayName("POST /api/owners with a blank name or no email returns 400")
    void invalidName() {
        Expect.status(post("/api/owners", Bodies.owner("   ", Bodies.email("owner-blank"))), 400);
        Expect.status(post("/api/owners", "{\"name\": \"Fred Noel\"}"), 400);
    }

    @Order(8)
    @Test
    @DisplayName("GET /api/owners/{id} with an unknown id returns a 404 problem")
    void unknownOwner() {
        Response response = get("/api/owners/999999");
        Expect.status(response, 404);
        Expect.number(Expect.object(response), "status", 404, response.request());
    }

    @Order(9)
    @Test
    @DisplayName("a refused owner creation saves nothing")
    void refusedCreationSavesNothing() {
        String email = Bodies.email("owner-refused");
        Expect.status(post("/api/owners", Bodies.owner("", email)), 400);
        Expect.isTrue(schema().countWhere("owners", "email", email) == 0,
                "expected no row with EMAIL = '" + email + "' in the table OWNERS after a refused creation");
    }

    @Order(10)
    @Test
    @DisplayName("the table OWNERS has a unique email and no column that accepts null")
    void ownersTable() {
        Expect.isTrue(schema().hasTable("owners"),
                "expected a table OWNERS in the database but found " + schema().tables());
        Set<String> unique = schema().uniqueColumns("owners");
        Expect.isTrue(unique.contains("EMAIL"),
                "expected a UNIQUE constraint on the column EMAIL of OWNERS but the unique columns are "
                        + unique);
        for (String column : new String[] {"name", "email"}) {
            Schema.Column found = schema().column("owners", column);
            Expect.isTrue(!found.nullable(),
                    "expected the column " + found.name() + " of OWNERS to be NOT NULL");
        }
    }

    private long createOwner(String name, String email) {
        Response response = post("/api/owners", Bodies.owner(name, email));
        Expect.status(response, 201);
        return Expect.number(Expect.object(response), "id", response.request());
    }
}
