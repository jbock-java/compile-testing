module com.google.testing.compile {
    requires java.compiler;
    requires jdk.compiler;
    requires com.google.auto.value;
    requires io.jbock.common.truth;
    requires org.junit.jupiter.api;

    exports io.jbock.testing.compile;
}
