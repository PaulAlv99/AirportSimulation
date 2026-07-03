package edu.uni.airportsim.domain;

public record DocumentKind(String code, String displayName) {
    public static final DocumentKind PASSPORT = new DocumentKind("PASSPORT", "Passport");
    public static final DocumentKind ID_CARD = new DocumentKind("ID_CARD", "ID Card");
    public static final DocumentKind RESIDENCE_PERMIT = new DocumentKind("RESIDENCE_PERMIT", "Residence Permit");

    public DocumentKind {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        displayName = displayName == null || displayName.isBlank() ? code : displayName;
    }
}
