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
package com.google.testing.compile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link Compiler}. */
public final class CompilerTest {

    private static final JavaFileObject HELLO_WORLD =
            JavaFileObjects.forResource("test/HelloWorld.java");

    @Test
    void options() {
        NoOpProcessor processor = new NoOpProcessor();
        Object[] options1 = {"-Agone=nowhere"};
        JavaFileObject[] files = {HELLO_WORLD};
        javac()
                .withOptions(options1)
                .withOptions(List.of("-Ab=2", "-Ac=3"))
                .withProcessors(processor)
                .compile(files);
        assertThat(processor.options)
                .containsExactly(
                        "b", "2",
                        "c", "3")
                .inOrder();
    }

    @Test
    void multipleProcesors() {
        NoOpProcessor noopProcessor1 = new NoOpProcessor();
        NoOpProcessor noopProcessor2 = new NoOpProcessor();
        NoOpProcessor noopProcessor3 = new NoOpProcessor();
        assertThat(noopProcessor1.invoked).isFalse();
        assertThat(noopProcessor2.invoked).isFalse();
        assertThat(noopProcessor3.invoked).isFalse();
        Processor[] processors = {noopProcessor1, noopProcessor3};
        JavaFileObject[] files = {HELLO_WORLD};
        javac()
                .withProcessors(processors)
                .withProcessors(noopProcessor1, noopProcessor2)
                .compile(files);
        assertThat(noopProcessor1.invoked).isTrue();
        assertThat(noopProcessor2.invoked).isTrue();
        assertThat(noopProcessor3.invoked).isFalse();
    }

    @Test
    void multipleProcesors_asIterable() {
        NoOpProcessor noopProcessor1 = new NoOpProcessor();
        NoOpProcessor noopProcessor2 = new NoOpProcessor();
        NoOpProcessor noopProcessor3 = new NoOpProcessor();
        assertThat(noopProcessor1.invoked).isFalse();
        assertThat(noopProcessor2.invoked).isFalse();
        assertThat(noopProcessor3.invoked).isFalse();
        JavaFileObject[] files = {HELLO_WORLD};
        javac()
                .withProcessors(Arrays.asList(noopProcessor1, noopProcessor3))
                .withProcessors(Arrays.asList(noopProcessor1, noopProcessor2))
                .compile(files);
        assertThat(noopProcessor1.invoked).isTrue();
        assertThat(noopProcessor2.invoked).isTrue();
        assertThat(noopProcessor3.invoked).isFalse();
    }

    @Test
    void classPath_default() {
        Compilation compilation =
                javac()
                        .compile(
                                JavaFileObjects.forSourceLines(
                                        "Test",
                                        "import com.google.testing.compile.CompilerTest;",
                                        "class Test {",
                                        "  CompilerTest t;",
                                        "}"));
        assertThat(compilation).succeeded();
    }

    @Test
    void classPath_empty() {
        Compilation compilation =
                javac()
                        .withClasspath(List.of())
                        .compile(
                                JavaFileObjects.forSourceLines(
                                        "Test",
                                        "import com.google.testing.compile.CompilerTest;",
                                        "class Test {",
                                        "  CompilerTest t;",
                                        "}"));
        assertThat(compilation).hadErrorContaining("com.google.testing.compile does not exist");
    }

