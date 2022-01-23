[![compile-testing](https://maven-badges.herokuapp.com/maven-central/io.github.jbock-java/compile-testing/badge.svg?subject=compile-testing)](https://maven-badges.herokuapp.com/maven-central/io.github.jbock-java/compile-testing)

This is a fork of [compile-testing](https://github.com/google/compile-testing) with the following modifications

* fully modular with `module-info.java`
* requires Java 11
* does not use Sun APIs
* JUnit 5 Jupiter
* since Version `0.19.11` it contains [Kiskae's CompileTestingExtension](https://github.com/Kiskae/compile-testing-extension/), instead of CompilationRule
* no guava dependency

Method `containsElementsIn(JavaFileObject)` in JavaFileObjectSubject was using internal sun APIs and had to be marked as deprecated. 
It throws `UnsupportedOperationException` now.

The new method `containsLines(JavaFileObject)` does not compile the expectation, but instead it does string comparison
on the contents of the JavaFileObject. It is an acceptable alternative but you may have to do some reformatting to migrate get existing tests.

