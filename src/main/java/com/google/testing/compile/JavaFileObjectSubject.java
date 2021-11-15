/*
 * Copyright (C) 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.testing.compile;

import com.google.common.io.ByteSource;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static com.google.common.truth.Fact.fact;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.asByteSource;
import static java.nio.charset.StandardCharsets.UTF_8;

/** Assertions about {@link JavaFileObject}s. */
public final class JavaFileObjectSubject extends Subject {

    private static final Subject.Factory<JavaFileObjectSubject, JavaFileObject> FACTORY =
            new JavaFileObjectSubjectFactory();

    /** Returns a {@link Subject.Factory} for {@link JavaFileObjectSubject}s. */
    public static Subject.Factory<JavaFileObjectSubject, JavaFileObject> javaFileObjects() {
        return FACTORY;
    }

    /** Starts making assertions about a {@link JavaFileObject}. */
    public static JavaFileObjectSubject assertThat(JavaFileObject actual) {
        return assertAbout(FACTORY).that(actual);
    }

    private final JavaFileObject actual;

    JavaFileObjectSubject(FailureMetadata failureMetadata, JavaFileObject actual) {
        super(failureMetadata, actual);
        this.actual = actual;
    }

    @Override
    protected String actualCustomStringRepresentation() {
        return actual.toUri().getPath();
    }

