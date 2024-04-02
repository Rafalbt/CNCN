package com.cn.tvn.awscopy.controller;

import com.cn.tvn.awscopy.model.*;
import com.cn.tvn.awscopy.model.status.ListStatus;
import com.cn.tvn.awscopy.model.status.ObjectStatus;
import com.cn.tvn.awscopy.model.wrapper.ListToCopyWrapper;
import com.cn.tvn.awscopy.model.wrapper.ParsedXlsxWrapper;
import com.cn.tvn.awscopy.service.FileToCopyService;
import com.cn.tvn.awscopy.service.ListToCopyService;
import com.cn.tvn.awscopy.service.ObjectToCopyService;
import com.cn.tvn.awscopy.utility.ParsedXlsx;
import com.cn.tvn.awscopy.utility.XlsxHelper;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Log
@RestController
@RequestMapping("/api/v1/lists")
public class ListToCopyController {

    @Autowired
    private ListToCopyService listToCopyService;

    @Autowired
    private FileToCopyService fileToCopyService;

    @Autowired
    private ObjectToCopyService objectToCopyService;

    @Value("${s3sync.sourceBucket}")
    String sourceBucket;

    @Autowired
    XlsxHelper xlsxHelper;

    @PostMapping("/add")
    public ResponseEntity<InitResult> load(
            @RequestParam(value = "file") MultipartFile file,
            @RequestParam(value = "sheetIndex", required = false, defaultValue = "0") Optional<Integer> sheetIndex,
            @RequestParam(value = "startRow", required = false, defaultValue = "1") Optional<Integer> startRow,
            @RequestParam(value = "objectsColumn", required = false, defaultValue = "B") Optional<Character> objectsColumn,
            @RequestParam(value = "prefixesColumn", required = false, defaultValue = "C") Optional<Character> prefixesColumn
    ) {
        try {
            if (file.isEmpty() ||
                    (startRow.isPresent() && startRow.get() < 0) ||
                    (objectsColumn.isPresent() && !Character.isLetter(objectsColumn.get())) ||
                    (sheetIndex.isPresent() && sheetIndex.get() < 0))
                throw new IllegalArgumentException("Invalid input parameters");

            ParsedXlsx parsedData = xlsxHelper.GetObjectsFromXlsx(
                    file,
                    sheetIndex.orElse(0),
                    startRow.orElse(1),
                    objectsColumn.orElse('B'),
                    prefixesColumn.orElse('C'));

            ListToCopy listToCopy = ListToCopy.builder()
                    .fileName(parsedData.fileName())
                    .status(ListStatus.CREATED)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            var savedList = listToCopyService.save(listToCopy);
            if (savedList == null) throw new Exception("Failed to save list");

            List<ObjectToCopy> objectsToCopy = ObjectToCopy.createObjectsToCopy(
                    sourceBucket,
                    parsedData.objects(),
                    ObjectStatus.TO_LIST,
                    listToCopy);

            if (objectToCopyService.saveAll(objectsToCopy) == null)
                throw new Exception("Failed to save objects to copy");

            listToCopy.setStatus(ListStatus.TO_LIST);
            savedList = listToCopyService.save(listToCopy);

            return ResponseEntity.ok(InitResult.from(
                    ParsedXlsxWrapper.builder()
                            .fileName(parsedData.fileName())
                            .validObjectsCount(parsedData.objects().size())
                            .ignoredObjects(parsedData.ignoredObjects())
                            .emptyObjectsRows(parsedData.emptyObjectRows())
                            .duplicateObjects(parsedData.duplicateObjects())
                            .build(),
                    ListToCopyWrapper.from(
                        savedList,
                        objectToCopyService.countAllWithParentList(savedList))));
        } catch (Exception e) {
            log.severe(e.getMessage());
            return ResponseEntity.badRequest().body(InitResult.from(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteList(@PathVariable Long id) {
        try {
            var list = listToCopyService.findById(id);
            if (list == null) throw new IllegalArgumentException("List not found: " + id);

            objectToCopyService.deleteAllByParentList(list);
            listToCopyService.deleteById(id);

            return ResponseEntity.ok(id.toString());
        } catch (Exception e) {
            log.severe(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Long>> getAllIDs() {
        try {
            return ResponseEntity.ok(listToCopyService.findAllIds());
        } catch (Exception e) {
            log.severe(e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListToCopyWrapper> getById(@PathVariable Long id) {
        try {
            var list = listToCopyService.findById(id);
            if (list != null)
                return ResponseEntity.ok(ListToCopyWrapper.from(list, objectToCopyService.countAllWithParentList(list)));
            else
                throw new IllegalArgumentException("List not found: " + id);
        } catch (Exception e) {
            log.severe(e.getMessage());
            return ResponseEntity.badRequest().body(ListToCopyWrapper.from(e.getMessage()));
        }
    }


    @GetMapping("/{id}/objects")
    public ResponseEntity<List<ObjectToCopy>> getObjects(@PathVariable Long id) {
        try {
            var list = listToCopyService.findById(id);
            if (list != null)
                return ResponseEntity.ok(objectToCopyService.findAllByParentList(list));
            else
                throw new IllegalArgumentException("List not found: " + id);
        } catch (Exception e) {
            log.severe(e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ListToCopyWrapper> cancelList(@PathVariable Long id) {
        try {
            var list = listToCopyService.findById(id);
            if (list != null) {
                fileToCopyService.cancel();

                list.setStatus(ListStatus.CANCELLED);

                listToCopyService.save(list);
                log.info("List " + id + " cancelled");
                return ResponseEntity.ok(ListToCopyWrapper.from(list, objectToCopyService.countAllWithParentList(list)));
            } else
                throw new IllegalArgumentException("List not found: " + id);
        } catch (Exception e) {
            log.severe(e.getMessage());
            return ResponseEntity.badRequest().body(ListToCopyWrapper.from(e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ListToCopy>> getByStatus(@PathVariable ListStatus status) {
        try {
            return ResponseEntity.ok(listToCopyService.findAllByStatus(status));
        } catch (Exception e) {
            log.severe(e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{listId}/report")
    public ResponseEntity<ListReport> getReport(@PathVariable Long listId) {
        try {
            ListToCopy list = listToCopyService.findById(listId);
            if (list == null) throw new IllegalArgumentException("List not found: " + listId);

            List<ObjectToCopy> objects = objectToCopyService.findAllByParentList(list);

            return ResponseEntity.ok(ListReport.builder()
                    .list(ListToCopyWrapper.from(list, objects.size()))
                    .objects(objects)
                    .build());
        } catch (Exception e) {
            log.severe(e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }
}
