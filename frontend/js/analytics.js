/**
 * analytics.js — Backend-driven analytics dashboard
 */

let performanceTrendChartRef = null;
let profitLossTrendChartRef = null;
let assetGrowthTimelineChartRef = null;
let transactionActivityTrendChartRef = null;

let sparkTotalValueRef = null;
let sparkRoiRef = null;
let sparkProfitLossRef = null;
let sparkRiskRef = null;

const analyticsState = {
  portfolios: [],
  investments: [],
  transactions: [],
  selectedPortfolioId: null,
  mode: 'portfolio',
  currentRisk: null,
};

document.addEventListener('DOMContentLoaded', () => {
  if (typeof requireAuth === 'function') {
    requireAuth();
  }
  if (typeof initNavbarUser === 'function') {
    initNavbarUser();
  }

  loadAnalyticsDashboard();
});

document.addEventListener('pm:theme-change', () => {
  rerenderAnalyticsChartsForTheme();
});

async function loadAnalyticsDashboard() {
  setInitialKpiState();

  try {
    const [investmentsResponse, transactionsResponse, portfoliosResponse] = await Promise.all([
      InvestmentAPI.getAll(),
      TransactionAPI.getAll(),
      PortfolioAPI.getAll(),
    ]);

    analyticsState.investments = Array.isArray(investmentsResponse) ? investmentsResponse : [];
    analyticsState.transactions = Array.isArray(transactionsResponse) ? transactionsResponse : [];
    analyticsState.portfolios = Array.isArray(portfoliosResponse) ? portfoliosResponse : [];

    const timeline = buildTimelineData(analyticsState.investments, analyticsState.transactions);
    renderPortfolioPerformanceTimeline(timeline);
    renderProfitLossTrend(timeline);
    renderAssetGrowthTimeline(timeline.assetLines);
    renderTransactionActivityTrend(timeline.transactionTrend);
    renderInsights(analyticsState.investments, analyticsState.portfolios);

    wireAnalyticsControls();
    initializePortfolioSelector();

    if (analyticsState.portfolios.length === 1) {
      analyticsState.selectedPortfolioId = toNumber(analyticsState.portfolios[0].portfolioId);
      await analyzeSelectedPortfolio();
      return;
    }

    if (analyticsState.portfolios.length > 1) {
      analyticsState.selectedPortfolioId = toNumber(analyticsState.portfolios[0].portfolioId);
      await analyzeSelectedPortfolio();
      return;
    }

    showAnalyticsError('Create a portfolio with investments to run risk analysis.');
    renderBackendRiskData(null, 'portfolio');
  } catch (error) {
    console.error('[Analytics] Failed to load analytics data:', error);
    showAnalyticsError(error.message || 'Unable to load analytics data.');

    const emptyTimeline = buildTimelineData([], []);
    renderPortfolioPerformanceTimeline(emptyTimeline);
    renderProfitLossTrend(emptyTimeline);
    renderAssetGrowthTimeline(emptyTimeline.assetLines);
    renderTransactionActivityTrend(emptyTimeline.transactionTrend);
    renderInsights([], []);
    renderBackendRiskData(null, analyticsState.mode);
  }
}

function wireAnalyticsControls() {
  const portfolioBtn = document.getElementById('analyzePortfolioBtn');
  const assetBtn = document.getElementById('analyzeAssetBtn');
  const assetRunBtn = document.getElementById('runAssetAnalysisBtn');
  const portfolioSelect = document.getElementById('analyticsPortfolioSelect');
  const symbolInput = document.getElementById('assetSymbolInput');

  if (portfolioBtn) {
    portfolioBtn.addEventListener('click', async () => {
      switchAnalysisMode('portfolio');
      await analyzeSelectedPortfolio();
    });
  }

  if (assetBtn) {
    assetBtn.addEventListener('click', () => {
      switchAnalysisMode('asset');
    });
  }

  if (assetRunBtn) {
    assetRunBtn.addEventListener('click', async () => {
      await analyzeIndividualAsset();
    });
  }

  if (portfolioSelect) {
    portfolioSelect.addEventListener('change', async (event) => {
      analyticsState.selectedPortfolioId = toNumber(event.target.value);
      if (analyticsState.mode === 'portfolio') {
        await analyzeSelectedPortfolio();
      }
    });
  }

  if (symbolInput) {
    symbolInput.addEventListener('keydown', async (event) => {
      if (event.key === 'Enter') {
        event.preventDefault();
        await analyzeIndividualAsset();
      }
    });
  }
}

