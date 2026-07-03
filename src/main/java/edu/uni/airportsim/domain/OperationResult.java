package edu.uni.airportsim.domain;

public record OperationResult(boolean success, String message) {
    public OperationResult {
        message = message == null ? "" : message;
    }

    public static OperationResult approved(String message) {
        return new OperationResult(true, message);
    }

    public static OperationResult rejected(String message) {
        return new OperationResult(false, message);
    }
}
