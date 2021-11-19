package com.google.testing.compile;

import com.google.common.io.ByteSource;
import com.google.common.truth.ExpectFailure;
import com.google.common.truth.Truth;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.ExpectFailure.assertThat;
import static com.google.testing.compile.CompilationSubject.compilations;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

@RunWith(JUnit4.class)
public class CompilationSubjectGeneratedFilesTest {
    @Rule
    public final ExpectFailure expectFailure = new ExpectFailure();

    @Test
    public void generatedSourceFile_fail() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .generatedSourceFile("ThisIsNotTheRightFile");
        AssertionError expected = expectFailure.getFailure();
        assertThat(expected)
                .factValue("expected to generate file")
                .isEqualTo("/ThisIsNotTheRightFile.java");
        Truth.assertThat(expected.getMessage()).contains(GeneratingProcessor.GENERATED_CLASS_NAME);
    }

    @Test
    public void generatedFilePath() {
        CompilationSubject.assertThat(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .generatedFile(CLASS_OUTPUT, "com/google/testing/compile/Foo")
                .hasContents(ByteSource.wrap("Bar".getBytes(UTF_8)));
    }

    @Test
    public void generatedFilePath_fail() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .generatedFile(CLASS_OUTPUT, "com/google/testing/compile/Bogus.class");
        AssertionError expected = expectFailure.getFailure();
        assertThat(expected)
                .factValue("expected to generate file")
                .isEqualTo("/com/google/testing/compile/Bogus.class");
    }

    @Test
    public void generatedFilePackageFile() {
        CompilationSubject.assertThat(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .generatedFile(CLASS_OUTPUT, "com.google.testing.compile", "Foo")
                .hasContents(ByteSource.wrap("Bar".getBytes(UTF_8)));
    }

    @Test
    public void generatedFilePackageFile_fail() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .generatedFile(CLASS_OUTPUT, "com.google.testing.compile", "Bogus.class");
        AssertionError expected = expectFailure.getFailure();
        assertThat(expected)
                .factValue("expected to generate file")
                .isEqualTo("/com/google/testing/compile/Bogus.class");
    }

    @Test
    public void generatedFileDefaultPackageFile_fail() {
        expectFailure
                .whenTesting()
                .about(compilations())
                .that(CompilationSubjectTests.compilerWithGenerator().compile(CompilationSubjectTests.HELLO_WORLD_RESOURCE))
                .generatedFile(CLASS_OUTPUT, "", "File.java");
        AssertionError expected = expectFailure.getFailure();
        assertThat(expected).factValue("expected to generate file").isEqualTo("/File.java");
        Truth.assertThat(expected.getMessage()).contains(GeneratingProcessor.GENERATED_CLASS_NAME);
    }
}
