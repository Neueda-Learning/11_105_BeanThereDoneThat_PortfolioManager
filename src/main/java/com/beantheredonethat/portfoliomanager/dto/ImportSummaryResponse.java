package com.beantheredonethat.portfoliomanager.dto;

import java.util.List;

public class ImportSummaryResponse {

    private final int successfulCount;
    private final int failedCount;
    private final List<ImportFailureResponse> failures;

    public ImportSummaryResponse(
            int successfulCount,
            int failedCount,
            List<ImportFailureResponse> failures) {
        this.successfulCount = successfulCount;
        this.failedCount = failedCount;
        this.failures = failures;
    }

    public int getSuccessfulCount() {
        return successfulCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public List<ImportFailureResponse> getFailures() {
        return failures;
    }
}