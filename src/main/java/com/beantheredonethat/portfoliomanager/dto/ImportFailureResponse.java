package com.beantheredonethat.portfoliomanager.dto;

public class ImportFailureResponse {

    private final int rowNumber;
    private final String reason;

    public ImportFailureResponse(int rowNumber, String reason) {
        this.rowNumber = rowNumber;
        this.reason = reason;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getReason() {
        return reason;
    }
}