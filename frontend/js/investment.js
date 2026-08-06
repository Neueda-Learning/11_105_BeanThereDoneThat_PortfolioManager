/**
 * investment.js — Investment module API integration
 * Handles both investments list page and add-investment page.
 */

let allInvestments = [];
let filteredInvestments = [];
let deleteTargetId = null;
let currentDisplayCurrency = 'INR';
let selectedPortfolioId = 0;
const investmentCurrencyController = window.PMCurrency
  ? window.PMCurrency.createDisplayController('INR')
  : null;

const IDENTIFIER_MODES = {
  SYMBOL: 'symbol',
  SCHEME_CODE: 'schemeCode',
  NONE: 'none',
};

document.addEventListener('DOMContentLoaded', () => {
  if (typeof requireAuth === 'function') {
    requireAuth();
  }
  if (typeof initNavbarUser === 'function') {
    initNavbarUser();
  }

  if (document.getElementById('investTable')) {
    initInvestmentsPage();
  }

  if (document.getElementById('addInvestmentForm')) {
    initAddInvestmentPage();
  }
});

async function initInvestmentsPage() {
  populateCurrencySelect('displayCurrency', currentDisplayCurrency);
  wireFilters();
  wireImportExport();
  wireEditInvestmentModal();
  wireDeleteInvestmentModal();
  await initPortfolioContextAndLoadInvestments();
}

async function initPortfolioContextAndLoadInvestments() {
  const portfolioFilter = document.getElementById('portfolioFilter');
  if (!portfolioFilter) {
    await loadInvestments();
    return;
  }

  portfolioFilter.disabled = true;
  portfolioFilter.innerHTML = '<option value="">Loading portfolios...</option>';

  try {
    const response = await PortfolioAPI.getAll();
    const portfolios = Array.isArray(response) ? response : [];
    populatePortfolioFilterOptions(portfolios);

    if (!portfolios.length) {
      selectedPortfolioId = 0;
      clearInvestmentsState('No portfolios found. Create a portfolio to view investments.');
      return;
    }

    const requestedPortfolioId = getPortfolioIdFromUrl();
    const requestedExists = portfolios.some((portfolio) => toNumber(portfolio.portfolioId) === requestedPortfolioId);
    selectedPortfolioId = requestedExists
      ? requestedPortfolioId
      : toNumber(portfolios[0].portfolioId);

    portfolioFilter.value = String(selectedPortfolioId);
    setPortfolioIdInUrl(selectedPortfolioId);
    portfolioFilter.disabled = false;

    await loadInvestments();
  } catch (error) {
    console.error('[Investments] Failed to load portfolios:', error);
    portfolioFilter.innerHTML = '<option value="">Unable to load portfolios</option>';
    clearInvestmentsState(error.message || 'Unable to load portfolios.', true);
  }
}

async function loadInvestments() {
  const tbody = getInvestmentsTbody();
  if (!tbody) return;

  if (!selectedPortfolioId) {
    clearInvestmentsState('Select a portfolio to view investments.');
    return;
  }

  tbody.innerHTML = `<tr><td colspan="13" style="padding:24px;text-align:center;color:var(--gray-500);"><span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Loading investments...</td></tr>`;

  try {
    const response = await InvestmentAPI.getByPortfolio(selectedPortfolioId);
    console.log('Investments API response:', response);
    allInvestments = Array.isArray(response) ? response : [];
    filteredInvestments = [...allInvestments];
    await preloadDisplayRates(allInvestments);

    renderInvestmentsTable(filteredInvestments);
    renderSummaryCards(allInvestments);
    updateTableFooter(filteredInvestments.length, allInvestments.length);
    hideInlineAlert();
  } catch (error) {
    console.error('[Investments] Failed to load investments:', error);
    renderInvestmentsTable([]);
    renderSummaryCards([]);
    showInlineAlert(error.message || 'Unable to load investments.', 'danger');
  }
}

