package com.beantheredonethat.portfoliomanager.dto;

import java.math.BigDecimal;

public class MilestoneResponse {

    private Integer milestoneId;
    private Integer customerId;
    private String item;
    private BigDecimal price;
    private String imageUrl;
    private Integer displayOrder;
    private BigDecimal totalProfit;
    private BigDecimal completedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal progressPercentage;

    public MilestoneResponse() {
    }

    public MilestoneResponse(Integer milestoneId,
                             Integer customerId,
                             String item,
                             BigDecimal price,
                             String imageUrl,
                             Integer displayOrder,
                             BigDecimal totalProfit,
                             BigDecimal completedAmount,
                             BigDecimal remainingAmount,
                             BigDecimal progressPercentage) {
        this.milestoneId = milestoneId;
        this.customerId = customerId;
        this.item = item;
        this.price = price;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
        this.totalProfit = totalProfit;
        this.completedAmount = completedAmount;
        this.remainingAmount = remainingAmount;
        this.progressPercentage = progressPercentage;
    }

    public Integer getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(Integer milestoneId) {
        this.milestoneId = milestoneId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public BigDecimal getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(BigDecimal totalProfit) {
        this.totalProfit = totalProfit;
    }

    public BigDecimal getCompletedAmount() {
        return completedAmount;
    }

    public void setCompletedAmount(BigDecimal completedAmount) {
        this.completedAmount = completedAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(BigDecimal progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
}
