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

import com.google.common.truth.ExpectFailure;
import com.google.common.truth.Truth;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.tools.JavaFileObject;

import java.util.Collections;

import static com.google.common.truth.ExpectFailure.assertThat;
import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.JavaFileObjectSubject.assertThat;
import static com.google.testing.compile.JavaFileObjectSubject.javaFileObjects;
import static java.nio.charset.StandardCharsets.UTF_8;

@RunWith(JUnit4.class)
public final class JavaFileObjectSubjectTest {
    @Rule
    public final ExpectFailure expectFailure = new ExpectFailure();

    private static final JavaFileObject CLASS =
            JavaFileObjects.forSourceLines(
                    "test.TestClass", //
                    "package test;",
                    "",
                    "public class TestClass {}");

    private static final JavaFileObject DIFFERENT_NAME =
            JavaFileObjects.forSourceLines(
                    "test.TestClass2", //
                    "package test;",
                    "",
                    "public class TestClass2 {}");

    private static final JavaFileObject CLASS_WITH_FIELD =
            JavaFileObjects.forSourceLines(
                    "test.TestClass", //
                    "package test;",
                    "",
                    "public class TestClass {",
                    "  Object field;",
                    "}");

    private static final JavaFileObject UNKNOWN_TYPES =
            JavaFileObjects.forSourceLines(
                    "test.TestClass",
                    "package test;",
                    "",
                    "public class TestClass {",
                    "  Bar badMethod(Baz baz) { return baz.what(); }",
                    "}");

    @Test
    public void hasContents() {
        assertThat(CLASS_WITH_FIELD).hasContents(JavaFileObjects.asByteSource(CLASS_WITH_FIELD));
    }

    @Test
    public void hasContents_failure() {
        expectFailure
                .whenTesting()
                .about(javaFileObjects())
                .that(CLASS_WITH_FIELD)
                .hasContents(JavaFileObjects.asByteSource(DIFFERENT_NAME));
        AssertionError expected = expectFailure.getFailure();
        assertThat(expected.getMessage()).contains(CLASS_WITH_FIELD.getName());
    }

    @Test
    public void contentsAsString() {
        assertThat(CLASS_WITH_FIELD).contentsAsString(UTF_8).containsMatch("Object +field;");
    }

    @Test
    public void contentsAsString_fail() {
        expectFailure
                .whenTesting()
                .about(javaFileObjects())
                .that(CLASS)
                .contentsAsString(UTF_8)
                .containsMatch("bad+");
        AssertionError expected = expectFailure.getFailure();
        assertThat(expected).factValue("value of").isEqualTo("javaFileObject.contents()");
        assertThat(expected).factValue("javaFileObject was").startsWith(CLASS.getName());
        assertThat(expected).factValue("expected to contain a match for").isEqualTo("bad+");
    }

    private static final JavaFileObject SAMPLE_ACTUAL_FILE_FOR_MATCHING =
            JavaFileObjects.forSourceLines(
                    "test.SomeFile",
                    "package test;",
                    "",
                    "import pkg.AnAnnotation;",
                    "import static another.something.Special.CONSTANT;",
                    "",
                    "@AnAnnotation(with = @Some(values = {1,2,3}), and = \"a string\")",
                    "public class SomeFile {",
                    "  private static final int CONSTANT_TIMES_2 = CONSTANT * 2;",
                    "  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;",
                    "  private static final int CONSTANT_TIMES_4 = CONSTANT * 4;",
                    "",
                    "  @Nullable private MaybeNull field;",
                    "",
                    "  @Inject SomeFile() {",
                    "    this.field = MaybeNull.constructorBody();",
                    "  }",
                    "",
                    "  protected int method(Parameter p, OtherParam o) {",
                    "    return CONSTANT_TIMES_4 / p.hashCode() + o.hashCode();",
                    "  }",
                    "",
                    "  public static class InnerClass {",
                    "    private static final int CONSTANT_TIMES_8 = CONSTANT_TIMES_4 * 2;",
                    "",
                    "    @Nullable private MaybeNull innerClassField;",
                    "",
                    "    @Inject",
                    "    InnerClass() {",
                    "      this.innerClassField = MaybeNull.constructorBody();",
                    "    }",
                    "",
                    "    protected int innerClassMethod(Parameter p, OtherParam o) {",
                    "      return CONSTANT_TIMES_8 / p.hashCode() + o.hashCode();",
                    "    }",
                    "  }",
                    "}");

