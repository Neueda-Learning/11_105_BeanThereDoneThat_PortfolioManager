package com.beantheredonethat.portfoliomanager.entity;

import java.math.BigDecimal;

public class Milestone {

    private Integer milestoneId;
    private Integer customerId;
    private String item;
    private BigDecimal price;
    private String imageUrl;
    private Integer displayOrder;

    public Milestone() {
    }

    public Milestone(Integer milestoneId,
                     Integer customerId,
                     String item,
                     BigDecimal price,
                     String imageUrl,
                     Integer displayOrder) {
        this.milestoneId = milestoneId;
        this.customerId = customerId;
        this.item = item;
        this.price = price;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    public Milestone(Integer customerId,
                     String item,
                     BigDecimal price,
                     String imageUrl,
                     Integer displayOrder) {
        this(null, customerId, item, price, imageUrl, displayOrder);
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
}
