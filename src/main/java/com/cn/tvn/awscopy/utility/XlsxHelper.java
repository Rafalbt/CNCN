package com.cn.tvn.awscopy.utility;

import com.cn.tvn.awscopy.model.PrefixedObject;
import lombok.extern.java.Log;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.hibernate.annotations.Comment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Log
@Component
public class XlsxHelper {

    @Value("${s3sync.includedPrefixes}")
    private List<String> includedPrefixes;

    public ParsedXlsx GetObjectsFromXlsx(MultipartFile xlsxFile, int sheetIndex, int startRow, Character objectsColumn, Character prefixesColumn)
            throws Exception {
        try (Workbook workbook = WorkbookFactory.create(xlsxFile.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            Set<PrefixedObject> prefixedObjects = new LinkedHashSet<>();
            Set<Integer> emptyObjectsRows = new LinkedHashSet<>();
            Set<String> duplicateObjects = new LinkedHashSet<>();
            Set<String> ignoredObjects = new LinkedHashSet<>();
            int columnIndex = MapColumnToIndex(objectsColumn);
            int prefixesColumnIndex = MapColumnToIndex(prefixesColumn);
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell objectCell = row.getCell(columnIndex);
                    if (objectCell != null) {
                        String objectValue = objectCell.getStringCellValue();
                        if (objectValue != null && !objectValue.isEmpty()) {
                            Cell srcPrefixCell = row.getCell(prefixesColumnIndex);
                            String srcPrefixValue = srcPrefixCell != null
                                    ? srcPrefixCell.getStringCellValue()
                                    : "";
                            if (srcPrefixValue == null) srcPrefixValue = "";

                            if (includedPrefixes != null &&
                                    !includedPrefixes.isEmpty() &&
                                    includedPrefixes.stream().noneMatch(srcPrefixValue::equalsIgnoreCase)) {
                                ignoredObjects.add(srcPrefixValue + "/" + objectValue);
                                continue;
                            }

                            if (!prefixedObjects.add(new PrefixedObject(srcPrefixValue, objectValue)))
                                duplicateObjects.add(objectValue);

                        } else {
                            emptyObjectsRows.add(row.getRowNum());
                        }
                    }
                }
            }
            return new ParsedXlsx(
                    xlsxFile.getOriginalFilename(),
                    prefixedObjects,
                    ignoredObjects,
                    emptyObjectsRows,
                    duplicateObjects);
        } catch (IOException | EncryptedDocumentException e) {
            log.severe(e.getMessage());
            throw e;
        }
    }

    private static int MapColumnToIndex(char c) {
        return c - 'A';
    }

}
