package com.beantheredonethat.portfoliomanager.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Portfolio")
public class Portfolio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "portfolio_id")
	private Integer portfolioId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Column(name = "portfolio_name", nullable = false)
	private String portfolioName;

	public Portfolio() {
	}

	public Portfolio(Integer portfolioId, Customer customer, String portfolioName) {
		this.portfolioId = portfolioId;
		this.customer = customer;
		this.portfolioName = portfolioName;
	}

	public Portfolio(Customer customer, String portfolioName) {
		this.customer = customer;
		this.portfolioName = portfolioName;
	}

	public Integer getPortfolioId() {
		return portfolioId;
	}

	public void setPortfolioId(Integer portfolioId) {
		this.portfolioId = portfolioId;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public String getPortfolioName() {
		return portfolioName;
	}

	public void setPortfolioName(String portfolioName) {
		this.portfolioName = portfolioName;
	}
}
