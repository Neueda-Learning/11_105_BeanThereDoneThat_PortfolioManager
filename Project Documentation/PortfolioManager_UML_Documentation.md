# Portfolio Manager — UML Documentation

Covers the full class structure (with operations) and the request flow for every major operation in the system, including the newly added **Milestone (financial goals)** feature. All diagrams are in Mermaid syntax — paste into any Mermaid-compatible viewer (GitHub/GitLab markdown, Mermaid Live Editor, VS Code Mermaid extension, Confluence, etc.) to render.

---

## 1. Class Diagram (structure + operations)

```mermaid
classDiagram
direction TB
class CustomerController {
  +register()
  +login()
  +getCurrentCustomerProfile()
  +updateCurrentCustomerProfile()
  +changePassword()
  +getProfileSummary()
  +deleteCurrentCustomerProfile()
  +getAllCustomers()
  +updateCustomer()
  +deleteCustomer()
  +getPortfoliosByCustomer()
}
class PortfolioController {
  +createPortfolio()
  +getAllPortfolios()
  +getPortfolioById()
  +updatePortfolio()
  +deletePortfolio()
}
class InvestmentController {
  +createInvestment()
  +getAllInvestments()
  +importInvestments()
  +getInvestmentById()
  +getInvestmentsByPortfolio()
  +updateInvestment()
  +deleteInvestment()
  +getExchangeRate()
}
class InvestmentTransactionController {
  +createTransaction()
  +importTransactions()
  +getAllTransactions()
  +getTransactionById()
  +getTransactionsByInvestment()
  +deleteTransaction()
}
class RiskAnalysisController {
  +analyzeStock()
  +analyzePortfolio()
}
class MilestoneController {
  +createMilestone()
  +getMilestones()
  +getNextMilestone()
  +updateMilestone()
  +updateMilestoneOrder()
  +deleteMilestone()
}
class CustomerService {
  +register()
  +login()
  +resolveCustomerId()
  +getCustomerById()
  +getAllCustomers()
  +updateCustomer()
  +updateProfile()
  +changePassword()
  +getAccountSummary()
  +exportCustomerDataCsv()
  +deleteCustomerProfile()
  +deleteCustomer()
}
class PortfolioService {
  +createPortfolio()
  +getPortfolioById()
  +getAllPortfolios()
  +updatePortfolio()
  +deletePortfolio()
  +ensurePortfolioOwnership()
}
class InvestmentService {
  +createInvestment()
  +getInvestmentById()
  +getAllInvestments()
  +getInvestmentsByPortfolio()
  +updateInvestment()
  +deleteInvestment()
  +importInvestments()
  +exportInvestmentsCsv()
  +exportInvestmentsTemplateCsv()
  +getExchangeRate()
}
class InvestmentTransactionService {
  +createTransaction()
  +getTransactionById()
  +getAllTransactions()
  +getTransactionsByInvestment()
  +deleteTransaction()
  +importTransactions()
  +exportTransactionsCsv()
  +exportTransactionsTemplateCsv()
}
class RiskAnalysisService {
  +analyzeStock()
  +analyzePortfolio()
}
class MilestoneService {
  +createMilestone()
  +getMilestones()
  +getNextMilestone()
  +updateMilestone()
  +updateMilestoneOrder()
  +deleteMilestone()
  +fetchMilestoneImage()
}
class SymbolResolverService {
  +resolveSymbol()
}
class CurrencyConversionService {
  +convert()
  +getExchangeRate()
}
class TabularDataService {
  +read()
  +validateHeaders()
  +writeCsv()
  +writeTemplate()
}
class MarketDataFactory {
  +getService(AssetType)
}
class MarketDataService {
  <<interface>>
  +supportedAssetTypes()
  +getCurrentPrice()
}
class YahooFinanceService {
  +getCurrentPrice()
  +getHistoricalPrices()
}
class AmfiMarketDataService
class CryptoMarketDataService
class GoldMarketDataService
class CustomerRepository
class PortfolioRepository
class InvestmentRepository
class InvestmentTransactionRepository
class MilestoneRepository {
  +getTotalProfitByCustomerId()
}
class Customer {
  Integer customerId
  String customerName
  String username
  String passwordHash
  String email
  String phoneNumber
}
class Portfolio {
  Integer portfolioId
  Integer customerId
  String portfolioName
}
class Investment {
  Integer investmentId
  Integer portfolioId
  String symbol
  String assetType
  BigDecimal quantity
  BigDecimal investedAmount
  BigDecimal currentValue
  BigDecimal profitLoss
}
class InvestmentTransaction {
  Integer transactionId
  Integer investmentId
  String transactionType
  BigDecimal quantity
  BigDecimal transactionAmount
}
class Milestone {
  Integer milestoneId
  Integer customerId
  String item
  BigDecimal price
  String imageUrl
  Integer displayOrder
}

CustomerController --> CustomerService
CustomerController --> PortfolioService
PortfolioController --> PortfolioService
InvestmentController --> InvestmentService
InvestmentController --> CustomerService
InvestmentTransactionController --> InvestmentTransactionService
RiskAnalysisController --> RiskAnalysisService
RiskAnalysisController --> SymbolResolverService
MilestoneController --> MilestoneService
MilestoneController --> CustomerService

InvestmentService --> SymbolResolverService
InvestmentService --> CurrencyConversionService
InvestmentService --> TabularDataService
InvestmentService --> MarketDataFactory
InvestmentTransactionService --> TabularDataService
RiskAnalysisService --> YahooFinanceService

MarketDataFactory --> MarketDataService
YahooFinanceService ..|> MarketDataService
AmfiMarketDataService ..|> MarketDataService
CryptoMarketDataService ..|> MarketDataService
GoldMarketDataService ..|> MarketDataService

CustomerService --> CustomerRepository
PortfolioService --> PortfolioRepository
InvestmentService --> InvestmentRepository
InvestmentTransactionService --> InvestmentTransactionRepository
MilestoneService --> MilestoneRepository

CustomerRepository --> Customer
PortfolioRepository --> Portfolio
InvestmentRepository --> Investment
InvestmentTransactionRepository --> InvestmentTransaction
MilestoneRepository --> Milestone

Customer "1" --> "many" Portfolio
Portfolio "1" --> "many" Investment
Investment "1" --> "many" InvestmentTransaction
Customer "1" --> "many" Milestone
```

