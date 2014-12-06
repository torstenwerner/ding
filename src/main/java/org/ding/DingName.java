package org.ding;

import static java.lang.String.format;

public class DingName {
    private final String namespace;
    private final String name;

    public DingName(String name) {
        this(null, name);
    }

    public DingName(String namespace, String name) {
        if (name == null) {
            throw new RuntimeException("name must not be null");
        }
        this.namespace = namespace;
        this.name = name;
    }

    public static DingName dingName(String namespace, String name) {
        return new DingName(namespace, name);
    }

    public static DingName dingName(String name) {
        return dingName(null, name);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DingName)) return false;

        final DingName dingName = (DingName) other;

        if (!name.equals(dingName.name)) return false;
        if (namespace != null ? !namespace.equals(dingName.namespace) : dingName.namespace != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int namespaceHash = namespace != null ? namespace.hashCode() : 0;
        return 31 * namespaceHash + name.hashCode();
    }

    @Override
    public String toString() {
        return namespace != null ? format("{%s}%s", namespace, name) : name;
    }
}
