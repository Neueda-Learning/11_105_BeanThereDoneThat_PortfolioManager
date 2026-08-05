/**
 * dashboard.js — Dashboard page logic
 * Loads dashboard data and renders live Chart.js visualizations.
 */

let growthChartRef = null;
let allocationChartRef = null;

const dashboardState = {
  portfolios: [],
  investments: [],
  transactions: [],
  growthPeriodMonths: 1,
  autoRefreshHandle: null,
  controlsBound: false,
};

document.addEventListener('DOMContentLoaded', () => {
  if (typeof requireAuth === 'function') {
    requireAuth();
  }
  if (typeof initNavbarUser === 'function') {
    initNavbarUser();
  }

  initializeGrowthControls();
  setGreeting();
  setLoadingState();
  loadDashboard();
  startAutoRefresh();
});

document.addEventListener('pm:theme-change', () => {
  rerenderDashboardChartsForTheme();
});

async function loadDashboard() {
  try {
    const dashboardData = await getDashboardData();
    dashboardState.portfolios = dashboardData.portfolios;
    dashboardState.investments = dashboardData.investments;
    dashboardState.transactions = dashboardData.transactions;

    loadDashboardCards(dashboardData.portfolios, dashboardData.investments);
    loadRecentTransactions(dashboardData.transactions);
    renderInvestmentSummary(dashboardData.investments);
    renderGrowthChart(dashboardData.investments, dashboardState.growthPeriodMonths);
    renderAllocationChart(dashboardData.investments);
  } catch (error) {
    console.error('[Dashboard] Failed to load data:', error);
    showDashboardError(error.message || 'Unable to load dashboard data.');
  }
}

async function getDashboardData() {
  const [portfoliosResponse, investmentsResponse, transactionsResponse] = await Promise.all([
    PortfolioAPI.getAll(),
    InvestmentAPI.getAll(),
    TransactionAPI.getAll(),
  ]);

  return {
    portfolios: Array.isArray(portfoliosResponse) ? portfoliosResponse : [],
    investments: Array.isArray(investmentsResponse) ? investmentsResponse : [],
    transactions: Array.isArray(transactionsResponse) ? transactionsResponse : [],
  };
}

function initializeGrowthControls() {
  if (dashboardState.controlsBound) return;

  document.querySelectorAll('[data-growth-period]').forEach((button) => {
    button.addEventListener('click', () => {
      const months = toNumber(button.getAttribute('data-growth-period'));
      if (!months) return;

      dashboardState.growthPeriodMonths = months;
      document.querySelectorAll('[data-growth-period]').forEach((item) => {
        item.classList.toggle('active-period', item === button);
      });
      renderGrowthChart(dashboardState.investments, months);
    });
  });

  dashboardState.controlsBound = true;
}

function startAutoRefresh() {
  if (dashboardState.autoRefreshHandle) {
    clearInterval(dashboardState.autoRefreshHandle);
  }

  dashboardState.autoRefreshHandle = setInterval(() => {
    loadDashboard();
  }, 60000);
}

