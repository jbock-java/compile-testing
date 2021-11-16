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

import static com.google.common.truth.Fact.fact;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.asByteSource;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.ByteSource;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaFileObject;

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
     * a list of lines.
     */
    public IterableSubject contentsAsIterable(Charset charset) {
        try {
            return check("contents()")
                    .that(Arrays.asList(JavaFileObjects.asByteSource(actual).asCharSource(charset).read().split("\\R")));
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
     * Returns a {@link StringSubject} that makes assertions about the contents of the actual file as
     * a list of lines.
     */
    public IterableSubject contentsAsUtf8Iterable() {
        return contentsAsIterable(UTF_8);
    }

    /**
     * <p><b>DO NOT USE</b>
     *
     * <p>Always throws {@link UnsupportedOperationException}.
     *
     * @deprecated use {@link #containsLines(List)} or
     * {@link #containsLines(String...)} instead
     */
    @Deprecated(forRemoval = true)
    public void hasSourceEquivalentTo(JavaFileObject expectedSource) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p><b>DO NOT USE</b>
     *
     * <p>Always throws {@link UnsupportedOperationException}.
     *
     * @deprecated use {@link #containsLines(List)} or
     * {@link #containsLines(String...)} instead
     */
    @Deprecated(forRemoval = true)
    public void containsElementsIn(JavaFileObject expectedPattern) {
        throw new UnsupportedOperationException();
    }

    /**
     * Asserts that the lines of the actual file contain the lines of
     * {@code expectedPattern} as a subsequence.
     *
     * @deprecated it should not be necessary to create
     * a {@code JavaFileObject}, use {@link #containsLines(String...)} or
     * {@link #containsLines(List)} instead
     */
    @Deprecated
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
            SubsequenceChecker.checkSubsequence(actualList, expectedPattern)
                    .ifPresent(subsequenceReport -> failWithoutActual(
                            fact("for file", this.actual.toUri().getPath()),
                            fact("unmatched", subsequenceReport.getUnmatched()),
                            fact("actual", subsequenceReport.getActual()),
                            fact("subsequence", subsequenceReport.getSubsequence())));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Couldn't read from JavaFileObject when it was already in memory.", e);
        }
    }
}
