/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.jbock.testing.compile;

final class Preconditions {
    private Preconditions() {
    }

    static void checkArgument(
            boolean b, String errorMessageTemplate, Object p1) {
        if (!b) {
            throw new IllegalArgumentException(String.format(errorMessageTemplate, p1));
        }
    }

    static void checkArgument(boolean b, String errorMessageTemplate, long p1, int p2) {
        if (!b) {
            throw new IllegalArgumentException(String.format(errorMessageTemplate, p1, p2));
        }
    }

    static void checkArgument(
            boolean b, String errorMessageTemplate, Object p1, Object p2) {
        if (!b) {
            throw new IllegalArgumentException(String.format(errorMessageTemplate, p1, p2));
        }
    }

    static void checkArgument(
            boolean b,
            String errorMessageTemplate,
            Object p1,
            Object p2,
            Object p3) {
        if (!b) {
            throw new IllegalArgumentException(String.format(errorMessageTemplate, p1, p2, p3));
        }
    }

    static void checkState(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalStateException(String.valueOf(errorMessage));
        }
    }

    static void checkState(boolean b, String errorMessageTemplate, Object p1) {
        if (!b) {
            throw new IllegalStateException(String.format(errorMessageTemplate, p1));
        }
    }
}
