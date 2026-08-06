# Portfolio Manager Application

## Project Overview

The Portfolio Manager application is designed to help users manage and track their investments across multiple asset classes such as Stocks, ETFs, Mutual Funds, Gold, and Cryptocurrency. The application allows users to create multiple portfolios, manage investments, track transactions, retrieve live market prices, and analyze portfolio risk.

## Technology Stack

**Backend:** Java, Spring Boot  
**Database:** MySQL  
**Data Access:** JDBC  
**Frontend:** React.js  
**Testing:** JUnit, Mockito  
**Deployment:** Docker, CI/CD Pipeline

### External APIs

- **Yahoo Finance** - Stocks and ETFs market prices
- **GoldAPI.io** - Gold prices
- **AMFI** - Mutual Fund NAV details
- **CoinGecko** - Cryptocurrency prices

## Meeting 1: Project Initiation and Repository Setup

### **Date:** July 31, 2026
### **Meeting Type:** Project Kickoff Meeting

### Agenda

- Understand project requirements.
- Plan application architecture.
- Setup development environment and repository.

### Discussion Points

- Discussed the objective of building an investment portfolio management system.
- Finalized technology stack and development approach.
- Created GitHub repository for source code management.
- Added team members as contributors for collaborative development.
- Discussed version control practices and task tracking approach.

### Decisions Made

- GitHub will be used for source code management.
- Jira Kanban board will be used for tracking development activities.
- Application will follow layered architecture:
  Controller  
  Service  
  Repository  
  Database

## Meeting 2: Database Design and Customer Module Implementation

### **Date:** August 1, 2026

### Agenda

- Finalize database structure.
- Implement customer authentication module.

### Discussion Points

- Discussed database entities required for application functionality.
- Designed tables:
  Customer  
  Portfolio  
  Investment  
  Transaction
- Created SQL scripts for database initialization.
- Discussed customer registration and authentication flow.
- Implemented password security using password hashing.

### Decisions Made

- Customer credentials will not be stored as plain text.
- Validation rules will be implemented for customer input fields.
- Customer authentication will be integrated before accessing portfolio features.

## Meeting 3: Portfolio and Investment Module Development

### **Date:** August 3, 2026

### Agenda

- Review portfolio management requirements.
- Implement investment management functionality.

### Discussion Points

- Discussed requirement for supporting multiple portfolios per customer.
- Finalized relationship:

```text
Customer
   |
   |
Multiple Portfolios
   |
   |
Multiple Investments
```

- Implemented portfolio management APIs.
- Reviewed investment entity design.
- Migrated data access layer from JPA to JDBC for better SQL control.
- Started investment API development.

### Decisions Made

- A customer can maintain multiple portfolios.
- Investments will belong to specific portfolios.
- JDBC will be used instead of JPA for database operations.

## Meeting 4: Market Data Integration and Transaction Management

### **Date:** August 4, 2026

### Agenda

- Discuss live market price retrieval.
- Implement transaction tracking.

### Discussion Points

- Discussed requirement for retrieving current market prices automatically.
- Finalized external API integrations based on asset type.

### Asset Price Sources

| Asset Type | Data Source |
| --- | --- |
| Stocks | Yahoo Finance |
| ETFs | Yahoo Finance |
| Gold | GoldAPI.io |
| Mutual Funds | AMFI |
| Cryptocurrency | CoinGecko |

- Discussed buy/sell transaction tracking.
- Implemented transaction module linked with investments.

### Decisions Made

- Market price retrieval will be dynamically selected based on asset type.
- Transaction history will maintain investment activities.

## Meeting 5: Risk Analysis and Application Improvements

### **Date:** August 5, 2026

### Agenda

- Review completed features.
- Resolve integration issues.

### Discussion Points

- Discussed portfolio risk evaluation requirements.
- Implemented risk analysis functionality.
- Reviewed cryptocurrency price retrieval issues.
- Improved crypto symbol mapping and exchange search handling.

### Decisions Made

- Risk analysis will use portfolio investment data.
- Crypto assets require proper CoinGecko identifier resolution.
- Exchange information should be validated before fetching market data.

## Meeting 6: Testing, Deployment and Documentation

### **Date:** August 6, 2026

### Agenda

- Final application review.
- Prepare project delivery documentation.

### Discussion Points

- Reviewed frontend issues and applied fixes.
- Planned unit testing implementation using JUnit and Mockito.
- Discussed Docker containerization and CI/CD pipeline setup.
- Prepared customer delivery documentation:
  High-Level Design Document  
  Kanban Board Documentation  
  Minutes of Meeting  
  API Documentation

### Decisions Made

- Application quality will be improved through automated testing.
- Deployment process will be automated using Docker and CI/CD.
- Complete project documentation will be delivered to stakeholders.