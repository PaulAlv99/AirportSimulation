package edu.uni.airportsim.io;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvParserTest {
    @Test
    void parsesQuotedCommasAndEscapedQuotes() {
        CsvParser parser = new CsvParser();

        List<String> values = parser.parseLine("APT-1,\"Airport, Main\",\"Terminal \"\"A\"\"\"");

        assertEquals(List.of("APT-1", "Airport, Main", "Terminal \"A\""), values);
    }
}
