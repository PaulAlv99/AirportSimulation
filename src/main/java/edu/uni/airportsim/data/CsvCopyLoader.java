package edu.uni.airportsim.data;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class CsvCopyLoader {
    private final JdbcTemplate jdbcTemplate;

    public CsvCopyLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean copyIfPresent(Path csvFile, String tableName) {
        if (!Files.exists(csvFile)) {
            return false;
        }

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            CopyManager copyManager = pgConnection.getCopyAPI();
            try (InputStream inputStream = new ByteArrayInputStream(normalizeLineEndings(Files.readString(csvFile, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8))) {
                copyManager.copyIn("COPY " + tableName + " FROM STDIN WITH (FORMAT csv, HEADER true)", inputStream);
            } catch (IOException exception) {
                throw new UncheckedIOException("Failed to copy CSV into " + tableName + " from " + csvFile, exception);
            }
            return null;
        });
        return true;
    }

    private static String normalizeLineEndings(String content) {
        return content.replace("\r\n", "\n").replace('\r', '\n');
    }
}
