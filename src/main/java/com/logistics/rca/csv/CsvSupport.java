package com.logistics.rca.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class CsvSupport {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<CSVRecord> read(String classpathFile) {
        ClassPathResource resource = new ClassPathResource(classpathFile);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .setIgnoreSurroundingSpaces(true)
                     .build()
                     .parse(reader)) {
            List<CSVRecord> records = new ArrayList<>();
            parser.forEach(records::add);
            return records;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + classpathFile, e);
        }
    }

    public <T> List<T> map(String classpathFile, Function<CSVRecord, T> mapper) {
        return read(classpathFile).stream().map(mapper).toList();
    }

    public static String str(CSVRecord r, String col) {
        if (!r.isMapped(col)) {
            return null;
        }
        String v = r.get(col);
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    public static long lng(CSVRecord r, String col) {
        String v = str(r, col);
        return v == null ? 0L : Long.parseLong(v);
    }

    public static int integer(CSVRecord r, String col) {
        String v = str(r, col);
        return v == null ? 0 : Integer.parseInt(v);
    }

    public static Double dbl(CSVRecord r, String col) {
        String v = str(r, col);
        return v == null ? null : Double.parseDouble(v);
    }

    public static LocalDateTime ts(CSVRecord r, String col) {
        String v = str(r, col);
        if (v == null) {
            return null;
        }
        return LocalDateTime.parse(v, TS);
    }
}
