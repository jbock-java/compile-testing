/*
 * Copyright (C) 2013 Google, Inc.
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

import javax.tools.ForwardingJavaFileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static javax.tools.JavaFileObject.Kind.SOURCE;

/**
 * A utility class for creating {@link JavaFileObject} instances.
 *
 * @author Gregory Kick
 */
public final class JavaFileObjects {
    private JavaFileObjects() {
    }

    /**
     * Creates a {@link JavaFileObject} with a path corresponding to the {@code fullyQualifiedName}
     * containing the give {@code source}. The returned object will always be read-only and have the
     * {@link Kind#SOURCE} {@linkplain JavaFileObject#getKind() kind}.
     *
     * <p>Note that this method makes no attempt to verify that the name matches the contents of the
     * source and compilation errors may result if they do not match.
     */
    public static JavaFileObject forSourceString(String fullyQualifiedName, String source) {
        requireNonNull(fullyQualifiedName);
        if (fullyQualifiedName.startsWith("package ")) {
            throw new IllegalArgumentException(
                    String.format("fullyQualifiedName starts with \"package\" (%s). Did you forget to "
                            + "specify the name and specify just the source text?", fullyQualifiedName));
        }
        return new StringSourceJavaFileObject(fullyQualifiedName, requireNonNull(source));
    }

    /**
     * Behaves exactly like {@link #forSourceString}, but joins lines so that multi-line source
     * strings may omit the newline characters.  For example: <pre>   {@code
     *
     *   JavaFileObjects.forSourceLines("example.HelloWorld",
     *       "package example;",
     *       "",
     *       "final class HelloWorld {",
     *       "  void sayHello() {",
     *       "    System.out.println(\"hello!\");",
     *       "  }",
     *       "}");
     *   }</pre>
     */
    public static JavaFileObject forSourceLines(String fullyQualifiedName, String... lines) {
        return forSourceLines(fullyQualifiedName, Arrays.asList(lines));
    }

    /** An overload of {@code #forSourceLines} that takes an {@code Iterable<String>}. */
    public static JavaFileObject forSourceLines(String fullyQualifiedName, Iterable<String> lines) {
        return forSourceString(fullyQualifiedName, StreamSupport.stream(lines.spliterator(), false).collect(Collectors.joining("\n")));
    }

    private static final class StringSourceJavaFileObject extends SimpleJavaFileObject {
        final String source;
        final long lastModified;

        StringSourceJavaFileObject(String fullyQualifiedName, String source) {
            super(createUri(fullyQualifiedName), SOURCE);
            // TODO(gak): check that fullyQualifiedName looks like a fully qualified class name
            this.source = source;
            this.lastModified = System.currentTimeMillis();
        }

        static URI createUri(String fullyQualifiedClassName) {
            return URI.create(fullyQualifiedClassName.replace('.', '/')
                    + SOURCE.extension);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }

        @Override
        public OutputStream openOutputStream() {
            throw new IllegalStateException();
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(source.getBytes(Charset.defaultCharset()));
        }

        @Override
        public Writer openWriter() {
            throw new IllegalStateException();
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) {
            return new StringReader(source);
        }

        @Override
        public long getLastModified() {
            return lastModified;
        }
    }

    /**
     * Returns a {@link JavaFileObject} for the resource at the given {@link URL}. The returned object
     * will always be read-only and the {@linkplain JavaFileObject#getKind() kind} is inferred via
     * the {@link Kind#extension}.
     */
    public static JavaFileObject forResource(URL resourceUrl) {
        if ("jar".equals(resourceUrl.getProtocol())) {
            return new JarFileJavaFileObject(resourceUrl);
        } else {
            return new ResourceSourceJavaFileObject(resourceUrl);
        }
    }

    /**
     * Returns a {@link JavaFileObject} for the class path resource with the given
     * {@code resourceName}. This method is equivalent to invoking
     * {@code forResource(Resources.getResource(resourceName))}.
     */
    public static JavaFileObject forResource(String resourceName) {
        ClassLoader loader =
                Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(resourceName);
        Preconditions.checkArgument(url != null, "resource %s not found.", resourceName);
        return forResource(url);
    }

    static Kind deduceKind(URI uri) {
        String path = uri.getPath();
        for (Kind kind : Kind.values()) {
            if (path.endsWith(kind.extension)) {
                return kind;
            }
        }
        return Kind.OTHER;
    }

    static byte[] asBytes(JavaFileObject javaFileObject) {
        return asBytes(() -> {
            try {
                return javaFileObject.openInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static byte[] asBytes(Supplier<InputStream> supplier) {
        try (InputStream is = supplier.get()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class JarFileJavaFileObject
            extends ForwardingJavaFileObject<ResourceSourceJavaFileObject> {
        JarFileJavaFileObject(URL jarUrl) {
            // this is a cheap way to give SimpleJavaFileObject a uri that satisfies the contract
            // then we just override the methods that we want to behave differently for jars
            super(new ResourceSourceJavaFileObject(jarUrl, getPathUri(jarUrl)));
        }

        static URI getPathUri(URL jarUrl) {
            List<String> parts = Arrays.asList(jarUrl.getPath().split("[!]", -1));
            Preconditions.checkArgument(parts.size() == 2,
                    "The jar url separator (!) appeared more than once in the url: %s", jarUrl);
            String pathPart = parts.get(1);
            Preconditions.checkArgument(!pathPart.endsWith("/"), "cannot create a java file object for a directory: %s",
                    pathPart);
            return URI.create(pathPart);
        }
    }

    private static final class ResourceSourceJavaFileObject extends SimpleJavaFileObject {
        final byte[] resourceByteSource;

        /** Only to avoid creating the URI twice. */
        ResourceSourceJavaFileObject(URL resourceUrl, URI resourceUri) {
            super(resourceUri, deduceKind(resourceUri));
            this.resourceByteSource = asBytes(() -> {
                try {
                    return resourceUrl.openStream();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        ResourceSourceJavaFileObject(URL resourceUrl) {
            this(resourceUrl, URI.create(resourceUrl.toString()));
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return new String(resourceByteSource, Charset.defaultCharset());
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(resourceByteSource);
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) {
            return new InputStreamReader(new ByteArrayInputStream(resourceByteSource));
        }
    }
}
