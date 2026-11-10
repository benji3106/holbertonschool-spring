package com.holberton.realestate.checks.support;

import java.net.http.HttpResponse;
import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The answer of the application to one request, kept as text.
 * The checks never use the classes of the application to read a response:
 * they read the JSON, exactly like any other HTTP client would.
 */
public final class Response {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final String method;
    private final String path;
    private final HttpResponse<String> raw;

    Response(String method, String path, HttpResponse<String> raw) {
        this.method = method;
        this.path = path;
        this.raw = raw;
    }

    public int status() {
        return raw.statusCode();
    }

    public String body() {
        return raw.body() == null ? "" : raw.body();
    }

    /** "GET /api/properties", used as a prefix of every failure message. */
    public String request() {
        return method + " " + path;
    }

    public Optional<String> header(String name) {
        return raw.headers().firstValue(name);
    }

    public String contentType() {
        return header("Content-Type").orElse("");
    }

    /** The body as a short one-line extract, for the failure messages. */
    public String shortBody() {
        String text = body().replaceAll("\\s+", " ").trim();
        if (text.length() > 180) {
            text = text.substring(0, 180) + "...";
        }
        return text.isEmpty() ? "<empty body>" : text;
    }

    public JsonNode json() {
        String text = body();
        if (text.isBlank()) {
            throw new AssertionError(request() + ": expected a JSON body but the body is empty");
        }
        try {
            return MAPPER.readTree(text);
        } catch (RuntimeException exception) {
            throw new AssertionError(request() + ": expected a JSON body but got " + shortBody());
        }
    }
}