function initializePortfolioSelector() {
  const group = document.getElementById('analyticsPortfolioGroup');
  const select = document.getElementById('analyticsPortfolioSelect');

  if (!group || !select) return;

  if (analyticsState.portfolios.length > 1) {
    group.style.display = 'block';
    select.innerHTML = analyticsState.portfolios
      .map((portfolio) => {
        const portfolioId = toNumber(portfolio.portfolioId);
        const name = portfolio.portfolioName || `Portfolio ${portfolioId}`;
        return `<option value="${escapeHtml(String(portfolioId))}">${escapeHtml(name)}</option>`;
      })
      .join('');
  } else {
    group.style.display = 'none';
    select.innerHTML = '';
  }
}

function switchAnalysisMode(mode) {
  analyticsState.mode = mode === 'asset' ? 'asset' : 'portfolio';

  const portfolioBtn = document.getElementById('analyzePortfolioBtn');
  const assetBtn = document.getElementById('analyzeAssetBtn');
  const assetPanel = document.getElementById('assetAnalyzePanel');
  const assetMeta = document.getElementById('assetMetaSummary');

  if (portfolioBtn) {
    portfolioBtn.classList.toggle('btn-primary', analyticsState.mode === 'portfolio');
    portfolioBtn.classList.toggle('btn-outline-primary', analyticsState.mode !== 'portfolio');
  }

  if (assetBtn) {
    assetBtn.classList.toggle('btn-primary', analyticsState.mode === 'asset');
    assetBtn.classList.toggle('btn-outline-primary', analyticsState.mode !== 'asset');
  }

  if (assetPanel) {
    assetPanel.style.display = analyticsState.mode === 'asset' ? 'flex' : 'none';
  }

  if (assetMeta && analyticsState.mode !== 'asset') {
    assetMeta.style.display = 'none';
  }

  setText('riskAnalysisModeValue', analyticsState.mode === 'asset' ? 'INDIVIDUAL ASSET' : 'PORTFOLIO');
}

async function analyzeSelectedPortfolio() {
  if (!analyticsState.selectedPortfolioId) {
    renderBackendRiskData(null, 'portfolio');
    return;
  }

  try {
    const response = await RiskAnalysisAPI.analyzePortfolio(analyticsState.selectedPortfolioId);
    const risk = normalizeRiskResponse(response);
    analyticsState.currentRisk = risk;
    renderBackendRiskData(risk, 'portfolio');
  } catch (error) {
    console.error('[Analytics] Portfolio risk analysis failed:', error);
    showAnalyticsError(error.message || 'Unable to analyze portfolio risk.');
    renderBackendRiskData(null, 'portfolio');
  }
}

async function analyzeIndividualAsset() {
  const symbolInput = document.getElementById('assetSymbolInput');
  const rawSymbol = symbolInput ? String(symbolInput.value || '').trim() : '';

  if (!rawSymbol) {
    showAnalyticsError('Enter a ticker symbol to analyze an individual asset.');
    return;
  }

  try {
    const response = await RiskAnalysisAPI.analyzeStock(rawSymbol, 'STOCK');
    const risk = normalizeRiskResponse(response);
    analyticsState.currentRisk = risk;
    renderBackendRiskData(risk, 'asset');
  } catch (error) {
    console.error('[Analytics] Asset risk analysis failed:', error);
    showAnalyticsError('Unable to fetch live market data. Using manually entered price.');
    renderBackendRiskData(null, 'asset');
  }
}

function normalizeRiskResponse(payload) {
  if (!payload || typeof payload !== 'object') {
    return null;
  }

  return {
    symbol: payload.symbol || null,
    exchange: payload.exchange || null,
    currency: payload.currency || null,
    portfolioId: toNumber(payload.portfolioId),
    portfolioValue: toNumber(payload.portfolioValue),
    annualizedVolatility: toNumber(payload.annualizedVolatility),
    maximumDrawdown: toNumber(payload.maximumDrawdown),
    averageAnnualReturn: toNumber(payload.averageAnnualReturn),
    sharpeRatio: toNumber(payload.sharpeRatio),
    riskLevel: payload.riskLevel || 'LOW',
  };
}