function wireFilters() {
  const portfolioFilter = document.getElementById('portfolioFilter');
  const searchInput = document.getElementById('searchInput');
  const typeFilter = document.getElementById('typeFilter');
  const displayCurrency = document.getElementById('displayCurrency');

  if (portfolioFilter) {
    portfolioFilter.addEventListener('change', async (event) => {
      selectedPortfolioId = toNumber(event.target.value);
      setPortfolioIdInUrl(selectedPortfolioId);
      await loadInvestments();
    });
  }
  if (searchInput) {
    searchInput.addEventListener('input', applyFilters);
  }
  if (typeFilter) {
    typeFilter.addEventListener('change', applyFilters);
  }
  if (displayCurrency) {
    displayCurrency.addEventListener('change', async (event) => {
      currentDisplayCurrency = normalizeCurrencyCode(event.target.value) || 'INR';
      if (investmentCurrencyController) {
        investmentCurrencyController.setCurrency(currentDisplayCurrency);
      }
      await preloadDisplayRates(allInvestments);
      renderInvestmentsTable(filteredInvestments);
      renderSummaryCards(allInvestments);
    });
  }
}

function applyFilters() {
  const query = (document.getElementById('searchInput')?.value || '').trim().toLowerCase();
  const type = (document.getElementById('typeFilter')?.value || '').trim().toLowerCase();

  filteredInvestments = allInvestments.filter((item) => {
    const company = String(item.companyName || '').toLowerCase();
    const symbol = String(item.symbol || '').toLowerCase();
    const schemeCode = String(item.schemeCode || '').toLowerCase();
    const assetType = String(item.assetType || '').toLowerCase();

    const matchesQuery = !query || company.includes(query) || symbol.includes(query) || schemeCode.includes(query) || assetType.includes(query);
    const matchesType = !type || assetType === type;
    return matchesQuery && matchesType;
  });

  renderInvestmentsTable(filteredInvestments);
  updateTableFooter(filteredInvestments.length, allInvestments.length);
}

function populatePortfolioFilterOptions(portfolios) {
  const select = document.getElementById('portfolioFilter');
  if (!select) return;

  select.innerHTML = portfolios.map((portfolio) => {
    const id = toNumber(portfolio.portfolioId);
    const name = portfolio.portfolioName || `Portfolio ${id}`;
    return `<option value="${escapeHtml(String(id))}">${escapeHtml(name)}</option>`;
  }).join('');
}

function getPortfolioIdFromUrl() {
  const params = new URLSearchParams(window.location.search || '');
  return toNumber(params.get('portfolioId'));
}

function setPortfolioIdInUrl(portfolioId) {
  const url = new URL(window.location.href);
  if (portfolioId > 0) {
    url.searchParams.set('portfolioId', String(portfolioId));
  } else {
    url.searchParams.delete('portfolioId');
  }
  window.history.replaceState({}, '', url.toString());
}

function clearInvestmentsState(message, isError = false) {
  allInvestments = [];
  filteredInvestments = [];
  renderInvestmentsTable([]);
  renderSummaryCards([]);
  updateTableFooter(0, 0);

  if (message) {
    showInlineAlert(message, isError ? 'danger' : 'warning');
  } else {
    hideInlineAlert();
  }
}

