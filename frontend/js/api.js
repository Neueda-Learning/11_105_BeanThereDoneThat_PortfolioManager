/**
 * api.js — API Client Stubs
 * TODO: Replace dummy data with real Fetch API calls to Spring Boot backend.
 * Base URL: http://localhost:8080
 */

const API_BASE = 'http://localhost:8080';

function buildCustomerScopedHeaders(includeJsonContentType = true) {
  const headers = {};
  if (includeJsonContentType) {
    headers['Content-Type'] = 'application/json';
  }
  const customerId = typeof window.getCustomerId === 'function'
    ? window.getCustomerId()
    : null;

  if (customerId) {
    headers['X-Customer-Id'] = String(customerId);
  }

  return headers;
}

async function parseApiResponse(response) {
  let payload = null;

  try {
    payload = await response.json();
  } catch (e) {
    payload = null;
  }

  if (!response.ok) {
    const message =
      (payload && (payload.message || payload.error || payload.details)) ||
      `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return payload;
}

async function parseBlobApiResponse(response) {
  if (!response.ok) {
    let payload = null;
    try {
      payload = await response.json();
    } catch (e) {
      payload = null;
    }
    const message =
      (payload && (payload.message || payload.error || payload.details)) ||
      `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return await response.blob();
}

function toNumeric(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function aggregateInvestmentsBySymbol(investments) {
  const grouped = new Map();

  (Array.isArray(investments) ? investments : []).forEach((investment) => {
    if (!investment || typeof investment !== 'object') return;

    const symbol = String(investment.symbol || '').trim();
    if (!symbol) return;

    const existing = grouped.get(symbol);
    const purchaseDate = investment.purchaseDate || null;

    if (!existing) {
      grouped.set(symbol, {
        symbol,
        companyName: investment.companyName || symbol,
        assetType: investment.assetType || 'Unknown',
        currency: investment.currency || null,
        quantity: toNumeric(investment.quantity),
        investedAmount: toNumeric(investment.investedAmount),
        currentValue: toNumeric(investment.currentValue),
        profitLoss: toNumeric(investment.profitLoss),
        purchaseDate,
      });
      return;
    }

    existing.quantity += toNumeric(investment.quantity);
    existing.investedAmount += toNumeric(investment.investedAmount);
    existing.currentValue += toNumeric(investment.currentValue);
    existing.profitLoss += toNumeric(investment.profitLoss);

    if (!existing.purchaseDate && purchaseDate) {
      existing.purchaseDate = purchaseDate;
    }
  });

  return Array.from(grouped.values());
}

async function registerCustomer(registerPayload) {
  try {
    const response = await fetch(`${API_BASE}/api/customers/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(registerPayload),
    });

    return await parseApiResponse(response);
  } catch (error) {
    throw new Error(error.message || 'Unable to register customer.');
  }
}

async function loginCustomer(loginPayload) {
  try {
    const response = await fetch(`${API_BASE}/api/customers/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(loginPayload),
    });

    return await parseApiResponse(response);
  } catch (error) {
    throw new Error(error.message || 'Unable to login customer.');
  }
}

/* ── Auth ─────────────────────────────────────────────────── */
const AuthAPI = {
  login:    loginCustomer,
  register: registerCustomer,
  logout:   () => Promise.resolve(),
};

/* ── Customer ─────────────────────────────────────────────── */
const CustomerAPI = {
  getProfile: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/customers/profile`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log('[API] GET /api/customers/profile response:', payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch customer profile.');
    }
  },
  update: async (data) => {
    try {
      const response = await fetch(`${API_BASE}/api/customers/profile`, {
        method: 'PUT',
        headers: buildCustomerScopedHeaders(),
        body: JSON.stringify(data),
      });
      const payload = await parseApiResponse(response);
      console.log('[API] PUT /api/customers/profile response:', payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to update customer profile.');
    }
  },
  changePassword: async (data) => {
    try {
      const response = await fetch(`${API_BASE}/api/customers/profile/password`, {
        method: 'PUT',
        headers: buildCustomerScopedHeaders(),
        body: JSON.stringify(data),
      });
      await parseApiResponse(response);
      return true;
    } catch (error) {
      throw new Error(error.message || 'Unable to change password.');
    }
  },
  getSummary: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/customers/profile/summary`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log('[API] GET /api/customers/profile/summary response:', payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch account summary.');
    }
  },
  exportCsv: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/customers/profile/export`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      return await parseBlobApiResponse(response);
    } catch (error) {
      throw new Error(error.message || 'Unable to export customer data.');
    }
  },
  deleteAccount: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/customers/profile`, {
        method: 'DELETE',
        headers: buildCustomerScopedHeaders(),
      });
      await parseApiResponse(response);
      return true;
    } catch (error) {
      throw new Error(error.message || 'Unable to delete account.');
    }
  },
};

