/**
 * dashboard.js — Dashboard page logic
 * Loads real dashboard data from backend APIs.
 */
console.log('Dashboard JS loaded');

document.addEventListener('DOMContentLoaded', () => {
  loadDashboard();
});

async function loadDashboard() {
  try {
    if (typeof requireAuth === 'function') {
      requireAuth();
    }
    if (typeof initNavbarUser === 'function') {
      initNavbarUser();
    }

    setGreeting();
    setLoadingState();

    const dashboardData = await getDashboardData();
    loadDashboardCards(dashboardData.portfolios, dashboardData.investments);
    loadRecentTransactions(dashboardData.transactions);
    renderInvestmentSummary(dashboardData.investments);
  } catch (error) {
    console.error('[Dashboard] Failed to load data:', error);
    showDashboardError(error.message || 'Unable to load dashboard data.');
  }
}

async function getDashboardData() {
  console.log('Fetching portfolios');
  console.log('Fetching investments');
  console.log('Fetching transactions');

  const [portfoliosResponse, investmentsResponse, transactionsResponse] = await Promise.all([
    PortfolioAPI.getAll(),
    InvestmentAPI.getAll(),
    TransactionAPI.getAll(),
  ]);

  console.log('Portfolios API response:', portfoliosResponse);
  console.log('Investments API response:', investmentsResponse);
  console.log('Transactions API response:', transactionsResponse);

  const data = {
    portfolios: Array.isArray(portfoliosResponse) ? portfoliosResponse : [],
    investments: Array.isArray(investmentsResponse) ? investmentsResponse : [],
    transactions: Array.isArray(transactionsResponse) ? transactionsResponse : [],
  };

  console.log('API Dashboard Data', data);

  return data;
}

function setGreeting() {
  const greetingEl = document.getElementById('dashboardGreeting');
  if (!greetingEl) return;

  const customerName = (getCustomerName() || '').trim();
  const firstName = customerName ? customerName.split(/\s+/)[0] : '';
  greetingEl.textContent = firstName ? `Good morning, ${firstName}` : 'Good morning';
}

function setLoadingState() {
  setText('statPortfolios', '0');
  setText('statCount', '0');
  setText('statInvested', '₹0');
  setText('statValue', '₹0');
  setText('statPL', '₹0');
  setText('statPLDelta', '0.00% overall');
}

function loadDashboardCards(portfolios, investments) {
  const metrics = getPortfolioMetrics(portfolios, investments);

  setText('statPortfolios', String(metrics.totalPortfolios));
  setText('statCount', String(metrics.totalInvestments));
  setText('statInvested', formatINR(metrics.totalInvested));
  setText('statValue', formatINR(metrics.totalCurrentValue));

  const statPLEl = document.getElementById('statPL');
  if (statPLEl) {
    statPLEl.textContent = `${metrics.totalPL > 0 ? '+' : ''}${formatINR(metrics.totalPL)}`;
    statPLEl.style.color = metrics.totalPL >= 0 ? 'var(--success)' : 'var(--danger)';
  }

  const statPLDeltaEl = document.getElementById('statPLDelta');
  if (statPLDeltaEl) {
    const icon = metrics.totalPL > 0 ? 'arrow-up-short' : metrics.totalPL < 0 ? 'arrow-down-short' : 'dash';
    const cls = metrics.totalPL > 0 ? 'delta-up' : metrics.totalPL < 0 ? 'delta-down' : 'delta-flat';
    statPLDeltaEl.className = `pm-stat__dlt ${cls}`;
    statPLDeltaEl.innerHTML = `<i class="bi bi-${icon}"></i> ${metrics.plPercent.toFixed(2)}% overall`;
  }
}

function getPortfolioMetrics(portfolios, investments) {
  const totalPortfolios = (portfolios || []).length;
  const totalInvestments = (investments || []).length;
  const totalInvested = sumBy(investments, 'investedAmount');
  const totalCurrentValue = sumBy(investments, 'currentValue');
  const totalPL = sumBy(investments, 'profitLoss');
  const plPercent = totalCurrentValue > 0 ? (totalPL / totalCurrentValue) * 100 : 0;

  return {
    totalPortfolios,
    totalInvestments,
    totalInvested,
    totalCurrentValue,
    totalPL,
    plPercent,
  };
}

