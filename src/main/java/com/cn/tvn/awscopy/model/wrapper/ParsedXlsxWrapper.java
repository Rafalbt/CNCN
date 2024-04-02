package com.cn.tvn.awscopy.model.wrapper;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class ParsedXlsxWrapper {
    private String fileName;
    private Integer validObjectsCount;
    private Set<String> ignoredObjects;
    private Set<Integer> emptyObjectsRows;
    private Set<String> duplicateObjects;
}
