package com.holberton.realestate.checks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Bodies;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Reflect;
import com.holberton.realestate.checks.support.Response;

import tools.jackson.databind.JsonNode;

/** Task 4: the repository and the derived query methods.
 *
 * <p>The checks that create a property use their own references, in the P-92x and P-93x ranges.</p>
 */
class Task04Test extends ApiCheck {

    private static final String REPOSITORY = Reflect.PACKAGE + ".repository.PropertyRepository";

    @Order(1)
    @Test
    @DisplayName("PropertyRepository is an interface that extends JpaRepository")
    void repositoryExtendsJpaRepository() {
        Class<?> repository = Reflect.require(REPOSITORY);
        Expect.isTrue(repository.isInterface(), "expected PropertyRepository to be an interface");
        boolean extendsJpa = false;
        for (Class<?> parent : repository.getInterfaces()) {
            if (parent.getName().equals("org.springframework.data.jpa.repository.JpaRepository")) {
                extendsJpa = true;
            }
        }
        Expect.isTrue(extendsJpa,
                "expected PropertyRepository to extend JpaRepository<Property, Long>");
    }

    @Order(2)
    @Test
    @DisplayName("PropertyRepository declares findByReference and existsByReference")
    void repositoryQueryMethods() {
        Class<?> repository = Reflect.require(REPOSITORY);
        Expect.isTrue(Reflect.hasMethod(repository, "findByReference"),
                "expected the method Optional<Property> findByReference(String) in PropertyRepository");
        Expect.isTrue(Reflect.hasMethod(repository, "existsByReference"),
                "expected the method boolean existsByReference(String) in PropertyRepository");
    }

    @Order(3)
    @Test
    @DisplayName("the three sample properties are rows of the table PROPERTIES")
    void samplesAreInTheDatabase() {
        for (String reference : new String[] {"P-001", "P-002", "P-003"}) {
            Expect.isTrue(schema().countWhere("properties", "reference", reference) == 1,
                    "expected one row with REFERENCE = '" + reference + "' in the table PROPERTIES but got "
                            + schema().countWhere("properties", "reference", reference)
                            + ": the DataLoader must save the three sample properties exactly once");
        }
    }

    @Order(4)
    @Test
    @DisplayName("DataLoader is a @Component that implements CommandLineRunner")
    void dataLoaderIsAComponent() {
        Class<?> loader = Reflect.require(Reflect.PACKAGE + ".config.DataLoader");
        Expect.isTrue(Reflect.hasAnnotation(loader, "org.springframework.stereotype.Component"),
                "expected DataLoader to be annotated with @Component but its annotations are "
                        + Reflect.annotationNames(loader));
        Class<?> runner = Reflect.find("org.springframework.boot.CommandLineRunner").orElse(null);
        Expect.isTrue(runner != null && runner.isAssignableFrom(loader),
                "expected DataLoader to implement CommandLineRunner");
    }

    @Order(5)
    @Test
    @DisplayName("a property created through the API is saved in the database")
    void createGoesToTheDatabase() {
        Expect.status(post("/api/properties", Bodies.property("P-920", "Rennes", 48, 760, "C")), 201);
        try {
            Expect.isTrue(schema().countWhere("properties", "reference", "P-920") == 1,
                    "expected the created property P-920 to be a row of the table PROPERTIES");
            Expect.isTrue(schema().number(
                            "select monthly_rent from properties where reference = 'P-920'") == 760,
                    "expected MONTHLY_RENT = 760 in the row P-920 of the table PROPERTIES");
        } finally {
            delete("/api/properties/P-920");
        }
    }

    @Order(6)
    @Test
    @DisplayName("a property updated through the API is updated in the database")
    void updateGoesToTheDatabase() {
        Expect.status(post("/api/properties", Bodies.property("P-921", "Rennes", 48, 760, "C")), 201);
        try {
            Expect.status(put("/api/properties/P-921", Bodies.property("P-921", "Rennes", 48, 999, "C")), 200);
            Expect.isTrue(schema().number(
                            "select monthly_rent from properties where reference = 'P-921'") == 999,
                    "expected MONTHLY_RENT = 999 in the row P-921 after the PUT");
        } finally {
            delete("/api/properties/P-921");
        }
    }

    @Order(7)
    @Test
    @DisplayName("a property deleted through the API is removed from the database")
    void deleteGoesToTheDatabase() {
        Expect.status(post("/api/properties", Bodies.property("P-922", "Rennes", 48, 760, "C")), 201);
        Expect.status(delete("/api/properties/P-922"), 204);
        Expect.isTrue(schema().countWhere("properties", "reference", "P-922") == 0,
                "expected no row with REFERENCE = 'P-922' in the table PROPERTIES after the DELETE");
    }

    @Order(8)
    @Test
    @DisplayName("PropertyService uses the repository and keeps no list in memory")
    void serviceUsesTheRepository() {
        Class<?> service = Reflect.require(Reflect.PACKAGE + ".service.PropertyService");
        Expect.isTrue(Reflect.constructorTakes(service, REPOSITORY),
                "expected PropertyService to receive a PropertyRepository in its constructor");
        for (Field field : service.getDeclaredFields()) {
            Expect.isTrue(!Collection.class.isAssignableFrom(field.getType()),
                    "expected PropertyService to keep no list of properties but it declares the field "
                            + field.getName() + " of type " + field.getType().getSimpleName());
        }
    }