function setInitialKpiState() {
  setText('kpiTotalValue', '₹0');
  setText('kpiRoi', '0.00%');
  setText('kpiProfitLoss', '0.00%');
  setText('kpiRiskScore', '0.0000');

  setTrend('kpiTotalValueTrend', 'flat', 'Portfolio Value');
  setTrend('kpiRoiTrend', 'flat', 'Average Return');
  setTrend('kpiProfitLossTrend', 'flat', 'Annualized Volatility');
  setTrend('kpiRiskScoreTrend', 'flat', 'LOW');
}

function renderBackendRiskData(riskData, mode) {
  const isAssetMode = mode === 'asset';
  const risk = riskData || {
    symbol: null,
    exchange: null,
    currency: null,
    portfolioValue: 0,
    annualizedVolatility: 0,
    maximumDrawdown: 0,
    averageAnnualReturn: 0,
    sharpeRatio: 0,
    riskLevel: 'LOW',
  };

  const timeline = buildTimelineData(analyticsState.investments, analyticsState.transactions);
  renderKpiCards(risk, timeline, isAssetMode);

  setText('riskPortfolioValue', isAssetMode ? 'N/A' : formatINR(risk.portfolioValue));
  setText('riskAverageReturn', formatPercent(risk.averageAnnualReturn));
  setText('riskAnnualizedVolatility', formatPercent(risk.annualizedVolatility));
  setText('riskMaximumDrawdown', formatPercent(risk.maximumDrawdown));
  setText('riskSharpeRatio', formatDecimal(risk.sharpeRatio, 4));
  setText('riskLevelValue', risk.riskLevel || 'LOW');
  setText('riskAnalysisModeValue', isAssetMode ? 'INDIVIDUAL ASSET' : 'PORTFOLIO');

  const assetMeta = document.getElementById('assetMetaSummary');
  if (assetMeta) {
    assetMeta.style.display = isAssetMode ? 'block' : 'none';
  }

  setText('assetSymbolValue', risk.symbol || 'N/A');
  setText('assetExchangeValue', risk.exchange || 'N/A');
  setText('assetCurrencyValue', risk.currency || 'N/A');
}

function renderKpiCards(riskData, timeline, isAssetMode) {
  animateCount('kpiTotalValue', isAssetMode ? 0 : riskData.portfolioValue, formatINR);
  animateCount('kpiRoi', riskData.averageAnnualReturn, (value) => `${value.toFixed(2)}%`);
  animateCount('kpiProfitLoss', riskData.annualizedVolatility, (value) => `${value.toFixed(2)}%`);
  animateCount('kpiRiskScore', riskData.sharpeRatio, (value) => value.toFixed(4));

  setTrend('kpiTotalValueTrend', 'flat', isAssetMode ? 'Individual asset mode' : 'Portfolio level');
  setTrend('kpiRoiTrend', riskData.averageAnnualReturn >= 0 ? 'up' : 'down', `${riskData.averageAnnualReturn.toFixed(2)}% return`);
  setTrend('kpiProfitLossTrend', riskData.annualizedVolatility >= 30 ? 'down' : 'up', `${riskData.annualizedVolatility.toFixed(2)}% volatility`);
  setTrend('kpiRiskScoreTrend', riskData.riskLevel === 'LOW' ? 'up' : 'flat', riskData.riskLevel || 'LOW');

  renderSparklines(timeline, riskData);
}

function renderPortfolioPerformanceTimeline(timeline) {
  const ctx = getCanvasContext('performanceTrendChart');
  if (!ctx || typeof Chart === 'undefined') return;

  const theme = getChartTheme();

  destroyChart(performanceTrendChartRef);
  performanceTrendChartRef = new Chart(ctx, {
    type: 'line',
    data: {
      labels: timeline.labels,
      datasets: [
        {
          label: 'Invested Amount',
          data: timeline.investedSeries,
          borderColor: '#7C4DFF',
          backgroundColor: 'rgba(124,77,255,0.18)',
          fill: true,
          tension: 0.4,
          borderWidth: 2,
          pointRadius: 2,
          pointHoverRadius: 5,
        },
        {
          label: 'Current Portfolio Value',
          data: timeline.currentSeries,
          borderColor: '#00B8D4',
          backgroundColor: 'rgba(0,184,212,0.16)',
          fill: true,
          tension: 0.4,
          borderWidth: 2,
          pointRadius: 2,
          pointHoverRadius: 5,
        },
      ],
    },
    options: buildCommonChartOptions(theme),
  });
}

