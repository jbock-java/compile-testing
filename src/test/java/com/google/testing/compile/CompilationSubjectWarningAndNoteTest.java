package com.google.testing.compile;

import com.google.common.truth.Truth;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.tools.JavaFileObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.CompilationSubject.compilations;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link CompilationSubject}'s assertions about warnings and notes, for both successful
 * and unsuccessful compilations.
 */
final class CompilationSubjectWarningAndNoteTest {

    private static List<Arguments> sourceFiles() {
        return List.of(
                Arguments.arguments(CompilationSubjectTests.HELLO_WORLD),
                Arguments.arguments(CompilationSubjectTests.HELLO_WORLD_BROKEN));
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingInFileOnLineAtColumn(JavaFileObject sourceFile) {
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContaining("this is a message")
                .inFile(sourceFile)
                .onLine(6)
                .atColumn(8);
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContaining("this is a message")
                .inFile(sourceFile)
                .onLine(7)
                .atColumn(29);
    }

    /* TODO(dpb): Positive cases for onLineContaining for
     * (error, warning, note) x
     * (containing(String), containingMatch(String), containingMatch(Pattern)). */
    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingInFileOnLineContaining(JavaFileObject sourceFile) {
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContaining("this is a message")
                .inFile(sourceFile)
                .onLineContaining("class HelloWorld");
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContaining("this is a message")
                .inFile(sourceFile)
                .onLineContaining("Object foo");
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingMatch(JavaFileObject sourceFile) {
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContainingMatch("this is a? message")
                .inFile(sourceFile)
                .onLine(6)
                .atColumn(8);
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContainingMatch("(this|here) is a message")
                .inFile(sourceFile)
                .onLine(7)
                .atColumn(29);
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingMatch_pattern(JavaFileObject sourceFile) {
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContainingMatch(Pattern.compile("this is a? message"))
                .inFile(sourceFile)
                .onLine(6)
                .atColumn(8);
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContainingMatch(Pattern.compile("(this|here) is a message"))
                .inFile(sourceFile)
                .onLine(7)
                .atColumn(29);
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContaining_noSuchWarning(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                        .hadWarningContaining("what is it?"));
        assertThat(expected.getMessage())
                .startsWith("Expected a warning containing \"what is it?\", but only found:\n");
        // some versions of javac wedge the file and position in the middle
        assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingMatch_noSuchWarning(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                        .hadWarningContainingMatch("(what|where) is it?"));
        assertThat(expected.getMessage())
                .startsWith(
                        "Expected a warning containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingMatch_pattern_noSuchWarning(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                        .hadWarningContainingMatch(Pattern.compile("(what|where) is it?")));
        assertThat(expected.getMessage())
                .startsWith(
                        "Expected a warning containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingInFile_wrongFile(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                        .hadWarningContaining("this is a message")
                        .inFile(CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE));
        assertThat(expected.getMessage())
                .contains(
                        format(
                                "Expected a warning containing \"this is a message\" in %s",
                                CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE.getName()));
        assertThat(expected.getMessage()).contains(sourceFile.getName());
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingInFileOnLine_wrongLine(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                        .hadWarningContaining("this is a message")
                        .inFile(sourceFile)
                        .onLine(1));
        int actualErrorLine = 6;
        assertThat(expected.getMessage())
                .contains(
                        CompilationSubjectTests.lines(
                                format(
                                        "Expected a warning containing \"this is a message\" in %s on line:",
                                        sourceFile.getName()),
                                "   1: "));
        assertThat(expected.getMessage()).contains("" + actualErrorLine);
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingInFileOnLine_lineTooBig(JavaFileObject sourceFile) throws IOException {
        long lineCount = new BufferedReader(sourceFile.openReader(false)).lines().count();
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                                        .hadWarningContainingMatch("this is a+ message")
                                        .inFile(sourceFile)
                                        .onLine(100));
        assertThat(exception)
                .hasMessageThat()
                .isEqualTo("Invalid line number 100; number of lines is only " + lineCount);
    }

    /* TODO(dpb): Negative cases for onLineContaining for
     * (warning, error, note) x
     * (containing(String), containingMatch(String), containingMatch(Pattern)). */
    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteContainingInFileOnLineContaining_wrongLine(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                        .hadNoteContaining("this is a message")
                        .inFile(sourceFile)
                        .onLineContaining("package"));
        assertThat(expected.getMessage())
                .isEqualTo(
                        CompilationSubjectTests.lines(
                                format(
                                        "Expected a note containing \"this is a message\" in %s on line:",
                                        sourceFile.getName()),
                                "   1: package test;",
                                "but found it on line(s):",
                                "   6: public class HelloWorld {",
                                "   7:   @DiagnosticMessage Object foo;"));
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingMatchInFileOnLineContaining_noMatches(JavaFileObject sourceFile) {
        try {
            Truth.assertAbout(compilations())
                    .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                    .hadWarningContainingMatch("this is a+ message")
                    .inFile(sourceFile)
                    .onLineContaining("not found!");
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage())
                    .isEqualTo(format("No line in %s contained \"not found!\"", sourceFile.getName()));
        }
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingInFileOnLineContaining_moreThanOneMatch(JavaFileObject sourceFile) {
        try {
            Truth.assertAbout(compilations())
                    .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                    .hadWarningContainingMatch(Pattern.compile("this is ab? message"))
                    .inFile(sourceFile)
                    .onLineContaining("@DiagnosticMessage");
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage())
                    .isEqualTo(
                            CompilationSubjectTests.lines(
                                    format(
                                            "More than one line in %s contained \"@DiagnosticMessage\":",
                                            sourceFile.getName()),
                                    "   5: @DiagnosticMessage",
                                    "   7:   @DiagnosticMessage Object foo;"));
        }
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningContainingInFileOnLineAtColumn_wrongColumn(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                        .hadWarningContaining("this is a message")
                        .inFile(sourceFile)
                        .onLine(6)
                        .atColumn(1));
        int actualErrorCol = 8;
        assertThat(expected.getMessage())
                .contains(
                        format(
                                "Expected a warning containing \"this is a message\" in %s "
                                        + "at column 1 of line 6",
                                sourceFile.getName()));
        assertThat(expected.getMessage()).contains("[" + actualErrorCol + "]");
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningCount(JavaFileObject sourceFile) {
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile)).hadWarningCount(2);
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadWarningCount_wrongCount(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                        .hadWarningCount(42));
        assertThat(expected.getMessage())
                .contains("Expected 42 warnings, but found the following 2 warnings:\n");
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteContaining(JavaFileObject sourceFile) {
        assertThat(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContaining("this is a message")
                .inFile(sourceFile)
                .onLine(6)
                .atColumn(8);
        assertThat(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContaining("this is a message")
                .inFile(sourceFile)
                .onLine(7)
                .atColumn(29);
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteContainingMatch(JavaFileObject sourceFile) {
        assertThat(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContainingMatch("this is a? message")
                .inFile(sourceFile)
                .onLine(6)
                .atColumn(8);
        assertThat(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContainingMatch("(this|here) is a message")
                .inFile(sourceFile)
                .onLine(7)
                .atColumn(29);
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteContainingMatch_pattern(JavaFileObject sourceFile) {
        assertThat(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContainingMatch(Pattern.compile("this is a? message"))
                .inFile(sourceFile)
                .onLine(6)
                .atColumn(8);
        assertThat(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContainingMatch(Pattern.compile("(this|here) is a message"))
                .inFile(sourceFile)
                .onLine(7)
                .atColumn(29);
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteContaining_noSuchNote(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                        .hadNoteContaining("what is it?"));
        assertThat(expected.getMessage())
                .startsWith("Expected a note containing \"what is it?\", but only found:\n");
        // some versions of javac wedge the file and position in the middle
        assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteContainingMatch_noSuchNote(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                        .hadNoteContainingMatch("(what|where) is it?"));
        assertThat(expected.getMessage())
                .startsWith(
                        "Expected a note containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteContainingMatch_pattern_noSuchNote(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                        .hadNoteContainingMatch(Pattern.compile("(what|where) is it?")));
        assertThat(expected.getMessage())
                .startsWith(
                        "Expected a note containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteContainingInFile_wrongFile(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                        .hadNoteContaining("this is a message")
                        .inFile(CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE));
        assertThat(expected.getMessage())
                .contains(
                        format(
                                "Expected a note containing \"this is a message\" in %s",
                                CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE.getName()));
        assertThat(expected.getMessage()).contains(sourceFile.getName());
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteContainingInFileOnLine_wrongLine(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                        .hadNoteContaining("this is a message")
                        .inFile(sourceFile)
                        .onLine(1));
        int actualErrorLine = 6;
        assertThat(expected.getMessage())
                .contains(
                        CompilationSubjectTests.lines(
                                format(
                                        "Expected a note containing \"this is a message\" in %s on line:",
                                        sourceFile.getName()),
                                "   1: "));
        assertThat(expected.getMessage()).contains("" + actualErrorLine);
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteContainingInFileOnLineAtColumn_wrongColumn(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                        .hadNoteContaining("this is a message")
                        .inFile(sourceFile)
                        .onLine(6)
                        .atColumn(1));
        int actualErrorCol = 8;
        assertThat(expected.getMessage())
                .contains(
                        format(
                                "Expected a note containing \"this is a message\" in %s at column 1 of line 6",
                                sourceFile.getName()));
        assertThat(expected.getMessage()).contains("[" + actualErrorCol + "]");
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteCount(JavaFileObject sourceFile) {
        assertThat(CompilationSubjectTests.compilerWithNote().compile(sourceFile)).hadNoteCount(2);
    }

    @ParameterizedTest
    @MethodSource("sourceFiles")
    void hadNoteCount_wrongCount(JavaFileObject sourceFile) {
        AssertionError expected = assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                        .hadNoteCount(42));
        assertThat(expected.getMessage())
                .contains("Expected 42 notes, but found the following 2 notes:\n");
    }
}
