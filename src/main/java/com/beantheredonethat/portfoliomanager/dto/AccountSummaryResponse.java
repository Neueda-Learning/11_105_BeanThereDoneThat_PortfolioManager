package com.beantheredonethat.portfoliomanager.dto;

public class AccountSummaryResponse {

    private int portfolios;
    private int investments;
    private int transactions;

    public AccountSummaryResponse() {
    }

    public AccountSummaryResponse(int portfolios, int investments, int transactions) {
        this.portfolios = portfolios;
        this.investments = investments;
        this.transactions = transactions;
    }

    public int getPortfolios() {
        return portfolios;
    }

    public void setPortfolios(int portfolios) {
        this.portfolios = portfolios;
    }

    public int getInvestments() {
        return investments;
    }

    public void setInvestments(int investments) {
        this.investments = investments;
    }

    public int getTransactions() {
        return transactions;
    }

    public void setTransactions(int transactions) {
        this.transactions = transactions;
    }
}
