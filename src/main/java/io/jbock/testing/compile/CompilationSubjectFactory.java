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

import io.jbock.common.truth.FailureMetadata;
import io.jbock.common.truth.Subject;
import io.jbock.common.truth.Truth;

/** A {@link Truth} subject factory for a {@link Compilation}. */
final class CompilationSubjectFactory implements Subject.Factory<CompilationSubject, Compilation> {

    @Override
    public CompilationSubject createSubject(FailureMetadata failureMetadata, Compilation that) {
        return new CompilationSubject(failureMetadata, that);
    }
}
