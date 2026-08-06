package com.beantheredonethat.portfoliomanager.dto;

import java.math.BigDecimal;

public class NextMilestoneResponse {

    private Integer milestoneId;
    private String item;
    private BigDecimal price;
    private String imageUrl;
    private BigDecimal totalProfit;
    private BigDecimal completedAmount;
    private BigDecimal progressPercentage;
    private BigDecimal remainingAmount;
    private boolean achieved;

    public NextMilestoneResponse() {
    }

    public NextMilestoneResponse(Integer milestoneId,
                                 String item,
                                 BigDecimal price,
                                 String imageUrl,
                                 BigDecimal totalProfit,
                                 BigDecimal completedAmount,
                                 BigDecimal progressPercentage,
                                 BigDecimal remainingAmount,
                                 boolean achieved) {
        this.milestoneId = milestoneId;
        this.item = item;
        this.price = price;
        this.imageUrl = imageUrl;
        this.totalProfit = totalProfit;
        this.completedAmount = completedAmount;
        this.progressPercentage = progressPercentage;
        this.remainingAmount = remainingAmount;
        this.achieved = achieved;
    }

    public Integer getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(Integer milestoneId) {
        this.milestoneId = milestoneId;
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

    public BigDecimal getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(BigDecimal progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public boolean isAchieved() {
        return achieved;
    }

    public void setAchieved(boolean achieved) {
        this.achieved = achieved;
    }
}
