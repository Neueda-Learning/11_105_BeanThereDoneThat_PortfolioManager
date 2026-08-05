/**
 * analytics.js — Professional trading analytics dashboard
 */

let performanceTrendChartRef = null;
let profitLossTrendChartRef = null;
let assetGrowthTimelineChartRef = null;
let transactionActivityTrendChartRef = null;

let sparkTotalValueRef = null;
let sparkRoiRef = null;
let sparkProfitLossRef = null;
let sparkRiskRef = null;

document.addEventListener('DOMContentLoaded', () => {
  if (typeof requireAuth === 'function') {
    requireAuth();
  }
  if (typeof initNavbarUser === 'function') {
    initNavbarUser();
  }

  loadAnalyticsDashboard();
});

async function loadAnalyticsDashboard() {
  setInitialKpiState();

  try {
    const [investmentsResponse, transactionsResponse, portfoliosResponse] = await Promise.all([
      InvestmentAPI.getAll(),
      TransactionAPI.getAll(),
      PortfolioAPI.getAll(),
    ]);

    const data = {
      investments: Array.isArray(investmentsResponse) ? investmentsResponse : [],
      transactions: Array.isArray(transactionsResponse) ? transactionsResponse : [],
      portfolios: Array.isArray(portfoliosResponse) ? portfoliosResponse : [],
    };

    console.log('Analytics Data', data);

    const timeline = buildTimelineData(data.investments, data.transactions);
    const riskData = calculateRiskAnalysis(data.investments, data.portfolios);

    renderKpiCards(data.investments, riskData, timeline);
    renderPortfolioPerformanceTimeline(timeline);
    renderProfitLossTrend(timeline);
    renderAssetGrowthTimeline(timeline.assetLines);
    renderTransactionActivityTrend(timeline.transactionTrend);

    renderRiskSection(riskData);
    renderInsights(data.investments, data.portfolios, riskData);

    console.log('Risk Analysis', riskData);
  } catch (error) {
    console.error('[Analytics] Failed to load analytics data:', error);
    showAnalyticsError(error.message || 'Unable to load analytics data.');

    const emptyTimeline = buildTimelineData([], []);
    const emptyRisk = calculateRiskAnalysis([], []);

    renderKpiCards([], emptyRisk, emptyTimeline);
    renderPortfolioPerformanceTimeline(emptyTimeline);
    renderProfitLossTrend(emptyTimeline);
    renderAssetGrowthTimeline(emptyTimeline.assetLines);
    renderTransactionActivityTrend(emptyTimeline.transactionTrend);

    renderRiskSection(emptyRisk);
    renderInsights([], [], emptyRisk);

    console.log('Risk Analysis', emptyRisk);
  }
}

function setInitialKpiState() {
  setText('kpiTotalValue', '₹0');
  setText('kpiRoi', '0.00%');
  setText('kpiProfitLoss', '₹0');
  setText('kpiRiskScore', '0');

  setTrend('kpiTotalValueTrend', 'flat', 'Stable');
  setTrend('kpiRoiTrend', 'flat', 'Stable');
  setTrend('kpiProfitLossTrend', 'flat', 'Stable');
  setTrend('kpiRiskScoreTrend', 'flat', 'Low Risk');
}

function renderKpiCards(investments, riskData, timeline) {
  const totalInvested = sumBy(investments, 'investedAmount');
  const currentValue = sumBy(investments, 'currentValue');
  const profitLoss = sumBy(investments, 'profitLoss');
  const roi = totalInvested > 0 ? (profitLoss / totalInvested) * 100 : 0;

  animateCount('kpiTotalValue', totalInvested, formatINR);
  animateCount('kpiProfitLoss', profitLoss, (value) => `${value > 0 ? '+' : ''}${formatINR(value)}`);
  animateCount('kpiRoi', roi, (value) => `${value.toFixed(2)}%`);
  animateCount('kpiRiskScore', riskData.riskScore, (value) => String(Math.round(value)));

  const plEl = document.getElementById('kpiProfitLoss');
  if (plEl) {
    plEl.style.color = profitLoss >= 0 ? 'var(--success)' : 'var(--danger)';
  }

  const roiEl = document.getElementById('kpiRoi');
  if (roiEl) {
    roiEl.style.color = roi >= 0 ? 'var(--success)' : 'var(--danger)';
  }

  setTrend('kpiTotalValueTrend', currentValue >= totalInvested ? 'up' : 'down', currentValue >= totalInvested ? 'Portfolio above cost' : 'Portfolio below cost');
  setTrend('kpiRoiTrend', roi > 0 ? 'up' : roi < 0 ? 'down' : 'flat', `${roi.toFixed(2)}% return`);
  setTrend('kpiProfitLossTrend', profitLoss > 0 ? 'up' : profitLoss < 0 ? 'down' : 'flat', profitLoss >= 0 ? 'Positive momentum' : 'Drawdown active');
  setTrend('kpiRiskScoreTrend', riskData.riskScore > 60 ? 'down' : riskData.riskScore > 30 ? 'flat' : 'up', riskData.riskCategory);

  renderSparklines(timeline, riskData);
}

