package com.holberton.realestate.checks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.holberton.realestate.checks.support.ApiCheck;
import com.holberton.realestate.checks.support.Bodies;
import com.holberton.realestate.checks.support.Expect;
import com.holberton.realestate.checks.support.Reflect;
import com.holberton.realestate.checks.support.Refs;
import com.holberton.realestate.checks.support.Response;

import tools.jackson.databind.JsonNode;

/** Task 15 (optional): the reports computed by the database. */
class Task15Test extends ApiCheck {

    /** The properties that stay in the database, because they have a lease. */
    private static final Refs LEASED = Refs.block(997, 998);
    private static final List<String> FIGURES = List.of("propertyCount", "rentedCount", "occupancyRate",
            "totalRent", "monthlyIncome", "averageRent");

    @Order(1)
    @Test
    @DisplayName("GET /api/reports/monthly returns the six figures of the portfolio")
    void monthlyReportFields() {
        Response response = get("/api/reports/monthly");
        Expect.status(response, 200);
        JsonNode body = Expect.object(response);
        for (String figure : FIGURES) {
            Expect.isTrue(Expect.at(body, figure, response.request()).isNumber(),
                    response.request() + ": expected \"" + figure + "\" to be a number but got " + body);
        }
    }

    @Order(2)
    @Test
    @DisplayName("propertyCount and totalRent follow the properties of the portfolio")
    void monthlyReportTotals() {
        JsonNode before = report();
        create("P-990", "Angers", 1000);
        create("P-991", "Angers", 2000);
        try {
            JsonNode after = report();
            String what = "GET /api/reports/monthly after two properties of 1000 and 2000 were created";
            Expect.number(after, "propertyCount", Expect.number(before, "propertyCount", what) + 2, what);
            Expect.number(after, "totalRent", Expect.number(before, "totalRent", what) + 3000, what);
        } finally {
            delete("/api/properties/P-990");
            delete("/api/properties/P-991");
        }
    }

    @Order(3)
    @Test
    @DisplayName("averageRent is the average monthly rent, rounded down")
    void averageRent() {
        JsonNode before = report();
        int count = Expect.number(before, "propertyCount", "GET /api/reports/monthly");
        int total = Expect.number(before, "totalRent", "GET /api/reports/monthly");
        // a rent that makes the average a number with decimals: rounding down must show
        int rent = (total + 1000) % (count + 1) == 0 ? 1001 : 1000;
        create("P-992", "Angers", rent);
        try {
            JsonNode after = report();
            String what = "GET /api/reports/monthly";
            int newCount = Expect.number(after, "propertyCount", what);
            int newTotal = Expect.number(after, "totalRent", what);
            Expect.number(after, "averageRent", newTotal / newCount,
                    what + " (totalRent " + newTotal + " / propertyCount " + newCount
                            + ", rounded down)");
        } finally {
            delete("/api/properties/P-992");
        }
    }

    @Order(4)
    @Test
    @DisplayName("occupancyRate is the percentage of rented properties, rounded down")
    void occupancyRate() {
        rentOneProperty();
        List<String> extras = new ArrayList<>();
        try {
            // add properties until the percentage is not a round number, so that rounding shows
            String[] spare = {"P-993", "P-994", "P-995"};
            for (int index = 0; index < spare.length && isRound(); index++) {
                create(spare[index], "Angers", 1000);
                extras.add(spare[index]);
            }
            JsonNode report = report();
            String what = "GET /api/reports/monthly";
            int count = Expect.number(report, "propertyCount", what);
            int rented = Expect.number(report, "rentedCount", what);
            Expect.number(report, "occupancyRate", rented * 100 / count,
                    what + " (rentedCount " + rented + " of propertyCount " + count
                            + ", rounded down)");
        } finally {
            for (String reference : extras) {
                delete("/api/properties/" + reference);
            }
        }
    }

    @Order(5)
    @Test
    @DisplayName("rentedCount and monthlyIncome follow the active leases")
    void rentedCountAndIncome() {
        String reference = LEASED.next();
        create(reference, "Angers", 1200);
        JsonNode before = report();
        Response signed = post("/api/leases", Bodies.lease(reference, tenant(9000), 24));
        Expect.status(signed, 201);
        long lease = Expect.number(Expect.object(signed), "id", signed.request());

        JsonNode after = report();
        String what = "GET /api/reports/monthly after a lease of 1200 was signed";
        Expect.number(after, "rentedCount", Expect.number(before, "rentedCount", what) + 1, what);
        Expect.number(after, "monthlyIncome", Expect.number(before, "monthlyIncome", what) + 1200, what);

        Expect.status(post("/api/leases/" + lease + "/end", null), 200);
        JsonNode ended = report();
        String whatEnded = "GET /api/reports/monthly after the lease was ended";
        Expect.number(ended, "rentedCount", Expect.number(before, "rentedCount", whatEnded), whatEnded);
        Expect.number(ended, "monthlyIncome", Expect.number(before, "monthlyIncome", whatEnded),
                whatEnded);
    }

