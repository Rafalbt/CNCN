package com.cn.tvn.awscopy.controller;

import com.cn.tvn.awscopy.model.ObjectToCopy;
import com.cn.tvn.awscopy.model.status.ObjectStatus;
import com.cn.tvn.awscopy.service.ListToCopyService;
import com.cn.tvn.awscopy.service.ObjectToCopyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/objects")
public class ObjectToCopyController {

    @Autowired
    private ObjectToCopyService service;

    @Autowired
    private ListToCopyService listToCopyService;

    @GetMapping("/{id}")
    public ResponseEntity<ObjectToCopy> getEntity(@PathVariable("id") Long objectId) {
        try {
            return ResponseEntity.ok(service.findById(objectId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ObjectToCopy>> getObjectsByStatus(@PathVariable("status") ObjectStatus status) {
        try {
            return ResponseEntity.ok(service.findByStatus(status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status/{status}/list/{listId}")
    public ResponseEntity<List<ObjectToCopy>> getListObjectsByStatus(@PathVariable("status") ObjectStatus status, @PathVariable("listId") Long listId) {
        try {
            var list = listToCopyService.findById(listId);
            if (list == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(service.findByParentListAndStatus(list, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
