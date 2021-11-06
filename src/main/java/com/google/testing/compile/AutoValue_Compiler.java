package com.google.testing.compile;

import com.google.common.collect.ImmutableList;

import javax.annotation.processing.Generated;
import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import java.io.File;
import java.util.Optional;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_Compiler extends Compiler {

  private final JavaCompiler javaCompiler;

  private final ImmutableList<Processor> processors;

  private final ImmutableList<String> options;

  private final Optional<ImmutableList<File>> classPath;

  private final Optional<ImmutableList<File>> annotationProcessorPath;

  AutoValue_Compiler(
      JavaCompiler javaCompiler,
      ImmutableList<Processor> processors,
      ImmutableList<String> options,
      Optional<ImmutableList<File>> classPath,
      Optional<ImmutableList<File>> annotationProcessorPath) {
    if (javaCompiler == null) {
      throw new NullPointerException("Null javaCompiler");
    }
    this.javaCompiler = javaCompiler;
    if (processors == null) {
      throw new NullPointerException("Null processors");
    }
    this.processors = processors;
    if (options == null) {
      throw new NullPointerException("Null options");
    }
    this.options = options;
    if (classPath == null) {
      throw new NullPointerException("Null classPath");
    }
    this.classPath = classPath;
    if (annotationProcessorPath == null) {
      throw new NullPointerException("Null annotationProcessorPath");
    }
    this.annotationProcessorPath = annotationProcessorPath;
  }

  @Override
  JavaCompiler javaCompiler() {
    return javaCompiler;
  }

  @Override
  public ImmutableList<Processor> processors() {
    return processors;
  }

  @Override
  public ImmutableList<String> options() {
    return options;
  }

  @Override
  public Optional<ImmutableList<File>> classPath() {
    return classPath;
  }

  @Override
  public Optional<ImmutableList<File>> annotationProcessorPath() {
    return annotationProcessorPath;
  }

  @Override
  public String toString() {
    return "Compiler{"
        + "javaCompiler=" + javaCompiler + ", "
        + "processors=" + processors + ", "
        + "options=" + options + ", "
        + "classPath=" + classPath + ", "
        + "annotationProcessorPath=" + annotationProcessorPath
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Compiler) {
      Compiler that = (Compiler) o;
      return this.javaCompiler.equals(that.javaCompiler())
          && this.processors.equals(that.processors())
          && this.options.equals(that.options())
          && this.classPath.equals(that.classPath())
          && this.annotationProcessorPath.equals(that.annotationProcessorPath());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= javaCompiler.hashCode();
    h$ *= 1000003;
    h$ ^= processors.hashCode();
    h$ *= 1000003;
    h$ ^= options.hashCode();
    h$ *= 1000003;
    h$ ^= classPath.hashCode();
    h$ *= 1000003;
    h$ ^= annotationProcessorPath.hashCode();
    return h$;
  }

}
