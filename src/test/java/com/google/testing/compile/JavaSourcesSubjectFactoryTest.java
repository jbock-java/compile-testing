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

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.ExpectFailure.assertThat;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link JavaSourcesSubjectFactory} (and {@link JavaSourceSubjectFactory}).
 *
 * @author Gregory Kick
 */
class JavaSourcesSubjectFactoryTest {

    private static final JavaFileObject HELLO_WORLD_RESOURCE =
            JavaFileObjects.forResource("test/HelloWorld.java");

    private static final JavaFileObject HELLO_WORLD =
            JavaFileObjects.forSourceLines(
                    "test.HelloWorld",
                    "package test;",
                    "",
                    "import " + DiagnosticMessage.class.getCanonicalName() + ";",
                    "",
                    "@DiagnosticMessage",
                    "public class HelloWorld {",
                    "  @DiagnosticMessage Object foo;",
                    "}");

    private static final JavaFileObject HELLO_WORLD_BROKEN =
            JavaFileObjects.forSourceLines(
                    "test.HelloWorld",
                    "package test;",
                    "",
                    "import " + DiagnosticMessage.class.getCanonicalName() + ";",
                    "",
                    "@DiagnosticMessage",
                    "public class HelloWorld {",
                    "  @DiagnosticMessage Object foo;",
                    "  Bar noSuchClass;",
                    "}");

    @Test
    void compilesWithoutError() {
        assertAbout(javaSource()).that(HELLO_WORLD_RESOURCE).compilesWithoutError();
        assertAbout(javaSource())
                .that(
                        JavaFileObjects.forSourceLines(
                                "test.HelloWorld",
                                "package test;",
                                "",
                                "public class HelloWorld {",
                                "  public static void main(String[] args) {",
                                "    System.out.println(\"Hello World!\");",
                                "  }",
                                "}"))
                .compilesWithoutError();
    }

    @Test
    void compilesWithoutWarnings() {
        assertAbout(javaSource()).that(HELLO_WORLD).compilesWithoutWarnings();
    }

    @Test
    void compilesWithoutError_warnings() {
        assertAbout(javaSource())
                .that(HELLO_WORLD)
                .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                .compilesWithoutError()
                .withWarningContaining("this is a message")
                .in(HELLO_WORLD)
                .onLine(6)
                .atColumn(8)
                .and()
                .withWarningContaining("this is a message")
                .in(HELLO_WORLD)
                .onLine(7)
                .atColumn(29);
    }

