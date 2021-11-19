package com.google.testing.compile;

import com.google.common.truth.ExpectFailure;
import com.google.common.truth.Truth;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.regex.Pattern;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.CompilationSubject.compilations;
import static com.google.testing.compile.Compiler.javac;
import static java.lang.String.format;

/** Tests for {@link CompilationSubject}'s assertions about errors. */
@RunWith(JUnit4.class)
public final class CompilationSubjectErrorTest {
    @Rule
    public final ExpectFailure expectFailure = new ExpectFailure();

    @Test
    public void hadErrorContaining() {
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
    public void hadErrorContainingMatch() {
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
    public void hadErrorContainingMatch_pattern() {
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
    public void hadErrorContaining_noSuchError() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .hadErrorContaining("some error");
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .startsWith("Expected an error containing \"some error\", but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("expected error!\n");
    }

    @Test
    public void hadErrorContainingMatch_noSuchError() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .hadErrorContainingMatch("(what|where) is it?");
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .startsWith(
                        "Expected an error containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("expected error!\n");
    }

    @Test
    public void hadErrorContainingMatch_pattern_noSuchError() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .hadErrorContainingMatch(Pattern.compile("(what|where) is it?"));
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .startsWith(
                        "Expected an error containing match for /(what|where) is it?/, but only found:\n");
        // some versions of javac wedge the file and position in the middle
        Truth.assertThat(expected.getMessage()).endsWith("expected error!\n");
    }

    @Test
    public void hadErrorContainingInFile_wrongFile() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .hadErrorContaining("expected error!")
                .inFile(CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE);
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected an error containing \"expected error!\" in %s",
                                CompilationSubjectTests.HELLO_WORLD_DIFFERENT_RESOURCE.getName()));
        Truth.assertThat(expected.getMessage()).contains(CompilationSubjectTests.HELLO_WORLD_RESOURCE.getName());
        //                  "(no associated file)")));
    }

    @Test
    public void hadErrorContainingInFileOnLine_wrongLine() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .hadErrorContaining("expected error!")
                .inFile(CompilationSubjectTests.HELLO_WORLD_RESOURCE)
                .onLine(1);
        AssertionError expected = expectFailure.getFailure();
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
    public void hadErrorContainingInFileOnLineAtColumn_wrongColumn() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .hadErrorContaining("expected error!")
                .inFile(CompilationSubjectTests.HELLO_WORLD_RESOURCE)
                .onLine(18)
                .atColumn(1);
        AssertionError expected = expectFailure.getFailure();
        int actualErrorCol = 8;
        Truth.assertThat(expected.getMessage())
                .contains(
                        String.format(
                                "Expected an error containing \"expected error!\" in %s at column 1 of line 18",
                                CompilationSubjectTests.HELLO_WORLD_RESOURCE.getName()));
        Truth.assertThat(expected.getMessage()).contains("" + actualErrorCol);
    }

    @Test
    public void hadErrorCount() {
        assertThat(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE)).hadErrorCount(4);
    }

    @Test
    public void hadErrorCount_wrongCount() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .hadErrorCount(42);
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .contains("Expected 42 errors, but found the following 2 errors:\n");
    }
}
