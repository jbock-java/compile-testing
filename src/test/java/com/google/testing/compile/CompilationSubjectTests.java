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

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import static com.google.testing.compile.Compiler.javac;

class CompilationSubjectTests {
    static final JavaFileObject HELLO_WORLD =
            JavaFileObjects.forSourceLines(
                    "test.HelloWorld",
                    "package test;",
                    "",
                    "import " + DiagnosticMessage.class.getCanonicalName() + ";",
                    "",
                    "@DiagnosticMessage",
                    "public class HelloWorld {",
                    "  @DiagnosticMessage Object foo;",
                    "  void weird() {",
                    "    foo.toString();",
                    "  }",
                    "}");

    static final JavaFileObject HELLO_WORLD_BROKEN =
            JavaFileObjects.forSourceLines(
                    "test.HelloWorld",
                    "package test;",
                    "",
                    "import " + DiagnosticMessage.class.getCanonicalName() + ";",
                    "",
                    "@DiagnosticMessage",
                    "public class HelloWorld {",
                    "  @DiagnosticMessage Object foo;",
                    "  Bar noSuchClass;",
                    "}");

    static final JavaFileObject HELLO_WORLD_RESOURCE =
            JavaFileObjects.forResource("test/HelloWorld.java");

    static final JavaFileObject HELLO_WORLD_BROKEN_RESOURCE =
            JavaFileObjects.forResource("test/HelloWorld-broken.java");

    static final JavaFileObject HELLO_WORLD_DIFFERENT_RESOURCE =
            JavaFileObjects.forResource("test/HelloWorld-different.java");

    static String lines(String... lines) {
        return String.join("\n", lines);
    }

    static Compiler compilerWithError() {
        return javac().withProcessors(new ErrorProcessor());
    }

    static Compiler compilerWithWarning() {
        return javac().withProcessors(new DiagnosticMessage.Processor(Diagnostic.Kind.WARNING));
    }

    static Compiler compilerWithNote() {
        return javac().withProcessors(new DiagnosticMessage.Processor(Diagnostic.Kind.NOTE));
    }

    static Compiler compilerWithGenerator() {
        return javac().withProcessors(new GeneratingProcessor());
    }

    static Compiler compilerWithGeneratorAndError() {
        return javac().withProcessors(new FailingGeneratingProcessor());
    }

    static Compiler throwingCompiler(RuntimeException e) {
        return javac().withProcessors(new ThrowingProcessor(e));
    }
}
