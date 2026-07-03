package edu.uni.airportsim.repository;

import edu.uni.airportsim.domain.Identifiable;

import java.util.List;
import java.util.Optional;

public interface Repository<T extends Identifiable> {
    void save(T item);

    Optional<T> findById(String id);

    List<T> findAll();
}
