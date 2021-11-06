package com.google.testing.compile;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.CompilationUnitTree;

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_JavaSourcesSubject_TypedCompilationUnit extends JavaSourcesSubject.TypedCompilationUnit {

  private final CompilationUnitTree tree;

  private final ImmutableSet<String> types;

  AutoValue_JavaSourcesSubject_TypedCompilationUnit(
      CompilationUnitTree tree,
      ImmutableSet<String> types) {
    if (tree == null) {
      throw new NullPointerException("Null tree");
    }
    this.tree = tree;
    if (types == null) {
      throw new NullPointerException("Null types");
    }
    this.types = types;
  }

  @Override
  CompilationUnitTree tree() {
    return tree;
  }

  @Override
  ImmutableSet<String> types() {
    return types;
  }

  @Override
  public String toString() {
    return "TypedCompilationUnit{"
        + "tree=" + tree + ", "
        + "types=" + types
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof JavaSourcesSubject.TypedCompilationUnit) {
      JavaSourcesSubject.TypedCompilationUnit that = (JavaSourcesSubject.TypedCompilationUnit) o;
      return this.tree.equals(that.tree())
          && this.types.equals(that.types());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= tree.hashCode();
    h$ *= 1000003;
    h$ ^= types.hashCode();
    return h$;
  }

}
