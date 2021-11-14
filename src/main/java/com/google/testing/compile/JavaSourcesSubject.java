/*
 * Copyright (C) 2013 Google, Inc.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.testing.compile.CompilationSubject.DiagnosticAtColumn;
import com.google.testing.compile.CompilationSubject.DiagnosticInFile;
import com.google.testing.compile.CompilationSubject.DiagnosticOnLine;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.CompilationSubject.compilations;
import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

/**
 * A <a href="https://github.com/truth0/truth">Truth</a> {@link Subject} that evaluates the result
 * of a {@code javac} compilation. See {@link com.google.testing.compile} for usage examples
 *
 * @author Gregory Kick
 */
@SuppressWarnings("restriction") // Sun APIs usage intended
public final class JavaSourcesSubject extends Subject
        implements CompileTester, ProcessedCompileTesterFactory {
    private final Iterable<? extends JavaFileObject> actual;
    private final List<String> options = new ArrayList<>(Arrays.asList("-Xlint"));
    private ClassLoader classLoader;
    private ImmutableList<File> classPath;

    JavaSourcesSubject(FailureMetadata failureMetadata, Iterable<? extends JavaFileObject> subject) {
        super(failureMetadata, subject);
        this.actual = subject;
    }

    @Override
    public JavaSourcesSubject withCompilerOptions(Iterable<String> options) {
        Iterables.addAll(this.options, options);
        return this;
    }

    @Override
    public JavaSourcesSubject withCompilerOptions(String... options) {
        this.options.addAll(Arrays.asList(options));
        return this;
    }

    /**
     * @deprecated prefer {@link #withClasspath(Iterable)}. This method only supports {@link
     *     java.net.URLClassLoader} and the default system classloader, and {@link File}s are usually
     *     a more natural way to expression compilation classpaths than class loaders.
     */
    @Deprecated
    @Override
    public JavaSourcesSubject withClasspathFrom(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @Override
    public JavaSourcesSubject withClasspath(Iterable<File> classPath) {
        this.classPath = ImmutableList.copyOf(classPath);
        return this;
    }

    @Override
    public CompileTester processedWith(Processor first, Processor... rest) {
        return processedWith(Lists.asList(first, rest));
    }

    @Override
    public CompileTester processedWith(Iterable<? extends Processor> processors) {
        return new CompilationClause(processors);
    }

    @Override
    public SuccessfulCompilationClause compilesWithoutError() {
        return new CompilationClause().compilesWithoutError();
    }

    @Override
    public CleanCompilationClause compilesWithoutWarnings() {
        return new CompilationClause().compilesWithoutWarnings();
    }

    @Override
    public UnsuccessfulCompilationClause failsToCompile() {
        return new CompilationClause().failsToCompile();
    }

    /** The clause in the fluent API for testing compilations. */
    private final class CompilationClause implements CompileTester {
        private final ImmutableSet<Processor> processors;

        private CompilationClause() {
            this(ImmutableSet.of());
        }

        private CompilationClause(Iterable<? extends Processor> processors) {
            this.processors = ImmutableSet.copyOf(processors);
        }

        @Override
        public SuccessfulCompilationClause compilesWithoutError() {
            Compilation compilation = compilation();
            check("compilation()").about(compilations()).that(compilation).succeeded();
            return new SuccessfulCompilationBuilder(compilation);
        }

        @Override
        public CleanCompilationClause compilesWithoutWarnings() {
            Compilation compilation = compilation();
            check("compilation()").about(compilations()).that(compilation).succeededWithoutWarnings();
            return new CleanCompilationBuilder(compilation);
        }

        @Override
        public UnsuccessfulCompilationClause failsToCompile() {
            Compilation compilation = compilation();
            check("compilation()").about(compilations()).that(compilation).failed();
            return new UnsuccessfulCompilationBuilder(compilation);
        }

        private Compilation compilation() {
            Compiler compiler = javac().withProcessors(processors).withOptions(options);
            if (classLoader != null) {
                compiler = compiler.withClasspathFrom(classLoader);
            }
            if (classPath != null) {
                compiler = compiler.withClasspath(classPath);
            }
            return compiler.compile(actual);
        }
    }

    /**
     * A helper method for {@link SingleSourceAdapter} to ensure that the inner class is created
     * correctly.
     */
    private CompilationClause newCompilationClause(Iterable<? extends Processor> processors) {
        return new CompilationClause(processors);
    }

    /**
     * Base implementation of {@link CompilationWithWarningsClause}.
     *
     * @param <T> the type parameter for {@link CompilationWithWarningsClause}. {@code this} must be
     *     an instance of {@code T}; otherwise some calls will throw {@link ClassCastException}.
     */
    abstract class CompilationWithWarningsBuilder<T> implements CompilationWithWarningsClause<T> {
        protected final Compilation compilation;

        protected CompilationWithWarningsBuilder(Compilation compilation) {
            this.compilation = compilation;
        }

        @Override
        public T withNoteCount(int noteCount) {
            check("compilation()").about(compilations()).that(compilation).hadNoteCount(noteCount);
            return thisObject();
        }

        @Override
        public FileClause<T> withNoteContaining(String messageFragment) {
            return new FileBuilder(
                    check("compilation()")
                            .about(compilations())
                            .that(compilation)
                            .hadNoteContaining(messageFragment));
        }

        @Override
        public T withWarningCount(int warningCount) {
            check("compilation()").about(compilations()).that(compilation).hadWarningCount(warningCount);
            return thisObject();
        }

        @Override
        public FileClause<T> withWarningContaining(String messageFragment) {
            return new FileBuilder(
                    check("compilation()")
                            .about(compilations())
                            .that(compilation)
                            .hadWarningContaining(messageFragment));
        }

        public T withErrorCount(int errorCount) {
            check("compilation()").about(compilations()).that(compilation).hadErrorCount(errorCount);
            return thisObject();
        }

        public FileClause<T> withErrorContaining(String messageFragment) {
            return new FileBuilder(
                    check("compilation()")
                            .about(compilations())
                            .that(compilation)
                            .hadErrorContaining(messageFragment));
        }

        /** Returns this object, cast to {@code T}. */
        @SuppressWarnings("unchecked")
        protected final T thisObject() {
            return (T) this;
        }

        private final class FileBuilder implements FileClause<T> {
            private final DiagnosticInFile diagnosticInFile;

            private FileBuilder(DiagnosticInFile diagnosticInFile) {
                this.diagnosticInFile = diagnosticInFile;
            }

            @Override
            public T and() {
                return thisObject();
            }

            @Override
            public LineClause<T> in(JavaFileObject file) {
                final DiagnosticOnLine diagnosticOnLine = diagnosticInFile.inFile(file);

                return new LineClause<T>() {
                    @Override
                    public T and() {
                        return thisObject();
                    }

                    @Override
                    public ColumnClause<T> onLine(long lineNumber) {
                        final DiagnosticAtColumn diagnosticAtColumn = diagnosticOnLine.onLine(lineNumber);

                        return new ColumnClause<T>() {
                            @Override
                            public T and() {
                                return thisObject();
                            }

                            @Override
                            public ChainingClause<T> atColumn(long columnNumber) {
                                diagnosticAtColumn.atColumn(columnNumber);
                                return this;
                            }
                        };
                    }
                };
            }
        }
    }

    /**
     * Base implementation of {@link GeneratedPredicateClause GeneratedPredicateClause<T>} and {@link
     * ChainingClause ChainingClause<GeneratedPredicateClause<T>>}.
     *
     * @param <T> the type parameter to {@link GeneratedPredicateClause}. {@code this} must be an
     *     instance of {@code T}.
     */
    private abstract class GeneratedCompilationBuilder<T> extends CompilationWithWarningsBuilder<T>
            implements GeneratedPredicateClause<T>, ChainingClause<GeneratedPredicateClause<T>> {

        protected GeneratedCompilationBuilder(Compilation compilation) {
            super(compilation);
        }

        @Override
        public final T generatesSources(JavaFileObject first, JavaFileObject... rest) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T generatesSources(String qualifiedName, JavaFileObject file) {
            try {
                List<String> expectation = Arrays.asList(file.getCharContent(false)
                        .toString()
                        .split("\\R", -1));
                return generatesSources(qualifiedName, expectation);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Couldn't read from JavaFileObject when it was already in memory.", e);
            }
        }

        @Override
        public T generatesSources(String qualifiedName, List<String> expectation) {
            CompilationSubject.assertThat(compilation).succeeded();
            CompilationSubject.assertThat(compilation)
                    .generatedSourceFile(qualifiedName)
                    .containsExactLines(expectation);
            return thisObject();
        }

        @Override
        public T generatesSources(String qualifiedName, String... expectation) {
            return generatesSources(qualifiedName, Arrays.asList(expectation));
        }

        @Override
        public T generatesFiles(JavaFileObject first, JavaFileObject... rest) {
            for (JavaFileObject expected : Lists.asList(first, rest)) {
                if (!wasGenerated(expected)) {
                    failWithoutActual(
                            simpleFact("Did not find a generated file corresponding to " + expected.getName()));
                }
            }
            return thisObject();
        }

        boolean wasGenerated(JavaFileObject expected) {
            ByteSource expectedByteSource = JavaFileObjects.asByteSource(expected);
            for (JavaFileObject generated : compilation.generatedFiles()) {
                try {
                    if (generated.getKind().equals(expected.getKind())
                            && expectedByteSource.contentEquals(JavaFileObjects.asByteSource(generated))) {
                        return true;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return false;
        }

        @Override
        public SuccessfulFileClause<T> generatesFileNamed(
                JavaFileManager.Location location, String packageName, String relativeName) {
            final JavaFileObjectSubject javaFileObjectSubject =
                    check("compilation()")
                            .about(compilations())
                            .that(compilation)
                            .generatedFile(location, packageName, relativeName);
            return new SuccessfulFileClause<T>() {
                @Override
                public GeneratedPredicateClause<T> and() {
                    return GeneratedCompilationBuilder.this;
                }

                @Override
                public SuccessfulFileClause<T> withContents(ByteSource expectedByteSource) {
                    javaFileObjectSubject.hasContents(expectedByteSource);
                    return this;
                }

                @Override
                public SuccessfulFileClause<T> withStringContents(Charset charset, String expectedString) {
                    javaFileObjectSubject.contentsAsString(charset).isEqualTo(expectedString);
                    return this;
                }
            };
        }

        @Override
        public GeneratedPredicateClause<T> and() {
            return this;
        }
    }

    private final class UnsuccessfulCompilationBuilder
            extends CompilationWithWarningsBuilder<UnsuccessfulCompilationClause>
            implements UnsuccessfulCompilationClause {

        UnsuccessfulCompilationBuilder(Compilation compilation) {
            super(compilation);
        }
    }

    private final class SuccessfulCompilationBuilder
            extends GeneratedCompilationBuilder<SuccessfulCompilationClause>
            implements SuccessfulCompilationClause {

        SuccessfulCompilationBuilder(Compilation compilation) {
            super(compilation);
        }
    }

    private final class CleanCompilationBuilder
            extends GeneratedCompilationBuilder<CleanCompilationClause>
            implements CleanCompilationClause {

        CleanCompilationBuilder(Compilation compilation) {
            super(compilation);
        }
    }

    public static JavaSourcesSubject assertThat(JavaFileObject javaFileObject) {
        return assertAbout(javaSources()).that(ImmutableList.of(javaFileObject));
    }

    public static JavaSourcesSubject assertThat(
            JavaFileObject javaFileObject, JavaFileObject... javaFileObjects) {
        return assertAbout(javaSources())
                .that(
                        ImmutableList.<JavaFileObject>builder()
                                .add(javaFileObject)
                                .add(javaFileObjects)
                                .build());
    }

    public static final class SingleSourceAdapter extends Subject
            implements CompileTester, ProcessedCompileTesterFactory {
        private final JavaSourcesSubject delegate;

        SingleSourceAdapter(FailureMetadata failureMetadata, JavaFileObject subject) {
            super(failureMetadata, subject);
            /*
             * TODO(b/131918061): It would make more sense to eliminate SingleSourceAdapter entirely.
             * Users can already use assertThat(JavaFileObject, JavaFileObject...) above for a single
             * file. Anyone who needs a Subject.Factory could fall back to
             * `about(javaSources()).that(ImmutableSet.of(source))`.
             *
             * We could take that on, or we could wait for JavaSourcesSubject to go away entirely in favor
             * of CompilationSubject.
             */
            this.delegate = check("delegate()").about(javaSources()).that(ImmutableList.of(subject));
        }

        @Override
        public JavaSourcesSubject withCompilerOptions(Iterable<String> options) {
            return delegate.withCompilerOptions(options);
        }

        @Override
        public JavaSourcesSubject withCompilerOptions(String... options) {
            return delegate.withCompilerOptions(options);
        }

        /**
         * @deprecated prefer {@link #withClasspath(Iterable)}. This method only supports {@link
         *     java.net.URLClassLoader} and the default system classloader, and {@link File}s are
         *     usually a more natural way to expression compilation classpaths than class loaders.
         */
        @Deprecated
        @Override
        public JavaSourcesSubject withClasspathFrom(ClassLoader classLoader) {
            return delegate.withClasspathFrom(classLoader);
        }

        @Override
        public JavaSourcesSubject withClasspath(Iterable<File> classPath) {
            return delegate.withClasspath(classPath);
        }

        @Override
        public CompileTester processedWith(Processor first, Processor... rest) {
            return delegate.newCompilationClause(Lists.asList(first, rest));
        }

        @Override
        public CompileTester processedWith(Iterable<? extends Processor> processors) {
            return delegate.newCompilationClause(processors);
        }

        @Override
        public SuccessfulCompilationClause compilesWithoutError() {
            return delegate.compilesWithoutError();
        }

        @Override
        public CleanCompilationClause compilesWithoutWarnings() {
            return delegate.compilesWithoutWarnings();
        }

        @Override
        public UnsuccessfulCompilationClause failsToCompile() {
            return delegate.failsToCompile();
        }
    }
}