function renderProfitLossTrend(timeline) {
  const ctx = getCanvasContext('profitLossTrendChart');
  if (!ctx || typeof Chart === 'undefined') return;

  const theme = getChartTheme();

  const gradient = ctx.createLinearGradient(0, 0, 0, 260);
  gradient.addColorStop(0, 'rgba(46, 125, 50, 0.35)');
  gradient.addColorStop(0.5, 'rgba(255, 152, 0, 0.15)');
  gradient.addColorStop(1, 'rgba(198, 40, 40, 0.30)');

  destroyChart(profitLossTrendChartRef);
  profitLossTrendChartRef = new Chart(ctx, {
    type: 'line',
    data: {
      labels: timeline.labels,
      datasets: [
        {
          label: 'Cumulative Profit/Loss',
          data: timeline.profitSeries,
          borderColor: '#00C853',
          backgroundColor: gradient,
          fill: true,
          tension: 0.4,
          borderWidth: 2,
          pointRadius: 2,
          pointHoverRadius: 5,
          segment: {
            borderColor: (segmentCtx) => {
              const y = segmentCtx.p1.parsed.y;
              return y >= 0 ? '#2E7D32' : '#C62828';
            },
          },
        },
      ],
    },
    options: buildCommonChartOptions(theme),
  });
}

function renderAssetGrowthTimeline(assetLines) {
  const ctx = getCanvasContext('assetGrowthTimelineChart');
  if (!ctx || typeof Chart === 'undefined') return;

  const theme = getChartTheme();

  destroyChart(assetGrowthTimelineChartRef);
  assetGrowthTimelineChartRef = new Chart(ctx, {
    type: 'line',
    data: {
      labels: assetLines.labels,
      datasets: assetLines.datasets,
    },
    options: buildCommonChartOptions(theme),
  });
}

function renderTransactionActivityTrend(trendData) {
  const ctx = getCanvasContext('transactionActivityTrendChart');
  if (!ctx || typeof Chart === 'undefined') return;

  const theme = getChartTheme();

  destroyChart(transactionActivityTrendChartRef);
  transactionActivityTrendChartRef = new Chart(ctx, {
    type: 'line',
    data: {
      labels: trendData.labels,
      datasets: [
        {
          label: 'BUY Activity',
          data: trendData.buySeries,
          borderColor: '#43A047',
          backgroundColor: 'rgba(67,160,71,0.18)',
          fill: true,
          tension: 0.4,
          borderWidth: 2,
          pointRadius: 2,
          pointHoverRadius: 5,
        },
        {
          label: 'SELL Activity',
          data: trendData.sellSeries,
          borderColor: '#EF5350',
          backgroundColor: 'rgba(239,83,80,0.18)',
          fill: true,
          tension: 0.4,
          borderWidth: 2,
          pointRadius: 2,
          pointHoverRadius: 5,
        },
      ],
    },
    options: buildCommonChartOptions(theme, true),
  });
}

function renderSparklines(timeline, riskData) {
  renderSparkline('sparkTotalValue', sparkTotalValueRef, timeline.currentSeries, '#00B8D4', (ref) => {
    sparkTotalValueRef = ref;
  });
  renderSparkline('sparkRoi', sparkRoiRef, timeline.profitSeries, '#7C4DFF', (ref) => {
    sparkRoiRef = ref;
  });
  renderSparkline('sparkProfitLoss', sparkProfitLossRef, timeline.profitSeries, '#00C853', (ref) => {
    sparkProfitLossRef = ref;
  });

  const riskBase = toNumber(riskData && riskData.annualizedVolatility);
  const riskSeries = timeline.labels.map((_, index) => {
    if (!timeline.labels.length) return 0;
    const ratio = (index + 1) / timeline.labels.length;
    return Number((riskBase * ratio).toFixed(2));
  });

  renderSparkline('sparkRisk', sparkRiskRef, riskSeries, '#FF8F00', (ref) => {
    sparkRiskRef = ref;
  });
}

