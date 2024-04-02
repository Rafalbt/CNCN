package com.cn.tvn.awscopy.service.scheduled;

import com.cn.tvn.awscopy.model.ListToCopy;
import com.cn.tvn.awscopy.model.ObjectToCopy;
import com.cn.tvn.awscopy.model.status.ListStatus;
import com.cn.tvn.awscopy.model.status.ObjectStatus;
import com.cn.tvn.awscopy.service.ListToCopyService;
import com.cn.tvn.awscopy.service.ObjectToCopyService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Log
@Service
public class FindFilesScheduledService {
    @Autowired
    private ListToCopyService listToCopyService;

    @Autowired
    private ObjectToCopyService objectToCopyService;

    // 5 minutes = 300,000 milliseconds
    @Scheduled(fixedRate = 100)
    @Transactional
    public void findFiles() {
        var list = getNextListToFindFiles(true);
        if (list == null) {
//            log.fine("No lists ready to find files.");
            return;
        }

        log.info("Processing list to find files: " + list.getId());

        saveListStatus(list, ListStatus.LISTING);

        var object = getNextObjectToFindFiles(list);

        if (isListCancelled(list)) return;

        if (object == null) {
            log.info("No objects to find files in list " + list.getId());
            saveListFindFilesFinish(list);
            return;
        }

        log.fine("Found object to find files, id = " + object.getId() + " from list " + list.getId());

        try {
            saveObjectFindFilesStart(object);

            objectToCopyService.populateFilesToCopy(object);

            log.fine("Files found for object " + object.getId() + " from list " + list.getId());

            saveObjectFindFilesFinish(object, ObjectStatus.LISTED_OK);
        } catch (Exception e) {
            log.severe("Error finding files for object " + object.getId() + " from list " + list.getId());
            saveObjectFindFilesFinish(object, ObjectStatus.LISTED_FAILED);
        }

        if (isListCancelled(list)) return;

        if (null == getNextObjectToFindFiles(list)) {
            log.info("No more objects to process in this list " + list.getId());
            saveListFindFilesFinish(list);
        }
    }

    private boolean isListCancelled(ListToCopy list) {
        if (listToCopyService.isCancelled(list.getId())) {
            log.info("List " + list.getId() + " is cancelled");
            return true;
        }
        return false;
    }

    private void saveObjectFindFilesStart(ObjectToCopy object) {
        if (object.getStatus() != ObjectStatus.LISTING) {
            object.setStatus(ObjectStatus.LISTING);
            object.setListingStartTimestamp(Instant.now());
            objectToCopyService.save(object);
        }
    }

    private void saveListStatus(ListToCopy list, ListStatus status) {
        if (list.getStatus() != status) {
            list.setStatus(status);
            listToCopyService.save(list);
        }
    }

    private void saveObjectFindFilesFinish(ObjectToCopy object, ObjectStatus status) {
        object.setListingFinishTimestamp(Instant.now());
        object.setStatus(status);
        objectToCopyService.save(object);
    }

    private void saveListFindFilesFinish(ListToCopy list) {
        int notOK = objectToCopyService.countAllWithParentList(list)
                    - objectToCopyService.countAllWithParentListAndStatus(list, ObjectStatus.LISTED_OK);
        list.setStatus(notOK > 0 ? ListStatus.LISTED_FAILED : ListStatus.LISTED_OK);
        listToCopyService.save(list);
    }

    private ObjectToCopy getNextObjectToFindFiles(ListToCopy list) {
        return objectToCopyService.findFirstWithParentListAndStatus(list, ObjectStatus.TO_LIST);
    }

    private ListToCopy getNextListToFindFiles(boolean includeCurrent) {
        var list = includeCurrent
                ? listToCopyService.findFirstWithStatus(ListStatus.LISTING)
                : null;
        return list == null
                ? listToCopyService.findFirstWithStatus(ListStatus.TO_LIST)
                : list;
    }
}
