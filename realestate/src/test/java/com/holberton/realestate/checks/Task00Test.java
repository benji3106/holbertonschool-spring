package com.holberton.realestate.checks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Project;
import com.holberton.realestate.checks.support.Reflect;
import com.holberton.realestate.checks.support.Response;

/** Task 0: the project, the dependencies and the application that starts. */
class Task00Test extends ApiCheck {

    @Order(1)
    @Test
    @DisplayName("the application starts and the Spring context loads")
    void applicationStarts() {
        Expect.isTrue(context() != null, "the Spring context did not start");
        Expect.isTrue(context().getBeanDefinitionCount() > 0, "the Spring context contains no bean");
    }

    @Order(2)
    @Test
    @DisplayName("RealestateApplication is annotated with @SpringBootApplication")
    void mainClass() {
        Class<?> main = Reflect.require(Reflect.PACKAGE + ".RealestateApplication");
        Reflect.requireAnnotation(main, "org.springframework.boot.autoconfigure.SpringBootApplication");
        Expect.isTrue(Reflect.hasMethod(main, "main"),
                "expected RealestateApplication to have a main method");
    }

    @Order(3)
    @Test
    @DisplayName("pom.xml uses Spring Boot 4.1.x and Java 21")
    void bootAndJavaVersions() {
        String pom = Project.pom();
        Matcher parent = Pattern.compile("<parent>(.*?)</parent>").matcher(pom);
        Expect.isTrue(parent.find(), "expected a <parent> block in pom.xml (the one of Spring Initializr)");
        Matcher version = Pattern.compile("<version>([^<]+)</version>").matcher(parent.group(1));
        Expect.isTrue(version.find(), "expected a <version> in the <parent> block of pom.xml");
        String boot = version.group(1);
        Expect.isTrue(boot.startsWith("4.1."),
                "expected Spring Boot 4.1.x in pom.xml but got " + boot);
        Expect.isTrue(pom.contains("<java.version>21</java.version>")
                        || pom.contains("<maven.compiler.release>21</maven.compiler.release>")
                        || pom.contains("<release>21</release>"),
                "expected Java 21 in pom.xml (<java.version>21</java.version>)");
    }

    @Order(4)
    @Test
    @DisplayName("pom.xml pins the version of H2 to 2.3.232")
    void h2Version() {
        String pom = Project.pom();
        Expect.isTrue(pom.contains("<h2.version>2.3.232</h2.version>"),
                "expected <h2.version>2.3.232</h2.version> in the <properties> block of pom.xml:"
                        + " the most recent version of H2 has a bug that breaks the tests of the"
                        + " optional task 14");
    }

    @Order(5)
    @Test
    @DisplayName("pom.xml declares the group com.holberton and the artifact realestate")
    void coordinates() {
        String pom = Project.pom();
        Expect.isTrue(pom.contains("<groupId>com.holberton</groupId>"),
                "expected <groupId>com.holberton</groupId> in pom.xml");
        Expect.isTrue(pom.contains("<artifactId>realestate</artifactId>"),
                "expected <artifactId>realestate</artifactId> in pom.xml");
    }

    @Order(6)
    @Test
    @DisplayName("Spring Web, Spring Data JPA, H2 and Validation are available")
    void dependencies() {
        requireLibrary("org.springframework.web.bind.annotation.RestController", "Spring Web");
        requireLibrary("org.springframework.data.jpa.repository.JpaRepository", "Spring Data JPA");
        requireLibrary("org.h2.Driver", "H2 Database");
        requireLibrary("jakarta.validation.Valid", "Validation");
    }

    @Order(7)
    @Test
    @DisplayName("the Maven wrapper and README.md are in the project")
    void wrapperAndReadme() {
        Expect.isTrue(Project.exists("mvnw"), "expected the file mvnw at the root of the project");
        Expect.isTrue(Project.exists("mvnw.cmd"), "expected the file mvnw.cmd at the root of the project");
        Expect.isTrue(Project.exists(".mvn"), "expected the folder .mvn/ at the root of the project");
        Expect.isTrue(Project.exists("README.md"), "expected a README.md at the root of the project");
    }

    @Order(8)
    @Test
    @DisplayName("the embedded web server answers HTTP requests")
    void webServerAnswers() {
        Response response = get("/this-address-does-not-exist");
        Expect.isTrue(response.status() > 0,
                "the embedded web server did not answer: is Spring Web in pom.xml?");
        Expect.isTrue(response.status() == 404,
                "expected status 404 on an unknown address but got " + response.status());
    }

    private void requireLibrary(String className, String dependency) {
        Expect.isTrue(Reflect.find(className).isPresent(),
                "expected the dependency " + dependency + " in pom.xml: the class " + className
                        + " is not on the classpath");
    }
}
