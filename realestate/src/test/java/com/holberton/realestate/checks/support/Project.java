package com.holberton.realestate.checks.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads the files of the project itself: the Maven build is started from the folder that
 * contains pom.xml, so the checks can look at it.
 */
public final class Project {

    private Project() {
    }

    public static Path root() {
        Path here = Paths.get("").toAbsolutePath();
        for (Path candidate : new Path[] {here, here.getParent()}) {
            if (candidate != null && Files.exists(candidate.resolve("pom.xml"))) {
                return candidate;
            }
        }
        throw new AssertionError("could not find pom.xml from " + here
                + ": run the tests with ./mvnw from the folder that contains pom.xml");
    }

    public static boolean exists(String relativePath) {
        return Files.exists(root().resolve(relativePath));
    }

    public static String read(String relativePath) {
        Path file = root().resolve(relativePath);
        if (!Files.exists(file)) {
            throw new AssertionError("expected the file " + relativePath + " at the root of the project ("
                    + root() + ") but it does not exist");
        }
        try {
            return Files.readString(file);
        } catch (IOException exception) {
            throw new AssertionError("could not read " + relativePath + ": " + exception.getMessage());
        }
    }

    /** The pom.xml, with its comments and its blanks removed, to search inside it. */
    public static String pom() {
        return read("pom.xml").replaceAll("<!--.*?-->", "").replaceAll("\\s+", "");
    }
}
