/**
 * api.js — API Client Stubs
 * TODO: Replace dummy data with real Fetch API calls to Spring Boot backend.
 * Base URL: http://localhost:8080
 */

const API_BASE = 'http://localhost:8080';

function buildCustomerScopedHeaders() {
  const headers = { 'Content-Type': 'application/json' };
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
  getProfile: ()     => Promise.resolve({ customerId:1, firstName:'Jane', lastName:'Doe', email:'jane@example.com', phone:'+91 98765 43210' }),
  update:     (data) => Promise.resolve({ ...data }),
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
  getByCustomer: async (customerId) => {
    try {
      const response = await fetch(`${API_BASE}/api/customers/${customerId}/portfolios`, {
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
        headers: { 'Content-Type': 'application/json' },
      });
      const payload = await parseApiResponse(response);
      console.log('[API] GET /api/investments response:', payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch investments.');
    }
  },
  getById: async (id) => {
    try {
      const response = await fetch(`${API_BASE}/api/investments/${id}`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
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
        headers: { 'Content-Type': 'application/json' },
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
        headers: { 'Content-Type': 'application/json' },
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
        headers: { 'Content-Type': 'application/json' },
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] DELETE /api/investments/${id} response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to delete investment.');
    }
  },
};

/* ── Transaction ──────────────────────────────────────────── */
const TransactionAPI = {
  getAll: async () => {
    try {
      const response = await fetch(`${API_BASE}/api/transactions`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
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
        headers: { 'Content-Type': 'application/json' },
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
        headers: { 'Content-Type': 'application/json' },
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
        headers: { 'Content-Type': 'application/json' },
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
        headers: { 'Content-Type': 'application/json' },
      });
      const payload = await parseApiResponse(response);
      console.log(`[API] GET /api/investments/${investmentId}/transactions response:`, payload);
      return payload;
    } catch (error) {
      throw new Error(error.message || 'Unable to fetch investment transactions.');
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