    @Order(9)
    @Test
    @DisplayName("PropertyController does not use the repository directly")
    void controllerDoesNotUseTheRepository() {
        Class<?> controller = Reflect.require(Reflect.PACKAGE + ".controller.PropertyController");
        Expect.isTrue(!Reflect.hasFieldOfType(controller, REPOSITORY)
                        && !Reflect.constructorTakes(controller, REPOSITORY),
                "expected PropertyController to call the service only, never PropertyRepository");
    }

    private static final String CITY = "Checkville";

    @BeforeEach
    void createTheProperties() {
        Expect.status(post("/api/properties", Bodies.property("P-930", CITY, 20, 500, "D")), 201);
        Expect.status(post("/api/properties", Bodies.property("P-931", CITY, 40, 1000, "C")), 201);
        Expect.status(post("/api/properties", Bodies.property("P-932", CITY, 60, 1500, "B")), 201);
    }

    @AfterEach
    void deleteTheProperties() {
        delete("/api/properties/P-930");
        delete("/api/properties/P-931");
        delete("/api/properties/P-932");
    }

    @Order(10)
    @Test
    @DisplayName("GET /api/properties?city= returns only the properties of this city")
    void filterByCity() {
        Response response = get("/api/properties?city=" + CITY);
        Expect.status(response, 200);
        JsonNode properties = Expect.array(response);
        Expect.size(properties, 3, response.request());
        Expect.isTrue(Expect.texts(properties, "reference", response.request())
                        .equals(List.of("P-930", "P-931", "P-932")),
                response.request() + ": expected exactly [P-930, P-931, P-932] but got "
                        + Expect.texts(properties, "reference", response.request()));
    }

    @Order(11)
    @Test
    @DisplayName("the search by city ignores the case")
    void filterByCityIgnoresCase() {
        Response response = get("/api/properties?city=" + CITY.toLowerCase());
        Expect.status(response, 200);
        List<String> references = Expect.texts(Expect.array(response), "reference", response.request());
        Expect.isTrue(references.equals(List.of("P-930", "P-931", "P-932")),
                response.request() + ": expected the same result as ?city=" + CITY
                        + " but got " + references);
    }

    @Order(12)
    @Test
    @DisplayName("GET /api/properties?maxRent= returns the properties at this rent or less")
    void filterByMaxRent() {
        Response response = get("/api/properties?maxRent=1000");
        Expect.status(response, 200);
        JsonNode properties = Expect.array(response);
        List<String> references = Expect.texts(properties, "reference", response.request());
        Expect.contains(references, "P-930", response.request());
        Expect.contains(references, "P-931", response.request() + " (1000 is included)");
        Expect.doesNotContain(references, "P-932", response.request());
        for (Integer rent : Expect.numbers(properties, "monthlyRent", response.request())) {
            Expect.isTrue(rent <= 1000,
                    response.request() + ": expected every rent to be 1000 or less but got " + rent);
        }
    }

    @Order(13)
    @Test
    @DisplayName("the parameters city and maxRent can be combined")
    void filterByCityAndMaxRent() {
        Response response = get("/api/properties?city=" + CITY + "&maxRent=1000");
        Expect.status(response, 200);
        List<String> references = Expect.texts(Expect.array(response), "reference", response.request());
        Expect.isTrue(references.equals(List.of("P-930", "P-931")),
                response.request() + ": expected exactly [P-930, P-931] but got " + references);
    }

    @Order(14)
    @Test
    @DisplayName("a search without result returns 200 with an empty list")
    void unknownCityReturnsAnEmptyList() {
        Response response = get("/api/properties?city=Nowhereville");
        Expect.status(response, 200);
        Expect.size(Expect.array(response), 0, response.request());
    }

    @Order(15)
    @Test
    @DisplayName("the result of a search is sorted by reference")
    void searchResultIsSorted() {
        Response response = get("/api/properties?maxRent=2000");
        Expect.status(response, 200);
        Expect.sorted(Expect.texts(Expect.array(response), "reference", response.request()),
                response.request());
    }

    @Order(16)
    @Test
    @DisplayName("GET /api/properties without parameter still returns every property")
    void noParameterReturnsEverything() {
        Response response = get("/api/properties");
        Expect.status(response, 200);
        List<String> references = Expect.texts(Expect.array(response), "reference", response.request());
        for (String reference : new String[] {"P-001", "P-002", "P-003", "P-930", "P-932"}) {
            Expect.contains(references, reference, response.request());
        }
    }

    @Order(17)
    @Test
    @DisplayName("the searches are done by the repository, not in Java")
    void searchesAreQueryMethods() {
        Class<?> repository = Reflect.require(Reflect.PACKAGE + ".repository.PropertyRepository");
        boolean byCity = false;
        boolean byRent = false;
        for (Method method : Reflect.ownMethods(repository)) {
            String name = method.getName().toLowerCase();
            boolean written = Reflect.hasAnnotation(method, "org.springframework.data.jpa.repository.Query");
            if (name.contains("city") || (written && name.contains("city"))) {
                byCity = true;
            }
            if (name.contains("rent") || (written && name.contains("rent"))) {
                byRent = true;
            }
        }
        Expect.isTrue(byCity,
                "expected a query method of PropertyRepository to search by city, such as"
                        + " findByAddressCityIgnoreCase: the database must do the filtering");
        Expect.isTrue(byRent,
                "expected a query method of PropertyRepository to search by maximum rent, such as"
                        + " findByMonthlyRentLessThanEqual: the database must do the filtering");
    }
}
