package com.google.testing.compile;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.ExpectFailure;
import com.google.common.truth.Truth;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.tools.JavaFileObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.CompilationSubject.compilations;
import static java.lang.String.format;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

/**
 * Tests for {@link CompilationSubject}'s assertions about warnings and notes, for both successful
 * and unsuccessful compilations.
 */
@RunWith(Parameterized.class)
public final class CompilationSubjectWarningAndNoteTest {
    @Rule
    public final ExpectFailure expectFailure = new ExpectFailure();
    private final JavaFileObject sourceFile;

    @Parameterized.Parameters
    public static ImmutableList<Object[]> parameters() {
        return ImmutableList.copyOf(
                new Object[][]{
                        {CompilationSubjectTests.HELLO_WORLD}, {CompilationSubjectTests.HELLO_WORLD_BROKEN},
                });
    }

    public CompilationSubjectWarningAndNoteTest(JavaFileObject sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Test
    public void hadWarningContainingInFileOnLineAtColumn() {
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
    @Test
    public void hadWarningContainingInFileOnLineContaining() {
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContaining("this is a message")
                .inFile(sourceFile)
                .onLineContaining("class HelloWorld");
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContaining("this is a message")
                .inFile(sourceFile)
                .onLineContaining("Object foo");
    }

    @Test
    public void hadWarningContainingMatch() {
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

    @Test
    public void hadWarningContainingMatch_pattern() {
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

    @Test
    public void hadWarningContaining_noSuchWarning() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContaining("what is it?");
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .startsWith("Expected a warning containing \"what is it?\", but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @Test
    public void hadWarningContainingMatch_noSuchWarning() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContainingMatch("(what|where) is it?");
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .startsWith(
                        "Expected a warning containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @Test
    public void hadWarningContainingMatch_pattern_noSuchWarning() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContainingMatch(Pattern.compile("(what|where) is it?"));
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .startsWith(
                        "Expected a warning containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @Test
    public void hadWarningContainingInFile_wrongFile() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContaining("this is a message")
                .inFile(CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE);
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a warning containing \"this is a message\" in %s",
                                CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE.getName()));
        Truth.assertThat(expected.getMessage()).contains(sourceFile.getName());
    }

    @Test
    public void hadWarningContainingInFileOnLine_wrongLine() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContaining("this is a message")
                .inFile(sourceFile)
                .onLine(1);
        AssertionError expected = expectFailure.getFailure();
        int actualErrorLine = 6;
        Truth.assertThat(expected.getMessage())
                .contains(
                        CompilationSubjectTests.lines(
                                format(
                                        "Expected a warning containing \"this is a message\" in %s on line:",
                                        sourceFile.getName()),
                                "   1: "));
        Truth.assertThat(expected.getMessage()).contains("" + actualErrorLine);
    }

    @Test
    public void hadWarningContainingInFileOnLine_lineTooBig() throws IOException {
        long lineCount = new BufferedReader(sourceFile.openReader(false)).lines().count();
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                                        .hadWarningContainingMatch("this is a+ message")
                                        .inFile(sourceFile)
                                        .onLine(100));
        Truth.assertThat(exception)
                .hasMessageThat()
                .isEqualTo("Invalid line number 100; number of lines is only " + lineCount);
    }

    /* TODO(dpb): Negative cases for onLineContaining for
     * (warning, error, note) x
     * (containing(String), containingMatch(String), containingMatch(Pattern)). */
    @Test
    public void hadNoteContainingInFileOnLineContaining_wrongLine() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContaining("this is a message")
                .inFile(sourceFile)
                .onLineContaining("package");
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
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

    @Test
    public void hadWarningContainingMatchInFileOnLineContaining_noMatches() {
        try {
            Truth.assertAbout(compilations())
                    .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                    .hadWarningContainingMatch("this is a+ message")
                    .inFile(sourceFile)
                    .onLineContaining("not found!");
            fail();
        } catch (IllegalArgumentException expected) {
            Truth.assertThat(expected.getMessage())
                    .isEqualTo(format("No line in %s contained \"not found!\"", sourceFile.getName()));
        }
    }

    @Test
    public void hadWarningContainingInFileOnLineContaining_moreThanOneMatch() {
        try {
            Truth.assertAbout(compilations())
                    .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                    .hadWarningContainingMatch(Pattern.compile("this is ab? message"))
                    .inFile(sourceFile)
                    .onLineContaining("@DiagnosticMessage");
            fail();
        } catch (IllegalArgumentException expected) {
            Truth.assertThat(expected.getMessage())
                    .isEqualTo(
                            CompilationSubjectTests.lines(
                                    format(
                                            "More than one line in %s contained \"@DiagnosticMessage\":",
                                            sourceFile.getName()),
                                    "   5: @DiagnosticMessage",
                                    "   7:   @DiagnosticMessage Object foo;"));
        }
    }

    @Test
    public void hadWarningContainingInFileOnLineAtColumn_wrongColumn() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningContaining("this is a message")
                .inFile(sourceFile)
                .onLine(6)
                .atColumn(1);
        AssertionError expected = expectFailure.getFailure();
        int actualErrorCol = 8;
        Truth.assertThat(expected.getMessage())
                .contains(
                        format(
                                "Expected a warning containing \"this is a message\" in %s "
                                        + "at column 1 of line 6",
                                sourceFile.getName()));
        Truth.assertThat(expected.getMessage()).contains("[" + actualErrorCol + "]");
    }

    @Test
    public void hadWarningCount() {
        assertThat(CompilationSubjectTests.compilerWithWarning().compile(sourceFile)).hadWarningCount(2);
    }

    @Test
    public void hadWarningCount_wrongCount() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithWarning().compile(sourceFile))
                .hadWarningCount(42);
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .contains("Expected 42 warnings, but found the following 2 warnings:\n");
    }

    @Test
    public void hadNoteContaining() {
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

    @Test
    public void hadNoteContainingMatch() {
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

    @Test
    public void hadNoteContainingMatch_pattern() {
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

    @Test
    public void hadNoteContaining_noSuchNote() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContaining("what is it?");
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .startsWith("Expected a note containing \"what is it?\", but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @Test
    public void hadNoteContainingMatch_noSuchNote() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContainingMatch("(what|where) is it?");
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .startsWith(
                        "Expected a note containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @Test
    public void hadNoteContainingMatch_pattern_noSuchNote() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContainingMatch(Pattern.compile("(what|where) is it?"));
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .startsWith(
                        "Expected a note containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("this is a message\n");
    }

    @Test
    public void hadNoteContainingInFile_wrongFile() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContaining("this is a message")
                .inFile(CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE);
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected a note containing \"this is a message\" in %s",
                                CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE.getName()));
        Truth.assertThat(expected.getMessage()).contains(sourceFile.getName());
    }

    @Test
    public void hadNoteContainingInFileOnLine_wrongLine() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContaining("this is a message")
                .inFile(sourceFile)
                .onLine(1);
        AssertionError expected = expectFailure.getFailure();
        int actualErrorLine = 6;
        Truth.assertThat(expected.getMessage())
                .contains(
                        CompilationSubjectTests.lines(
                                format(
                                        "Expected a note containing \"this is a message\" in %s on line:",
                                        sourceFile.getName()),
                                "   1: "));
        Truth.assertThat(expected.getMessage()).contains("" + actualErrorLine);
    }

    @Test
    public void hadNoteContainingInFileOnLineAtColumn_wrongColumn() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteContaining("this is a message")
                .inFile(sourceFile)
                .onLine(6)
                .atColumn(1);
        AssertionError expected = expectFailure.getFailure();
        int actualErrorCol = 8;
        Truth.assertThat(expected.getMessage())
                .contains(
                        format(
                                "Expected a note containing \"this is a message\" in %s at column 1 of line 6",
                                sourceFile.getName()));
        Truth.assertThat(expected.getMessage()).contains("[" + actualErrorCol + "]");
    }

    @Test
    public void hadNoteCount() {
        assertThat(CompilationSubjectTests.compilerWithNote().compile(sourceFile)).hadNoteCount(2);
    }

    @Test
    public void hadNoteCount_wrongCount() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithNote().compile(sourceFile))
                .hadNoteCount(42);
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .contains("Expected 42 notes, but found the following 2 notes:\n");
    }
}
