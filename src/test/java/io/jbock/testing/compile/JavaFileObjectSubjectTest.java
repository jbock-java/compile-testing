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

import io.jbock.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.Collections;

import static com.google.common.truth.ExpectFailure.assertThat;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static io.jbock.testing.compile.JavaFileObjectSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjectSubject.javaFileObjects;
import static java.nio.charset.StandardCharsets.UTF_8;

final class JavaFileObjectSubjectTest {

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
    void hasContents() {
        assertThat(CLASS_WITH_FIELD).hasContents(CLASS_WITH_FIELD);
    }

    @Test
    void hasContents_failure() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaFileObjects())
                        .that(CLASS_WITH_FIELD)
                        .hasContents(DIFFERENT_NAME));
        assertThat(expected.getMessage()).contains(CLASS_WITH_FIELD.getName());
    }

    @Test
    void contentsAsString() {
        assertThat(CLASS_WITH_FIELD).contentsAsString(UTF_8).containsMatch("Object +field;");
    }

    @Test
    void contentsAsString_fail() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaFileObjects())
                        .that(CLASS)
                        .contentsAsString(UTF_8)
                        .containsMatch("bad+"));
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
    void containsLines_completeMatch() {
        assertThat(SAMPLE_ACTUAL_FILE_FOR_MATCHING).containsLines(SAMPLE_ACTUAL_FILE_FOR_MATCHING);
    }

    @Test
    void containsLines_failOnEmpty() {
        assertThat(SAMPLE_ACTUAL_FILE_FOR_MATCHING).containsLines(Collections.emptyList());
    }

    @Test
    void containsLines_fail_longSubsequence() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaFileObjects())
                        .that(UNKNOWN_TYPES)
                        .containsLines(
                                "package test;",
                                "",
                                "// extra line",
                                "public class TestClass {",
                                "  Bar badMethod(Baz baz) { return baz.what(); }",
                                "}"));
        assertThat(expected.getMessage()).contains(String.join("\n", Arrays.asList(
                "for file:",
                "    test/TestClass.java",
                "unmatched:",
                "    2: \"// extra line\"",
                "actual:",
                "    \"package test;\", // 0",
                "    \"\", // 1, last match",
                "    \"public class TestClass {\",",
                "    \"  Bar badMethod(Baz baz) { return baz.what(); }\",",
                "    \"}\"",
                "subsequence:",
                "    \"package test;\", // 0",
                "    \"\", // 1",
                "    \"// extra line\", // no match",
                "    \"public class TestClass {\",",
                "    \"  Bar badMethod(Baz baz) { return baz.what(); }\",",
                "    \"}\"")));
    }

    @Test
    void containsLines_fail_longSubsequenceTrailing() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaFileObjects())
                        .that(UNKNOWN_TYPES)
                        .containsLines(
                                "package test;",
                                "",
                                "public class TestClass {",
                                "  Bar badMethod(Baz baz) { return baz.what(); }",
                                "}",
                                "// extra line"));
        assertThat(expected.getMessage()).contains(String.join("\n", Arrays.asList(
                "for file:",
                "    test/TestClass.java",
                "unmatched:",
                "    5: \"// extra line\"",
                "actual:",
                "    \"package test;\", // 0",
                "    \"\", // 1",
                "    \"public class TestClass {\", // 2",
                "    \"  Bar badMethod(Baz baz) { return baz.what(); }\", // 3",
                "    \"}\" // 4, last match",
                "subsequence:",
                "    \"package test;\", // 0",
                "    \"\", // 1",
                "    \"public class TestClass {\", // 2",
                "    \"  Bar badMethod(Baz baz) { return baz.what(); }\", // 3",
                "    \"}\", // 4",
                "    \"// extra line\" // no match")));
    }

    @Test
    void containsLinesIn_match() {
        assertThat(SAMPLE_ACTUAL_FILE_FOR_MATCHING).containsLines(
                "public class SomeFile {",
                "  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;",
                "  private static final int CONSTANT_TIMES_4 = CONSTANT * 4;",
                "}");
    }

    @Test
    void containsLinesIn_failNoMatch() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaFileObjects())
                        .that(SAMPLE_ACTUAL_FILE_FOR_MATCHING)
                        .containsLines(
                                "public class SomeFile {",
                                "  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;",
                                "  private static final int CONSTANT_TIMES_4 = CONSTANT * 4;",
                                "  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;",
                                "}"));
        assertThat(expected.getMessage()).contains(String.join("\n", Arrays.asList(
                "unmatched:",
                "    3: \"  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;\"",
                "actual:",
                "    \"package test;\",",
                "    \"\",",
                "    \"import pkg.AnAnnotation;\",",
                "    \"import static another.something.Special.CONSTANT;\",",
                "    \"\",",
                "    \"@AnAnnotation(with = @Some(values = {1,2,3}), and = \\\"a string\\\")\",",
                "    \"public class SomeFile {\", // 0",
                "    \"  private static final int CONSTANT_TIMES_2 = CONSTANT * 2;\",",
                "    \"  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;\", // 1",
                "    \"  private static final int CONSTANT_TIMES_4 = CONSTANT * 4;\", // 2, last match",
                "    \"\",",
                "    \"  @Nullable private MaybeNull field;\",",
                "    \"\",",
                "    \"  @Inject SomeFile() {\",",
                "    \"    this.field = MaybeNull.constructorBody();\",",
                "    \"  }\",",
                "    \"\",",
                "    \"  protected int method(Parameter p, OtherParam o) {\",",
                "    \"    return CONSTANT_TIMES_4 / p.hashCode() + o.hashCode();\",",
                "    \"  }\",",
                "    \"\",",
                "    \"  public static class InnerClass {\",",
                "    \"    private static final int CONSTANT_TIMES_8 = CONSTANT_TIMES_4 * 2;\",",
                "    \"\",",
                "    \"    @Nullable private MaybeNull innerClassField;\",",
                "    \"\",",
                "    \"    @Inject\",",
                "    \"    InnerClass() {\",",
                "    \"      this.innerClassField = MaybeNull.constructorBody();\",",
                "    \"    }\",",
                "    \"\",",
                "    \"    protected int innerClassMethod(Parameter p, OtherParam o) {\",",
                "    \"      return CONSTANT_TIMES_8 / p.hashCode() + o.hashCode();\",",
                "    \"    }\",",
                "    \"  }\",",
                "    \"}\"",
                "subsequence:",
                "    \"public class SomeFile {\", // 6",
                "    \"  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;\", // 8",
                "    \"  private static final int CONSTANT_TIMES_4 = CONSTANT * 4;\", // 9",
                "    \"  private static final int CONSTANT_TIMES_3 = CONSTANT * 3;\", // no match",
                "    \"}\"")));
    }
}
