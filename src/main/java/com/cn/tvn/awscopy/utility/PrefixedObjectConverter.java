package com.cn.tvn.awscopy.utility;

import com.cn.tvn.awscopy.model.PrefixedObject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter
public class PrefixedObjectConverter implements AttributeConverter<PrefixedObject, String> {

    @Override
    public String convertToDatabaseColumn(PrefixedObject prefixedObject) {
        if (prefixedObject == null) {
            return null;
        }
        return prefixedObject.getPrefix() + ":" + prefixedObject.getObject();
    }

    @Override
    public PrefixedObject convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // split the string by the first occurrence of the colon
        String[] split = dbData.split(":", 2);
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid database value: " + dbData);
        }
        return new PrefixedObject(split[0], split[1]);
    }
}
