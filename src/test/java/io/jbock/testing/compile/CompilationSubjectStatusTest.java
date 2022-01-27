package io.jbock.testing.compile;

import com.google.common.truth.Truth;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertAbout;
import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.CompilationSubject.compilations;
import static io.jbock.testing.compile.Compiler.javac;

class CompilationSubjectStatusTest {

    @Test
    void succeeded() {
        assertThat(javac().compile(CompilationSubjectTests.HELLO_WORLD)).succeeded();
        assertThat(javac().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE)).succeeded();
    }

    @Test
    void succeeded_failureReportsGeneratedFiles() {
        Compilation compilation = CompilationSubjectTests.compilerWithGeneratorAndError()
                .compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE);
        AssertionError expected = Assertions.assertThrows(AssertionError.class, () ->
                CompilationSubject.assertThat(compilation).succeeded());
        Assertions.assertTrue(expected.getMessage().contains(
                "Compilation produced the following diagnostics:\n"));
        Assertions.assertTrue(expected.getMessage().contains(
                FailingGeneratingProcessor.GENERATED_CLASS_NAME));
        Assertions.assertTrue(expected.getMessage().contains(
                FailingGeneratingProcessor.GENERATED_SOURCE));
    }

    @Test
    void succeeded_failureReportsNoGeneratedFiles() {
        Compilation compilation = CompilationSubjectTests.compilerWithGeneratorAndError()
                .compile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE);
        AssertionError expected = Assertions.assertThrows(AssertionError.class, () ->
                CompilationSubject.assertThat(compilation).succeeded());
        Assertions.assertTrue(expected.getMessage().startsWith(
                "Compilation produced the following diagnostics:\n"));
        Assertions.assertTrue(expected.getMessage().contains(
                "No files were generated."));
    }

    @Test
    void succeeded_exceptionCreatedOrPassedThrough() {
        RuntimeException e = new RuntimeException();
        RuntimeException expected = Assertions.assertThrows(RuntimeException.class, () -> Truth.assertAbout(compilations())
                .that(CompilationSubjectTests.throwingCompiler(e).compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .succeeded());
        Assertions.assertSame(e, expected.getCause());
    }

    @Test
    void succeeded_failureReportsWarnings() {
        Compilation compilation = CompilationSubjectTests.compilerWithWarning()
                .compile(CompilationSubjectTests.HELLO_WORLD_BROKEN);
        AssertionError expected = Assertions.assertThrows(AssertionError.class, () ->
                CompilationSubject.assertThat(compilation).succeeded());
        Assertions.assertTrue(expected.getMessage()
                .startsWith("Compilation produced the following diagnostics:\n"));
        Assertions.assertTrue(expected.getMessage()
                .contains("No files were generated."));
        // "this is a message" is output by compilerWithWarning() since the source has
        // @DiagnosticMessage
        Assertions.assertTrue(expected.getMessage()
                .contains("warning: this is a message"));
    }

    @Test
    void succeededWithoutWarnings() {
        assertThat(javac().compile(CompilationSubjectTests.HELLO_WORLD)).succeededWithoutWarnings();
    }

    @Test
    void succeededWithoutWarnings_failsWithWarnings() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithWarning().compile(CompilationSubjectTests.HELLO_WORLD))
                        .succeededWithoutWarnings());
        Truth.assertThat(expected.getMessage())
                .contains("Expected 0 warnings, but found the following 2 warnings:\n");
    }

    @Test
    void failedToCompile() {
        assertThat(javac().compile(CompilationSubjectTests.HELLO_WORLD_BROKEN_RESOURCE)).failed();
    }

    @Test
    void failedToCompile_compilationSucceeded() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(javac().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .failed());
        Truth.assertThat(expected.getMessage())
                .startsWith("Compilation was expected to fail, but contained no errors");
        Truth.assertThat(expected.getMessage()).contains("No files were generated.");
    }
}
