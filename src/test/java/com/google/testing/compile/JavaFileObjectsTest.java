/*
 * Copyright (C) 2013 Google, Inc.
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

import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static javax.tools.JavaFileObject.Kind.CLASS;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *  Tests {@link JavaFileObjects}.
 *
 *  @author Gregory Kick
 */
class JavaFileObjectsTest {
    @Test
    void forResource_inJarFile() {
        JavaFileObject resourceInJar =
                JavaFileObjects.forResource("com/google/testing/compile/JavaFileObjectsTest.class");
        assertThat(resourceInJar.getKind()).isEqualTo(CLASS);
        assertThat(resourceInJar.toUri().getPath())
                .endsWith("/com/google/testing/compile/JavaFileObjectsTest.class");
        assertThat(resourceInJar.getName())
                .endsWith("/com/google/testing/compile/JavaFileObjectsTest.class");
        assertThat(resourceInJar.isNameCompatible("JavaFileObjectsTest", CLASS)).isTrue();
    }

    @Test
    void forSourceLines() throws IOException {
        JavaFileObject fileObject = JavaFileObjects.forSourceLines("example.HelloWorld",
                "package example;",
                "",
                "final class HelloWorld {",
                "  void sayHello() {",
                "    System.out.println(\"hello!\");",
                "  }",
                "}");
        assertThat(fileObject.getCharContent(false)).isEqualTo(
                "package example;\n"
                        + "\n"
                        + "final class HelloWorld {\n"
                        + "  void sayHello() {\n"
                        + "    System.out.println(\"hello!\");\n"
                        + "  }\n"
                        + "}");
    }

    @Test
    void forSourceLinesWithoutName() {
        try {
            JavaFileObjects.forSourceLines(
                    "package example;",
                    "",
                    "final class HelloWorld {",
                    "  void sayHello() {",
                    "    System.out.println(\"hello!\");",
                    "  }",
                    "}");
            fail("An exception should have been thrown.");
        } catch (IllegalArgumentException expected) {
        }
    }
}
