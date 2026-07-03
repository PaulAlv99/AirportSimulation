package edu.uni.airportsim.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvParser {
    public List<Map<String, String>> parse(Path path) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8)
                    .stream()
                    .filter(line -> !line.isBlank())
                    .toList();
            if (lines.isEmpty()) {
                return List.of();
            }

            List<String> headers = parseLine(lines.getFirst());
            List<Map<String, String>> rows = new ArrayList<>();
            for (int index = 1; index < lines.size(); index++) {
                List<String> values = parseLine(lines.get(index));
                Map<String, String> row = new LinkedHashMap<>();
                for (int column = 0; column < headers.size(); column++) {
                    String value = column < values.size() ? values.get(column) : "";
                    row.put(headers.get(column), value);
                }
                rows.add(row);
            }
            return rows;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to parse CSV file: " + path, exception);
        }
    }

    public List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                boolean escapedQuote = insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"';
                if (escapedQuote) {
                    current.append('"');
                    index++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (character == ',' && !insideQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        values.add(current.toString());
        return values;
    }
}