function renderPortfolioPerformanceTimeline(timeline) {
  const ctx = getCanvasContext('performanceTrendChart');
  if (!ctx || typeof Chart === 'undefined') return;

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
    options: buildCommonChartOptions(),
  });
}

function renderProfitLossTrend(timeline) {
  const ctx = getCanvasContext('profitLossTrendChart');
  if (!ctx || typeof Chart === 'undefined') return;

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
    options: buildCommonChartOptions(),
  });
}

function renderAssetGrowthTimeline(assetLines) {
  const ctx = getCanvasContext('assetGrowthTimelineChart');
  if (!ctx || typeof Chart === 'undefined') return;

  destroyChart(assetGrowthTimelineChartRef);
  assetGrowthTimelineChartRef = new Chart(ctx, {
    type: 'line',
    data: {
      labels: assetLines.labels,
      datasets: assetLines.datasets,
    },
    options: buildCommonChartOptions(),
  });
}

function renderTransactionActivityTrend(trendData) {
  const ctx = getCanvasContext('transactionActivityTrendChart');
  if (!ctx || typeof Chart === 'undefined') return;

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
    options: buildCommonChartOptions(true),
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

  const riskSeries = timeline.labels.map((_, index) => {
    if (!timeline.labels.length) return 0;
    const ratio = (index + 1) / timeline.labels.length;
    return Math.round(riskData.riskScore * ratio);
  });

  renderSparkline('sparkRisk', sparkRiskRef, riskSeries, '#FF8F00', (ref) => {
    sparkRiskRef = ref;
  });
}

function renderSparkline(canvasId, existingRef, data, color, setRef) {
  const ctx = getCanvasContext(canvasId);
  if (!ctx || typeof Chart === 'undefined') return;

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
      plugins: { legend: { display: false }, tooltip: { enabled: true } },
      scales: {
        x: { display: false },
        y: { display: false },
      },
    },
  });

  setRef(chart);
}

function buildCommonChartOptions(integerYAxis) {
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
        labels: { usePointStyle: true, boxWidth: 8 },
      },
      tooltip: {
        enabled: true,
        backgroundColor: 'rgba(17,24,39,0.92)',
        titleColor: '#ffffff',
        bodyColor: '#e5e7eb',
        borderColor: 'rgba(255,255,255,0.15)',
        borderWidth: 1,
      },
    },
    scales: {
      x: {
        grid: { color: 'rgba(148,163,184,0.15)' },
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(148,163,184,0.15)' },
        ticks: integerYAxis ? { precision: 0 } : {},
      },
    },
  };
}

function calculateRiskAnalysis(investments, portfolios) {
  if (!investments.length) {
    return {
      riskScore: 0,
      riskCategory: 'Low Risk',
      diversificationScore: 0,
      portfolioHealthScore: 0,
      diversification: 'No assets',
      topRiskAsset: 'N/A',
      recommendation: 'Portfolio is well diversified',
      mostDiversifiedPortfolio: portfolios && portfolios.length ? (portfolios[0].portfolioName || `Portfolio ${portfolios[0].portfolioId}`) : 'N/A',
    };
  }

  const totalCurrentValue = sumBy(investments, 'currentValue');
  const totalInvested = sumBy(investments, 'investedAmount');
  const totalProfitLoss = sumBy(investments, 'profitLoss');

  const sortedByValue = [...investments].sort((a, b) => toNumber(b.currentValue) - toNumber(a.currentValue));
  const topRisk = sortedByValue[0] || null;
  const topWeight = totalCurrentValue > 0 ? (toNumber(topRisk?.currentValue) / totalCurrentValue) * 100 : 0;

  const uniqueAssets = new Set(investments.map((item) => String(item.symbol || item.companyName || item.investmentId || '').trim()).filter(Boolean));
  const assetCount = uniqueAssets.size;

  const plPercent = totalInvested > 0 ? (totalProfitLoss / totalInvested) * 100 : 0;

  const concentrationRisk = clamp(topWeight, 0, 100);
  const performanceRisk = plPercent >= 10 ? 10 : plPercent >= 0 ? 25 : plPercent >= -10 ? 55 : 80;
  const diversificationRisk = assetCount >= 10 ? 10 : assetCount >= 6 ? 30 : assetCount >= 4 ? 55 : 80;

  const riskScore = Math.round((0.45 * concentrationRisk) + (0.30 * performanceRisk) + (0.25 * diversificationRisk));
  const clampedRiskScore = clamp(riskScore, 0, 100);

  const diversificationScore = clamp(Math.round(100 - diversificationRisk), 0, 100);
  const portfolioHealthScore = clamp(Math.round((100 - performanceRisk) * 0.6 + diversificationScore * 0.4), 0, 100);

  const riskCategory = clampedRiskScore <= 30 ? 'Low Risk' : clampedRiskScore <= 60 ? 'Medium Risk' : 'High Risk';

  const diversification = assetCount >= 10
    ? 'Strong diversification'
    : assetCount >= 6
      ? 'Moderate diversification'
      : 'Diversification needs improvement';

  let recommendation = 'Portfolio is well diversified';
  if (riskCategory === 'Medium Risk') {
    recommendation = 'Consider increasing diversification';
  } else if (riskCategory === 'High Risk') {
    recommendation = 'Reduce concentration in risky assets';
  }

  const mostDiversifiedPortfolio = calculateMostDiversifiedPortfolio(investments, portfolios);

  return {
    riskScore: clampedRiskScore,
    riskCategory,
    diversificationScore,
    portfolioHealthScore,
    diversification,
    topRiskAsset: topRisk ? (topRisk.companyName || topRisk.symbol || 'N/A') : 'N/A',
    recommendation,
    mostDiversifiedPortfolio,
  };
}