    /**
     * If {@code other} is a {@link JavaFileObject}, tests that their contents are equal. Otherwise
     * uses {@link Object#equals(Object)}.
     */
    @Override
    public void isEqualTo(Object other) {
        if (!(other instanceof JavaFileObject)) {
            super.isEqualTo(other);
            return;
        }

        JavaFileObject otherFile = (JavaFileObject) other;
        try {
            if (!asByteSource(actual).contentEquals(asByteSource(otherFile))) {
                failWithActual("expected to be equal to", other);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Asserts that the actual file's contents are equal to {@code expected}. */
    public void hasContents(ByteSource expected) {
        try {
            if (!asByteSource(actual).contentEquals(expected)) {
                failWithActual("expected to have contents", expected);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a {@link StringSubject} that makes assertions about the contents of the actual file as
     * a string.
     */
    public StringSubject contentsAsString(Charset charset) {
        try {
            return check("contents()")
                    .that(JavaFileObjects.asByteSource(actual).asCharSource(charset).read());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a {@link StringSubject} that makes assertions about the contents of the actual file as
     * a UTF-8 string.
     */
    public StringSubject contentsAsUtf8String() {
        return contentsAsString(UTF_8);
    }

    /**
     * <p><b>DO NOT USE</b>
     *
     * Always throws {@link UnsupportedOperationException}.
     *
     * @deprecated use {@link #hasExactContent(List)} instead
     */
    @Deprecated(forRemoval = true)
    public void hasSourceEquivalentTo(JavaFileObject expectedSource) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p><b>DO NOT USE</b>
     *
     * Always throws {@link UnsupportedOperationException}.
     *
     * @deprecated use {@link #containsLines(List)} instead
     */
    @Deprecated(forRemoval = true)
    public void containsElementsIn(JavaFileObject expectedPattern) {
        throw new UnsupportedOperationException();
    }

    /**
     * Asserts that the actual file contains exactly the same lines of code
     * as {@code expectedSource}.
     */
    public void hasExactContent(JavaFileObject expectedSource) {
        try {
            String[] expected = expectedSource.getCharContent(false)
                    .toString()
                    .split("\\R", -1);
            hasExactContent(expected);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Couldn't read from JavaFileObject when it was already in memory.", e);
        }
    }

    /**
     * Asserts that the actual file contains exactly the {@code expectedSource},
     * by comparing line by line.
     */
    public void hasExactContent(String... expected) {
        hasExactContent(Arrays.asList(expected));
    }

    /**
     * Asserts that the actual file contains exactly the {@code expectedSource},
     * by comparing line by line.
     */
    public void hasExactContent(List<String> expected) {
        try {
            List<String> actualList = Arrays.asList(actual.getCharContent(false)
                    .toString()
                    .split("\\R", -1));
            String diffReport = exactContentsReport(expected, actualList);
            if (diffReport == null) {
                return;
            }
            failWithoutActual(
                    fact("for file", actual.toUri().getPath()),
                    fact("diff", diffReport));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Couldn't read from JavaFileObject when it was already in memory.", e);
        }
    }

    /**
     * Asserts that the lines of the actual file contain the lines of
     * {@code expectedPattern} as a subsequence.
     */
    public void containsLines(JavaFileObject expectedPattern) {
        try {
            List<String> expected = Arrays.asList(expectedPattern.getCharContent(false)
                    .toString()
                    .split("\\R", -1));
            containsLines(expected);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Couldn't read from JavaFileObject when it was already in memory.", e);
        }
    }

    /**
     * Asserts that the lines of the actual file contain the lines of
     * {@code expectedPattern} as a subsequence.
     */
    public void containsLines(String... expectedPattern) {
        containsLines(Arrays.asList(expectedPattern));
    }

    /**
     * Asserts that the lines of the actual file contain the lines of
     * {@code expectedPattern} as a subsequence.
     */
    public void containsLines(List<String> expectedPattern) {
        try {
            List<String> actualList = Arrays.asList(this.actual.getCharContent(false)
                    .toString()
                    .split("\\R", -1));
            String diffReport = containsLinesInReport(expectedPattern, actualList);
            if (diffReport == null) {
                return;
            }
            failWithoutActual(
                    fact("for file", this.actual.toUri().getPath()),
                    fact("diff", diffReport));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Couldn't read from JavaFileObject when it was already in memory.", e);
        }
    }

    private String exactContentsReport(List<String> expected, List<String> actual) {
        boolean failure = false;
        int expectedIndex = 0;
        final int expectedSize = expected.size();
        final int actualSize = actual.size();
        if (expectedSize < actualSize) {
            return exactContentsReportShortExpectation(expected, actual);
        }
        for (; expectedIndex < expectedSize; expectedIndex++) {
            if (expectedIndex >= actualSize ||
                    !Objects.equals(actual.get(expectedIndex), expected.get(expectedIndex))) {
                failure = true;
                break;
            }
        }
        if (!failure) {
            return null;
        }
        String unmatchedToken = expected.get(expectedIndex);
        List<String> message = new ArrayList<>();
        message.add(String.format(
                "Unmatched token at index %d in expectation:",
                expectedIndex));
        message.add("    " + toStringLiteral(unmatchedToken));
        message.add("Expecting actual:");
        int unmatchedExpected = expectedIndex; // so we can use it in the lambda
        IntStream.range(0, actualSize)
                .mapToObj(i -> {
                    boolean isOffendingToken = i == unmatchedExpected;
                    String suffix = i == actualSize - 1 ? "" : ",";
                    if (isOffendingToken) {
                        suffix += " // actual";
                    }
                    return toStringLiteral(actual.get(i)) + suffix;
                })
                .forEach(message::add);
        message.add("to match exactly:");
        IntStream.range(0, expectedSize)
                .mapToObj(i -> {
                    boolean isOffendingToken = i == unmatchedExpected;
                    String prefix = isOffendingToken ? ">>> " : "    ";
                    String suffix = i == expectedSize - 1 ? "" : ",";
                    if (isOffendingToken) {
                        suffix += " // expectation";
                    }
                    return prefix + toStringLiteral(expected.get(i)) + suffix;
                })
                .forEach(message::add);
        return String.join("\n", message);
    }

    private String exactContentsReportShortExpectation(List<String> expected, List<String> actual) {
        boolean failure = false;
        int actualIndex = 0;
        final int expectedSize = expected.size();
        final int actualSize = actual.size();
        for (; actualIndex < actualSize; actualIndex++) {
            if (actualIndex >= expectedSize ||
                    !Objects.equals(actual.get(actualIndex), expected.get(actualIndex))) {
                failure = true;
                break;
            }
        }
        if (!failure) {
            return null;
        }
        String unmatchedActual = actual.get(actualIndex);
        List<String> message = new ArrayList<>();
        message.add(String.format(
                "Unmatched token at index %d in actual:",
                actualIndex));
        message.add("    " + toStringLiteral(unmatchedActual));
        message.add("Expecting actual:");
        int unmatchedIndex = actualIndex; // so we can use it in the lambda
        IntStream.range(0, actualSize)
                .mapToObj(i -> {
                    boolean isOffendingToken = i == unmatchedIndex;
                    String suffix = i == actualSize - 1 ? "" : ",";
                    if (isOffendingToken) {
                        suffix += " // actual";
                    }
                    return toStringLiteral(actual.get(i)) + suffix;
                })
                .forEach(message::add);
        message.add("to match exactly:");
        IntStream.range(0, expectedSize)
                .mapToObj(i -> {
                    boolean isOffendingToken = i == unmatchedIndex;
                    String prefix = isOffendingToken ? ">>> " : "    ";
                    String suffix = i == expectedSize - 1 ? "" : ",";
                    if (isOffendingToken) {
                        suffix += " // expectation";
                    }
                    return prefix + toStringLiteral(expected.get(i)) + suffix;
                })
                .forEach(message::add);
        return String.join("\n", message);
    }

    private String containsLinesInReport(List<String> subsequence, List<String> actual) {
        int index = 0;
        int actualIndex = 0;
        boolean failure = false;
        int subsequenceSize = subsequence.size();
        int actualSize = actual.size();
        outer:
        for (; index < subsequenceSize; index++) {
            String subsequenceToken = subsequence.get(index);
            for (int i = actualIndex; i < actualSize; i++) {
                if (Objects.equals(subsequenceToken, actual.get(i))) {
                    actualIndex = i + 1;
                    continue outer;
                }
            }
            failure = true;
            break;
        }
        if (!failure) {
            return null;
        }
        String unmatchedToken = subsequence.get(index);
        List<String> message = new ArrayList<>();
        message.add(String.format(
                "Failed to find token at subsequence index %d in actual:",
                index));
        message.add("    " + toStringLiteral(unmatchedToken));
        message.add("Expecting actual:");
        int mainIndex = actualIndex; // so we can use it in the lambda
        IntStream.range(0, actualSize)
                .mapToObj(i -> {
                    boolean isOffendingToken = i == mainIndex;
                    String suffix = i == actualSize - 1 ? "" : ",";
                    if (isOffendingToken) {
                        suffix += " // last match";
                    }
                    return toStringLiteral(actual.get(i)) + suffix;
                })
                .forEach(message::add);
        message.add("to contain subsequence:");
        int subsequenceIndex = index; // so we can use it in the lambda
        IntStream.range(0, subsequenceSize)
                .mapToObj(i -> {
                    boolean isOffendingToken = i == subsequenceIndex;
                    String prefix = isOffendingToken ? ">>> " : "    ";
                    String suffix = i == subsequenceSize - 1 ? "" : ",";
                    if (isOffendingToken) {
                        suffix += " <<<";
                    }
                    return prefix + toStringLiteral(subsequence.get(i)) + suffix;
                })
                .forEach(message::add);
        return String.join("\n", message);
    }

    private String toStringLiteral(String token) {
        return "\"" + token.replace("\"", "\\\"") + "\"";
    }
}