function renderSparkline(canvasId, existingRef, data, color, setRef) {
  const ctx = getCanvasContext(canvasId);
  if (!ctx || typeof Chart === 'undefined') return;

  const theme = getChartTheme();

  destroyChart(existingRef);
  const safeData = data && data.length ? data : [0, 0, 0, 0];

  const gradient = ctx.createLinearGradient(0, 0, 0, 40);
  gradient.addColorStop(0, `${color}66`);
  gradient.addColorStop(1, `${color}05`);

  const chart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: safeData.map((_, idx) => idx + 1),
      datasets: [
        {
          data: safeData,
          borderColor: color,
          backgroundColor: gradient,
          tension: 0.4,
          fill: true,
          borderWidth: 1.8,
          pointRadius: 0,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 2000 },
      plugins: {
        legend: { display: false },
        tooltip: {
          enabled: true,
          backgroundColor: theme.tooltipBackground,
          titleColor: theme.tooltipTitle,
          bodyColor: theme.tooltipBody,
          borderColor: theme.tooltipBorder,
          borderWidth: 1,
        },
      },
      scales: {
        x: { display: false },
        y: { display: false },
      },
    },
  });

  setRef(chart);
}

function buildCommonChartOptions(theme, integerYAxis) {
  const resolvedTheme = theme || getChartTheme();

  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
      duration: 2000,
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
        ticks: integerYAxis
          ? { precision: 0, color: resolvedTheme.tick }
          : { color: resolvedTheme.tick },
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

function rerenderAnalyticsChartsForTheme() {
  if (!document.getElementById('performanceTrendChart')) return;

  const timeline = buildTimelineData(analyticsState.investments, analyticsState.transactions);
  renderPortfolioPerformanceTimeline(timeline);
  renderProfitLossTrend(timeline);
  renderAssetGrowthTimeline(timeline.assetLines);
  renderTransactionActivityTrend(timeline.transactionTrend);
  renderBackendRiskData(analyticsState.currentRisk, analyticsState.mode);
}

function renderInsights(investments, portfolios) {
  const container = document.getElementById('insightsContainer');
  if (!container) return;

  if (!investments.length) {
    container.innerHTML = '<div class="col-12"><div class="pm-insight-card p-3">No insights available yet.</div></div>';
    return;
  }

  const sortedByPL = [...investments].sort((a, b) => toNumber(b.profitLoss) - toNumber(a.profitLoss));
  const best = sortedByPL[0];
  const worst = sortedByPL[sortedByPL.length - 1];
  const highestInvestment = [...investments].sort((a, b) => toNumber(b.investedAmount) - toNumber(a.investedAmount))[0];
  const bestDiversified = calculateMostDiversifiedPortfolio(investments, portfolios);

  const insights = [
    {
      title: 'Best Performing Asset',
      value: best ? (best.companyName || best.symbol || 'N/A') : 'N/A',
      sub: best ? `P/L: ${formatSignedINR(best.profitLoss)}` : 'No data',
      icon: 'graph-up-arrow',
      badgeClass: 'bg-success-subtle text-success-emphasis',
    },
    {
      title: 'Worst Performing Asset',
      value: worst ? (worst.companyName || worst.symbol || 'N/A') : 'N/A',
      sub: worst ? `P/L: ${formatSignedINR(worst.profitLoss)}` : 'No data',
      icon: 'graph-down-arrow',
      badgeClass: 'bg-danger-subtle text-danger-emphasis',
    },
    {
      title: 'Highest Investment',
      value: highestInvestment ? (highestInvestment.companyName || highestInvestment.symbol || 'N/A') : 'N/A',
      sub: highestInvestment ? `Invested: ${formatINR(highestInvestment.investedAmount)}` : 'No data',
      icon: 'currency-rupee',
      badgeClass: 'bg-info-subtle text-info-emphasis',
    },
    {
      title: 'Most Diversified Portfolio',
      value: bestDiversified,
      sub: 'Based on unique asset spread',
      icon: 'diagram-3',
      badgeClass: 'bg-warning-subtle text-warning-emphasis',
    },
  ];

  container.innerHTML = insights.map((item) => `
    <div class="col-12 col-md-6 col-xl-3">
      <div class="pm-insight-card p-3 h-100">
        <div class="pm-insight-card__head">
          <span class="badge ${item.badgeClass}" style="font-size:.72rem;">Insight</span>
          <i class="bi bi-${item.icon} pm-insight-card__icon"></i>
        </div>
        <div class="pm-insight-card__title">${item.title}</div>
        <div class="pm-insight-card__value">${escapeHtml(item.value)}</div>
        <div class="pm-insight-card__sub">${escapeHtml(item.sub)}</div>
      </div>
    </div>
  `).join('');
}

function buildTimelineData(investments, transactions) {
  const normalizedInvestments = [...(investments || [])]
    .map((item) => ({
      ...item,
      _date: parseDate(item.purchaseDate),
    }))
    .sort((a, b) => {
      const at = a._date ? a._date.getTime() : 0;
      const bt = b._date ? b._date.getTime() : 0;
      return at - bt;
    });

  const labels = normalizedInvestments.length
    ? normalizedInvestments.map((item, index) => item._date ? formatShortDate(item._date) : `Asset ${index + 1}`)
    : ['No Data'];

  let cumulativeInvested = 0;
  let cumulativeCurrent = 0;

  const investedSeries = [];
  const currentSeries = [];

  normalizedInvestments.forEach((item) => {
    cumulativeInvested += toNumber(item.investedAmount);
    cumulativeCurrent += toNumber(item.currentValue);
    investedSeries.push(cumulativeInvested);
    currentSeries.push(cumulativeCurrent);
  });

  if (!normalizedInvestments.length) {
    investedSeries.push(0);
    currentSeries.push(0);
  }

  const profitSeries = currentSeries.map((value, index) => value - (investedSeries[index] || 0));

  const txByMonth = groupTransactionsByMonth(transactions || []);
  const transactionLabels = Object.keys(txByMonth);
  const buySeries = transactionLabels.map((key) => txByMonth[key].buy);
  const sellSeries = transactionLabels.map((key) => txByMonth[key].sell);

  const safeTxLabels = transactionLabels.length ? transactionLabels : ['No Data'];
  const safeBuySeries = transactionLabels.length ? buySeries : [0];
  const safeSellSeries = transactionLabels.length ? sellSeries : [0];

  const aggregatedAssets = getAggregatedInvestments(normalizedInvestments)
    .map((item) => ({
      ...item,
      _date: parseDate(item.purchaseDate),
    }));

  const topAssets = aggregatedAssets
    .slice()
    .sort((a, b) => toNumber(b.currentValue) - toNumber(a.currentValue))
    .slice(0, 6);

  const palette = ['#5C6BC0', '#26A69A', '#EC407A', '#42A5F5', '#FFA726', '#AB47BC'];
  const assetLines = {
    labels,
    datasets: topAssets.length
      ? topAssets.map((asset, index) => {
          const start = toNumber(asset.investedAmount);
          const end = toNumber(asset.currentValue);
          const points = labels.map((_, pointIndex) => {
            if (labels.length === 1) return end;
            const progress = pointIndex / (labels.length - 1);
            return start + ((end - start) * progress);
          });

          return {
            label: asset.companyName || asset.symbol || `Asset ${index + 1}`,
            data: points,
            borderColor: palette[index % palette.length],
            backgroundColor: `${palette[index % palette.length]}22`,
            fill: true,
            tension: 0.4,
            borderWidth: 2,
            pointRadius: 1.5,
            pointHoverRadius: 4,
          };
        })
      : [
          {
            label: 'No Data',
            data: [0],
            borderColor: '#90A4AE',
            backgroundColor: 'rgba(144,164,174,0.2)',
            fill: true,
            tension: 0.4,
            borderWidth: 2,
            pointRadius: 0,
          },
        ],
  };

  return {
    labels,
    investedSeries,
    currentSeries,
    profitSeries,
    transactionTrend: {
      labels: safeTxLabels,
      buySeries: safeBuySeries,
      sellSeries: safeSellSeries,
    },
    assetLines,
  };
}

function groupTransactionsByMonth(transactions) {
  const groups = {};

  transactions.forEach((tx) => {
    const date = parseDate(tx.transactionDate);
    if (!date) return;
    const key = `${date.toLocaleString('en-IN', { month: 'short' })} ${date.getFullYear()}`;
    if (!groups[key]) {
      groups[key] = { buy: 0, sell: 0, order: date.getFullYear() * 12 + date.getMonth() };
    }

    const type = String(tx.transactionType || '').toUpperCase();
    if (type === 'BUY') groups[key].buy += 1;
    if (type === 'SELL') groups[key].sell += 1;
  });

  return Object.fromEntries(
    Object.entries(groups)
      .sort((a, b) => a[1].order - b[1].order)
      .map(([key, value]) => [key, value])
  );
}

function calculateMostDiversifiedPortfolio(investments, portfolios) {
  if (!portfolios || !portfolios.length) return 'N/A';

  const portfolioMap = {};
  (investments || []).forEach((item) => {
    const pid = toNumber(item.portfolioId);
    if (!pid) return;
    if (!portfolioMap[pid]) portfolioMap[pid] = new Set();
    portfolioMap[pid].add(String(item.symbol || item.assetType || item.companyName || 'Unknown'));
  });

  let bestPortfolio = portfolios[0];
  let bestScore = -1;

  portfolios.forEach((portfolio) => {
    const pid = toNumber(portfolio.portfolioId);
    const score = portfolioMap[pid] ? portfolioMap[pid].size : 0;
    if (score > bestScore) {
      bestScore = score;
      bestPortfolio = portfolio;
    }
  });

  return bestPortfolio ? (bestPortfolio.portfolioName || `Portfolio ${bestPortfolio.portfolioId}`) : 'N/A';
}

function showAnalyticsError(message) {
  const main = document.querySelector('main.pm-main');
  if (!main) return;

  const oldAlert = document.getElementById('analyticsApiAlert');
  if (oldAlert) oldAlert.remove();

  const alert = document.createElement('div');
  alert.id = 'analyticsApiAlert';
  alert.className = 'alert alert-warning alert-dismissible fade show';
  alert.setAttribute('role', 'alert');
  alert.innerHTML = `
    <strong>Analytics notice:</strong> ${escapeHtml(message)}
    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
  `;
  main.prepend(alert);
}

function animateCount(id, target, formatter) {
  const el = document.getElementById(id);
  if (!el) return;

  const duration = 1000;
  const start = performance.now();
  const from = 0;
  const safeTarget = Number.isFinite(target) ? target : 0;

  function frame(now) {
    const progress = clamp((now - start) / duration, 0, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    const value = from + ((safeTarget - from) * eased);
    el.textContent = formatter(value);
    if (progress < 1) {
      requestAnimationFrame(frame);
    } else {
      el.textContent = formatter(safeTarget);
    }
  }

  requestAnimationFrame(frame);
}

function setTrend(id, trend, text) {
  const el = document.getElementById(id);
  if (!el) return;

  const meta = trend === 'up'
    ? { icon: 'arrow-up-short', cls: 'delta-up' }
    : trend === 'down'
      ? { icon: 'arrow-down-short', cls: 'delta-down' }
      : { icon: 'dash', cls: 'delta-flat' };

  el.className = `pm-stat__dlt ${meta.cls}`;
  el.innerHTML = `<i class="bi bi-${meta.icon}"></i> ${escapeHtml(text || '')}`;
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

function getAggregatedInvestments(investments) {
  if (typeof window.aggregateInvestmentsBySymbol === 'function') {
    return window.aggregateInvestmentsBySymbol(investments);
  }
  return Array.isArray(investments) ? investments : [];
}

function setText(id, value) {
  const element = document.getElementById(id);
  if (element) {
    element.textContent = value;
  }
}

function toNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function formatINR(value) {
  const n = toNumber(value);
  if (n === 0) return '₹0';
  return `₹${n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatSignedINR(value) {
  const n = toNumber(value);
  return `${n > 0 ? '+' : ''}${formatINR(n)}`;
}

function formatPercent(value) {
  return `${toNumber(value).toFixed(2)}%`;
}

function formatDecimal(value, scale) {
  return toNumber(value).toFixed(scale);
}

function parseDate(value) {
  if (!value) return null;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return null;
  return d;
}

function formatShortDate(date) {
  return date.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: '2-digit',
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
