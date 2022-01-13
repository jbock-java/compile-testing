[![compile-testing](https://maven-badges.herokuapp.com/maven-central/io.github.jbock-java/compile-testing/badge.svg?subject=compile-testing)](https://maven-badges.herokuapp.com/maven-central/io.github.jbock-java/compile-testing)

This is a fork of [compile-testing](https://github.com/google/compile-testing)
but with a `module-info.java` and a gradle build.
It requires Java 11 and does not use Sun APIs.
It has a JUnit 5 (Jupiter) dependency rather than JUnit 4.
Since Version `0.19.11` it contains [Kiskae's CompileTestingExtension](https://github.com/Kiskae/compile-testing-extension/), instead of CompilationRule.

Methods `containsElementsIn`, `hasSourceEquivalentTo` and `generatesSources`
have been marked as deprecated. They throw `UnsupportedOperationException` now.

The new method `containsLines`, which does fancy string comparison
on the generated code,
can be used instead of `containsElementsIn`.

