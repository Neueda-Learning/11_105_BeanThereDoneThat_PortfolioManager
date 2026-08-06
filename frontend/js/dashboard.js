/**
 * dashboard.js — Dashboard page logic
 * Loads dashboard data and renders live Chart.js visualizations.
 */

let growthChartRef = null;
let allocationChartRef = null;
const dashboardCurrencyController = window.PMCurrency
  ? window.PMCurrency.createDisplayController('INR')
  : null;

const dashboardState = {
  portfolios: [],
  investments: [],
  transactions: [],
  milestones: [],
  growthPeriodMonths: 1,
  displayCurrency: 'INR',
  autoRefreshHandle: null,
  controlsBound: false,
  milestoneHandlersBound: false,
  isLoading: false,
  hasPendingReload: false,
};

document.addEventListener('DOMContentLoaded', () => {
  if (typeof requireAuth === 'function') {
    requireAuth();
  }
  if (typeof initNavbarUser === 'function') {
    initNavbarUser();
  }

  initializeGrowthControls();
  initializeDashboardCurrencyControl();
  initializeMilestoneForm();
  initializeMilestoneInteractions();
  setGreeting();
  setLoadingState();
  loadDashboard();
  startAutoRefresh();
});

document.addEventListener('pm:theme-change', () => {
  rerenderDashboardChartsForTheme();
});

async function loadDashboard() {
  if (dashboardState.isLoading) {
    dashboardState.hasPendingReload = true;
    return;
  }

  dashboardState.isLoading = true;

  try {
    const dashboardData = await getDashboardData();
    dashboardState.portfolios = dashboardData.portfolios;
    dashboardState.investments = dashboardData.investments;
    dashboardState.transactions = dashboardData.transactions;
    dashboardState.milestones = dashboardData.milestones;

    if (dashboardCurrencyController) {
      await dashboardCurrencyController.preloadFromItems(dashboardData.investments, (item) => item && item.currency);
      await dashboardCurrencyController.preloadFromItems(dashboardData.transactions, (item) => item && item.currency);
      await dashboardCurrencyController.preloadFromCurrencies(['INR']);
    }

    loadDashboardCards(dashboardData.portfolios, dashboardData.investments);
    renderMilestones(dashboardData.milestones);
    loadRecentTransactions(dashboardData.transactions);
    renderInvestmentSummary(dashboardData.investments);
    renderGrowthChart(dashboardData.investments, dashboardState.growthPeriodMonths);
    renderAllocationChart(dashboardData.investments);
  } catch (error) {
    console.error('[Dashboard] Failed to load data:', error);
    showDashboardError(error.message || 'Unable to load dashboard data.');
  } finally {
    dashboardState.isLoading = false;
    if (dashboardState.hasPendingReload) {
      dashboardState.hasPendingReload = false;
      loadDashboard();
    }
  }
}

async function getDashboardData() {
  const [
    portfoliosResponse,
    investmentsResponse,
    transactionsResponse,
    milestonesResponse,
  ] = await Promise.all([
    PortfolioAPI.getAll(),
    InvestmentAPI.getAll(),
    TransactionAPI.getAll(),
    MilestoneAPI.getAll(),
  ]);

  return {
    portfolios: Array.isArray(portfoliosResponse) ? portfoliosResponse : [],
    investments: Array.isArray(investmentsResponse) ? investmentsResponse : [],
    transactions: Array.isArray(transactionsResponse) ? transactionsResponse : [],
    milestones: Array.isArray(milestonesResponse) ? milestonesResponse : [],
  };
}

function initializeMilestoneForm() {
  const form = document.getElementById('addMilestoneForm');
  if (!form) return;

  form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const itemInput = document.getElementById('milestoneItem');
    const priceInput = document.getElementById('milestonePrice');
    const submitButton = document.getElementById('milestoneSubmitBtn');
    const milestoneEditId = document.getElementById('milestoneEditId');
    const modalTitle = document.getElementById('milestoneModalTitle');

    const item = (itemInput?.value || '').trim();
    const price = Number(priceInput?.value);
    const editingId = Number(milestoneEditId?.value || 0);

    if (!item || !Number.isFinite(price) || price <= 0) {
      injectBootstrapAlert('Please enter a valid milestone item and target price.');
      return;
    }

    try {
      if (submitButton) {
        submitButton.disabled = true;
        submitButton.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Saving...';
      }

      if (editingId > 0) {
        await MilestoneAPI.update(editingId, { item, price });
      } else {
        await MilestoneAPI.create({ item, price });
      }

      form.reset();
      if (milestoneEditId) milestoneEditId.value = '';
      if (modalTitle) modalTitle.textContent = 'Add Milestone';
      const modalEl = document.getElementById('addMilestoneModal');
      if (modalEl && window.bootstrap?.Modal) {
        const modalInstance = window.bootstrap.Modal.getOrCreateInstance(modalEl);
        modalInstance.hide();
      }

      await loadDashboard();
    } catch (error) {
      injectBootstrapAlert(error.message || 'Unable to create milestone.');
    } finally {
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.innerHTML = '<i class="bi bi-check2"></i> Save Milestone';
      }
    }
  });

  const openAddButton = document.getElementById('openAddMilestoneBtn');
  if (openAddButton) {
    openAddButton.addEventListener('click', () => {
      const editId = document.getElementById('milestoneEditId');
      const modalTitle = document.getElementById('milestoneModalTitle');
      if (editId) editId.value = '';
      if (modalTitle) modalTitle.textContent = 'Add Milestone';
      form.reset();
    });
  }
}