    /** Sets up a jar containing a single class 'Lib', for use in classpath tests. */
    private File compileTestLib(Path temporaryFolder) throws IOException {
        File lib = temporaryFolder.resolve("tmp").toFile();
        assertTrue(lib.mkdirs());
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(/* diagnosticListener= */ null, Locale.getDefault(), UTF_8);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(lib));
        CompilationTask task =
                compiler.getTask(
                        /* out= */ null,
                        fileManager,
                        /* diagnosticListener= */ null,
                        /* options= */ List.of(),
                        /* classes= */ null,
                        List.of(JavaFileObjects.forSourceLines("Lib", "class Lib {}")));
        assertThat(task.call()).isTrue();
        return lib;
    }

    @Test
    void classPath_customFiles(@TempDir Path temporaryFolder) throws Exception {
        File lib = compileTestLib(temporaryFolder);
        // compile with only 'Lib' on the classpath
        Compilation compilation =
                javac()
                        .withClasspath(List.of(lib))
                        .withOptions("-verbose")
                        .compile(
                                JavaFileObjects.forSourceLines(
                                        "Test", //
                                        "class Test {",
                                        "  Lib lib;",
                                        "}"));
        assertThat(compilation).succeeded();
    }

    @Test
    void classPath_empty_urlClassLoader() {
        Compilation compilation =
                javac()
                        .withClasspathFrom(new URLClassLoader(new URL[0], Compiler.platformClassLoader))
                        .compile(
                                JavaFileObjects.forSourceLines(
                                        "Test",
                                        "import com.google.testing.compile.CompilerTest;",
                                        "class Test {",
                                        "  CompilerTest t;",
                                        "}"));
        assertThat(compilation).hadErrorContaining("com.google.testing.compile does not exist");
    }

    @Test
    void classPath_customFiles_urlClassLoader(@TempDir Path temporaryFolder) throws Exception {
        File lib = compileTestLib(temporaryFolder);
        Compilation compilation =
                javac()
                        .withClasspathFrom(new URLClassLoader(new URL[]{lib.toURI().toURL()}))
                        .withOptions("-verbose")
                        .compile(JavaFileObjects.forSourceLines("Test", "class Test {", "  Lib lib;", "}"));
        assertThat(compilation).succeeded();
    }

    @Test
    void annotationProcessorPath_empty() {
        AnnotationFileProcessor processor = new AnnotationFileProcessor();
        Compiler compiler =
                javac().withProcessors(processor).withAnnotationProcessorPath(List.of());
        RuntimeException expected =
                assertThrows(
                        RuntimeException.class,
                        () -> compiler.compile(JavaFileObjects.forSourceLines("Test", "class Test {}")));
        // exception, and this bug is fixed after JDK 8
        assertThat(expected).hasCauseThat().hasCauseThat().hasMessageThat().contains("tmp.txt");
    }

    @Test
    void annotationProcessorPath_customFiles() throws Exception {
        AnnotationFileProcessor processor = new AnnotationFileProcessor();
        File jar = compileTestJar();
        // compile with only 'tmp.txt' on the annotation processor path
        Compilation compilation =
                javac()
                        .withProcessors(processor)
                        .withAnnotationProcessorPath(List.of(jar))
                        .compile(JavaFileObjects.forSourceLines("Test", "class Test {}"));
        assertThat(compilation).succeeded();
    }

    @Test
        // See https://github.com/google/compile-testing/issues/189
    void readInputFile() throws IOException {
        AtomicReference<String> content = new AtomicReference<>();
        Compilation compilation =
                javac()
                        .withProcessors(
                                new AbstractProcessor() {
                                    @Override
                                    public synchronized void init(ProcessingEnvironment processingEnv) {
                                        Filer filer = processingEnv.getFiler();
                                        try {
                                            FileObject helloWorld =
                                                    filer.getResource(
                                                            StandardLocation.SOURCE_PATH, "test", "HelloWorld.java");
                                            content.set(helloWorld.getCharContent(true).toString());
                                        } catch (IOException e) {
                                            throw new UncheckedIOException(e);
                                        }
                                    }

                                    @Override
                                    public boolean process(
                                            Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                                        return false;
                                    }

                                    @Override
                                    public Set<String> getSupportedAnnotationTypes() {
                                        return Set.of("*");
                                    }

                                    @Override
                                    public SourceVersion getSupportedSourceVersion() {
                                        return SourceVersion.latestSupported();
                                    }
                                })
                        .compile(HELLO_WORLD);
        assertThat(compilation).succeeded();
        assertThat(content.get()).isEqualTo(HELLO_WORLD.getCharContent(true).toString());
    }

    /**
     * Sets up a jar containing a single file 'tmp.txt', for use in annotation processor path tests.
     */
    private static File compileTestJar() throws IOException {
        File file = File.createTempFile("tmp", ".jar");
        try (ZipOutputStream zipOutput = new ZipOutputStream(new FileOutputStream(file))) {
            ZipEntry entry = new ZipEntry("tmp.txt");
            zipOutput.putNextEntry(entry);
            zipOutput.closeEntry();
        }
        return file;
    }

    @Test
    void releaseFlag() {
        Compilation compilation =
                javac()
                        .withOptions("--release", "8")
                        .compile(JavaFileObjects.forSourceString("HelloWorld", "final class HelloWorld {}"));
        assertThat(compilation).succeeded();
    }
}
