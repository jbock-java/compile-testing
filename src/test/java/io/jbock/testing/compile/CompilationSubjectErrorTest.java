package io.jbock.testing.compile;

import com.google.common.truth.Truth;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static com.google.common.truth.Truth.assertAbout;
import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.CompilationSubject.compilations;
import static io.jbock.testing.compile.Compiler.javac;

/** Tests for {@link CompilationSubject}'s assertions about errors. */
final class CompilationSubjectErrorTest {

    @Test
    void hadErrorContaining() {
        assertThat(javac().compile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE))
                .hadErrorContaining("not a statement")
                .inFile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE)
                .onLine(23)
                .atColumn(5);
        assertThat(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .hadErrorContaining("expected error!")
                .inFile(CompilationSubjectTests.HELLO_WORLD_RESOURCE)
                .onLine(18)
                .atColumn(8);
    }

    @Test
    void hadErrorContainingMatch() {
        assertThat(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE))
                .hadErrorContainingMatch("not+ +a? statement")
                .inFile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE)
                .onLine(23)
                .atColumn(5);
        assertThat(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .hadErrorContainingMatch("(wanted|expected) error!")
                .inFile(CompilationSubjectTests.HELLO_WORLD_RESOURCE)
                .onLine(18)
                .atColumn(8);
    }

    @Test
    void hadErrorContainingMatch_pattern() {
        assertThat(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE))
                .hadErrorContainingMatch("not+ +a? statement")
                .inFile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE)
                .onLine(23)
                .atColumn(5);
        assertThat(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .hadErrorContainingMatch("(wanted|expected) error!")
                .inFile(CompilationSubjectTests.HELLO_WORLD_RESOURCE)
                .onLine(18)
                .atColumn(8);
    }

    @Test
    void hadErrorContaining_noSuchError() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .hadErrorContaining("some error"));
        Truth.assertThat(expected.getMessage())
                .startsWith("Expected an error containing \"some error\", but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("expected error!\n");
    }

    @Test
    void hadErrorContainingMatch_noSuchError() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .hadErrorContainingMatch("(what|where) is it?"));
        Truth.assertThat(expected.getMessage())
                .startsWith(
                        "Expected an error containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("expected error!\n");
    }

    @Test
    void hadErrorContainingMatch_pattern_noSuchError() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .hadErrorContainingMatch(Pattern.compile("(what|where) is it?")));
        Truth.assertThat(expected.getMessage())
                .startsWith(
                        "Expected an error containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("expected error!\n");
    }

    @Test
    void hadErrorContainingInFile_wrongFile() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .hadErrorContaining("expected error!")
                        .inFile(CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE));
        Truth.assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected an error containing \"expected error!\" in %s",
                                CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE.getName()));
        Truth.assertThat(expected.getMessage()).contains(CompilationSubjectTests.HELLO_WORLD_RESOURCE.getName());
        //                  "(no associated file)")));
    }

    @Test
    void hadErrorContainingInFileOnLine_wrongLine() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .hadErrorContaining("expected error!")
                        .inFile(CompilationSubjectTests.HELLO_WORLD_RESOURCE)
                        .onLine(1));
        int actualErrorLine = 18;
        Truth.assertThat(expected.getMessage())
                .contains(
                        CompilationSubjectTests.lines(
                                String.format(
                                        "Expected an error containing \"expected error!\" in %s on line:",
                                        CompilationSubjectTests.HELLO_WORLD_RESOURCE.getName()),
                                "   1: "));
        Truth.assertThat(expected.getMessage()).contains("" + actualErrorLine);
    }

    @Test
    void hadErrorContainingInFileOnLineAtColumn_wrongColumn() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .hadErrorContaining("expected error!")
                        .inFile(CompilationSubjectTests.HELLO_WORLD_RESOURCE)
                        .onLine(18)
                        .atColumn(1));
        int actualErrorCol = 8;
        Truth.assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected an error containing \"expected error!\" in %s at column 1 of line 18",
                                CompilationSubjectTests.HELLO_WORLD_RESOURCE.getName()));
        Truth.assertThat(expected.getMessage()).contains("" + actualErrorCol);
    }

    @Test
    void hadErrorCount() {
        assertThat(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE)).hadErrorCount(4);
    }

    @Test
    void hadErrorCount_wrongCount() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .hadErrorCount(42));
        Truth.assertThat(expected.getMessage())
                .contains("Expected 42 errors, but found the following 2 errors:\n");
    }
}
