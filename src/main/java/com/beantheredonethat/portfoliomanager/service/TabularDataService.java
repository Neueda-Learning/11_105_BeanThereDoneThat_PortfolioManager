package com.beantheredonethat.portfoliomanager.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class TabularDataService {

    public ParsedTabularData read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a CSV or Excel file to import.");
        }

        String fileName = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().trim().toLowerCase(Locale.ROOT);

        try {
            if (fileName.endsWith(".csv")) {
                return readCsv(file);
            }
            if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                return readWorkbook(file);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read the uploaded file.", ex);
        }

        throw new IllegalArgumentException("Unsupported file format. Upload a .csv, .xlsx, or .xls file.");
    }

    public void validateHeaders(List<String> actualHeaders, List<String> expectedHeaders) {
        if (!Objects.equals(actualHeaders, expectedHeaders)) {
            throw new IllegalArgumentException(
                    "Invalid file format. Expected headers: " + String.join(", ", expectedHeaders));
        }
    }

    public String writeCsv(List<String> headers, List<List<String>> rows) {
        StringWriter writer = new StringWriter();

        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            printer.printRecord(headers);
            for (List<String> row : rows) {
                printer.printRecord(row);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate CSV content.", ex);
        }

        return writer.toString();
    }

    public String writeTemplate(List<String> headers) {
        return writeCsv(headers, List.of());
    }

    private ParsedTabularData readCsv(MultipartFile file) throws IOException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                 .build()
                     .parse(reader)) {

            List<String> headers = new ArrayList<>(parser.getHeaderNames());
            List<TabularRow> rows = new ArrayList<>();

            for (CSVRecord record : parser) {
                Map<String, String> values = new LinkedHashMap<>();
                boolean hasValue = false;
                for (String header : headers) {
                    String value = record.isMapped(header) ? record.get(header) : "";
                    value = value == null ? "" : value.trim();
                    if (!value.isEmpty()) {
                        hasValue = true;
                    }
                    values.put(header, value);
                }
                if (hasValue) {
                    rows.add(new TabularRow((int) record.getRecordNumber() + 1, values));
                }
            }

            return new ParsedTabularData(headers, rows);
        }
    }

    private ParsedTabularData readWorkbook(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("The uploaded workbook is empty.");
            }

            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("The uploaded workbook does not contain a header row.");
            }

            List<String> headers = new ArrayList<>();
            int lastCellIndex = headerRow.getLastCellNum();
            for (int cellIndex = 0; cellIndex < lastCellIndex; cellIndex++) {
                headers.add(readCell(headerRow.getCell(cellIndex), formatter));
            }

            List<TabularRow> rows = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Map<String, String> values = new LinkedHashMap<>();
                boolean hasValue = false;
                for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                    String value = readCell(row.getCell(cellIndex), formatter).trim();
                    if (!value.isEmpty()) {
                        hasValue = true;
                    }
                    values.put(headers.get(cellIndex), value);
                }

                if (hasValue) {
                    rows.add(new TabularRow(rowIndex + 1, values));
                }
            }

            return new ParsedTabularData(headers, rows);
        }
    }

    private String readCell(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell);
    }

    public static class ParsedTabularData {

        private final List<String> headers;
        private final List<TabularRow> rows;

        public ParsedTabularData(List<String> headers, List<TabularRow> rows) {
            this.headers = headers;
            this.rows = rows;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<TabularRow> getRows() {
            return rows;
        }
    }

    public static class TabularRow {

        private final int rowNumber;
        private final Map<String, String> values;

        public TabularRow(int rowNumber, Map<String, String> values) {
            this.rowNumber = rowNumber;
            this.values = values;
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public String get(String header) {
            return values.getOrDefault(header, "");
        }

        public Map<String, String> getValues() {
            return values;
        }
    }
}