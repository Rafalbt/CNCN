package com.cn.tvn.awscopy.utility;

import com.cn.tvn.awscopy.model.PrefixedObject;

import java.util.Set;

public record ParsedXlsx(
        String fileName,
        Set<PrefixedObject> objects,
        Set<String> ignoredObjects,
        Set<Integer> emptyObjectRows,

        Set<String> duplicateObjects
) {}
