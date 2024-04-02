package com.cn.tvn.awscopy.model;

import com.cn.tvn.awscopy.model.wrapper.ListToCopyWrapper;
import com.cn.tvn.awscopy.model.wrapper.ParsedXlsxWrapper;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InitResult {
    ParsedXlsxWrapper parsedXlsxWrapper;
    ListToCopyWrapper listToCopyWrapper;

    private String error;   // fill only for failures

    public static InitResult from(ParsedXlsxWrapper parsedXlsxWrapper, ListToCopyWrapper listToCopyWrapper) {
        return InitResult.builder()
                .parsedXlsxWrapper(parsedXlsxWrapper)
                .listToCopyWrapper(listToCopyWrapper)
                .error("")
                .build();
    }

    public static InitResult from(String error) {
        return InitResult.builder()
                .error(error != null ? error : "")
                .build();
    }
}
