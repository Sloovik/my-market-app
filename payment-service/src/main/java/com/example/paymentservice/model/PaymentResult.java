package com.example.paymentservice.model;

public record PaymentResult(
        boolean success,
        long remainingBalance,
        String transactionId,
        String errorCode,
        String message
) {
    public static PaymentResult success(long remainingBalance, String transactionId, String message) {
        return new PaymentResult(true, remainingBalance, transactionId, null, message);
    }

    public static PaymentResult failure(long remainingBalance, String errorCode, String message) {
        return new PaymentResult(false, remainingBalance, null, errorCode, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public long getRemainingBalance() {
        return remainingBalance;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}