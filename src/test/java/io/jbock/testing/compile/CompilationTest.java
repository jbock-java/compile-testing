/*
 * Copyright (C) 2016 Google, Inc.
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

package io.jbock.testing.compile;

import com.google.common.truth.Truth;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertThat;
import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.Compiler.javac;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.junit.jupiter.api.Assertions.fail;

public class CompilationTest {

    private static final JavaFileObject source1 =
            JavaFileObjects.forSourceLines(
                    "test.Source1", // format one per line
                    "package test;",
                    "",
                    "class Source1 {}");

    private static final JavaFileObject source2 =
            JavaFileObjects.forSourceLines(
                    "test.Source2", // format one per line
                    "package test;",
                    "",
                    "interface Source2 {}");

    private static final JavaFileObject brokenSource =
            JavaFileObjects.forSourceLines(
                    "test.BrokenSource", // format one per line
                    "package test;",
                    "",
                    "interface BrokenSource { what is this }");

    @Test
    void compiler() {
        Compiler compiler = compilerWithGenerator();
        Compilation compilation = compiler.compile(source1, source2);
        assertThat(compilation.compiler()).isEqualTo(compiler);
        assertThat(compilation.sourceFiles()).containsExactly(source1, source2).inOrder();
        assertThat(compilation.status()).isEqualTo(Compilation.Status.SUCCESS);
    }

    @Test
    void compilerStatusFailure() {
        Compiler compiler = compilerWithGenerator();
        Compilation compilation = compiler.compile(brokenSource);
        assertThat(compilation.status()).isEqualTo(Compilation.Status.FAILURE);
        assertThat(compilation.errors()).hasSize(1);
        assertThat(compilation.errors().get(0).getLineNumber()).isEqualTo(3);
    }

    @Test
    void generatedFilePath() {
        Compiler compiler = compilerWithGenerator();
        Compilation compilation = compiler.compile(source1, source2);
        Truth.assertThat(compilation.generatedFile(SOURCE_OUTPUT, "test/generated/Blah.java")).isPresent();
    }

    @Test
    void generatedFilePackage() {
        Compiler compiler = compilerWithGenerator();
        Compilation compilation = compiler.compile(source1, source2);
        Truth.assertThat(compilation.generatedFile(SOURCE_OUTPUT, "test.generated", "Blah.java")).isPresent();
    }

    @Test
    void generatedSourceFile() {
        Compiler compiler = compilerWithGenerator();
        Compilation compilation = compiler.compile(source1, source2);
        Truth.assertThat(compilation.generatedSourceFile("test.generated.Blah")).isPresent();
    }

    private static Compiler compilerWithGenerator() {
        return javac().withProcessors(new GeneratingProcessor("test.generated"));
    }

    @Test
    void generatedFiles_unsuccessfulCompilationThrows() {
        Compilation compilation =
                javac()
                        .compile(
                                JavaFileObjects.forSourceLines(
                                        "test.Bad", "package test;", "", "this doesn't compile!"));
        assertThat(compilation).failed();
        try {
            compilation.generatedFiles();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    void describeFailureDiagnostics_includesWarnings_whenCompilingWerror() {
        // Arrange
        Compiler compiler = javac().withOptions("-Xlint:cast", "-Werror");
        JavaFileObject source =
                JavaFileObjects.forSourceLines(
                        "test.CastWarning", //
                        "package test;", //
                        "class CastWarning {", //
                        "  int i = (int) 0;", //
                        "}");

        // Act
        Compilation compilation = compiler.compile(source);

        // Assert
        assertThat(compilation).failed();
        assertThat(compilation.describeFailureDiagnostics()).contains("[cast] redundant cast to int");
    }
}
