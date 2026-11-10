package com.holberton.realestate.checks.support;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hands out the references of the properties a check creates, one at a time.
 *
 * <p>Each task owns its own range in P-900..P-998, so the checks never fight over a reference,
 * and never touch P-001, P-002 and P-003. P-999 never exists: it is the unknown reference.</p>
 */
public final class Refs {

    private final int first;
    private final int last;
    private final AtomicInteger next;

    private Refs(int first, int last) {
        this.first = first;
        this.last = last;
        this.next = new AtomicInteger(first);
    }

    public static Refs block(int first, int last) {
        return new Refs(first, last);
    }

    /** A reference of this range that no other check has used yet. */
    public String next() {
        int number = next.getAndIncrement();
        if (number > last) {
            throw new AssertionError("the checks ran out of references in the range P-" + first
                    + "..P-" + last);
        }
        return "P-" + number;
    }
}
