package edu.uni.airportsim.domain;

import java.time.LocalDate;
import java.util.Objects;

public record Document(DocumentKind kind, String number, String issuingCountry, LocalDate expiryDate) {
    public Document {
        kind = Objects.requireNonNull(kind, "kind");
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("number must not be blank");
        }
        issuingCountry = issuingCountry == null ? "" : issuingCountry;
    }
}
