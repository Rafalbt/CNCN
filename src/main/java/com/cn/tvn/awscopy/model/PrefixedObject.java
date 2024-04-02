package com.cn.tvn.awscopy.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrefixedObject {
    private String prefix;
    private String object;

    @Override
    public String toString() {
        return GetPrefixedObject(prefix, object);
    }

    public static String GetPrefixedObject(String prefix, String object) {
        return prefix + (prefix.endsWith("/") ? "" : "/") + object;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        PrefixedObject rhs = (PrefixedObject) obj;
        return this.object.equals(rhs.object);
    }

    @Override
    public int hashCode() {
        return this.object.hashCode();
    }
}
