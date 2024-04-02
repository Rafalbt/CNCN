package com.cn.tvn.awscopy.service;

import com.cn.tvn.awscopy.model.ListToCopy;
import com.cn.tvn.awscopy.model.status.FileStatus;
import com.cn.tvn.awscopy.model.FileToCopy;
import com.cn.tvn.awscopy.model.ObjectToCopy;
import com.cn.tvn.awscopy.model.S3FileCopyRequest;
import com.cn.tvn.awscopy.repository.FileToCopyRepository;
import com.cn.tvn.awscopy.service.s3.IS3CopyService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Log
@Service
public class FileToCopyService {

    @Autowired
    private FileToCopyRepository repository;

    @Autowired
    @Qualifier("s3TransferService")
    private IS3CopyService s3CopyService;

    public void copy(FileToCopy file) throws Exception {
        s3CopyService.copyObject(new S3FileCopyRequest(
                file.getSourceBucket(),
                file.getSourceFile(),
                file.getDestBucket(),
                file.getDestFile()
        ));
    }

    public void cancel() {
        s3CopyService.cancel();
    }

    public FileToCopy findFirstWithParentObjectAndStatus(ObjectToCopy object, FileStatus fileStatus) {
        return repository.findFirstWithParentObjectAndStatus(object, fileStatus);
    }

    public void save(FileToCopy file) {
        repository.save(file);
    }

    public void deleteAllByParentList(ListToCopy parentList) {
        repository.deleteAllByParentList(parentList);
    }

    public int countAllWithParentObject(ObjectToCopy object) {
        return repository.countAllWithParentObject(object);
    }

    public int countAllWithParentObjectAndStatus(ObjectToCopy object, FileStatus fileStatus) {
        return repository.countAllWithParentObjectAndStatus(object, fileStatus);
    }
}
