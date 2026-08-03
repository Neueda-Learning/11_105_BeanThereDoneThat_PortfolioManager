package com.beantheredonethat.portfoliomanager.entity;

public class Portfolio {

	private Integer portfolioId;
	private Integer customerId;
	private String portfolioName;

	public Portfolio() {
	}

	public Portfolio(Integer portfolioId, Integer customerId, String portfolioName) {
		this.portfolioId = portfolioId;
		this.customerId = customerId;
		this.portfolioName = portfolioName;
	}

	public Portfolio(Integer customerId, String portfolioName) {
		this.customerId = customerId;
		this.portfolioName = portfolioName;
	}

	public Integer getPortfolioId() {
		return portfolioId;
	}

	public void setPortfolioId(Integer portfolioId) {
		this.portfolioId = portfolioId;
	}

	public Integer getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Integer customerId) {
		this.customerId = customerId;
	}

	public String getPortfolioName() {
		return portfolioName;
	}

	public void setPortfolioName(String portfolioName) {
		this.portfolioName = portfolioName;
	}
}