    @Order(6)
    @Test
    @DisplayName("GET /api/reports/cities returns one line per city, sorted by city name")
    void cityReportIsSorted() {
        Response response = get("/api/reports/cities");
        Expect.status(response, 200);
        JsonNode cities = Expect.array(response);
        List<String> names = Expect.texts(cities, "city", response.request());
        Expect.sorted(names, response.request());
        Expect.isTrue(names.size() == names.stream().distinct().count(),
                response.request() + ": expected one line per city but got " + names);
        Expect.contains(names, "Paris", response.request());
    }

    @Order(7)
    @Test
    @DisplayName("the report by city gives the number of properties and the average rent of the city")
    void cityReportFigures() {
        create("P-990", "Reportville", 1000);
        create("P-991", "Reportville", 1500);
        try {
            Response response = get("/api/reports/cities");
            Expect.status(response, 200);
            JsonNode line = Expect.element(Expect.array(response), "city", "Reportville",
                    response.request());
            String what = response.request() + " (Reportville: two properties of 1000 and 1500)";
            Expect.number(line, "propertyCount", 2, what);
            Expect.number(line, "averageRent", 1250, what);
        } finally {
            delete("/api/properties/P-990");
            delete("/api/properties/P-991");
        }
    }

    @Order(8)
    @Test
    @DisplayName("the figures are computed by the database with @Query")
    void figuresComeFromQueries() {
        int queries = 0;
        for (String name : new String[] {"PropertyRepository", "LeaseRepository"}) {
            Class<?> repository = Reflect.find(Reflect.PACKAGE + ".repository." + name).orElse(null);
            if (repository == null) {
                continue;
            }
            for (Method method : Reflect.ownMethods(repository)) {
                if (Reflect.hasAnnotation(method, "org.springframework.data.jpa.repository.Query")) {
                    queries++;
                }
            }
        }
        Expect.isTrue(queries > 0,
                "expected at least one @Query method in the repositories to compute the totals:"
                        + " the database must compute them, not a loop in Java");
    }

    @Order(9)
    @Test
    @DisplayName("ReportService and ReportController are in their own packages")
    void reportClasses() {
        Class<?> service = Reflect.require(Reflect.PACKAGE + ".service.ReportService");
        Reflect.requireAnnotation(service, "org.springframework.stereotype.Service");
        Class<?> controller = Reflect.require(Reflect.PACKAGE + ".controller.ReportController");
        Reflect.requireAnnotation(controller, "org.springframework.web.bind.annotation.RestController");
        Reflect.require(Reflect.PACKAGE + ".dto.MonthlyReportResponse");
        Reflect.require(Reflect.PACKAGE + ".dto.CityReportResponse");
    }

    private JsonNode report() {
        Response response = get("/api/reports/monthly");
        Expect.status(response, 200);
        return Expect.object(response);
    }

    /** True when the occupancy rate is a round percentage: rounding would not show. */
    private boolean isRound() {
        JsonNode report = report();
        int count = Expect.number(report, "propertyCount", "GET /api/reports/monthly");
        int rented = Expect.number(report, "rentedCount", "GET /api/reports/monthly");
        return count == 0 || rented * 100 % count == 0;
    }

    /** One more rented property, so that the occupancy rate is not zero. */
    private void rentOneProperty() {
        String reference = LEASED.next();
        create(reference, "Angers", 1000);
        Expect.status(post("/api/leases", Bodies.lease(reference, tenant(9000), 24)), 201);
    }

    private void create(String reference, String city, int monthlyRent) {
        delete("/api/properties/" + reference);
        Expect.status(post("/api/properties", Bodies.property(reference, city, 45, monthlyRent, "D")),
                201);
    }

    private long tenant(int monthlyIncome) {
        Response response = post("/api/tenants",
                Bodies.tenant("Tenant Task16", Bodies.email("tenant-task16"), monthlyIncome));
        Expect.status(response, 201);
        return Expect.number(Expect.object(response), "id", response.request());
    }
}