function initializeMilestoneInteractions() {
  if (dashboardState.milestoneHandlersBound) return;

  const container = document.getElementById('nextMilestoneCardBody');
  if (!container) return;

  container.addEventListener('click', async (event) => {
    const target = event.target.closest('[data-milestone-action]');
    if (!target) return;

    const action = target.getAttribute('data-milestone-action');
    const milestoneId = Number(target.getAttribute('data-id') || 0);
    if (!action || !milestoneId) return;

    if (action === 'edit') {
      openMilestoneEdit(milestoneId);
      return;
    }

    if (action === 'delete') {
      if (!window.confirm('Delete this milestone?')) return;
      try {
        await MilestoneAPI.delete(milestoneId);
        await loadDashboard();
      } catch (error) {
        injectBootstrapAlert(error.message || 'Unable to delete milestone.');
      }
      return;
    }

    if (action === 'move-up' || action === 'move-down') {
      const direction = action === 'move-up' ? -1 : 1;
      await moveMilestone(milestoneId, direction);
    }
  });

  const reorderButton = document.getElementById('editMilestonesBtn');
  if (reorderButton) {
    reorderButton.addEventListener('click', () => {
      const firstCompactRow = container.querySelector('.milestone-list-item');
      if (firstCompactRow) {
        firstCompactRow.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
    });
  }

  dashboardState.milestoneHandlersBound = true;
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

function initializeDashboardCurrencyControl() {
  const select = document.getElementById('dashboardDisplayCurrency');
  if (!select || !window.PMCurrency) return;

  window.PMCurrency.populateCurrencySelect(select, dashboardState.displayCurrency);

  select.addEventListener('change', async (event) => {
    dashboardState.displayCurrency = window.PMCurrency.normalizeCurrencyCode(event.target.value) || 'INR';
    if (dashboardCurrencyController) {
      dashboardCurrencyController.setCurrency(dashboardState.displayCurrency);
      await dashboardCurrencyController.preloadFromItems(dashboardState.investments, (item) => item && item.currency);
      await dashboardCurrencyController.preloadFromItems(dashboardState.transactions, (item) => item && item.currency);
      await dashboardCurrencyController.preloadFromCurrencies(['INR']);
    }

    loadDashboardCards(dashboardState.portfolios, dashboardState.investments);
    renderMilestones(dashboardState.milestones);
    loadRecentTransactions(dashboardState.transactions);
    renderInvestmentSummary(dashboardState.investments);
    renderGrowthChart(dashboardState.investments, dashboardState.growthPeriodMonths);
    renderAllocationChart(dashboardState.investments);
  });
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
  setText('statInvested', formatDashboardMoney(0));
  setText('statValue', formatDashboardMoney(0));
  setText('statPL', formatDashboardMoney(0));
  setText('statPLDelta', '0.00% overall');

  const milestoneBody = document.getElementById('nextMilestoneCardBody');
  if (milestoneBody) {
    milestoneBody.innerHTML = '<div style="padding:8px 0;color:var(--gray-500);"><span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Loading milestone...</div>';
  }
}

function renderMilestones(milestones) {
  const container = document.getElementById('nextMilestoneCardBody');
  if (!container) return;

  if (!Array.isArray(milestones) || milestones.length === 0) {
    container.innerHTML = `
      <div class="milestone-empty-state">
        <div class="milestone-empty-state__icon"><i class="bi bi-bullseye"></i></div>
        <p>Add your first milestone and start tracking your progress.</p>
      </div>
    `;
    return;
  }

  const ordered = [...milestones].sort((left, right) => toNumber(left.displayOrder) - toNumber(right.displayOrder));
  const nextMilestone = ordered[0];
  const item = nextMilestone.item || 'Untitled milestone';
  const price = toNumber(nextMilestone.price);
  const completedAmount = toNumber(nextMilestone.completedAmount);
  const remainingAmount = toNumber(nextMilestone.remainingAmount);
  const progress = toNumber(nextMilestone.progressPercentage);
  const progressBarWidth = Math.max(0, Math.min(100, progress));
  const imageUrl = nextMilestone.imageUrl || getMilestonePlaceholderUrl(item);
  const others = ordered.slice(1);
  const displayPrice = convertDashboardAmount(price, 'INR');
  const displayCompletedAmount = convertDashboardAmount(completedAmount, 'INR');
  const displayRemainingAmount = convertDashboardAmount(remainingAmount, 'INR');

  container.innerHTML = `
    <div class="milestone-main">
      <div class="milestone-main__top">
        <img class="milestone-main__image" id="nextMilestoneImage" src="${escapeHtml(imageUrl)}" alt="${escapeHtml(item)}" />
        <div>
          <div class="milestone-main__name">${escapeHtml(item)}</div>
          <div class="milestone-main__target">Target: ${formatDashboardMoney(displayPrice)}</div>
          <div class="milestone-list-item__actions" style="margin-top:6px;">
            <button class="milestone-icon-btn" type="button" title="Move Up" data-milestone-action="move-up" data-id="${nextMilestone.milestoneId}"><i class="bi bi-arrow-up"></i></button>
            <button class="milestone-icon-btn" type="button" title="Move Down" data-milestone-action="move-down" data-id="${nextMilestone.milestoneId}"><i class="bi bi-arrow-down"></i></button>
            <button class="milestone-icon-btn" type="button" title="Edit" data-milestone-action="edit" data-id="${nextMilestone.milestoneId}"><i class="bi bi-pencil"></i></button>
            <button class="milestone-icon-btn" type="button" title="Delete" data-milestone-action="delete" data-id="${nextMilestone.milestoneId}"><i class="bi bi-trash"></i></button>
          </div>
        </div>
      </div>

      <div class="milestone-main__amount">${formatDashboardMoney(displayCompletedAmount)} / ${formatDashboardMoney(displayPrice)}</div>

      <div class="milestone-main__progress-track">
        <div class="milestone-main__progress-fill" style="width:${progressBarWidth.toFixed(2)}%;"></div>
      </div>

      <div class="milestone-main__meta">
        <span>${formatDashboardMoney(displayRemainingAmount)} remaining</span>
        <strong>${progress.toFixed(2)}%</strong>
      </div>
    </div>

    ${others.length ? '<div class="milestone-list-title">Remaining milestones</div>' : ''}

    <div class="milestone-list">
      ${others.map((milestone) => {
        const pct = toNumber(milestone.progressPercentage);
        const completed = toNumber(milestone.completedAmount);
        const remaining = toNumber(milestone.remainingAmount);

        return `
          <div class="milestone-list-item">
            <div class="milestone-list-item__row">
              <div class="milestone-list-item__name">${escapeHtml(milestone.item || 'Untitled milestone')}</div>
              <div class="milestone-list-item__actions">
                <button class="milestone-icon-btn" type="button" title="Move Up" data-milestone-action="move-up" data-id="${milestone.milestoneId}"><i class="bi bi-arrow-up"></i></button>
                <button class="milestone-icon-btn" type="button" title="Move Down" data-milestone-action="move-down" data-id="${milestone.milestoneId}"><i class="bi bi-arrow-down"></i></button>
                <button class="milestone-icon-btn" type="button" title="Edit" data-milestone-action="edit" data-id="${milestone.milestoneId}"><i class="bi bi-pencil"></i></button>
                <button class="milestone-icon-btn" type="button" title="Delete" data-milestone-action="delete" data-id="${milestone.milestoneId}"><i class="bi bi-trash"></i></button>
              </div>
            </div>
            <div class="milestone-list-item__meta">
              Target ${formatDashboardMoney(convertDashboardAmount(toNumber(milestone.price), 'INR'))} • ${pct.toFixed(2)}% complete • ${formatDashboardMoney(convertDashboardAmount(completed, 'INR'))} done / ${formatDashboardMoney(convertDashboardAmount(remaining, 'INR'))} remaining
            </div>
          </div>
        `;
      }).join('')}
    </div>
  `;

  const imageEl = document.getElementById('nextMilestoneImage');
  if (imageEl) {
    imageEl.addEventListener('error', () => {
      imageEl.src = getMilestonePlaceholderUrl(item);
    }, { once: true });
  }
}

function openMilestoneEdit(milestoneId) {
  const milestone = (dashboardState.milestones || []).find((item) => toNumber(item.milestoneId) === toNumber(milestoneId));
  if (!milestone) return;

  const modalTitle = document.getElementById('milestoneModalTitle');
  const editIdInput = document.getElementById('milestoneEditId');
  const itemInput = document.getElementById('milestoneItem');
  const priceInput = document.getElementById('milestonePrice');
  const modalEl = document.getElementById('addMilestoneModal');

  if (modalTitle) modalTitle.textContent = 'Edit Milestone';
  if (editIdInput) editIdInput.value = String(milestone.milestoneId);
  if (itemInput) itemInput.value = milestone.item || '';
  if (priceInput) priceInput.value = toNumber(milestone.price) || '';

  if (modalEl && window.bootstrap?.Modal) {
    const modalInstance = window.bootstrap.Modal.getOrCreateInstance(modalEl);
    modalInstance.show();
  }
}

async function moveMilestone(milestoneId, direction) {
  const ordered = [...(dashboardState.milestones || [])]
    .sort((left, right) => toNumber(left.displayOrder) - toNumber(right.displayOrder));

  const index = ordered.findIndex((milestone) => toNumber(milestone.milestoneId) === toNumber(milestoneId));
  if (index < 0) return;

  const targetIndex = index + direction;
  if (targetIndex < 0 || targetIndex >= ordered.length) return;

  const temp = ordered[index];
  ordered[index] = ordered[targetIndex];
  ordered[targetIndex] = temp;

  const orderedIds = ordered.map((milestone) => milestone.milestoneId);

  try {
    const updated = await MilestoneAPI.reorder(orderedIds);
    dashboardState.milestones = Array.isArray(updated) ? updated : ordered;
    renderMilestones(dashboardState.milestones);
  } catch (error) {
    injectBootstrapAlert(error.message || 'Unable to reorder milestones.');
  }
}

function loadDashboardCards(portfolios, investments) {
  const metrics = getPortfolioMetrics(portfolios, investments);

  setText('statPortfolios', String(metrics.totalPortfolios));
  setText('statCount', String(metrics.totalInvestments));
  setText('statInvested', formatDashboardMoney(metrics.totalInvested));
  setText('statValue', formatDashboardMoney(metrics.totalCurrentValue));

  const statPLEl = document.getElementById('statPL');
  if (statPLEl) {
    statPLEl.textContent = `${metrics.totalPL > 0 ? '+' : ''}${formatDashboardMoney(metrics.totalPL)}`;
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
  const totalInvested = sumByConvertedField(investments, 'investedAmount');
  const totalCurrentValue = sumByConvertedField(investments, 'currentValue');
  const totalPL = sumByConvertedField(investments, 'profitLoss');
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
  const config = {
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
  };

  if (!growthChartRef) {
    growthChartRef = new Chart(ctx, config);
    return;
  }

  growthChartRef.data.labels = timeline.labels;
  growthChartRef.data.datasets = config.data.datasets;
  growthChartRef.options = config.options;
  growthChartRef.update('none');
}

function renderAllocationChart(investments) {
  const ctx = getCanvasContext('allocChart');
  if (!ctx || typeof Chart === 'undefined') return;

  const theme = getChartTheme();

  const aggregated = getAggregatedInvestments(investments)
    .filter((item) => convertDashboardAmount(item.currentValue, item.currency) > 0)
    .sort((a, b) => convertDashboardAmount(b.currentValue, b.currency) - convertDashboardAmount(a.currentValue, a.currency));

  const labels = aggregated.length ? aggregated.map((item) => item.symbol || 'Unknown') : ['No Data'];
  const values = aggregated.length
    ? aggregated.map((item) => convertDashboardAmount(item.currentValue, item.currency))
    : [0];
  const total = values.reduce((sum, value) => sum + value, 0);

  const palette = ['#F48FB1', '#1976D2', '#F57C00', '#388E3C', '#7E57C2', '#0097A7', '#6D4C41', '#546E7A'];

  const config = {
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
              return `${context.label}: ${formatDashboardMoney(value)} (${pct.toFixed(2)}%)`;
            },
          },
        },
      },
      cutout: '55%',
    },
  };

  if (!allocationChartRef) {
    allocationChartRef = new Chart(ctx, config);
    return;
  }

  allocationChartRef.data.labels = config.data.labels;
  allocationChartRef.data.datasets = config.data.datasets;
  allocationChartRef.options = config.options;
  allocationChartRef.update('none');
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
    return sum + convertDashboardAmount(item.investedAmount, item.currency);
  }, 0));

  const currentSeries = monthPoints.map((pointDate) => parsed.reduce((sum, item) => {
    return sum + estimateAssetValueAtDate(item, pointDate, now);
  }, 0));

  if (currentSeries.length) {
    const latest = parsed.reduce((sum, item) => sum + convertDashboardAmount(item.currentValue, item.currency), 0);
    currentSeries[currentSeries.length - 1] = latest;
  }

  return { labels, investedSeries, currentSeries };
}

