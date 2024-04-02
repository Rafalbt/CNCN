package com.cn.tvn.awscopy.service;

import com.cn.tvn.awscopy.model.status.ListStatus;
import com.cn.tvn.awscopy.model.ListToCopy;
import com.cn.tvn.awscopy.repository.ListToCopyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListToCopyService {

    @Autowired
    private ListToCopyRepository repository;

    public ListToCopy save(ListToCopy listToCopy) {
        return repository.save(listToCopy);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public ListToCopy findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Long> findAllIds() {
        return repository.findAllIds();
    }

    public ListToCopy findFirstWithStatus(ListStatus listStatus) {
        return repository.findFirstWithStatus(listStatus);
    }

    public boolean isCancelled(Long id) {
        return repository.isCancelled(id) != 0;
    }

    public List<ListToCopy> findAllByStatus(ListStatus status) {
        return repository.findAllByStatus(status);
    }
}