    @Test
    void compilesWithoutWarnings_failsWithWarnings() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                        .compilesWithoutWarnings());
        assertThat(expected.getMessage())
                .contains("Expected 0 warnings, but found the following 2 warnings:\n");
    }

    @Test
    void compilesWithoutError_noWarning() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                        .compilesWithoutError()
                        .withWarningContaining("what is it?"));
        assertThat(expected.getMessage())
                .contains("Expected a warning containing \"what is it?\", but only found:\n");
        // some versions of javac wedge the file and position in the middle
        assertThat(expected).hasMessageThat().contains("this is a message\n");
    }

    @Test
    void compilesWithoutError_warningNotInFile() {
        JavaFileObject otherSource = HELLO_WORLD_RESOURCE;
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                        .compilesWithoutError()
                        .withWarningContaining("this is a message")
                        .in(otherSource));
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a warning containing \"this is a message\" in %s",
                                otherSource.getName()));
        assertThat(expected.getMessage()).contains(HELLO_WORLD.getName());
    }

    @Test
    void compilesWithoutError_warningNotOnLine() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                        .compilesWithoutError()
                        .withWarningContaining("this is a message")
                        .in(HELLO_WORLD)
                        .onLine(1));
        int actualErrorLine = 6;
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a warning containing \"this is a message\" in %s on line:\n   1: ",
                                HELLO_WORLD.getName()));
        assertThat(expected.getMessage()).contains("" + actualErrorLine);
    }

    @Test
    void compilesWithoutError_warningNotAtColumn() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                        .compilesWithoutError()
                        .withWarningContaining("this is a message")
                        .in(HELLO_WORLD)
                        .onLine(6)
                        .atColumn(1));
        int actualErrorCol = 8;
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a warning containing \"this is a message\" in %s at column 1 of line 6",
                                HELLO_WORLD.getName()));
        assertThat(expected.getMessage()).contains("[" + actualErrorCol + "]");
    }

    @Test
    void compilesWithoutError_wrongWarningCount() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                        .compilesWithoutError()
                        .withWarningCount(42));
        assertThat(expected.getMessage())
                .contains("Expected 42 warnings, but found the following 2 warnings:\n");
    }

    @Test
    void compilesWithoutError_notes() {
        assertAbout(javaSource())
                .that(HELLO_WORLD)
                .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE))
                .compilesWithoutError()
                .withNoteContaining("this is a message")
                .in(HELLO_WORLD)
                .onLine(6)
                .atColumn(8)
                .and()
                .withNoteContaining("this is a message")
                .in(HELLO_WORLD)
                .onLine(7)
                .atColumn(29)
                .and()
                .withNoteCount(2);
    }

    @Test
    void compilesWithoutError_noNote() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE))
                        .compilesWithoutError()
                        .withNoteContaining("what is it?"));
        assertThat(expected.getMessage())
                .contains("Expected a note containing \"what is it?\", but only found:\n");
        // some versions of javac wedge the file and position in the middle
        assertThat(expected).hasMessageThat().contains("this is a message\n");
    }

    @Test
    void compilesWithoutError_noteNotInFile() {
        JavaFileObject otherSource = HELLO_WORLD_RESOURCE;
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE))
                        .compilesWithoutError()
                        .withNoteContaining("this is a message")
                        .in(otherSource));
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a note containing \"this is a message\" in %s", otherSource.getName()));
        assertThat(expected.getMessage()).contains(HELLO_WORLD.getName());
    }

    @Test
    void compilesWithoutError_noteNotOnLine() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE))
                        .compilesWithoutError()
                        .withNoteContaining("this is a message")
                        .in(HELLO_WORLD)
                        .onLine(1));
        int actualErrorLine = 6;
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a note containing \"this is a message\" in %s on line:\n   1: ",
                                HELLO_WORLD.getName()));
        assertThat(expected.getMessage()).contains("" + actualErrorLine);
    }

    @Test
    void compilesWithoutError_noteNotAtColumn() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE))
                        .compilesWithoutError()
                        .withNoteContaining("this is a message")
                        .in(HELLO_WORLD)
                        .onLine(6)
                        .atColumn(1));
        int actualErrorCol = 8;
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a note containing \"this is a message\" in %s at column 1 of line 6",
                                HELLO_WORLD.getName()));
        assertThat(expected.getMessage()).contains("[" + actualErrorCol + "]");
    }

    @Test
    void compilesWithoutError_wrongNoteCount() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE))
                        .compilesWithoutError()
                        .withNoteCount(42));
        assertThat(expected.getMessage())
                .contains("Expected 42 notes, but found the following 2 notes:\n");
    }

    @Test
    void compilesWithoutError_failureReportsFiles() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_RESOURCE)
                        .processedWith(new FailingGeneratingProcessor())
                        .compilesWithoutError());
        assertThat(expected.getMessage()).contains("Compilation produced the following diagnostics:\n");
        assertThat(expected.getMessage()).contains(FailingGeneratingProcessor.GENERATED_CLASS_NAME);
        assertThat(expected.getMessage()).contains(FailingGeneratingProcessor.GENERATED_SOURCE);
    }

    @Test
    void compilesWithoutError_throws() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(JavaFileObjects.forResource("test/HelloWorld-broken.java"))
                        .compilesWithoutError());
        assertThat(expected)
                .hasMessageThat()
                .contains("Compilation produced the following" + " diagnostics:\n");
        assertThat(expected.getMessage()).contains("No files were generated.");
    }

    @Test
    void compilesWithoutError_exceptionCreatedOrPassedThrough() {
        RuntimeException e = new RuntimeException();
        try {
            assertAbout(javaSource())
                    .that(HELLO_WORLD_RESOURCE)
                    .processedWith(new ThrowingProcessor(e))
                    .compilesWithoutError();
            fail();
        } catch (CompilationFailureException expected) {
            // some old javacs don't pass through exceptions, so we create one
        } catch (RuntimeException expected) {
            // newer jdks throw a runtime exception whose cause is the original exception
            assertThat(expected.getCause()).isEqualTo(e);
        }
    }

    @Test
    void failsToCompile_throws() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_RESOURCE)
                        .failsToCompile());
        assertThat(expected.getMessage())
                .contains("Compilation was expected to fail, but contained no errors");
        assertThat(expected.getMessage()).contains("No files were generated.");
    }

    @Test
    void failsToCompile_throwsNoMessage() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_RESOURCE)
                        .processedWith(new ErrorProcessor())
                        .failsToCompile()
                        .withErrorContaining("some error"));
        assertThat(expected.getMessage())
                .contains("Expected an error containing \"some error\", but only found:\n");
        // some versions of javac wedge the file and position in the middle
        assertThat(expected).hasMessageThat().contains("expected error!\n");
    }

    @Test
    void failsToCompile_throwsNotInFile() {
        JavaFileObject fileObject = HELLO_WORLD_RESOURCE;
        JavaFileObject otherFileObject = JavaFileObjects.forResource("test/HelloWorld-different.java");
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(fileObject)
                        .processedWith(new ErrorProcessor())
                        .failsToCompile()
                        .withErrorContaining("expected error!")
                        .in(otherFileObject));
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected an error containing \"expected error!\" in %s",
                                otherFileObject.getName()));
        assertThat(expected.getMessage()).contains(fileObject.getName());
    }

    @Test
    void failsToCompile_throwsNotOnLine() {
        JavaFileObject fileObject = HELLO_WORLD_RESOURCE;
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(fileObject)
                        .processedWith(new ErrorProcessor())
                        .failsToCompile()
                        .withErrorContaining("expected error!")
                        .in(fileObject)
                        .onLine(1));
        int actualErrorLine = 18;
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected an error containing \"expected error!\" in %s on line:\n   1: ",
                                fileObject.getName()));
        assertThat(expected.getMessage()).contains("" + actualErrorLine);
    }

    @Test
    void failsToCompile_throwsNotAtColumn() {
        JavaFileObject fileObject = HELLO_WORLD_RESOURCE;
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(fileObject)
                        .processedWith(new ErrorProcessor())
                        .failsToCompile()
                        .withErrorContaining("expected error!")
                        .in(fileObject)
                        .onLine(18)
                        .atColumn(1));
        int actualErrorCol = 8;
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected an error containing \"expected error!\" in %s at column 1 of line 18",
                                fileObject.getName()));
        assertThat(expected.getMessage()).contains("" + actualErrorCol);
    }

    @Test
    void failsToCompile_wrongErrorCount() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_RESOURCE)
                        .processedWith(new ErrorProcessor())
                        .failsToCompile()
                        .withErrorCount(42));
        assertThat(expected.getMessage())
                .contains("Expected 42 errors, but found the following 2 errors:\n");
    }

    @Test
    void failsToCompile_noWarning() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_BROKEN)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                        .failsToCompile()
                        .withWarningContaining("what is it?"));
        assertThat(expected.getMessage())
                .contains("Expected a warning containing \"what is it?\", but only found:\n");
        // some versions of javac wedge the file and position in the middle
        assertThat(expected).hasMessageThat().contains("this is a message\n");
    }

    @Test
    void failsToCompile_warningNotInFile() {
        JavaFileObject otherSource = HELLO_WORLD_RESOURCE;
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_BROKEN)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                        .failsToCompile()
                        .withWarningContaining("this is a message")
                        .in(otherSource));
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a warning containing \"this is a message\" in %s",
                                otherSource.getName()));
        assertThat(expected.getMessage()).contains(HELLO_WORLD_BROKEN.getName());
    }

    @Test
    void failsToCompile_warningNotOnLine() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_BROKEN)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                        .failsToCompile()
                        .withWarningContaining("this is a message")
                        .in(HELLO_WORLD_BROKEN)
                        .onLine(1));
        int actualErrorLine = 6;
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a warning containing \"this is a message\" in %s on line:\n   1: ",
                                HELLO_WORLD_BROKEN.getName()));
        assertThat(expected.getMessage()).contains("" + actualErrorLine);
    }

    @Test
    void failsToCompile_warningNotAtColumn() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_BROKEN)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                        .failsToCompile()
                        .withWarningContaining("this is a message")
                        .in(HELLO_WORLD_BROKEN)
                        .onLine(6)
                        .atColumn(1));
        int actualErrorCol = 8;
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a warning containing \"this is a message\" in %s at column 1 of line 6",
                                HELLO_WORLD_BROKEN.getName()));
        assertThat(expected.getMessage()).contains("[" + actualErrorCol + "]");
    }

    @Test
    void failsToCompile_wrongWarningCount() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_BROKEN)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING))
                        .failsToCompile()
                        .withWarningCount(42));
        assertThat(expected.getMessage())
                .contains("Expected 42 warnings, but found the following 2 warnings:\n");
    }

    @Test
    void failsToCompile_noNote() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_BROKEN)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE))
                        .failsToCompile()
                        .withNoteContaining("what is it?"));
        assertThat(expected.getMessage())
                .contains("Expected a note containing \"what is it?\", but only found:\n");
        // some versions of javac wedge the file and position in the middle
        assertThat(expected).hasMessageThat().contains("this is a message\n");
    }

    @Test
    void failsToCompile_noteNotInFile() {
        JavaFileObject otherSource = HELLO_WORLD_RESOURCE;
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_BROKEN)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE))
                        .failsToCompile()
                        .withNoteContaining("this is a message")
                        .in(otherSource));
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a note containing \"this is a message\" in %s", otherSource.getName()));
        assertThat(expected.getMessage()).contains(HELLO_WORLD_BROKEN.getName());
    }

    @Test
    void failsToCompile_noteNotOnLine() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_BROKEN)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE))
                        .failsToCompile()
                        .withNoteContaining("this is a message")
                        .in(HELLO_WORLD_BROKEN)
                        .onLine(1));
        int actualErrorLine = 6;
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a note containing \"this is a message\" in %s on line:\n   1: ",
                                HELLO_WORLD_BROKEN.getName()));
        assertThat(expected.getMessage()).contains("" + actualErrorLine);
    }

    @Test
    void failsToCompile_noteNotAtColumn() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_BROKEN)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE))
                        .failsToCompile()
                        .withNoteContaining("this is a message")
                        .in(HELLO_WORLD_BROKEN)
                        .onLine(6)
                        .atColumn(1));
        int actualErrorCol = 8;
        assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a note containing \"this is a message\" in %s at column 1 of line 6",
                                HELLO_WORLD_BROKEN.getName()));
        assertThat(expected.getMessage()).contains("[" + actualErrorCol + "]");
    }

    @Test
    void failsToCompile_wrongNoteCount() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_BROKEN)
                        .processedWith(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE))
                        .failsToCompile()
                        .withNoteCount(42));
        assertThat(expected.getMessage())
                .contains("Expected 42 notes, but found the following 2 notes:\n");
    }

    @Test
    void failsToCompile() {
        JavaFileObject brokenFileObject = JavaFileObjects.forResource("test/HelloWorld-broken.java");
        assertAbout(javaSource())
                .that(brokenFileObject)
                .failsToCompile()
                .withErrorContaining("not a statement")
                .in(brokenFileObject)
                .onLine(23)
                .atColumn(5)
                .and()
                .withErrorCount(4);

        JavaFileObject happyFileObject = HELLO_WORLD_RESOURCE;
        assertAbout(javaSource())
                .that(happyFileObject)
                .processedWith(new ErrorProcessor())
                .failsToCompile()
                .withErrorContaining("expected error!")
                .in(happyFileObject)
                .onLine(18)
                .atColumn(8);
    }

    @Test
    void generatesFileNamed() {
        assertAbout(javaSource())
                .that(HELLO_WORLD_RESOURCE)
                .processedWith(new GeneratingProcessor())
                .compilesWithoutError()
                .and()
                .generatesFileNamed(CLASS_OUTPUT, "com.google.testing.compile", "Foo")
                .withContents(ByteSource.wrap("Bar".getBytes(UTF_8)));
    }

    @Test
    void generatesFileNamed_failOnFileExistence() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_RESOURCE)
                        .processedWith(new GeneratingProcessor())
                        .compilesWithoutError()
                        .and()
                        .generatesFileNamed(CLASS_OUTPUT, "com.google.testing.compile", "Bogus")
                        .withContents(ByteSource.wrap("Bar".getBytes(UTF_8))));
        assertThat(expected)
                .factValue("expected to generate file")
                .isEqualTo("/com/google/testing/compile/Bogus");
        assertThat(expected.getMessage()).contains(GeneratingProcessor.GENERATED_RESOURCE_NAME);
    }

    @Test
    void generatesFileNamed_failOnFileContents() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(javaSource())
                        .that(HELLO_WORLD_RESOURCE)
                        .processedWith(new GeneratingProcessor())
                        .compilesWithoutError()
                        .and()
                        .generatesFileNamed(CLASS_OUTPUT, "com.google.testing.compile", "Foo")
                        .withContents(ByteSource.wrap("Bogus".getBytes(UTF_8))));
        assertThat(expected.getMessage()).contains("Foo");
        assertThat(expected.getMessage()).contains(" have contents");
    }

    @Test
    void withStringContents() {
        assertAbout(javaSource())
                .that(HELLO_WORLD_RESOURCE)
                .processedWith(new GeneratingProcessor())
                .compilesWithoutError()
                .and()
                .generatesFileNamed(CLASS_OUTPUT, "com.google.testing.compile", "Foo")
                .withStringContents(UTF_8, "Bar");
    }

    @Test
    void passesOptions() {
        NoOpProcessor processor = new NoOpProcessor();
        assertAbout(javaSource())
                .that(HELLO_WORLD_RESOURCE)
                .withCompilerOptions("-Aa=1")
                .withCompilerOptions(ImmutableList.of("-Ab=2", "-Ac=3"))
                .processedWith(processor)
                .compilesWithoutError();
        assertThat(processor.options).containsEntry("a", "1");
        assertThat(processor.options).containsEntry("b", "2");
        assertThat(processor.options).containsEntry("c", "3");
        assertThat(processor.options).hasSize(3);
    }

    @Test
    void invokesMultipleProcesors() {
        NoOpProcessor noopProcessor1 = new NoOpProcessor();
        NoOpProcessor noopProcessor2 = new NoOpProcessor();
        assertThat(noopProcessor1.invoked).isFalse();
        assertThat(noopProcessor2.invoked).isFalse();
        assertAbout(javaSource())
                .that(HELLO_WORLD_RESOURCE)
                .processedWith(List.of(noopProcessor1, noopProcessor2))
                .compilesWithoutError();
        assertThat(noopProcessor1.invoked).isTrue();
        assertThat(noopProcessor2.invoked).isTrue();
    }

    @Test
    void invokesMultipleProcesors_asIterable() {
        NoOpProcessor noopProcessor1 = new NoOpProcessor();
        NoOpProcessor noopProcessor2 = new NoOpProcessor();
        assertThat(noopProcessor1.invoked).isFalse();
        assertThat(noopProcessor2.invoked).isFalse();
        assertAbout(javaSource())
                .that(HELLO_WORLD_RESOURCE)
                .processedWith(Arrays.asList(noopProcessor1, noopProcessor2))
                .compilesWithoutError();
        assertThat(noopProcessor1.invoked).isTrue();
        assertThat(noopProcessor2.invoked).isTrue();
    }
}
