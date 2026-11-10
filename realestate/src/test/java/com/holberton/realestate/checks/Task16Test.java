package com.holberton.realestate.checks;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Response;

import tools.jackson.databind.JsonNode;

/** Task 16 (optional): the OpenAPI documentation. */
class Task16Test extends ApiCheck {

    @Order(1)
    @Test
    @DisplayName("GET /v3/api-docs returns the OpenAPI description of the API")
    void apiDocsIsServed() {
        Response response = get("/v3/api-docs");
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        Expect.isTrue(body.has("openapi"),
                response.request() + ": expected an \"openapi\" field in the description but got "
                        + response.shortBody());
    }

    @Order(2)
    @Test
    @DisplayName("the API is titled Property Management API, in version 1.0")
    void apiTitleAndVersion() {
        Response response = get("/v3/api-docs");
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        Expect.text(body, "info.title", "Property Management API", response.request());
        Expect.text(body, "info.version", "1.0", response.request());
    }

    @Order(3)
    @Test
    @DisplayName("the documentation lists the endpoints of the API")
    void apiDocsListsThePaths() {
        JsonNode paths = Expect.at(Expect.object(get("/v3/api-docs")), "paths", "GET /v3/api-docs");
        for (String path : new String[] {"/api/properties", "/api/owners", "/api/tenants", "/api/leases",
                "/api/reports/monthly", "/api/reports/cities"}) {
            Expect.isTrue(paths.has(path),
                    "GET /v3/api-docs: expected the path \"" + path + "\" in the documentation but got "
                            + keys(paths));
        }
    }

    @Order(4)
    @Test
    @DisplayName("the endpoints are grouped under the tags of the task")
    void apiDocsHasTags() {
        JsonNode paths = Expect.at(Expect.object(get("/v3/api-docs")), "paths", "GET /v3/api-docs");
        List<String> tags = new ArrayList<>();
        for (JsonNode operations : paths) {
            for (JsonNode operation : operations) {
                JsonNode found = operation.get("tags");
                if (found != null) {
                    for (JsonNode tag : found) {
                        tags.add(tag.asString(""));
                    }
                }
            }
        }
        for (String tag : new String[] {"Properties", "Owners", "Tenants", "Leases", "Reports"}) {
            Expect.isTrue(tags.contains(tag),
                    "GET /v3/api-docs: expected the tag \"" + tag + "\" on a controller (@Tag) but the"
                            + " tags of the documentation are " + tags.stream().distinct().toList());
        }
    }

    @Order(5)
    @Test
    @DisplayName("every endpoint of the API has a summary")
    void apiDocsHasSummaries() {
        JsonNode paths = Expect.at(Expect.object(get("/v3/api-docs")), "paths", "GET /v3/api-docs");
        List<String> missing = new ArrayList<>();
        for (String path : keys(paths)) {
            if (!path.startsWith("/api/") || path.startsWith("/api/hello")) {
                continue;
            }
            JsonNode operations = paths.get(path);
            for (String method : keys(operations)) {
                JsonNode summary = operations.get(method).get("summary");
                if (summary == null || summary.asString("").isBlank()) {
                    missing.add(method.toUpperCase() + " " + path);
                }
            }
        }
        Expect.isTrue(missing.isEmpty(),
                "GET /v3/api-docs: expected an @Operation(summary = \"...\") on every endpoint but these"
                        + " have none: " + missing);
    }

    @Order(6)
    @Test
    @DisplayName("Swagger UI is served by the application")
    void swaggerUiIsServed() {
        Response page = get("/swagger-ui/index.html");
        if (page.status() == 200) {
            return;
        }
        Response shortcut = get("/swagger-ui.html");
        Expect.isTrue(shortcut.status() == 200 || shortcut.status() == 302 || shortcut.status() == 301,
                "expected Swagger UI at /swagger-ui.html but got " + shortcut.status()
                        + " (and " + page.status() + " at /swagger-ui/index.html): is the dependency"
                        + " springdoc-openapi-starter-webmvc-ui in pom.xml?");
    }

    private List<String> keys(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.propertyNames().forEach(names::add);
        return names;
    }
}
