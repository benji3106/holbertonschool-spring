package com.holberton.realestate.checks.support;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * The assertions of the checks. Every failure says what was expected and what was received.
 *
 * <p>They are permissive on purpose: the order of the fields of a JSON object never matters,
 * extra fields are always accepted, and a generated id is never compared to a fixed value.</p>
 */
public final class Expect {

    private Expect() {
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            fail(message);
        }
    }

    // ----- status codes and headers -----

    public static void status(Response response, int expected) {
        if (response.status() != expected) {
            fail(response.request() + ": expected status " + expected + " but got " + response.status()
                    + " [body: " + response.shortBody() + "]");
        }
    }

    public static void statusIn(Response response, String what, int... expected) {
        for (int candidate : expected) {
            if (response.status() == candidate) {
                return;
            }
        }
        StringBuilder list = new StringBuilder();
        for (int index = 0; index < expected.length; index++) {
            list.append(index == 0 ? "" : " or ").append(expected[index]);
        }
        fail(response.request() + ": expected status " + list + " " + what + " but got " + response.status()
                + " [body: " + response.shortBody() + "]");
    }

    public static void header(Response response, String name, String expected) {
        String actual = response.header(name).orElse(null);
        if (actual == null) {
            fail(response.request() + ": expected the header " + name + ": " + expected + " but there is no "
                    + name + " header");
        }
        if (!expected.equals(actual)) {
            fail(response.request() + ": expected the header " + name + ": " + expected + " but got " + actual);
        }
    }

    public static void jsonContentType(Response response) {
        String type = response.contentType();
        if (!type.contains("json")) {
            fail(response.request() + ": expected a JSON content type but got "
                    + (type.isEmpty() ? "none" : type));
        }
    }

    public static void problemContentType(Response response) {
        String type = response.contentType();
        if (!type.contains("application/problem+json")) {
            fail(response.request() + ": expected the content type application/problem+json but got "
                    + (type.isEmpty() ? "none" : type));
        }
    }

    public static void emptyBody(Response response) {
        if (!response.body().isBlank()) {
            fail(response.request() + ": expected an empty body but got " + response.shortBody());
        }
    }

    // ----- JSON objects and arrays -----

    /** The body of the response, which must be a JSON object. */
    public static JsonNode object(Response response) {
        JsonNode body = response.json();
        if (!body.isObject()) {
            fail(response.request() + ": expected a JSON object but got " + response.shortBody());
        }
        return body;
    }

    /** The body of the response, which must be a JSON array. */
    public static JsonNode array(Response response) {
        JsonNode body = response.json();
        if (!body.isArray()) {
            fail(response.request() + ": expected a JSON array but got " + response.shortBody());
        }
        return body;
    }

    /** The node at this path ("address.city"), or a failure saying which field is missing. */
    public static JsonNode at(JsonNode node, String path, String what) {
        JsonNode current = node;
        StringBuilder done = new StringBuilder();
        for (String step : path.split("\\.")) {
            if (current == null || !current.isObject()) {
                fail(what + ": expected an object at \"" + done + "\" to read \"" + path + "\" but got "
                        + shorten(current));
            }
            if (!current.has(step)) {
                fail(what + ": expected a field \"" + path + "\" but it is missing [got: " + shorten(node) + "]");
            }
            current = current.get(step);
            done.append(done.length() == 0 ? "" : ".").append(step);
        }
        return current;
    }

    public static void text(JsonNode node, String path, String expected, String what) {
        JsonNode value = at(node, path, what);
        if (!value.isString() || !expected.equals(value.stringValue())) {
            fail(what + ": expected \"" + path + "\" to be the text \"" + expected + "\" but got "
                    + shorten(value));
        }
    }

    public static void number(JsonNode node, String path, int expected, String what) {
        JsonNode value = at(node, path, what);
        if (!value.isNumber() || value.asInt() != expected) {
            fail(what + ": expected \"" + path + "\" to be " + expected + " but got " + shorten(value));
        }
    }

    public static int number(JsonNode node, String path, String what) {
        JsonNode value = at(node, path, what);
        if (!value.isNumber()) {
            fail(what + ": expected \"" + path + "\" to be a number but got " + shorten(value));
        }
        return value.asInt();
    }

    public static void bool(JsonNode node, String path, boolean expected, String what) {
        JsonNode value = at(node, path, what);
        if (!value.isBoolean() || value.asBoolean() != expected) {
            fail(what + ": expected \"" + path + "\" to be " + expected + " but got " + shorten(value));
        }
    }

    /** The field must be there, and must be null. */
    public static void isNull(JsonNode node, String path, String what) {
        JsonNode value = at(node, path, what);
        if (!value.isNull()) {
            fail(what + ": expected \"" + path + "\" to be null but got " + shorten(value));
        }
    }

    /** The field must not be in the JSON at all. */
    public static void absent(JsonNode node, String field, String what) {
        if (node.has(field)) {
            fail(what + ": expected no field \"" + field + "\" but got " + shorten(node.get(field)));
        }
    }

    public static void present(JsonNode node, String path, String what) {
        at(node, path, what);
    }

    /** The values of one field of every element of an array, in order. */
    public static List<String> texts(JsonNode array, String field, String what) {
        List<String> values = new ArrayList<>();
        for (JsonNode element : array) {
            JsonNode value = element.get(field);
            if (value == null) {
                fail(what + ": expected every element to have a field \"" + field + "\" but got "
                        + shorten(element));
            }
            values.add(value.isNull() ? "null" : value.asString());
        }
        return values;
    }

    public static List<Integer> numbers(JsonNode array, String field, String what) {
        List<Integer> values = new ArrayList<>();
        for (JsonNode element : array) {
            JsonNode value = element.get(field);
            if (value == null || !value.isNumber()) {
                fail(what + ": expected every element to have a number \"" + field + "\" but got "
                        + shorten(element));
            }
            values.add(value.asInt());
        }
        return values;
    }

    /** The element of an array whose field has this value, or a failure. */
    public static JsonNode element(JsonNode array, String field, String value, String what) {
        for (JsonNode element : array) {
            JsonNode candidate = element.get(field);
            if (candidate != null && value.equals(candidate.asString())) {
                return element;
            }
        }
        fail(what + ": expected an element with " + field + " = \"" + value + "\" but got "
                + texts(array, field, what));
        return null;
    }

    public static void contains(List<String> values, String expected, String what) {
        if (!values.contains(expected)) {
            fail(what + ": expected \"" + expected + "\" in the result but got " + values);
        }
    }

    public static void doesNotContain(List<String> values, String unexpected, String what) {
        if (values.contains(unexpected)) {
            fail(what + ": expected \"" + unexpected + "\" to be out of the result but got " + values);
        }
    }

    public static void size(JsonNode array, int expected, String what) {
        if (array.size() != expected) {
            fail(what + ": expected " + expected + " element(s) but got " + array.size()
                    + " " + shorten(array));
        }
    }

    /** The values must be sorted, ignoring the case (the order of the API is not case sensitive). */
    public static void sorted(List<String> values, String what) {
        for (int index = 1; index < values.size(); index++) {
            String previous = values.get(index - 1);
            String current = values.get(index);
            if (String.CASE_INSENSITIVE_ORDER.compare(previous, current) > 0) {
                fail(what + ": expected a sorted result but \"" + previous + "\" comes before \"" + current
                        + "\" in " + values);
            }
        }
    }

    public static void ascending(List<Integer> values, String what) {
        for (int index = 1; index < values.size(); index++) {
            if (values.get(index - 1) > values.get(index)) {
                fail(what + ": expected a sorted result but got " + values);
            }
        }
    }

    private static String shorten(JsonNode node) {
        if (node == null) {
            return "nothing";
        }
        String text = node.toString();
        return text.length() > 180 ? text.substring(0, 180) + "..." : text;
    }
}
