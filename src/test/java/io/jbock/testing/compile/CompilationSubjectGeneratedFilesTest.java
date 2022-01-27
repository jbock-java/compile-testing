package io.jbock.testing.compile;

import com.google.common.truth.Truth;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.ExpectFailure.assertThat;
import static com.google.common.truth.Truth.assertAbout;
import static io.jbock.testing.compile.CompilationSubject.compilations;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

class CompilationSubjectGeneratedFilesTest {

    @Test
    void generatedSourceFile_fail() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .generatedSourceFile("ThisIsNotTheRightFile"));
        assertThat(expected)
                .factValue("expected to generate file")
                .isEqualTo("/ThisIsNotTheRightFile.java");
        Truth.assertThat(expected.getMessage()).contains(GeneratingProcessor.GENERATED_CLASS_NAME);
    }

    @Test
    void generatedFilePath() {
        CompilationSubject.assertThat(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .generatedFile(CLASS_OUTPUT, "io/jbock/testing/compile/Foo")
                .hasContents("Bar".getBytes(UTF_8));
    }

    @Test
    void generatedFilePath_fail() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .generatedFile(CLASS_OUTPUT, "io/jbock/testing/compile/Bogus.class"));
        assertThat(expected)
                .factValue("expected to generate file")
                .isEqualTo("/io/jbock/testing/compile/Bogus.class");
    }

    @Test
    void generatedFilePackageFile() {
        CompilationSubject.assertThat(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .generatedFile(CLASS_OUTPUT, "io.jbock.testing.compile", "Foo")
                .hasContents("Bar".getBytes(UTF_8));
    }

    @Test
    void generatedFilePackageFile_fail() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .generatedFile(CLASS_OUTPUT, "io.jbock.testing.compile", "Bogus.class"));
        assertThat(expected)
                .factValue("expected to generate file")
                .isEqualTo("/io/jbock/testing/compile/Bogus.class");
    }

    @Test
    void generatedFileDefaultPackageFile_fail() {
        AssertionError expected = Assertions.assertThrows(
                AssertionError.class,
                () -> assertAbout(compilations())
                        .that(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                        .generatedFile(CLASS_OUTPUT, "", "File.java"));
        assertThat(expected).factValue("expected to generate file").isEqualTo("/File.java");
        Truth.assertThat(expected.getMessage()).contains(GeneratingProcessor.GENERATED_CLASS_NAME);
    }
}