---

## 2. Operation flows (sequence diagrams)

### 2.1 Register / Login

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant CTRL as CustomerController
  participant SVC as CustomerService
  participant ENC as BCryptPasswordEncoder
  participant REPO as CustomerRepository
  participant DB as MySQL

  FE->>CTRL: POST /api/customers/register
  CTRL->>SVC: register(request)
  SVC->>ENC: encode(password)
  ENC-->>SVC: passwordHash
  SVC->>REPO: save(customer)
  REPO->>DB: INSERT customer
  DB-->>REPO: saved row
  REPO-->>SVC: Customer entity
  SVC-->>CTRL: CustomerResponse
  CTRL-->>FE: 201 Created

  FE->>CTRL: POST /api/customers/login
  CTRL->>SVC: login(request)
  SVC->>REPO: findByUsername(username)
  REPO->>DB: SELECT customer
  DB-->>REPO: row
  REPO-->>SVC: Customer entity
  SVC->>ENC: matches(password, hash)
  ENC-->>SVC: true / false
  SVC-->>CTRL: LoginResponse
  CTRL-->>FE: 200 OK
```

### 2.2 Create portfolio

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant CTRL as PortfolioController
  participant SVC as PortfolioService
  participant REPO as PortfolioRepository
  participant DB as MySQL

  FE->>CTRL: POST /api/portfolios
  CTRL->>SVC: createPortfolio(customerId, request)
  SVC->>REPO: save(portfolio)
  REPO->>DB: INSERT portfolio
  DB-->>REPO: saved row
  REPO-->>SVC: Portfolio entity
  SVC-->>CTRL: PortfolioResponse
  CTRL-->>FE: 201 Created
```

### 2.3 Add investment (live price lookup)

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant CTRL as InvestmentController
  participant SVC as InvestmentService
  participant SYM as SymbolResolverService
  participant MDF as MarketDataFactory
  participant EXT as External price API
  participant REPO as InvestmentRepository
  participant DB as MySQL

  FE->>CTRL: POST /api/investments
  CTRL->>SVC: createInvestment(request)
  SVC->>SYM: resolveSymbol(symbol, assetType)
  SYM-->>SVC: resolved symbol
  SVC->>MDF: getService(assetType)
  MDF-->>SVC: matching provider
  SVC->>EXT: getCurrentPrice(symbol)
  EXT-->>SVC: current price
  SVC->>REPO: save(investment)
  REPO->>DB: INSERT investment
  DB-->>REPO: saved row
  REPO-->>SVC: Investment entity
  SVC-->>CTRL: InvestmentResponse
  CTRL-->>FE: 201 Created
```

### 2.4 Record transaction (buy/sell)

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant CTRL as InvestmentTransactionController
  participant SVC as InvestmentTransactionService
  participant IREPO as InvestmentRepository
  participant TREPO as InvestmentTransactionRepository
  participant DB as MySQL

  FE->>CTRL: POST /api/transactions
  CTRL->>SVC: createTransaction(request)
  SVC->>IREPO: findById(investmentId)
  IREPO->>DB: SELECT investment
  DB-->>IREPO: row
  IREPO-->>SVC: Investment entity
  SVC->>SVC: recalculate quantity, invested amount
  SVC->>TREPO: save(transaction)
  TREPO->>DB: INSERT transaction
  SVC->>IREPO: save(updated investment)
  IREPO->>DB: UPDATE investment
  DB-->>SVC: confirmation
  SVC-->>CTRL: TransactionResponse
  CTRL-->>FE: 201 Created
```

