package com.holberton.realestate.checks.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * Prints the result of every check while it runs, then the score of each task and the total.
 *
 * <p>It is a JUnit extension, added to every check by {@link ApiCheck}: nothing to install,
 * and it never changes the result of a check, it only prints it.</p>
 */
public class CheckReport implements TestWatcher, AfterAllCallback {

    private static final String LINE = "--------------------------------------------------------------";
    private static final Map<String, int[]> SCORES = new LinkedHashMap<>();
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(CheckReport.class);
    private static String currentClass = "";

    @Override
    public void testSuccessful(ExtensionContext context) {
        record(context, true);
        print("[OK] " + context.getDisplayName());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        record(context, false);
        print("[KO] " + context.getDisplayName() + " -> " + reason(cause));
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        record(context, false);
        print("[KO] " + context.getDisplayName() + " -> stopped: " + reason(cause));
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        record(context, false);
        print("[--] " + context.getDisplayName() + " -> disabled");
    }

    @Override
    public void afterAll(ExtensionContext context) {
        String name = context.getRequiredTestClass().getSimpleName();
        int[] score = SCORES.get(name);
        if (score != null) {
            print(score[0] + "/" + score[1] + " tests passed");
        }
    }

    private synchronized void record(ExtensionContext context, boolean passed) {
        String name = context.getRequiredTestClass().getSimpleName();
        if (!name.equals(currentClass)) {
            currentClass = name;
            print("");
            print("--- " + name + " "
                    + LINE.substring(Math.min(LINE.length(), name.length() + 5)));
        }
        int[] score = SCORES.computeIfAbsent(name, key -> new int[2]);
        score[1]++;
        if (passed) {
            score[0]++;
        }
        context.getRoot().getStore(NAMESPACE)
                .getOrComputeIfAbsent("summary", key -> new Summary(), Summary.class);
    }

    /** The total is printed once, when JUnit closes its root store: at the very end of the run. */
    private static final class Summary implements AutoCloseable {

        @Override
        public void close() {
            printSummary();
        }
    }

    private static void printSummary() {
        if (SCORES.isEmpty()) {
            return;
        }
        int passed = 0;
        int total = 0;
        print("");
        print(LINE);
        print("  Spring Boot: Property Management API -- checks");
        print(LINE);
        for (Map.Entry<String, int[]> entry : new java.util.TreeMap<>(SCORES).entrySet()) {
            int[] score = entry.getValue();
            print(String.format("  %-24s %3d/%-3d", entry.getKey(), score[0], score[1]));
            passed += score[0];
            total += score[1];
        }
        print(LINE);
        print("  " + passed + "/" + total + " tests passed");
        print(LINE);
        print("");
    }

    private String reason(Throwable cause) {
        if (cause == null) {
            return "failed";
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = cause.getClass().getSimpleName();
            Throwable root = cause.getCause();
            if (root != null && root.getMessage() != null) {
                message = message + ": " + root.getMessage();
            }
        }
        String line = message.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        return line.length() > 300 ? line.substring(0, 300) + "..." : line;
    }

    private static void print(String message) {
        System.out.println(message);
    }
}
