package edu.uni.airportsim.io;

import java.nio.file.Path;
import java.util.List;

public interface CsvImporter<T> {
    List<T> importFrom(Path csvFile);
}