function setGreeting() {
  const greetingEl = document.getElementById('dashboardGreeting');
  if (!greetingEl) return;

  const customerName = (getCustomerName() || '').trim();
  const firstName = customerName ? customerName.split(/\s+/)[0] : '';
  const hour = new Date().getHours();

  let greeting = 'Good Evening';
  if (hour >= 5 && hour < 12) greeting = 'Good Morning';
  else if (hour >= 12 && hour < 17) greeting = 'Good Afternoon';
  else greeting = 'Good Evening';

  greetingEl.textContent = firstName ? `${greeting}, ${firstName}` : greeting;
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

function renderGrowthChart(investments, periodMonths) {
  const ctx = getCanvasContext('growthChart');
  if (!ctx || typeof Chart === 'undefined') return;

  const theme = getChartTheme();

  const timeline = buildGrowthTimeline(investments, periodMonths);
  destroyChart(growthChartRef);

  growthChartRef = new Chart(ctx, {
    type: 'line',
    data: {
      labels: timeline.labels,
      datasets: [
        {
          label: 'Invested Amount',
          data: timeline.investedSeries,
          borderColor: '#EC407A',
          backgroundColor: 'rgba(236,64,122,0.16)',
          fill: true,
          tension: 0.35,
          borderWidth: 2,
          pointRadius: 2,
          pointHoverRadius: 5,
        },
        {
          label: 'Current Portfolio Value',
          data: timeline.currentSeries,
          borderColor: '#1976D2',
          backgroundColor: 'rgba(25,118,210,0.16)',
          fill: true,
          tension: 0.35,
          borderWidth: 2,
          pointRadius: 2,
          pointHoverRadius: 5,
        },
      ],
    },
    options: buildChartOptions({ currencyAxis: true, theme }),
  });
}

function renderAllocationChart(investments) {
  const ctx = getCanvasContext('allocChart');
  if (!ctx || typeof Chart === 'undefined') return;

  const theme = getChartTheme();

  const aggregated = getAggregatedInvestments(investments)
    .filter((item) => toNumber(item.currentValue) > 0)
    .sort((a, b) => toNumber(b.currentValue) - toNumber(a.currentValue));

  const labels = aggregated.length ? aggregated.map((item) => item.symbol || 'Unknown') : ['No Data'];
  const values = aggregated.length ? aggregated.map((item) => toNumber(item.currentValue)) : [0];
  const total = values.reduce((sum, value) => sum + value, 0);

  const palette = ['#F48FB1', '#1976D2', '#F57C00', '#388E3C', '#7E57C2', '#0097A7', '#6D4C41', '#546E7A'];

  destroyChart(allocationChartRef);
  allocationChartRef = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [
        {
          label: 'Allocation by Symbol',
          data: values,
          backgroundColor: labels.map((_, index) => palette[index % palette.length]),
          borderColor: theme.surface,
          borderWidth: 2,
          hoverOffset: 8,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 1200, easing: 'easeOutCubic' },
      plugins: {
        legend: {
          position: 'bottom',
          labels: { usePointStyle: true, boxWidth: 8, color: theme.label },
        },
        tooltip: {
          backgroundColor: theme.tooltipBackground,
          titleColor: theme.tooltipTitle,
          bodyColor: theme.tooltipBody,
          borderColor: theme.tooltipBorder,
          borderWidth: 1,
          callbacks: {
            label: (context) => {
              const value = toNumber(context.parsed);
              const pct = total > 0 ? (value / total) * 100 : 0;
              return `${context.label}: ${formatINR(value)} (${pct.toFixed(2)}%)`;
            },
          },
        },
      },
      cutout: '55%',
    },
  });
}

function buildGrowthTimeline(investments, periodMonths) {
  const parsed = (investments || []).map((item) => ({
    ...item,
    _purchaseDate: parseDate(item.purchaseDate),
  }));

  if (!parsed.length) {
    return {
      labels: ['No Data'],
      investedSeries: [0],
      currentSeries: [0],
    };
  }

  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth() - (periodMonths - 1), 1);
  const monthPoints = [];
  for (let i = 0; i < periodMonths; i += 1) {
    monthPoints.push(new Date(start.getFullYear(), start.getMonth() + i + 1, 0));
  }

  const labels = monthPoints.map((date) => date.toLocaleDateString('en-IN', { month: 'short', year: '2-digit' }));

  const investedSeries = monthPoints.map((pointDate) => parsed.reduce((sum, item) => {
    if (!item._purchaseDate || item._purchaseDate > pointDate) return sum;
    return sum + toNumber(item.investedAmount);
  }, 0));

  const currentSeries = monthPoints.map((pointDate) => parsed.reduce((sum, item) => {
    return sum + estimateAssetValueAtDate(item, pointDate, now);
  }, 0));

  if (currentSeries.length) {
    const latest = sumBy(parsed, 'currentValue');
    currentSeries[currentSeries.length - 1] = latest;
  }

  return { labels, investedSeries, currentSeries };
}

function estimateAssetValueAtDate(investment, pointDate, now) {
  const purchaseDate = investment._purchaseDate;
  if (!purchaseDate || pointDate < purchaseDate) return 0;

  const invested = toNumber(investment.investedAmount);
  const current = toNumber(investment.currentValue);

  if (invested <= 0) return current;
  if (current <= 0) return 0;

  const totalDays = Math.max(1, Math.floor((now.getTime() - purchaseDate.getTime()) / 86400000));
  const elapsedDays = Math.max(0, Math.min(totalDays, Math.floor((pointDate.getTime() - purchaseDate.getTime()) / 86400000)));
  const progress = elapsedDays / totalDays;

  const ratio = current / invested;
  const estimatedRatio = 1 + ((ratio - 1) * progress);
  return invested * estimatedRatio;
}

