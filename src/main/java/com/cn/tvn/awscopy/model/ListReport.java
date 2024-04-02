package com.cn.tvn.awscopy.model;

import com.cn.tvn.awscopy.model.wrapper.ListToCopyWrapper;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ListReport {
    ListToCopyWrapper list;
    List<ObjectToCopy> objects;
}
