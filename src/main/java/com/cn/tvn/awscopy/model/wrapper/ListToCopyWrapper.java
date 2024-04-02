package com.cn.tvn.awscopy.model.wrapper;

import com.cn.tvn.awscopy.model.ListToCopy;
import lombok.*;

import java.time.Instant;

@Data
@Builder
public class ListToCopyWrapper {
    private Long id;
    private String status;
    private Integer totalObjects;
    private String fileName;
    private Instant createdAt;
    private Instant updatedAt;

    private String error;   // fill only for failures

    public static ListToCopyWrapper from(ListToCopy listToCopy, int totalObjects) {
        return ListToCopyWrapper.builder()
                .id(listToCopy.getId())
                .status(listToCopy.getStatus().name())
                .fileName(listToCopy.getFileName())
                .createdAt(listToCopy.getCreatedAt())
                .updatedAt(listToCopy.getUpdatedAt())
                .totalObjects(totalObjects)
                .error("")
                .build();
    }

    public static ListToCopyWrapper from(String error) {
        return ListToCopyWrapper.builder()
                .error(error != null ? error : "")
                .build();
    }
}
