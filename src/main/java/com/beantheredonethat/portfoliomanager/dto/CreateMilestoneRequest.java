package com.beantheredonethat.portfoliomanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateMilestoneRequest {

    @NotBlank(message = "Milestone item is required")
    @Size(max = 255, message = "Milestone item must be at most 255 characters")
    private String item;

    @NotNull(message = "Target price is required")
    @DecimalMin(value = "0.01", message = "Target price must be greater than 0")
    private BigDecimal price;

    public CreateMilestoneRequest() {
    }

    public CreateMilestoneRequest(String item, BigDecimal price) {
        this.item = item;
        this.price = price;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
