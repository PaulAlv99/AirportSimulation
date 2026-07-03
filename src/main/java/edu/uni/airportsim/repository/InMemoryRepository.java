package edu.uni.airportsim.repository;

import edu.uni.airportsim.domain.Identifiable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryRepository<T extends Identifiable> implements Repository<T> {
    private final Map<String, T> items = new LinkedHashMap<>();

    @Override
    public void save(T item) {
        items.put(item.getId(), item);
    }

    @Override
    public Optional<T> findById(String id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(items.values());
    }
}
