module com.google.testing.compile {
    requires com.google.common.truth;
    requires java.compiler;
    requires jdk.compiler;
    requires com.google.auto.value;
    requires org.junit.jupiter.api;

    exports io.jbock.testing.compile;
}
