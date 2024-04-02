package com.cn.tvn.awscopy.repository;

import com.cn.tvn.awscopy.model.status.ListStatus;
import com.cn.tvn.awscopy.model.ListToCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListToCopyRepository extends JpaRepository<ListToCopy, Long> {
     @Query("SELECT id FROM ListToCopy ORDER BY createdAt DESC")
     List<Long> findAllIds();

     @Query("SELECT o FROM ListToCopy o WHERE o.status = :listStatus ORDER BY o.createdAt ASC LIMIT 1")
     ListToCopy findFirstWithStatus(ListStatus listStatus);

     @Query("SELECT COUNT(o) FROM ListToCopy o WHERE o.id = :id AND o.status = 'CANCELLED'")
     Integer isCancelled(Long id);

     List<ListToCopy> findAllByStatus(ListStatus status);
}


