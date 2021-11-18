[![compile-testing](https://maven-badges.herokuapp.com/maven-central/io.github.jbock-java/compile-testing/badge.svg?subject=compile-testing)](https://maven-badges.herokuapp.com/maven-central/io.github.jbock-java/compile-testing)

This is a fork of [compile-testing](https://github.com/google/compile-testing)
but with a `module-info.java` and a gradle build.
It does not use Sun APIs and is buildable with Java 17.

Methods `containsElementsIn`, `hasSourceEquivalentTo` and `generatesSources`
have been marked as deprecated. They throw `UnsupportedOperationException` now.
The new method `containsLines`, which does fancy string comparison
on the generated code,
can be used instead.

