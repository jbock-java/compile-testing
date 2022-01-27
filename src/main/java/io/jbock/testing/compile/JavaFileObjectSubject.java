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
package io.jbock.testing.compile;

import io.jbock.common.truth.FailureMetadata;
import io.jbock.common.truth.IterableSubject;
import io.jbock.common.truth.StringSubject;
import io.jbock.common.truth.Subject;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static io.jbock.common.truth.Fact.fact;
import static io.jbock.common.truth.Truth.assertAbout;
import static io.jbock.testing.compile.JavaFileObjects.asBytes;
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
        if (!Arrays.equals(asBytes(actual), asBytes(otherFile))) {
            failWithActual("expected to be equal to", other);
        }
    }

    /** Asserts that the actual file's contents are equal to {@code expected}. */
    public void hasContents(byte[] expected) {
        byte[] actualBytes = JavaFileObjects.asBytes(actual);
        if (!Arrays.equals(actualBytes, expected)) {
            failWithActual("expected to have contents", expected);
        }
    }

    public void hasContents(JavaFileObject javaFileObject) {
        hasContents(JavaFileObjects.asBytes(javaFileObject));
    }

    /**
     * Returns a {@link StringSubject} that makes assertions about the contents of the actual file as
     * a string.
     */
    public StringSubject contentsAsString(Charset charset) {
        return check("contents()")
                .that(new String(JavaFileObjects.asBytes(actual), charset));
    }

    /**
     * Returns a {@link StringSubject} that makes assertions about the contents of the actual file as
     * a list of lines.
     */
    public IterableSubject contentsAsIterable(Charset charset) {
        return check("contents()")
                .that(Arrays.asList(new String(JavaFileObjects.asBytes(actual), charset).split("\\R")));
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
