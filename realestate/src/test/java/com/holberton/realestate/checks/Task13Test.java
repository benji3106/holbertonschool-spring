package com.holberton.realestate.checks;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Reflect;

/** Task 13: the two test classes written by the learner. */
class Task13Test extends ApiCheck {

    private static final String SERVICE_TEST = Reflect.PACKAGE + ".service.PropertyServiceTest";
    private static final String CONTROLLER_TEST = Reflect.PACKAGE + ".controller.PropertyControllerTest";

    @Order(1)
    @Test
    @DisplayName("PropertyServiceTest exists with at least 3 methods annotated with @Test")
    void propertyServiceTestExists() {
        requireTests(requireTestClass(SERVICE_TEST), 3);
    }

    @Order(2)
    @Test
    @DisplayName("PropertyServiceTest is a plain unit test: it does not start Spring")
    void propertyServiceTestStartsNoSpring() {
        Class<?> type = requireTestClass(SERVICE_TEST);
        for (String annotation : new String[] {"SpringBootTest", "WebMvcTest", "DataJpaTest"}) {
            Expect.isTrue(!Reflect.hasAnnotationNamed(type, annotation),
                    "expected PropertyServiceTest to be a plain unit test, with a mocked repository"
                            + " and without Spring, but it is annotated with @" + annotation);
        }
    }

    @Order(3)
    @Test
    @DisplayName("every test of PropertyServiceTest has a @DisplayName")
    void propertyServiceTestNamesItsTests() {
        Class<?> type = requireTestClass(SERVICE_TEST);
        for (Method method : type.getDeclaredMethods()) {
            if (Reflect.hasAnnotationNamed(method, "Test")) {
                Expect.isTrue(Reflect.hasAnnotationNamed(method, "DisplayName"),
                        "expected a @DisplayName describing the expected behavior on every @Test of"
                                + " PropertyServiceTest but the method " + method.getName()
                                + " does not have one");
            }
        }
    }

    @Order(4)
    @Test
    @DisplayName("PropertyControllerTest uses @WebMvcTest with at least 3 tests")
    void propertyControllerTestIsAWebSlice() {
        Class<?> type = requireTestClass(CONTROLLER_TEST);
        requireTests(type, 3);
        Expect.isTrue(Reflect.hasAnnotationNamed(type, "WebMvcTest"),
                "expected @WebMvcTest(PropertyController.class) on PropertyControllerTest but its"
                        + " annotations are " + Reflect.annotationNames(type));
    }

    @Order(5)
    @Test
    @DisplayName("PropertyControllerTest replaces the service with @MockitoBean")
    void propertyControllerTestMocksTheService() {
        Class<?> type = requireTestClass(CONTROLLER_TEST);
        Expect.isTrue(Reflect.hasFieldAnnotatedNamed(type, "MockitoBean"),
                "expected a field annotated with @MockitoBean in PropertyControllerTest to replace"
                        + " PropertyService (@MockBean no longer exists in Spring Boot 4)");
    }

    private Class<?> requireTestClass(String className) {
        return Reflect.find(className).orElseThrow(() -> new AssertionError(
                "expected the test class " + className + " (file src/test/java/"
                        + className.replace('.', '/') + ".java) but it does not exist"));
    }

    private void requireTests(Class<?> type, int minimum) {
        int tests = Reflect.testMethods(type);
        Expect.isTrue(tests >= minimum,
                "expected at least " + minimum + " methods annotated with @Test in "
                        + type.getSimpleName() + " but found " + tests);
    }
}
