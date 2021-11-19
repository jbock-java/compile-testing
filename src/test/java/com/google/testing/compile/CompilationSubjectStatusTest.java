package com.google.testing.compile;

import com.google.common.truth.ExpectFailure;
import com.google.common.truth.Truth;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.CompilationSubject.compilations;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class CompilationSubjectStatusTest {
    @Rule
    public final ExpectFailure expectFailure = new ExpectFailure();

    @Test
    public void succeeded() {
        assertThat(javac().compile(CompilationSubjectTests.HELLO_WORLD)).succeeded();
        assertThat(javac().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE)).succeeded();
    }

    @Test
    public void succeeded_failureReportsGeneratedFiles() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithGeneratorAndError().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .succeeded();
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage()).contains(
                "Compilation produced the following diagnostics:\n");
        Truth.assertThat(expected.getMessage()).contains(FailingGeneratingProcessor.GENERATED_CLASS_NAME);
        Truth.assertThat(expected.getMessage()).contains(FailingGeneratingProcessor.GENERATED_SOURCE);
    }

    @Test
    public void succeeded_failureReportsNoGeneratedFiles() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(javac().compile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE))
                .succeeded();
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage()).startsWith(
                "Compilation produced the following diagnostics:\n");
        Truth.assertThat(expected.getMessage()).contains("No files were generated.");
    }

    @Test
    public void succeeded_exceptionCreatedOrPassedThrough() {
        RuntimeException e = new RuntimeException();
        try {
            Truth.assertAbout(compilations())
                    .that(CompilationSubjectTests.throwingCompiler(e).compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                    .succeeded();
            fail();
        } catch (CompilationFailureException expected) {
            // some old javacs don't pass through exceptions, so we create one
        } catch (RuntimeException expected) {
            // newer jdks throw a runtime exception whose cause is the original exception
            Truth.assertThat(expected.getCause()).isEqualTo(e);
        }
    }

    @Test
    public void succeeded_failureReportsWarnings() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithWarning().compile(CompilationSubjectTests.HELLO_WORLD_BROKEN))
                .succeeded();
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .startsWith("Compilation produced the following diagnostics:\n");
        Truth.assertThat(expected.getMessage()).contains("No files were generated.");
        // "this is a message" is output by compilerWithWarning() since the source has
        // @DiagnosticMessage
        Truth.assertThat(expected.getMessage()).contains("warning: this is a message");
    }

    @Test
    public void succeededWithoutWarnings() {
        assertThat(javac().compile(CompilationSubjectTests.HELLO_WORLD)).succeededWithoutWarnings();
    }

    @Test
    public void succeededWithoutWarnings_failsWithWarnings() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithWarning().compile(CompilationSubjectTests.HELLO_WORLD))
                .succeededWithoutWarnings();
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .contains("Expected 0 warnings, but found the following 2 warnings:\n");
    }

    @Test
    public void failedToCompile() {
        assertThat(javac().compile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE)).failed();
    }

    @Test
    public void failedToCompile_compilationSucceeded() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(javac().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .failed();
        AssertionError expected = expectFailure.getFailure();
        Truth.assertThat(expected.getMessage())
                .startsWith("Compilation was expected to fail, but contained no errors");
        Truth.assertThat(expected.getMessage()).contains("No files were generated.");
    }
}
