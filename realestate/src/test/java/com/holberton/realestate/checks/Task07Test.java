package com.holberton.realestate.checks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Bodies;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Reflect;
import com.holberton.realestate.checks.support.Response;

import tools.jackson.databind.JsonNode;

/** Task 7: the error responses. */
class Task07Test extends ApiCheck {

    @Order(1)
    @Test
    @DisplayName("a 404 is a ProblemDetail with status, title and detail")
    void notFoundIsAProblem() {
        Response response = get("/api/properties/P-999");
        Expect.status(response, 404);
        problem(response, 404);
    }

    @Order(2)
    @Test
    @DisplayName("a 409 is a ProblemDetail with status, title and detail")
    void conflictIsAProblem() {
        Expect.status(post("/api/properties", Bodies.property("P-950", "Caen", 30, 600, "D")), 201);
        try {
            Response response = post("/api/properties", Bodies.property("P-950", "Caen", 30, 600, "D"));
            Expect.status(response, 409);
            problem(response, 409);
        } finally {
            delete("/api/properties/P-950");
        }
    }

    @Order(3)
    @Test
    @DisplayName("the errors use the content type application/problem+json")
    void problemContentType() {
        Expect.problemContentType(get("/api/properties/P-999"));
    }

    @Order(4)
    @Test
    @DisplayName("a validation error returns a 400 problem with one key per invalid field in errors")
    void validationErrorsHaveKeys() {
        Response response = post("/api/properties",
                "{\"reference\": \"P-1\", \"address\": {\"street\": \"1 Rue des Tests\","
                        + " \"zipCode\": \"75001\", \"city\": \"Paris\"},"
                        + " \"surface\": 0, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
        Expect.status(response, 400);
        JsonNode body = problem(response, 400);
        JsonNode errors = Expect.at(body, "errors", response.request());
        Expect.isTrue(errors.isObject(),
                response.request() + ": expected \"errors\" to be a JSON object whose keys are the names"
                        + " of the invalid fields but got " + errors);
        for (String field : new String[] {"reference", "surface"}) {
            Expect.isTrue(errors.has(field),
                    response.request() + ": expected a key \"" + field + "\" in \"errors\" but got "
                            + errors.propertyNames());
            Expect.isTrue(!errors.get(field).asString("").isBlank(),
                    response.request() + ": expected a message for the field \"" + field + "\"");
        }
    }

    @Order(5)
    @Test
    @DisplayName("the key of an invalid field of the address is address.zipCode")
    void nestedValidationKey() {
        Response response = post("/api/properties",
                "{\"reference\": \"P-951\", \"address\": {\"street\": \"1 Rue des Tests\","
                        + " \"zipCode\": \"75\", \"city\": \"Paris\"},"
                        + " \"surface\": 30, \"monthlyRent\": 700, \"energyClass\": \"D\"}");
        Expect.status(response, 400);
        JsonNode errors = Expect.at(Expect.object(response), "errors", response.request());
        Expect.isTrue(errors.has("address.zipCode"),
                response.request() + ": expected the key \"address.zipCode\" in \"errors\" but got "
                        + errors.propertyNames());
    }

    @Order(6)
    @Test
    @DisplayName("a malformed JSON body returns a 400 problem")
    void malformedJson() {
        Response response = post("/api/properties", "{\"reference\": \"P-952\", ");
        Expect.status(response, 400);
        problem(response, 400);
    }

    @Order(7)
    @Test
    @DisplayName("an energy class that does not exist returns a 400 problem")
    void unknownEnergyClass() {
        Response response = post("/api/properties", Bodies.property("P-953", "Caen", 30, 600, "H"));
        if (response.status() == 201) {
            delete("/api/properties/P-953");
        }
        Expect.status(response, 400);
        problem(response, 400);
    }

    @Order(8)
    @Test
    @DisplayName("no error answers with a 500 or a stack trace")
    void noInternalError() {
        Response[] responses = {
            get("/api/properties/P-999"),
            delete("/api/properties/P-999"),
            put("/api/properties/P-999", Bodies.property("P-999", "Caen", 30, 600, "D")),
            post("/api/properties", "{\"oops\": true}"),
            post("/api/properties", "not json at all")
        };
        for (Response response : responses) {
            Expect.isTrue(response.status() != 500,
                    response.request() + ": expected a 4xx error but got 500 [body: "
                            + response.shortBody() + "]");
            Expect.isTrue(!response.body().contains("\"trace\"")
                            && !response.body().contains("at com.holberton"),
                    response.request() + ": expected no stack trace in the body but got "
                            + response.shortBody());
        }
    }

    @Order(9)
    @Test
    @DisplayName("the exceptions and the global handler are in the package exception")
    void exceptionClassesExist() {
        for (String name : new String[] {"PropertyNotFoundException", "DuplicateReferenceException"}) {
            Class<?> type = Reflect.require(Reflect.PACKAGE + ".exception." + name);
            Expect.isTrue(RuntimeException.class.isAssignableFrom(type),
                    "expected " + name + " to extend RuntimeException");
        }
        Class<?> handler = Reflect.require(Reflect.PACKAGE + ".exception.GlobalExceptionHandler");
        Expect.isTrue(Reflect.hasAnnotation(handler,
                        "org.springframework.web.bind.annotation.RestControllerAdvice")
                        || Reflect.hasAnnotation(handler,
                            "org.springframework.web.bind.annotation.ControllerAdvice"),
                "expected GlobalExceptionHandler to be annotated with @RestControllerAdvice but its"
                        + " annotations are " + Reflect.annotationNames(handler));
    }

    /** A problem must at least carry its status, a title and a detail. */
    private JsonNode problem(Response response, int status) {
        JsonNode body = Expect.object(response);
        String what = response.request();
        Expect.number(body, "status", status, what);
        Expect.isTrue(!Expect.at(body, "title", what).asString("").isBlank(),
                what + ": expected a non-empty \"title\" in the problem but got " + body);
        Expect.isTrue(!Expect.at(body, "detail", what).asString("").isBlank(),
                what + ": expected a non-empty \"detail\" in the problem but got " + body);
        return body;
    }
}
