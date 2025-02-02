/*
 * Copyright 2018 David van Leusen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jbock.testing.compile;

import io.jbock.testing.compile.CompilationExtension;
import io.jbock.testing.compile.CompilationSubject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static io.jbock.common.truth.Truth.assertThat;
import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the {@link CompilationExtension} by applying it to this test.
 */
class CompilationExtensionTest {

    @Test
    void testAsyncCompiler() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompilationExtension.CompilerState state = new CompilationExtension.CompilerState(
                executor,
                TestInstance.Lifecycle.PER_CLASS
        );
        executor.shutdown(); // Allow executor to finish with the compiler

        // Calling .allowTermination before .prepareForTests should not result in invalid state
        assertThrows(IllegalStateException.class, state::allowTermination);

        // prepareForTests is idempotent
        assertDoesNotThrow(() -> {
            assertThat(state.prepareForTests()).isTrue();
            assertThat(state.prepareForTests()).isTrue();
        });

        // it should only need to finish the compilation once.
        assertDoesNotThrow(() -> {
            CompilationSubject subject = assertThat(state.allowTermination());

            subject.succeeded();

            // Repeat calls should just return the same results
            subject.isEqualTo(state.allowTermination());
        });

        // Prepare after termination should fail.
        assertThrows(IllegalStateException.class, state::prepareForTests);
    }

    @Nested
    @ExtendWith(CompilationExtension.class)
    @DisplayName("@ExtendWith Class")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ClassExtendWith extends ExtensionTests {
        @BeforeAll
        void testBeforeAll(Elements elements, Types types) {
            elementsAreValidAndWorking(elements);
            typeMirrorsAreValidAndWorking(elements, types);
        }
    }

    @Nested
    @DisplayName("@ExtendWith Method")
    class MethodExtendWith extends ExtensionTests {
        @Override
        @ExtendWith(CompilationExtension.class)
        @Test
        void testMethodsExecuteExactlyOnce() {
            // By definition a method-based extension would not support @BeforeEach
            super.testMethodsExecuteExactlyOnce();
        }

        @Override
        @ExtendWith(CompilationExtension.class)
        @Test
        void getElements(Elements elements) {
            super.getElements(elements);
        }

        @Override
        @ExtendWith(CompilationExtension.class)
        @Test
        void getTypes(Types types) {
            super.getTypes(types);
        }

        @Override
        @ExtendWith(CompilationExtension.class)
        @Test
        void elementsAreValidAndWorking(Elements elements) {
            super.elementsAreValidAndWorking(elements);
        }

        @Override
        @ExtendWith(CompilationExtension.class)
        @Test
        void typeMirrorsAreValidAndWorking(Elements elements, Types types) {
            super.typeMirrorsAreValidAndWorking(elements, types);
        }
    }

    @Nested
    @DisplayName("@RegisterWith - Lifecycle.PER_METHOD")
    class RegisterWithPerMethod extends ExtensionTests {
        @RegisterExtension
        CompilationExtension ext = CompilationExtension.create();
    }

    @Nested
    @DisplayName("@RegisterWith - Lifecycle.PER_CLASS")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class RegisterWithPerClass extends ExtensionTests {
        @RegisterExtension
        CompilationExtension ext = CompilationExtension.create();

        @BeforeAll
        void testBeforeAll(Elements elements, Types types) {
            elementsAreValidAndWorking(elements);
            typeMirrorsAreValidAndWorking(elements, types);
        }
    }

    abstract static class ExtensionTests {
        private final AtomicInteger executions = new AtomicInteger();

        @Test
        void testMethodsExecuteExactlyOnce() {
            assertThat(executions.getAndIncrement()).isEqualTo(0);
        }

        @BeforeEach
        @Test
        void getElements(Elements elements) {
            assertThat(elements).isNotNull();
        }


        @BeforeEach
        @Test
        void getTypes(Types types) {
            assertThat(types).isNotNull();
        }

        /**
         * Do some non-trivial operation with {@link Element} instances because they stop working after
         * compilation stops.
         */
        @Test
        void elementsAreValidAndWorking(Elements elements) {
            TypeElement stringElement = elements.getTypeElement(String.class.getName());
            assertThat(stringElement.getEnclosingElement())
                    .isEqualTo(elements.getPackageElement("java.lang"));
        }

        /**
         * Do some non-trivial operation with {@link TypeMirror} instances because they stop working after
         * compilation stops.
         */
        @Test
        void typeMirrorsAreValidAndWorking(Elements elements, Types types) {
            DeclaredType arrayListOfString = types.getDeclaredType(
                    elements.getTypeElement(ArrayList.class.getName()),
                    elements.getTypeElement(String.class.getName()).asType());
            DeclaredType listOfExtendsObjectType = types.getDeclaredType(
                    elements.getTypeElement(List.class.getName()),
                    types.getWildcardType(elements.getTypeElement(Object.class.getName()).asType(), null));
            assertThat(types.isAssignable(arrayListOfString, listOfExtendsObjectType)).isTrue();
        }
    }
}
