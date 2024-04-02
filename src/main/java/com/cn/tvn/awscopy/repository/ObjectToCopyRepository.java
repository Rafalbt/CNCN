package com.cn.tvn.awscopy.repository;

import com.cn.tvn.awscopy.model.ListToCopy;
import com.cn.tvn.awscopy.model.status.ObjectStatus;
import com.cn.tvn.awscopy.model.ObjectToCopy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ObjectToCopyRepository extends JpaRepository<ObjectToCopy, Long> {

    Page<ObjectToCopy> findAll(Pageable pageable);

    @Query("SELECT COUNT(o) FROM ObjectToCopy o WHERE o.parentList = :parentList")
    Integer countAllWithParentList(ListToCopy parentList);

    List<ObjectToCopy> findAllByParentListOrderByCreatedAtDesc(ListToCopy parentList);

    @Query("SELECT o FROM ObjectToCopy o WHERE o.parentList = :list AND o.status = :objectStatus ORDER BY o.createdAt ASC LIMIT 1")
    ObjectToCopy findFirstWithParentListAndStatus(ListToCopy list, ObjectStatus objectStatus);

    @Modifying
    @Transactional
    @Query("DELETE FROM ObjectToCopy o WHERE o.parentList = :parentList")
    void deleteAllByParentList(ListToCopy parentList);

    @Query("SELECT COUNT(o) FROM ObjectToCopy o WHERE o.parentList = :list AND o.status = :objectStatus")
    int countAllWithParentListAndStatus(ListToCopy list, ObjectStatus objectStatus);

    List<ObjectToCopy> findAllByStatus(ObjectStatus status);

    @Query("SELECT o FROM ObjectToCopy o WHERE o.parentList = :list AND o.status = :status")
    List<ObjectToCopy> findAllByParentListAndStatus(ListToCopy list, ObjectStatus status);

}
