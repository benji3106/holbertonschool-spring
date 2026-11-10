package com.holberton.realestate.checks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Reflect;
import com.holberton.realestate.checks.support.Response;

import tools.jackson.databind.JsonNode;

/** Task 1: the first controller. */
class Task01Test extends ApiCheck {

    @Order(1)
    @Test
    @DisplayName("GET /api/hello returns 200 with the message Hello, World!")
    void helloWorld() {
        Response response = get("/api/hello");
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        Expect.text(body, "message", "Hello, World!", response.request());
    }

    @Order(2)
    @Test
    @DisplayName("GET /api/hello?name=Alice greets Alice")
    void helloName() {
        Response response = get("/api/hello?name=Alice");
        Expect.status(response, 200);
        Expect.text(Expect.object(response), "message", "Hello, Alice!", response.request());
    }

    @Order(3)
    @Test
    @DisplayName("the parameter name is optional and can be any value")
    void helloOtherName() {
        Response response = get("/api/hello?name=Bob");
        Expect.status(response, 200);
        Expect.text(Expect.object(response), "message", "Hello, Bob!", response.request());
    }

    @Order(4)
    @Test
    @DisplayName("GET /api/hello answers with the content type application/json")
    void helloIsJson() {
        Response response = get("/api/hello");
        Expect.status(response, 200);
        Expect.jsonContentType(response);
    }

    @Order(5)
    @Test
    @DisplayName("HelloResponse is a record with one String component named message")
    void helloResponseIsARecord() {
        Class<?> type = Reflect.require(Reflect.PACKAGE + ".dto.HelloResponse");
        Expect.isTrue(type.isRecord(), "expected HelloResponse to be a record");
        Expect.isTrue(type.getRecordComponents().length == 1,
                "expected HelloResponse to have exactly one component but it has "
                        + type.getRecordComponents().length);
        Expect.isTrue("message".equals(type.getRecordComponents()[0].getName()),
                "expected the component of HelloResponse to be named message but got "
                        + type.getRecordComponents()[0].getName());
        Expect.isTrue(type.getRecordComponents()[0].getType() == String.class,
                "expected the component message to be a String but got "
                        + type.getRecordComponents()[0].getType().getSimpleName());
    }

    @Order(6)
    @Test
    @DisplayName("HelloController is a @RestController that returns a HelloResponse")
    void helloControllerIsARestController() {
        Class<?> controller = Reflect.require(Reflect.PACKAGE + ".controller.HelloController");
        Reflect.requireAnnotation(controller, "org.springframework.web.bind.annotation.RestController");
        boolean returnsDto = false;
        for (java.lang.reflect.Method method : controller.getDeclaredMethods()) {
            if (Reflect.mentions(method.getGenericReturnType(), Reflect.PACKAGE + ".dto.HelloResponse")) {
                returnsDto = true;
            }
        }
        Expect.isTrue(returnsDto,
                "expected a method of HelloController to return a HelloResponse, not a String built by hand");
    }
}
