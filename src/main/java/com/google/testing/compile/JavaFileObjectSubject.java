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
}
