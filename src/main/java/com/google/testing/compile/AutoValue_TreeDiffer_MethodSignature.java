package com.google.testing.compile;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableList;

import javax.annotation.processing.Generated;
import javax.lang.model.type.TypeMirror;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_TreeDiffer_MethodSignature extends TreeDiffer.MethodSignature {

    private final String name;

    private final ImmutableList<Equivalence.Wrapper<TypeMirror>> parameterTypes;

    AutoValue_TreeDiffer_MethodSignature(
            String name,
            ImmutableList<Equivalence.Wrapper<TypeMirror>> parameterTypes) {
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        this.name = name;
        if (parameterTypes == null) {
            throw new NullPointerException("Null parameterTypes");
        }
        this.parameterTypes = parameterTypes;
    }

    @Override
    String name() {
        return name;
    }

    @Override
    ImmutableList<Equivalence.Wrapper<TypeMirror>> parameterTypes() {
        return parameterTypes;
    }

    @Override
    public String toString() {
        return "MethodSignature{"
                + "name=" + name + ", "
                + "parameterTypes=" + parameterTypes
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof TreeDiffer.MethodSignature) {
            TreeDiffer.MethodSignature that = (TreeDiffer.MethodSignature) o;
            return this.name.equals(that.name())
                    && this.parameterTypes.equals(that.parameterTypes());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= name.hashCode();
        h$ *= 1000003;
        h$ ^= parameterTypes.hashCode();
        return h$;
    }

}
