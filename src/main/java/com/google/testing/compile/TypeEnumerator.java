/*
 * Copyright (C) 2014 Google, Inc.
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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides information about the set of types that are declared by a {@code CompilationUnitTree}.
 *
 * @author Stephen Pratt
 */
final class TypeEnumerator {
    private static final TypeScanner nameVisitor = new TypeScanner();

    private TypeEnumerator() {
    }

    /**
     * Returns a set of strings containing the fully qualified names of all
     * the types that are declared by the given CompilationUnitTree
     */
    static Set<String> getTopLevelTypes(CompilationUnitTree t) {
        return new LinkedHashSet<>(nameVisitor.scan(t, null));
    }

    /** A {@link TreeScanner} for determining type declarations */
    @SuppressWarnings("restriction") // Sun APIs usage intended
    static final class TypeScanner extends TreeScanner<Set<String>, Void> {
        @Override
        public Set<String> scan(Tree node, Void v) {
            Set<String> result = super.scan(node, v);
            return result != null ? result : Set.of();
        }

        @Override
        public Set<String> reduce(Set<String> r1, Set<String> r2) {
            return Util.union(r1, r2);
        }

        @Override
        public Set<String> visitClass(ClassTree reference, Void v) {
            return Set.of(reference.getSimpleName().toString());
        }

        @Override
        public Set<String> visitExpressionStatement(
                ExpressionStatementTree reference, Void v) {
            return scan(reference.getExpression(), v);
        }

        @Override
        public Set<String> visitIdentifier(IdentifierTree reference, Void v) {
            return Set.of(reference.getName().toString());
        }

        @Override
        public Set<String> visitMemberSelect(MemberSelectTree reference, Void v) {
            Set<String> expressionSet = scan(reference.getExpression(), v);
            if (expressionSet.size() != 1) {
                throw new AssertionError("Internal error in NameFinder. Expected to find exactly one "
                        + "identifier in the expression set. Found " + expressionSet);
            }
            String expressionStr = expressionSet.iterator().next();
            return Set.of(String.format("%s.%s", expressionStr, reference.getIdentifier()));
        }

        @Override
        public Set<String> visitCompilationUnit(CompilationUnitTree reference, Void v) {
            Set<String> packageSet = reference.getPackageName() == null ?
                    Set.of("") : scan(reference.getPackageName(), v);
            if (packageSet.size() != 1) {
                throw new AssertionError("Internal error in NameFinder. Expected to find at most one " +
                        "package identifier. Found " + packageSet);
            }
            final String packageName = packageSet.iterator().next();
            Set<String> typeDeclSet = scan(reference.getTypeDecls(), v);
            if (typeDeclSet == null) {
                return Set.of();
            }
            return typeDeclSet.stream()
                    .map(typeName -> packageName.isEmpty() ? typeName :
                            String.format("%s.%s", packageName, typeName)).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}