/* ── Portfolio ────────────────────────────────────────────── */
const PortfolioAPI = {
  getAll: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/portfolios`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log('[API] GET /api/portfolios response:', payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch portfolios.');
    }
  },
  getByCustomer: async () => {
    try {
      const currentCustomerId = typeof window.getCustomerId === 'function'
        ? window.getCustomerId()
        : null;
      const response = await fetch(`${API_BASE}/api/customers/${currentCustomerId}/portfolios`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log('[API] GET /api/customers/{customerId}/portfolios response:', payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch customer portfolios.');
    }
  },
  getById: async (id) => {
    try {
      const response = await fetch(`${API_BASE}/api/portfolios/${id}`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] GET /api/portfolios/${id} response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch portfolio details.');
    }
  },
  create: async (data) => {
    try {
      const response = await fetch(`${API_BASE}/api/portfolios`, {
        method: 'POST',
        headers: buildCustomerScopedHeaders(),
        body: JSON.stringify(data),
      });
      const payload = await parseApiResponse(response);
      console.log('[API] POST /api/portfolios response:', payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to create portfolio.');
    }
  },
  update: async (id, data) => {
    try {
      const response = await fetch(`${API_BASE}/api/portfolios/${id}`, {
        method: 'PUT',
        headers: buildCustomerScopedHeaders(),
        body: JSON.stringify(data),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] PUT /api/portfolios/${id} response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to update portfolio.');
    }
  },
  delete: async (id) => {
    try {
      const response = await fetch(`${API_BASE}/api/portfolios/${id}`, {
        method: 'DELETE',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] DELETE /api/portfolios/${id} response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to delete portfolio.');
    }
  },
};

/* ── Investment ───────────────────────────────────────────── */
const InvestmentAPI = {
  getAll: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/investments`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log('[API] GET /api/investments response:', payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch investments.');
    }
  },
  getByPortfolio: async (portfolioId) => {
    try {
      const response = await fetch(`${API_BASE}/api/portfolios/${portfolioId}/investments`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] GET /api/portfolios/${portfolioId}/investments response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch portfolio investments.');
    }
  },
  getById: async (id) => {
    try {
      const response = await fetch(`${API_BASE}/api/investments/${id}`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] GET /api/investments/${id} response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch investment details.');
    }
  },
  create: async (data) => {
    try {
      const response = await fetch(`${API_BASE}/api/investments`, {
        method: 'POST',
        headers: buildCustomerScopedHeaders(),
        body: JSON.stringify(data),
      });
      const payload = await parseApiResponse(response);
      console.log('[API] POST /api/investments response:', payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to create investment.');
    }
  },
  update: async (id, data) => {
    try {
      const response = await fetch(`${API_BASE}/api/investments/${id}`, {
        method: 'PUT',
        headers: buildCustomerScopedHeaders(),
        body: JSON.stringify(data),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] PUT /api/investments/${id} response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to update investment.');
    }
  },
  delete: async (id) => {
    try {
      const response = await fetch(`${API_BASE}/api/investments/${id}`, {
        method: 'DELETE',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] DELETE /api/investments/${id} response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to delete investment.');
    }
  },
  getExchangeRate: async (fromCurrency, toCurrency) => {
    try {
      const from = encodeURIComponent(String(fromCurrency || '').trim().toUpperCase());
      const to = encodeURIComponent(String(toCurrency || '').trim().toUpperCase());
      const response = await fetch(`${API_BASE}/api/investments/exchange-rate?from=${from}&to=${to}`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch exchange rate.');
    }
  },
  exportCsv: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/investments/export`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      return await parseBlobApiResponse(response);
    } catch (error) {
      throw new Error(error.message || 'Unable to export investments.');
    }
  },
  downloadImportTemplate: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/investments/import-template`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      return await parseBlobApiResponse(response);
    } catch (error) {
      throw new Error(error.message || 'Unable to download the investment import template.');
    }
  },
  importFile: async (file) => {
    try {
      const formData = new FormData();
      formData.append('file', file);
      const response = await fetch(`${API_BASE}/api/investments/import`, {
        method: 'POST',
        headers: buildCustomerScopedHeaders(false),
        body: formData,
      });
      return await parseApiResponse(response);
    } catch (error) {
      throw new Error(error.message || 'Unable to import investments.');
    }
  },
};

/* ── Transaction ──────────────────────────────────────────── */
const TransactionAPI = {
  getAll: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/transactions`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log('[API] GET /api/transactions response:', payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch transactions.');
    }
  },
  getById: async (id) => {
    try {
      const response = await fetch(`${API_BASE}/api/transactions/${id}`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] GET /api/transactions/${id} response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch transaction details.');
    }
  },
  create: async (data) => {
    try {
      const response = await fetch(`${API_BASE}/api/transactions`, {
        method: 'POST',
        headers: buildCustomerScopedHeaders(),
        body: JSON.stringify(data),
      });
      const payload = await parseApiResponse(response);
      console.log('[API] POST /api/transactions response:', payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to create transaction.');
    }
  },
  delete: async (id) => {
    try {
      const response = await fetch(`${API_BASE}/api/transactions/${id}`, {
        method: 'DELETE',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] DELETE /api/transactions/${id} response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to delete transaction.');
    }
  },
  getByInvestment: async (investmentId) => {
    try {
      const response = await fetch(`${API_BASE}/api/investments/${investmentId}/transactions`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] GET /api/investments/${investmentId}/transactions response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch investment transactions.');
    }
  },
  exportCsv: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/transactions/export`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      return await parseBlobApiResponse(response);
    } catch (error) {
      throw new Error(error.message || 'Unable to export transactions.');
    }
  },
  downloadImportTemplate: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/transactions/import-template`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      return await parseBlobApiResponse(response);
    } catch (error) {
      throw new Error(error.message || 'Unable to download the transaction import template.');
    }
  },
  importFile: async (file) => {
    try {
      const formData = new FormData();
      formData.append('file', file);
      const response = await fetch(`${API_BASE}/api/transactions/import`, {
        method: 'POST',
        headers: buildCustomerScopedHeaders(false),
        body: formData,
      });
      return await parseApiResponse(response);
    } catch (error) {
      throw new Error(error.message || 'Unable to import transactions.');
    }
  },
};

/* ── Risk Analysis ────────────────────────────────────────── */
const RiskAnalysisAPI = {
  analyzePortfolio: async (portfolioId) => {
    try {
      const response = await fetch(`${API_BASE}/api/risk-analysis/portfolio/${portfolioId}`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] GET /api/risk-analysis/portfolio/${portfolioId} response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to analyze portfolio risk.');
    }
  },
  analyzeStock: async (symbol, assetType = 'STOCK') => {
    try {
      const encodedSymbol = encodeURIComponent(String(symbol || '').trim());
      const encodedAssetType = encodeURIComponent(String(assetType || 'STOCK').trim().toUpperCase());
      const response = await fetch(`${API_BASE}/api/risk-analysis/stock/${encodedSymbol}?assetType=${encodedAssetType}`, {
        method: 'GET',
        headers: buildCustomerScopedHeaders(),
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] GET /api/risk-analysis/stock/${encodedSymbol}?assetType=${encodedAssetType} response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to analyze stock risk.');
    }
  },
};

/* ── Exports ──────────────────────────────────────────────── */
window.AuthAPI        = AuthAPI;
window.registerCustomer = registerCustomer;
window.loginCustomer = loginCustomer;
window.CustomerAPI    = CustomerAPI;
window.PortfolioAPI   = PortfolioAPI;
window.InvestmentAPI  = InvestmentAPI;
window.TransactionAPI = TransactionAPI;
window.RiskAnalysisAPI = RiskAnalysisAPI;
window.aggregateInvestmentsBySymbol = aggregateInvestmentsBySymbol;