    @Test
    public void hasExactContent_completeMatch() {
        assertThat(SAMPLE_ACTUAL_FILE_FOR_MATCHING).hasExactContent(SAMPLE_ACTUAL_FILE_FOR_MATCHING);
    }

    @Test
    public void hasExactContent_failOnEmpty() {
        expectFailure
                .whenTesting()
                .about(javaFileObjects())
                .that(CLASS)
                .hasExactContent(Collections.emptyList());
        AssertionError expected = expectFailure.getFailure();
        assertThat(expected).factKeys().contains("diff");
        assertThat(expected.getMessage()).contains("Unmatched token at index 0 in actual:");
        assertThat(expected.getMessage()).contains(">>> \"package test;\", <<<");
    }

    @Test
    public void hasExactContent_failOnDifferences() {
        expectFailure
                .whenTesting()
                .about(javaFileObjects())
                .that(CLASS)
                .hasExactContent(DIFFERENT_NAME);
        AssertionError expected = expectFailure.getFailure();
        assertThat(expected).factKeys().contains("diff");
        assertThat(expected.getMessage()).contains("Unmatched token at index 2 in expectation:");
        assertThat(expected.getMessage()).contains(">>> \"public class TestClass2 {}\" <<<");
    }

    @Test
    public void containsLines_completeMatch() {
        assertThat(SAMPLE_ACTUAL_FILE_FOR_MATCHING).containsLines(SAMPLE_ACTUAL_FILE_FOR_MATCHING);
    }

    @Test
    public void containsLines_failOnEmpty() {
        assertThat(SAMPLE_ACTUAL_FILE_FOR_MATCHING).containsLines(Collections.emptyList());
    }

    @Test
    public void containsLines_fail_longSubsequence() {
        expectFailure
                .whenTesting()
                .about(javaFileObjects())
                .that(UNKNOWN_TYPES)
                .containsLines(
                        "package test;",
                        "",
                        "// extra line",
                        "public class TestClass {",
                        "  Bar badMethod(Baz baz) { return baz.what(); }",
                        "}");
        AssertionError expected = expectFailure.getFailure();
        ExpectFailure.assertThat(expected).factKeys().contains("diff");
        Truth.assertThat(expected.getMessage()).contains("Failed to find token at subsequence index 2 in actual:");
        Truth.assertThat(expected.getMessage()).contains(">>> \"// extra line\", <<<");
    }

    @Test
    public void containsLines_fail_longSubsequenceTrailing() {
        expectFailure
                .whenTesting()
                .about(javaFileObjects())
                .that(UNKNOWN_TYPES)
                .containsLines(
                        "package test;",
                        "",
                        "public class TestClass {",
                        "  Bar badMethod(Baz baz) { return baz.what(); }",
                        "}",
                        "// extra line");
        AssertionError expected = expectFailure.getFailure();
        ExpectFailure.assertThat(expected).factKeys().contains("diff");
        Truth.assertThat(expected.getMessage()).contains("Failed to find token at subsequence index 5 in actual:");
        Truth.assertThat(expected.getMessage()).contains(">>> \"// extra line\" <<<");
    }

    @Test
    public void containsLinesIn_match() {
        assertThat(SAMPLE_ACTUAL_FILE_FOR_MATCHING).containsLines(
                "public class SomeFile {",
                "  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;",
                "  private static final int CONSTANT_TIMES_4 = CONSTANT * 4;",
                "}");
    }

    @Test
    public void containsLinesIn_failNoMatch() {
        expectFailure
                .whenTesting()
                .about(javaFileObjects())
                .that(SAMPLE_ACTUAL_FILE_FOR_MATCHING)
                .containsLines(
                        "public class SomeFile {",
                        "  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;",
                        "  private static final int CONSTANT_TIMES_4 = CONSTANT * 4;",
                        "  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;",
                        "}");
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage()).contains("Failed to find token at subsequence index 3 in actual:");
        assertThat(expected.getMessage()).contains(">>> \"  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;\", <<<");
    }
}