### 2.5 Import investments/transactions (CSV/Excel)

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant CTRL as InvestmentController
  participant SVC as InvestmentService
  participant TAB as TabularDataService
  participant REPO as InvestmentRepository
  participant DB as MySQL

  FE->>CTRL: POST /api/investments/import (file)
  CTRL->>SVC: importInvestments(customerId, file)
  SVC->>TAB: read(file)
  TAB-->>SVC: ParsedTabularData
  SVC->>TAB: validateHeaders(headers)
  loop each row
    SVC->>REPO: save(investment)
    REPO->>DB: INSERT investment
  end
  SVC-->>CTRL: ImportSummaryResponse
  CTRL-->>FE: 200 OK (success/error counts)
```

### 2.6 Create milestone (financial goal) with image lookup

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant CTRL as MilestoneController
  participant CSVC as CustomerService
  participant SVC as MilestoneService
  participant PEX as Pexels API
  participant REPO as MilestoneRepository
  participant DB as MySQL

  FE->>CTRL: POST /api/milestones
  CTRL->>CSVC: resolveCustomerId(authentication, headerCustomerId)
  CSVC-->>CTRL: customerId
  CTRL->>SVC: createMilestone(customerId, request)
  alt no imageUrl supplied
    SVC->>PEX: search(item)
    PEX-->>SVC: image URL (or failure -> fallback image)
  end
  SVC->>REPO: save(milestone)
  REPO->>DB: INSERT milestone
  DB-->>REPO: saved row
  REPO-->>SVC: Milestone entity
  SVC->>REPO: getTotalProfitByCustomerId(customerId)
  REPO->>DB: SELECT SUM(profit_loss) ...
  DB-->>REPO: total profit
  REPO-->>SVC: totalProfit
  SVC->>SVC: compute completedAmount, progressPercentage, remainingAmount, achieved
  SVC-->>CTRL: MilestoneResponse
  CTRL-->>FE: 201 Created
```

### 2.7 Get next milestone (progress tracking)

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant CTRL as MilestoneController
  participant SVC as MilestoneService
  participant REPO as MilestoneRepository
  participant DB as MySQL

  FE->>CTRL: GET /api/milestones/next
  CTRL->>SVC: getNextMilestone(customerId)
  SVC->>REPO: findByCustomerId(customerId, ordered)
  REPO->>DB: SELECT milestones ORDER BY display_order
  DB-->>REPO: rows
  REPO-->>SVC: Milestone list
  SVC->>REPO: getTotalProfitByCustomerId(customerId)
  REPO->>DB: SELECT SUM(profit_loss) ...
  DB-->>REPO: total profit
  REPO-->>SVC: totalProfit
  SVC->>SVC: find closest not-yet-achieved milestone, compute progress
  SVC-->>CTRL: NextMilestoneResponse
  CTRL-->>FE: 200 OK
```

### 2.8 Risk analysis

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant CTRL as RiskAnalysisController
  participant SYM as SymbolResolverService
  participant SVC as RiskAnalysisService
  participant YF as YahooFinanceService
  participant EXT as Yahoo Finance API

  FE->>CTRL: GET /api/risk-analysis/stock/{symbol}
  CTRL->>SYM: resolveSymbol(symbol)
  SYM-->>CTRL: resolved symbol
  CTRL->>SVC: analyzeStock(symbol)
  SVC->>YF: getHistoricalPrices(symbol, range)
  YF->>EXT: fetch historical prices
  EXT-->>YF: price series
  YF-->>SVC: HistoricalPricePoint list
  SVC->>SVC: compute volatility, drawdown, Sharpe ratio
  SVC-->>CTRL: RiskAnalysisResponse
  CTRL-->>FE: 200 OK
```

---

## 3. Notes

- All controller-level operations delegate straight to a matching service method — there is no logic in the controllers beyond request/response mapping.
- `InvestmentService` and `RiskAnalysisService` are the two integration points that reach out to external market data — everything else stays inside the backend and MySQL. `MilestoneService` is a third, separate integration point (Pexels, for goal images) that has no bearing on pricing.
- Import/export operations reuse a single shared `TabularDataService` for both investments and transactions, rather than each having its own CSV/Excel logic.
- The `Milestone` feature (`MilestoneController` → `MilestoneService` → `MilestoneRepository` → `Milestone` entity) lets a customer define savings goals and tracks progress against their total investment profit, independent of any single portfolio or holding. Every milestone endpoint resolves the acting customer the same way as other controllers, via `CustomerService.resolveCustomerId(authentication, headerCustomerId)`.