function renderRiskSection(riskData) {
  setText('riskScoreValue', String(riskData.riskScore));
  setText('riskCategoryValue', riskData.riskCategory);
  setText('riskDiversificationScoreValue', String(riskData.diversificationScore));
  setText('riskHealthScoreValue', String(riskData.portfolioHealthScore));
  setText('riskDiversificationValue', riskData.diversification);
  setText('riskTopAssetValue', riskData.topRiskAsset);
  setText('riskRecommendationValue', riskData.recommendation);
}

function renderInsights(investments, portfolios, riskData) {
  const container = document.getElementById('insightsContainer');
  if (!container) return;

  if (!investments.length) {
    container.innerHTML = '<div class="col-12"><div class="p-3" style="border:1px solid var(--gray-300);border-radius:var(--r-sm);background:rgba(255,255,255,.6);">No insights available yet.</div></div>';
    return;
  }

  const sortedByPL = [...investments].sort((a, b) => toNumber(b.profitLoss) - toNumber(a.profitLoss));
  const best = sortedByPL[0];
  const worst = sortedByPL[sortedByPL.length - 1];
  const highestInvestment = [...investments].sort((a, b) => toNumber(b.investedAmount) - toNumber(a.investedAmount))[0];

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
      value: riskData.mostDiversifiedPortfolio || 'N/A',
      sub: riskData.recommendation,
      icon: 'diagram-3',
      badgeClass: 'bg-warning-subtle text-warning-emphasis',
    },
  ];

  container.innerHTML = insights.map((item) => `
    <div class="col-12 col-md-6 col-xl-3">
      <div class="p-3 h-100" style="border:1px solid var(--gray-300);border-radius:var(--r-sm);background:rgba(255,255,255,.6);backdrop-filter:blur(8px);">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
          <span class="badge ${item.badgeClass}" style="font-size:.72rem;">Insight</span>
          <i class="bi bi-${item.icon}" style="font-size:1rem;color:var(--gray-600);"></i>
        </div>
        <div style="font-size:.72rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.8px;margin-bottom:4px;">${item.title}</div>
        <div style="font-weight:700;font-size:.92rem;margin-bottom:5px;">${escapeHtml(item.value)}</div>
        <div style="font-size:.8rem;color:var(--gray-600);">${escapeHtml(item.sub)}</div>
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

  const topAssets = normalizedInvestments
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
    portfolioMap[pid].add(String(item.assetType || item.symbol || item.companyName || 'Unknown'));
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
  alert.className = 'alert alert-danger alert-dismissible fade show';
  alert.setAttribute('role', 'alert');
  alert.innerHTML = `
    <strong>Failed to load analytics data.</strong> ${escapeHtml(message)}
    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
  `;
  main.prepend(alert);
}

function animateCount(id, target, formatter) {
  const el = document.getElementById(id);
  if (!el) return;

  const duration = 1400;
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

function setText(id, value) {
  const element = document.getElementById(id);
  if (element) {
    element.textContent = value;
  }
}

function sumBy(list, key) {
  return (list || []).reduce((sum, item) => sum + toNumber(item && item[key]), 0);
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
