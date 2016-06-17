package com.twitter.inject.annotations;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/* Pattern copied from com.google.inject.name.NamedImpl */
public class FlagImpl implements Flag, Serializable {
    private final String value;

    public FlagImpl(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public int hashCode() {
        // This is specified in java.lang.Annotation.
        return (127 * "value".hashCode()) ^ value.hashCode();
    }

    /** Flag specific equals */
    public boolean equals(Object o) {
        if (!(o instanceof Flag)) {
            return false;
        }

        Flag other = (Flag) o;
        return value.equals(other.value());
    }

    public String toString() {
        return "@" + Flag.class.getName() + "(value=" + value + ")";
    }

    public Class<? extends Annotation> annotationType() {
        return Flag.class;
    }

    private static final long serialVersionUID = 0;
}
