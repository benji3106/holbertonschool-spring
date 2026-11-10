package com.holberton.realestate.checks.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.sql.DataSource;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/**
 * The base class of every check: it starts the whole application on a free port,
 * then sends it real HTTP requests.
 *
 * <p>The checks are black-box: they never import a class of your application,
 * so a class that does not exist yet can never stop the other checks from compiling.</p>
 */
@ExtendWith(CheckReport.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.main.banner-mode=off",
    "spring.jpa.show-sql=false",
    "logging.level.root=WARN",
    "logging.level.org.springframework=WARN",
    "logging.level.org.hibernate=WARN"
})
public abstract class ApiCheck {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private DataSource dataSource;

    protected ApplicationContext context() {
        return context;
    }

    /** Access to the database through JDBC, to look at the schema Hibernate created. */
    protected Schema schema() {
        if (dataSource == null) {
            throw new AssertionError("no DataSource in the application: is spring-boot-starter-data-jpa in pom.xml?");
        }
        return new Schema(dataSource);
    }

    protected Response get(String path) {
        return send("GET", path, null);
    }

    protected Response post(String path, String jsonBody) {
        return send("POST", path, jsonBody);
    }

    protected Response put(String path, String jsonBody) {
        return send("PUT", path, jsonBody);
    }

    protected Response delete(String path) {
        return send("DELETE", path, null);
    }

    private Response send(String method, String path, String jsonBody) {
        HttpRequest.BodyPublisher content = jsonBody == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(jsonBody);
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json, application/problem+json, */*")
                .method(method, content);
        if (jsonBody != null) {
            request.header("Content-Type", "application/json");
        }
        try {
            HttpResponse<String> raw = CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(method, path, raw);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(method + " " + path + ": the request was interrupted");
        } catch (Exception exception) {
            throw new AssertionError(method + " " + path + ": the application did not answer ("
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage() + ")");
        }
    }
}