function loadRecentTransactions(transactions) {
  const tbody = document.getElementById('recentTransactionsBody');
  if (!tbody) return;

  if (!transactions || transactions.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" style="padding:24px;text-align:center;color:var(--gray-500);">No recent transactions found.</td></tr>';
    return;
  }

  const sorted = [...transactions].sort((a, b) => {
    const aTime = Date.parse(a.transactionDate || '') || 0;
    const bTime = Date.parse(b.transactionDate || '') || 0;
    return bTime - aTime;
  }).slice(0, 5);

  tbody.innerHTML = sorted.map((tx) => {
    const type = String(tx.transactionType || '').toUpperCase();
    const isBuy = type === 'BUY';
    const badgeClass = isBuy ? 'pm-badge--green' : 'pm-badge--red';
    const icon = isBuy ? 'arrow-down-circle-fill' : 'arrow-up-circle-fill';

    return `
      <tr>
        <td>
          <div style="font-weight:600;">${escapeHtml(tx.symbol || 'N/A')}</div>
          <div style="font-size:.74rem;color:var(--gray-500);">${escapeHtml(tx.companyName || 'Unknown')}</div>
        </td>
        <td><span class="pm-badge ${badgeClass}"><i class="bi bi-${icon}"></i>${escapeHtml(type || 'N/A')}</span></td>
        <td>${formatNumber(tx.quantity)}</td>
        <td style="font-weight:700;">${formatINR(toNumber(tx.transactionAmount))}</td>
        <td style="color:var(--gray-500);font-size:.78rem;">${formatDate(tx.transactionDate)}</td>
      </tr>
    `;
  }).join('');
}

function renderInvestmentSummary(investments) {
  const container = document.getElementById('investmentSummaryList');
  if (!container) return;

  if (!investments || investments.length === 0) {
    container.innerHTML = '<div style="padding:8px 0;color:var(--gray-500);">No investments found for this account.</div>';
    return;
  }

  const byValue = [...investments].sort(
    (a, b) => toNumber(b.currentValue) - toNumber(a.currentValue)
  );

  const topInvestments = byValue.slice(0, 3);
  const totalCurrentValue = sumBy(investments, 'currentValue') || 1;
  const colors = ['var(--pink-300)', 'var(--info)', 'var(--warning)'];

  container.innerHTML = topInvestments.map((inv, index) => {
    const value = toNumber(inv.currentValue);
    const percent = Math.max(1, Math.round((value / totalCurrentValue) * 100));
    const name = inv.companyName || inv.symbol || 'Unnamed Investment';

    return `
      <div style="margin-bottom:${index === topInvestments.length - 1 ? 0 : 14}px;">
        <div style="display:flex;justify-content:space-between;margin-bottom:4px;font-size:.79rem;">
          <span style="color:var(--gray-700);font-weight:500;">${escapeHtml(name)}</span>
          <span style="color:var(--gray-600);">${formatINR(value)}</span>
        </div>
        <div style="height:6px;background:var(--gray-200);border-radius:99px;overflow:hidden;">
          <div style="height:100%;background:${colors[index % colors.length]};border-radius:99px;width:${percent}%;"></div>
        </div>
      </div>
    `;
  }).join('');
}

function showDashboardError(message) {
  const text = escapeHtml(message || 'Unable to load dashboard data.');

  injectBootstrapAlert(text);

  setText('statPortfolios', '0');
  setText('statCount', '0');
  setText('statInvested', '₹0');
  setText('statValue', '₹0');
  setText('statPL', '₹0');
  setText('statPLDelta', '0.00% overall');

  const tbody = document.getElementById('recentTransactionsBody');
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="5" style="padding:24px;text-align:center;color:var(--danger);">${text}</td></tr>`;
  }

  const summary = document.getElementById('investmentSummaryList');
  if (summary) {
    summary.innerHTML = `<div style="padding:8px 0;color:var(--danger);">${text}</div>`;
  }
}

function injectBootstrapAlert(message) {
  const main = document.querySelector('main.pm-main');
  if (!main) return;

  const oldAlert = document.getElementById('dashboardApiAlert');
  if (oldAlert) oldAlert.remove();

  const alert = document.createElement('div');
  alert.id = 'dashboardApiAlert';
  alert.className = 'alert alert-danger alert-dismissible fade show';
  alert.setAttribute('role', 'alert');
  alert.innerHTML = `
    <strong>Failed to load dashboard data.</strong> ${message}
    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
  `;

  main.prepend(alert);
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

function toNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function sumBy(list, key) {
  return (list || []).reduce((sum, item) => sum + toNumber(item && item[key]), 0);
}

function formatINR(value) {
  const n = toNumber(value);
  if (n === 0) return '₹0';
  return `₹${n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatNumber(value) {
  const n = toNumber(value);
  return n.toLocaleString('en-IN', { maximumFractionDigits: 4 });
}

function formatDate(value) {
  if (!value) return 'N/A';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return escapeHtml(String(value));
  return date.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
