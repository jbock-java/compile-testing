package com.google.testing.compile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class Util {

    static <E> List<E> listOf(Iterable<? extends E> elements) {
        ArrayList<E> result = new ArrayList<>();
        for (E element : elements) {
            result.add(element);
        }
        return result;
    }

    static <E> Set<E> setOf(Iterable<? extends E> elements) {
        LinkedHashSet<E> result = new LinkedHashSet<>();
        for (E element : elements) {
            result.add(element);
        }
        return Collections.unmodifiableSet(result);
    }
}