function estimateAssetValueAtDate(investment, pointDate, now) {
  const purchaseDate = investment._purchaseDate;
  if (!purchaseDate || pointDate < purchaseDate) return 0;

  const invested = convertDashboardAmount(investment.investedAmount, investment.currency);
  const current = convertDashboardAmount(investment.currentValue, investment.currency);

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
              label: (context) => `${context.dataset.label}: ${formatDashboardMoney(context.parsed.y)}`,
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
        <td style="font-weight:700;">${formatDashboardMoney(convertDashboardAmount(tx.transactionAmount, tx.currency))}</td>
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
    (a, b) => convertDashboardAmount(b.currentValue, b.currency) - convertDashboardAmount(a.currentValue, a.currency)
  );

  const topInvestments = byValue.slice(0, 3);
  const totalCurrentValue = sumByConvertedField(assets, 'currentValue') || 1;
  const colors = ['var(--pink-300)', 'var(--info)', 'var(--warning)'];

  container.innerHTML = topInvestments.map((inv, index) => {
    const value = convertDashboardAmount(inv.currentValue, inv.currency);
    const percent = Math.max(1, Math.round((value / totalCurrentValue) * 100));
    const name = inv.companyName || inv.symbol || 'Unnamed Investment';

    return `
      <div style="margin-bottom:${index === topInvestments.length - 1 ? 0 : 14}px;">
        <div style="display:flex;justify-content:space-between;margin-bottom:4px;font-size:.79rem;">
          <span style="color:var(--gray-700);font-weight:500;">${escapeHtml(name)}</span>
          <span style="color:var(--gray-600);">${formatDashboardMoney(value)}</span>
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
  setText('statInvested', formatDashboardMoney(0));
  setText('statValue', formatDashboardMoney(0));
  setText('statPL', formatDashboardMoney(0));
  setText('statPLDelta', '0.00% overall');

  destroyChart(growthChartRef);
  destroyChart(allocationChartRef);
  growthChartRef = null;
  allocationChartRef = null;

  const tbody = document.getElementById('recentTransactionsBody');
  if (tbody) {
    tbody.innerHTML = `<tr><td colspan="5" style="padding:24px;text-align:center;color:var(--danger);">${text}</td></tr>`;
  }

  const summary = document.getElementById('investmentSummaryList');
  if (summary) {
    summary.innerHTML = `<div style="padding:8px 0;color:var(--danger);">${text}</div>`;
  }

  const milestone = document.getElementById('nextMilestoneCardBody');
  if (milestone) {
    milestone.innerHTML = `<div style="padding:8px 0;color:var(--danger);">${text}</div>`;
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

function sumByConvertedField(list, key) {
  return (list || []).reduce((sum, item) => {
    return sum + convertDashboardAmount(item && item[key], item && item.currency);
  }, 0);
}

function normalizeCurrencyCode(currency) {
  if (window.PMCurrency && typeof window.PMCurrency.normalizeCurrencyCode === 'function') {
    return window.PMCurrency.normalizeCurrencyCode(currency);
  }
  const normalized = String(currency || '').trim().toUpperCase();
  return normalized || 'INR';
}

function convertDashboardAmount(value, sourceCurrency) {
  const amount = toNumber(value);
  if (!dashboardCurrencyController) return amount;
  return dashboardCurrencyController.convert(amount, sourceCurrency || 'INR');
}

function formatDashboardMoney(value) {
  const amount = toNumber(value);
  const currency = dashboardCurrencyController
    ? dashboardCurrencyController.getCurrency()
    : (normalizeCurrencyCode(dashboardState.displayCurrency) || 'INR');

  if (window.PMCurrency && typeof window.PMCurrency.formatMoney === 'function') {
    return window.PMCurrency.formatMoney(amount, currency);
  }

  if (currency === 'INR') {
    return `₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }

  return amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatINR(value) {
  return formatDashboardMoney(value);
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

function getMilestonePlaceholderUrl(item) {
  const text = encodeURIComponent((item || 'Goal').slice(0, 18));
  return `https://dummyimage.com/96x96/e0e0e0/616161.png?text=${text}`;
}
