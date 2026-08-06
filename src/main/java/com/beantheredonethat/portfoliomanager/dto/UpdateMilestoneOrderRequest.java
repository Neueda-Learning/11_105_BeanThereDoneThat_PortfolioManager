package com.beantheredonethat.portfoliomanager.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class UpdateMilestoneOrderRequest {

    @NotEmpty(message = "Milestone order cannot be empty")
    private List<@NotNull Integer> milestoneIds;

    public UpdateMilestoneOrderRequest() {
    }

    public UpdateMilestoneOrderRequest(List<Integer> milestoneIds) {
        this.milestoneIds = milestoneIds;
    }

    public List<Integer> getMilestoneIds() {
        return milestoneIds;
    }

    public void setMilestoneIds(List<Integer> milestoneIds) {
        this.milestoneIds = milestoneIds;
    }
}
