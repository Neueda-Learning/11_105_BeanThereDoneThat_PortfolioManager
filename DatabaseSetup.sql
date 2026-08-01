CREATE DATABASE portfolio_management;
USE portfolio_management;

CREATE TABLE Customer (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(15)
);

CREATE TABLE Portfolio (
    portfolio_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    portfolio_name VARCHAR(100) NOT NULL,

    CONSTRAINT fk_portfolio_customer
        FOREIGN KEY (customer_id)
        REFERENCES Customer(customer_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE Investment (
    investment_id INT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id INT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(100),
    asset_type VARCHAR(50),
    custom_asset_type VARCHAR(100),
    quantity DECIMAL(15,2) NOT NULL,
    invested_amount DECIMAL(15,2),
    purchase_price DECIMAL(15,2),
    current_price DECIMAL(15,2),
    current_value DECIMAL(15,2),
    profit_loss DECIMAL(15,2),
    purchase_date DATE,

    CONSTRAINT fk_investment_portfolio
        FOREIGN KEY (portfolio_id)
        REFERENCES Portfolio(portfolio_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);


CREATE TABLE Investment_Transaction (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    investment_id INT NOT NULL,
    transaction_date DATE NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    quantity DECIMAL(15,2) NOT NULL,
    transaction_price DECIMAL(15,2) NOT NULL,
    transaction_amount DECIMAL(15,2) NOT NULL,

    CONSTRAINT fk_transaction_investment
        FOREIGN KEY (investment_id)
        REFERENCES Investment(investment_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

