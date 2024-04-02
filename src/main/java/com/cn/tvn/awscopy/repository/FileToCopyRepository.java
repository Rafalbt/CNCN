package com.cn.tvn.awscopy.repository;

import com.cn.tvn.awscopy.model.ListToCopy;
import com.cn.tvn.awscopy.model.status.FileStatus;
import com.cn.tvn.awscopy.model.FileToCopy;
import com.cn.tvn.awscopy.model.ObjectToCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FileToCopyRepository extends JpaRepository<FileToCopy, Long> {

    @Query("SELECT f FROM FileToCopy f WHERE f.parentObject = ?1 AND f.status = ?2 ORDER BY f.priority ASC LIMIT 1")
    FileToCopy findFirstWithParentObjectAndStatus(ObjectToCopy object, FileStatus fileStatus);

    @Modifying
    @Transactional
    @Query("DELETE FROM FileToCopy f WHERE f.parentObject IN (SELECT o FROM ObjectToCopy o WHERE o.parentList = :parentList)")
    void deleteAllByParentList(ListToCopy parentList);

    @Query("SELECT COUNT(f) FROM FileToCopy f WHERE f.parentObject = :object")
    int countAllWithParentObject(ObjectToCopy object);

    @Query("SELECT COUNT(f) FROM FileToCopy f WHERE f.parentObject = :object AND f.status = :fileStatus")
    int countAllWithParentObjectAndStatus(ObjectToCopy object, FileStatus fileStatus);
}