function renderInvestmentsTable(list) {
  const tbody = getInvestmentsTbody();
  if (!tbody) return;

  if (!list.length) {
    tbody.innerHTML = '<tr><td colspan="13" style="padding:24px;text-align:center;color:var(--gray-500);">No investments found.</td></tr>';
    return;
  }

  tbody.innerHTML = list.map((investment) => {
    const id = toNumber(investment.investmentId);
    const profitLoss = toNumber(investment.profitLoss);
    const plColor = profitLoss >= 0 ? 'var(--success)' : 'var(--danger)';
    const originalCurrency = normalizeCurrencyCode(investment.currency);
    const displayProfitLoss = convertForDisplay(investment.profitLoss, originalCurrency);
    const plText = `${displayProfitLoss > 0 ? '+' : ''}${formatMoney(displayProfitLoss)}`;

    return `
      <tr>
        <td style="color:var(--gray-500);">${escapeHtml(String(id || '0'))}</td>
        <td style="font-weight:600;">${escapeHtml(investment.companyName || 'N/A')}</td>
        <td>${escapeHtml(investment.symbol || investment.schemeCode || 'N/A')}</td>
        <td>${escapeHtml(investment.exchange || 'N/A')}</td>
        <td>${escapeHtml(investment.currency || 'N/A')}</td>
        <td>${assetTypeBadge(investment.assetType || investment.customAssetType || 'N/A')}</td>
        <td>${formatNumber(investment.quantity)}</td>
        <td>${formatMoney(convertForDisplay(investment.purchasePrice, originalCurrency))}</td>
        <td>${formatMoney(convertForDisplay(investment.currentPrice, originalCurrency))}</td>
        <td style="font-weight:700;">${formatMoney(convertForDisplay(investment.currentValue, originalCurrency))}</td>
        <td style="color:${plColor};font-weight:600;">${plText}</td>
        <td style="color:var(--gray-500);font-size:.78rem;">${formatDate(investment.purchaseDate)}</td>
        <td style="text-align:center;">
          <div style="display:flex;gap:4px;justify-content:center;">
            <button class="pm-icon-btn" title="Edit" onclick="openInvestmentEdit(${id})"><i class="bi bi-pencil-fill"></i></button>
            <button class="pm-icon-btn pm-icon-btn--del" title="Delete" onclick="openInvestmentDelete(${id}, '${escapeHtml(investment.companyName || investment.symbol || investment.schemeCode || 'Investment')}')"><i class="bi bi-trash3-fill"></i></button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function renderSummaryCards(list) {
  const totalHoldings = list.length;
  const totalInvested = sumConvertedBy(list, 'investedAmount');
  const totalCurrentValue = sumConvertedBy(list, 'currentValue');
  const totalPL = sumConvertedBy(list, 'profitLoss');

  setText('investHoldingsValue', String(totalHoldings));
  setText('investInvestedValue', formatMoney(totalInvested));
  setText('investCurrentValue', formatMoney(totalCurrentValue));

  const plEl = document.getElementById('investPlValue');
  if (plEl) {
    plEl.textContent = `${totalPL > 0 ? '+' : ''}${formatMoney(totalPL)}`;
    plEl.style.color = totalPL >= 0 ? 'var(--success)' : 'var(--danger)';
  }
}

function updateTableFooter(visibleCount, totalCount) {
  const footerText = document.getElementById('investTableFooterText');
  if (!footerText) return;
  footerText.textContent = `Showing ${visibleCount} of ${totalCount} records`;
}

function wireEditInvestmentModal() {
  const editForm = document.getElementById('editInvestmentForm');
  if (!editForm) return;

  editForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideModalAlert('editModalAlert');

    const id = toNumber(document.getElementById('editInvestmentId')?.value);
    const payload = {
      quantity: toNumber(document.getElementById('editQuantity')?.value),
      purchasePrice: toNumber(document.getElementById('editPurchasePrice')?.value),
      purchaseDate: String(document.getElementById('editPurchaseDate')?.value || '').trim(),
      customAssetType: String(document.getElementById('editCustomAssetType')?.value || '').trim() || null,
    };

    if (!id || payload.quantity <= 0 || payload.purchasePrice <= 0 || !payload.purchaseDate) {
      showModalAlert('editModalAlert', 'Please enter valid update values.', 'danger');
      return;
    }

    const submitBtn = document.getElementById('editSaveBtn');
    setButtonLoading(submitBtn, true, 'Saving...');

    try {
      const updated = await InvestmentAPI.update(id, payload);
      console.log('Update investment API response:', updated);
      showPageAlert('Investment updated successfully.', 'success');
      closeModal('editModal');
      await loadInvestments();
    } catch (error) {
      console.error('[Investments] Update failed:', error);
      showModalAlert('editModalAlert', error.message || 'Failed to update investment.', 'danger');
    } finally {
      setButtonLoading(submitBtn, false, '<i class="bi bi-check-lg"></i> Save');
    }
  });
}

function wireDeleteInvestmentModal() {
  const confirmBtn = document.getElementById('confirmDeleteBtn');
  if (!confirmBtn) return;

  confirmBtn.addEventListener('click', async () => {
    if (!deleteTargetId) return;

    setButtonLoading(confirmBtn, true, 'Deleting...');
    try {
      const deleted = await InvestmentAPI.delete(deleteTargetId);
      console.log('Delete investment API response:', deleted);
      showPageAlert('Investment deleted successfully.', 'success');
      closeModal('deleteModal');
      deleteTargetId = null;
      await loadInvestments();
    } catch (error) {
      console.error('[Investments] Delete failed:', error);
      showPageAlert(error.message || 'Failed to delete investment.', 'danger');
    } finally {
      setButtonLoading(confirmBtn, false, 'Delete');
    }
  });
}

function wireImportExport() {
  const exportButton = document.getElementById('exportInvestmentsBtn');
  const templateButton = document.getElementById('downloadInvestmentTemplateBtn');
  const importForm = document.getElementById('investmentImportForm');
  const importModal = document.getElementById('investmentImportModal');

  if (exportButton) {
    exportButton.addEventListener('click', async () => {
      setButtonLoading(exportButton, true, 'Exporting...');
      try {
        const blob = await InvestmentAPI.exportCsv();
        triggerBrowserDownload(blob, `investments-customer-${getCustomerId() || 'data'}.csv`);
        showPageAlert('Investment CSV export generated successfully.', 'success');
      } catch (error) {
        console.error('[Investments] Export failed:', error);
        showPageAlert(error.message || 'Unable to export investments.', 'danger');
      } finally {
        setButtonLoading(exportButton, false, '<i class="bi bi-download"></i> Export');
      }
    });
  }

  if (templateButton) {
    templateButton.addEventListener('click', async () => {
      setButtonLoading(templateButton, true, 'Downloading...');
      try {
        const blob = await InvestmentAPI.downloadImportTemplate();
        triggerBrowserDownload(blob, 'investment-import-template.csv');
        showImportAlert('investmentImportAlert', 'Template downloaded successfully.', 'success');
      } catch (error) {
        console.error('[Investments] Template download failed:', error);
        showImportAlert('investmentImportAlert', error.message || 'Unable to download the import template.', 'danger');
      } finally {
        setButtonLoading(templateButton, false, '<i class="bi bi-file-earmark-arrow-down"></i> Download Import Template');
      }
    });
  }

  if (importModal) {
    importModal.addEventListener('show.bs.modal', resetInvestmentImportState);
  }

  if (importForm) {
    importForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      hideImportAlert('investmentImportAlert');

      const fileInput = document.getElementById('investmentImportFile');
      const file = fileInput?.files?.[0] || null;
      if (!file) {
        showImportAlert('investmentImportAlert', 'Please choose a CSV or Excel file to import.', 'danger');
        return;
      }

      const submitButton = document.getElementById('investmentImportSubmitBtn');
      setButtonLoading(submitButton, true, 'Importing...');

      try {
        const summary = await InvestmentAPI.importFile(file);
        renderInvestmentImportSummary(summary);
        const alertType = (summary.failedCount || 0) > 0 ? 'warning' : 'success';
        showImportAlert('investmentImportAlert', buildImportSummaryMessage(summary, 'investment', 'investments'), alertType);
        showPageAlert(buildImportSummaryMessage(summary, 'investment', 'investments'), alertType === 'warning' ? 'warning' : 'success');
        await initPortfolioContextAndLoadInvestments();
      } catch (error) {
        console.error('[Investments] Import failed:', error);
        showImportAlert('investmentImportAlert', error.message || 'Unable to import investments.', 'danger');
      } finally {
        setButtonLoading(submitButton, false, '<i class="bi bi-upload"></i> Import');
      }
    });
  }
}

async function initAddInvestmentPage() {
  await loadPortfolioOptions('portfolioId');
  populateCurrencySelect('currency', 'INR');
  wireAddInvestmentPageForm();
  wireAddInvestmentCustomType();
  syncAddInvestmentFormState();
}

function wireAddInvestmentPageForm() {
  const form = document.getElementById('addInvestmentForm');
  const submitBtn = document.getElementById('submitBtn');
  if (!form) return;

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideFormAlert();

    const payload = {
      portfolioId: toNumber(document.getElementById('portfolioId')?.value),
      symbol: String(document.getElementById('symbol')?.value || '').trim().toUpperCase(),
      schemeCode: String(document.getElementById('schemeCode')?.value || '').trim(),
      companyName: String(document.getElementById('companyName')?.value || '').trim(),
      assetType: String(document.getElementById('assetType')?.value || '').trim(),
      customAssetType: String(document.getElementById('customAssetType')?.value || '').trim() || null,
      currency: normalizeCurrencyCode(document.getElementById('currency')?.value),
      quantity: toNumber(document.getElementById('quantity')?.value),
      purchasePrice: toNumber(document.getElementById('purchasePrice')?.value),
      purchaseDate: String(document.getElementById('purchaseDate')?.value || '').trim(),
      currentPrice: toNullablePositiveNumber(document.getElementById('currentPrice')?.value),
    };

    if (!isValidCreatePayload(payload)) {
      showFormAlert('Please fill all required fields for the selected asset type.', 'danger');
      return;
    }

    setButtonLoading(submitBtn, true, 'Adding...');
    try {
      const created = await InvestmentAPI.create(payload);
      console.log('Add page create investment API response:', created);
      showFormAlert('Investment added successfully.', 'success');
      form.reset();
      syncAddInvestmentFormState();
      setTimeout(() => {
        window.location.href = `investments.html?portfolioId=${payload.portfolioId}`;
      }, 1000);
    } catch (error) {
      console.error('[Add Investment] Create failed:', error);
      showFormAlert(error.message || 'Failed to add investment.', 'danger');
    } finally {
      setButtonLoading(submitBtn, false, '<i class="bi bi-plus-circle-fill me-1"></i>Add Investment');
    }
  });
}

function wireAddInvestmentCustomType() {
  const select = document.getElementById('assetType');
  const currencySelect = document.getElementById('currency');
  if (!select) return;
  select.addEventListener('change', syncAddInvestmentFormState);
  if (currencySelect) {
    currencySelect.addEventListener('change', updateCreateCurrencyHints);
  }
}

async function loadPortfolioOptions(selectId) {
  const select = document.getElementById(selectId);
  if (!select) return;

  try {
    const response = await PortfolioAPI.getAll();
    console.log('Portfolios API response for select:', response);
    const portfolios = Array.isArray(response) ? response : [];

    if (!portfolios.length) {
      select.innerHTML = '<option value="">No portfolios available</option>';
      return;
    }

    select.innerHTML = '<option value="">Select portfolio...</option>' + portfolios.map((portfolio) => (
      `<option value="${escapeHtml(String(portfolio.portfolioId))}">${escapeHtml(portfolio.portfolioName || `Portfolio ${portfolio.portfolioId}`)}</option>`
    )).join('');
  } catch (error) {
    console.error('[Investments] Failed to load portfolios:', error);
    select.innerHTML = '<option value="">Unable to load portfolios</option>';
  }
}

function toggleCustomAssetType(assetTypeId, wrapId, inputId) {
  const select = document.getElementById(assetTypeId);
  const wrap = document.getElementById(wrapId);
  const input = document.getElementById(inputId);
  if (!select || !wrap || !input) return;

  const isCustom = String(select.value || '').trim().toLowerCase() === 'custom';
  wrap.style.display = isCustom ? '' : 'none';
  input.required = isCustom;
  if (!isCustom) {
    input.value = '';
  }
}

async function openInvestmentEdit(id) {
  try {
    hideModalAlert('editModalAlert');
    const investment = await InvestmentAPI.getById(id);
    console.log('Get investment by id API response:', investment);

    document.getElementById('editInvestmentId').value = investment.investmentId || '';
    document.getElementById('editCompanyName').value = investment.companyName || '';
    document.getElementById('editSymbol').value = investment.symbol || investment.schemeCode || '';
    document.getElementById('editAssetType').value = investment.assetType || '';
    document.getElementById('editCustomAssetType').value = investment.customAssetType || '';
    document.getElementById('editQuantity').value = investment.quantity || '';
    document.getElementById('editPurchasePrice').value = investment.purchasePrice || '';
    document.getElementById('editPurchaseDate').value = formatInputDate(investment.purchaseDate);
    setText('editIdentifierLabel', getIdentifierLabel(investment.assetType));
    setCurrencyHint('editPurchasePriceCurrencyHint', investment.currency);
    toggleCustomAssetType('editAssetType', 'editCustomAssetTypeWrap', 'editCustomAssetType');

    openModal('editModal');
  } catch (error) {
    console.error('[Investments] Failed to fetch investment by id:', error);
    showPageAlert(error.message || 'Failed to load investment details.', 'danger');
  }
}

function openInvestmentDelete(id, name) {
  deleteTargetId = id;
  const label = document.getElementById('deleteInvestmentName');
  if (label) {
    label.textContent = name || 'this investment';
  }
  openModal('deleteModal');
}

window.openInvestmentEdit = openInvestmentEdit;
window.openInvestmentDelete = openInvestmentDelete;

function showPageAlert(message, type) {
  const main = document.querySelector('main.pm-main, main.app-main');
  if (!main) return;

  const old = document.getElementById('investmentPageAlert');
  if (old) old.remove();

  const alert = document.createElement('div');
  alert.id = 'investmentPageAlert';
  alert.className = `alert alert-${type} alert-dismissible fade show`;
  alert.setAttribute('role', 'alert');
  alert.innerHTML = `${escapeHtml(message)}<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>`;
  main.prepend(alert);
}

function showInlineAlert(message, type) {
  const alert = document.getElementById('investAlert');
  if (!alert) return;
  alert.className = `alert alert-${type} m-3`;
  alert.textContent = message;
  alert.classList.remove('d-none');
}

function hideInlineAlert() {
  const alert = document.getElementById('investAlert');
  if (alert) alert.classList.add('d-none');
}

function showModalAlert(id, message, type) {
  const alert = document.getElementById(id);
  if (!alert) return;
  alert.className = `alert alert-${type}`;
  alert.textContent = message;
  alert.classList.remove('d-none');
}

function hideModalAlert(id) {
  const alert = document.getElementById(id);
  if (alert) alert.classList.add('d-none');
}

function showFormAlert(message, type) {
  const alert = document.getElementById('formAlert');
  if (!alert) return;
  alert.className = `alert alert-${type} mb-4`;
  alert.textContent = message;
  alert.classList.remove('d-none');
}

function hideFormAlert() {
  const alert = document.getElementById('formAlert');
  if (alert) alert.classList.add('d-none');
}

function resetInvestmentImportState() {
  hideImportAlert('investmentImportAlert');
  const summary = document.getElementById('investmentImportSummary');
  if (summary) {
    summary.classList.add('d-none');
    summary.innerHTML = '';
  }

  const fileInput = document.getElementById('investmentImportFile');
  if (fileInput) {
    fileInput.value = '';
  }
}

function renderInvestmentImportSummary(summary) {
  const container = document.getElementById('investmentImportSummary');
  if (!container) return;

  const failures = Array.isArray(summary?.failures) ? summary.failures : [];
  const rowsHtml = failures.length
    ? `<div class="alert alert-warning mb-0"><div style="font-weight:600;margin-bottom:6px;">Failed rows</div><ul class="mb-0 ps-3">${failures.map((failure) => `<li>Row ${escapeHtml(String(failure.rowNumber || ''))}: ${escapeHtml(failure.reason || 'Unable to import this row.')}</li>`).join('')}</ul></div>`
    : '<div class="alert alert-success mb-0">No row-level validation errors were found.</div>';

  container.classList.remove('d-none');
  container.innerHTML = `
    <div class="pm-card" style="border:1px solid var(--gray-200);box-shadow:none;">
      <div class="pm-card__bd" style="padding:12px 14px;">
        <div style="font-size:.82rem;font-weight:700;margin-bottom:8px;">Import Summary</div>
        <div style="font-size:.8rem;color:var(--gray-600);margin-bottom:10px;">Successful imports: ${escapeHtml(String(summary?.successfulCount || 0))} | Failed rows: ${escapeHtml(String(summary?.failedCount || 0))}</div>
        ${rowsHtml}
      </div>
    </div>`;
}

function buildImportSummaryMessage(summary, singularLabel, pluralLabel) {
  const successful = Number(summary?.successfulCount || 0);
  const failed = Number(summary?.failedCount || 0);
  const successLabel = successful === 1 ? singularLabel : pluralLabel;
  if (failed > 0) {
    return `Imported ${successful} ${successLabel}. ${failed} row${failed === 1 ? '' : 's'} failed validation.`;
  }
  return `Imported ${successful} ${successLabel} successfully.`;
}

function showImportAlert(elementId, message, type) {
  const alert = document.getElementById(elementId);
  if (!alert) return;
  alert.className = `alert alert-${type}`;
  alert.textContent = message;
  alert.classList.remove('d-none');
}

function hideImportAlert(elementId) {
  const alert = document.getElementById(elementId);
  if (alert) {
    alert.classList.add('d-none');
  }
}

function isValidCreatePayload(payload) {
  const identifierMode = getIdentifierMode(payload.assetType);
  return payload.portfolioId > 0
    && !!payload.companyName
    && !!payload.assetType
    && !!payload.currency
    && (identifierMode !== IDENTIFIER_MODES.SYMBOL || !!payload.symbol)
    && (identifierMode !== IDENTIFIER_MODES.SCHEME_CODE || !!payload.schemeCode)
    && payload.quantity > 0
    && payload.purchasePrice > 0
    && !!payload.purchaseDate;
}

function getInvestmentsTbody() {
  return document.querySelector('#investTable tbody');
}

function openModal(modalId) {
  const el = document.getElementById(modalId);
  if (!el || typeof bootstrap === 'undefined') return;
  bootstrap.Modal.getOrCreateInstance(el).show();
}

function closeModal(modalId) {
  const el = document.getElementById(modalId);
  if (!el || typeof bootstrap === 'undefined') return;
  bootstrap.Modal.getOrCreateInstance(el).hide();
}

function setButtonLoading(button, loading, loadingText) {
  if (!button) return;
  if (!button.dataset.originalHtml) {
    button.dataset.originalHtml = button.innerHTML;
  }

  button.disabled = loading;
  button.innerHTML = loading
    ? `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>${loadingText}`
    : button.dataset.originalHtml;
}

function renderValueWithZero(value) {
  return toNumber(value) === 0 ? formatMoney(0) : formatMoney(value);
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

function sumBy(list, key) {
  return (list || []).reduce((sum, item) => sum + toNumber(item && item[key]), 0);
}

function sumConvertedBy(list, key) {
  return (list || []).reduce((sum, item) => {
    const currency = normalizeCurrencyCode(item && item.currency);
    return sum + convertForDisplay(item && item[key], currency);
  }, 0);
}

function toNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function toNullablePositiveNumber(value) {
  if (value === undefined || value === null || String(value).trim() === '') {
    return null;
  }

  const num = Number(value);
  return Number.isFinite(num) && num > 0 ? num : null;
}

function formatMoney(value, currency = currentDisplayCurrency) {
  const resolvedCurrency = normalizeCurrencyCode(currency) || 'INR';
  if (window.PMCurrency && typeof window.PMCurrency.formatMoney === 'function') {
    return window.PMCurrency.formatMoney(value, resolvedCurrency);
  }
  const number = toNumber(value);
  return new Intl.NumberFormat(getCurrencyLocale(resolvedCurrency), {
    style: 'currency',
    currency: resolvedCurrency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(number);
}

function formatNumber(value) {
  return toNumber(value).toLocaleString('en-IN', { maximumFractionDigits: 4 });
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

function formatInputDate(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toISOString().slice(0, 10);
}

function assetTypeBadge(type) {
  const safeType = escapeHtml(String(type || 'N/A'));
  return `<span class="pm-badge pm-badge--blue">${safeType}</span>`;
}

function populateCurrencySelect(selectId, selectedCurrency) {
  if (window.PMCurrency) {
    window.PMCurrency.populateCurrencySelect(selectId, selectedCurrency);
  }
}

function convertForDisplay(value, sourceCurrency) {
  if (investmentCurrencyController) {
    return investmentCurrencyController.convert(value, sourceCurrency);
  }
  return toNumber(value);
}

async function preloadDisplayRates(list) {
  if (investmentCurrencyController) {
    await investmentCurrencyController.preloadFromItems(list, (item) => item && item.currency);
  }
}

function getCurrencyLocale(currency) {
  const normalized = normalizeCurrencyCode(currency) || 'INR';
  return normalized === 'INR' ? 'en-IN' : 'en-US';
}

function normalizeCurrencyCode(currency) {
  if (window.PMCurrency && typeof window.PMCurrency.normalizeCurrencyCode === 'function') {
    return window.PMCurrency.normalizeCurrencyCode(currency);
  }
  const normalized = String(currency || '').trim().toUpperCase();
  return normalized || null;
}

function syncAddInvestmentFormState() {
  toggleCustomAssetType('assetType', 'customTypeWrap', 'customAssetType');
  syncIdentifierFields();
  updateCreateCurrencyHints();
}

function syncIdentifierFields() {
  const assetType = document.getElementById('assetType')?.value;
  const mode = getIdentifierMode(assetType);
  const symbolWrap = document.getElementById('symbolWrap');
  const symbolInput = document.getElementById('symbol');
  const schemeCodeWrap = document.getElementById('schemeCodeWrap');
  const schemeCodeInput = document.getElementById('schemeCode');

  if (!symbolWrap || !symbolInput || !schemeCodeWrap || !schemeCodeInput) return;

  symbolWrap.style.display = mode === IDENTIFIER_MODES.SYMBOL ? '' : 'none';
  schemeCodeWrap.style.display = mode === IDENTIFIER_MODES.SCHEME_CODE ? '' : 'none';
  symbolInput.required = mode === IDENTIFIER_MODES.SYMBOL;
  schemeCodeInput.required = mode === IDENTIFIER_MODES.SCHEME_CODE;

  if (mode !== IDENTIFIER_MODES.SYMBOL) {
    symbolInput.value = '';
  }
  if (mode !== IDENTIFIER_MODES.SCHEME_CODE) {
    schemeCodeInput.value = '';
  }
}

function updateCreateCurrencyHints() {
  const currency = normalizeCurrencyCode(document.getElementById('currency')?.value) || 'INR';
  setCurrencyHint('purchasePriceCurrencyHint', currency);
  setCurrencyHint('currentPriceCurrencyHint', currency);
}

function setCurrencyHint(elementId, currency) {
  const hint = document.getElementById(elementId);
  if (hint) {
    hint.textContent = `(${normalizeCurrencyCode(currency) || 'INR'})`;
  }
}

function getIdentifierMode(assetType) {
  const normalizedType = String(assetType || '').trim().toLowerCase();
  if (normalizedType === 'mutual fund') return IDENTIFIER_MODES.SCHEME_CODE;
  if (normalizedType === 'gold') return IDENTIFIER_MODES.NONE;
  return IDENTIFIER_MODES.SYMBOL;
}

function getIdentifierLabel(assetType) {
  const mode = getIdentifierMode(assetType);
  if (mode === IDENTIFIER_MODES.SCHEME_CODE) return 'Scheme Code';
  if (mode === IDENTIFIER_MODES.NONE) return 'Identifier';
  return 'Symbol';
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function triggerBrowserDownload(blob, fileName) {
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}
