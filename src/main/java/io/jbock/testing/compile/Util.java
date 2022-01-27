package io.jbock.testing.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class Util {

    static <E> List<E> listOf(Iterable<? extends E> elements) {
        if (elements instanceof List) {
            return (List) elements;
        }
        ArrayList<E> result = new ArrayList<>();
        for (E element : elements) {
            result.add(element);
        }
        return result;
    }

    static <E> Set<E> setOf(Iterable<? extends E> elements) {
        if (elements instanceof Set) {
            return (Set) elements;
        }
        LinkedHashSet<E> result = new LinkedHashSet<>();
        for (E element : elements) {
            result.add(element);
        }
        return Collections.unmodifiableSet(result);
    }

    static <E> Set<E> union(Set<E> set1, Set<E> set2) {
        Set<E> result = new LinkedHashSet<>(Math.max(4, (int) (1.5 * (set1.size() + set2.size()))));
        result.addAll(set1);
        result.addAll(set2);
        return result;
    }

    static <E> E getOnlyElement(Collection<E> collection) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException("Expecting exactly one element but found empty list");
        }
        if (collection.size() >= 2) {
            throw new IllegalArgumentException("Expecting exactly one element but found: " + collection);
        }
        return collection.iterator().next();
    }

    static <E> E getOnlyElement(Collection<? extends E> collection, E defaultValue) {
        if (collection.isEmpty()) {
            return defaultValue;
        }
        return getOnlyElement(collection);
    }

}