function buildChartOptions({ currencyAxis, theme } = {}) {
  const resolvedTheme = theme || getChartTheme();

  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
      duration: 1300,
      easing: 'easeInOutQuart',
    },
    interaction: {
      intersect: false,
      mode: 'index',
    },
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          usePointStyle: true,
          boxWidth: 8,
          color: resolvedTheme.label,
        },
      },
      tooltip: {
        enabled: true,
        backgroundColor: resolvedTheme.tooltipBackground,
        titleColor: resolvedTheme.tooltipTitle,
        bodyColor: resolvedTheme.tooltipBody,
        borderColor: resolvedTheme.tooltipBorder,
        borderWidth: 1,
        callbacks: currencyAxis
          ? {
              label: (context) => `${context.dataset.label}: ${formatINR(context.parsed.y)}`,
            }
          : {},
      },
    },
    scales: {
      x: {
        grid: { color: resolvedTheme.grid },
        ticks: { color: resolvedTheme.tick },
      },
      y: {
        beginAtZero: true,
        grid: { color: resolvedTheme.grid },
        ticks: { color: resolvedTheme.tick },
      },
    },
  };
}

function getChartTheme() {
  const css = getComputedStyle(document.documentElement);

  return {
    label: css.getPropertyValue('--gray-800').trim() || '#424242',
    tick: css.getPropertyValue('--gray-600').trim() || '#757575',
    grid: toRgba(css.getPropertyValue('--gray-400').trim() || '#BDBDBD', 0.25),
    surface: css.getPropertyValue('--white').trim() || '#FFFFFF',
    tooltipBackground: toRgba(css.getPropertyValue('--gray-50').trim() || '#FAFAFA', 0.96),
    tooltipTitle: css.getPropertyValue('--gray-900').trim() || '#212121',
    tooltipBody: css.getPropertyValue('--gray-800').trim() || '#424242',
    tooltipBorder: css.getPropertyValue('--gray-300').trim() || '#E0E0E0',
  };
}

function toRgba(color, alpha) {
  const normalized = String(color || '').trim();
  if (/^#([\da-f]{3}|[\da-f]{6})$/i.test(normalized)) {
    const hex = normalized.length === 4
      ? normalized.slice(1).split('').map((c) => c + c).join('')
      : normalized.slice(1);
    const r = parseInt(hex.slice(0, 2), 16);
    const g = parseInt(hex.slice(2, 4), 16);
    const b = parseInt(hex.slice(4, 6), 16);
    return `rgba(${r},${g},${b},${alpha})`;
  }

  return normalized;
}

function rerenderDashboardChartsForTheme() {
  if (!document.getElementById('growthChart') || !document.getElementById('allocChart')) return;
  renderGrowthChart(dashboardState.investments, dashboardState.growthPeriodMonths);
  renderAllocationChart(dashboardState.investments);
}

function getAggregatedInvestments(investments) {
  if (typeof window.aggregateInvestmentsBySymbol === 'function') {
    return window.aggregateInvestmentsBySymbol(investments);
  }
  return Array.isArray(investments) ? investments : [];
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

  const assets = getAggregatedInvestments(investments);
  if (!assets || assets.length === 0) {
    container.innerHTML = '<div style="padding:8px 0;color:var(--gray-500);">No investments found for this account.</div>';
    return;
  }

  const byValue = [...assets].sort(
    (a, b) => toNumber(b.currentValue) - toNumber(a.currentValue)
  );

  const topInvestments = byValue.slice(0, 3);
  const totalCurrentValue = sumBy(assets, 'currentValue') || 1;
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

  destroyChart(growthChartRef);
  destroyChart(allocationChartRef);

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

function getCanvasContext(id) {
  const canvas = document.getElementById(id);
  return canvas ? canvas.getContext('2d') : null;
}

function destroyChart(chartRef) {
  if (chartRef && typeof chartRef.destroy === 'function') {
    chartRef.destroy();
  }
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

function parseDate(value) {
  if (!value) return null;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return null;
  return d;
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
