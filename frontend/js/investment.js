/**
 * investment.js — Investment module API integration
 * Handles both investments list page and add-investment page.
 */

let allInvestments = [];
let filteredInvestments = [];
let deleteTargetId = null;

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
  wireFilters();
  wireEditInvestmentModal();
  wireDeleteInvestmentModal();
  await loadInvestments();
}

async function loadInvestments() {
  const tbody = getInvestmentsTbody();
  if (!tbody) return;

  tbody.innerHTML = `<tr><td colspan="10" style="padding:24px;text-align:center;color:var(--gray-500);"><span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Loading investments...</td></tr>`;

  try {
    const response = await InvestmentAPI.getAll();
    console.log('Investments API response:', response);
    allInvestments = Array.isArray(response) ? response : [];
    filteredInvestments = [...allInvestments];

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
  const searchInput = document.getElementById('searchInput');
  const typeFilter = document.getElementById('typeFilter');

  if (searchInput) {
    searchInput.addEventListener('input', applyFilters);
  }
  if (typeFilter) {
    typeFilter.addEventListener('change', applyFilters);
  }
}

function applyFilters() {
  const query = (document.getElementById('searchInput')?.value || '').trim().toLowerCase();
  const type = (document.getElementById('typeFilter')?.value || '').trim().toLowerCase();

  filteredInvestments = allInvestments.filter((item) => {
    const company = String(item.companyName || '').toLowerCase();
    const symbol = String(item.symbol || '').toLowerCase();
    const assetType = String(item.assetType || '').toLowerCase();

    const matchesQuery = !query || company.includes(query) || symbol.includes(query) || assetType.includes(query);
    const matchesType = !type || assetType === type;
    return matchesQuery && matchesType;
  });

  renderInvestmentsTable(filteredInvestments);
  updateTableFooter(filteredInvestments.length, allInvestments.length);
}

function renderInvestmentsTable(list) {
  const tbody = getInvestmentsTbody();
  if (!tbody) return;

  if (!list.length) {
    tbody.innerHTML = '<tr><td colspan="10" style="padding:24px;text-align:center;color:var(--gray-500);">No investments found.</td></tr>';
    return;
  }

  tbody.innerHTML = list.map((investment) => {
    const id = toNumber(investment.investmentId);
    const profitLoss = toNumber(investment.profitLoss);
    const plColor = profitLoss >= 0 ? 'var(--success)' : 'var(--danger)';
    const plText = `${profitLoss > 0 ? '+' : ''}${formatINR(profitLoss)}`;

    return `
      <tr>
        <td style="color:var(--gray-500);">${escapeHtml(String(id || '0'))}</td>
        <td style="font-weight:600;">${escapeHtml(investment.companyName || 'N/A')}</td>
        <td>${escapeHtml(investment.symbol || 'N/A')}</td>
        <td>${assetTypeBadge(investment.assetType || investment.customAssetType || 'N/A')}</td>
        <td>${formatNumber(investment.quantity)}</td>
        <td>${formatINR(investment.purchasePrice)}</td>
        <td style="font-weight:700;">${formatINR(investment.currentValue)}</td>
        <td style="color:${plColor};font-weight:600;">${plText}</td>
        <td style="color:var(--gray-500);font-size:.78rem;">${formatDate(investment.purchaseDate)}</td>
        <td style="text-align:center;">
          <div style="display:flex;gap:4px;justify-content:center;">
            <button class="pm-icon-btn" title="Edit" onclick="openInvestmentEdit(${id})"><i class="bi bi-pencil-fill"></i></button>
            <button class="pm-icon-btn pm-icon-btn--del" title="Delete" onclick="openInvestmentDelete(${id}, '${escapeHtml(investment.companyName || investment.symbol || 'Investment')}')"><i class="bi bi-trash3-fill"></i></button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function renderSummaryCards(list) {
  const totalHoldings = list.length;
  const totalInvested = sumBy(list, 'investedAmount');
  const totalCurrentValue = sumBy(list, 'currentValue');
  const totalPL = sumBy(list, 'profitLoss');

  setText('investHoldingsValue', String(totalHoldings));
  setText('investInvestedValue', formatINR(totalInvested));
  setText('investCurrentValue', formatINR(totalCurrentValue));

  const plEl = document.getElementById('investPlValue');
  if (plEl) {
    plEl.textContent = `${totalPL > 0 ? '+' : ''}${formatINR(totalPL)}`;
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

  const typeSelect = document.getElementById('editAssetType');
  if (typeSelect) {
    typeSelect.addEventListener('change', () => {
      toggleCustomAssetType('editAssetType', 'editCustomAssetTypeWrap', 'editCustomAssetType');
    });
  }

  editForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideModalAlert('editModalAlert');

    const id = toNumber(document.getElementById('editInvestmentId')?.value);
    const payload = {
      quantity: toNumber(document.getElementById('editQuantity')?.value),
      purchasePrice: toNumber(document.getElementById('editPurchasePrice')?.value),
      purchaseDate: String(document.getElementById('editPurchaseDate')?.value || '').trim(),
      assetType: String(document.getElementById('editAssetType')?.value || '').trim(),
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

async function initAddInvestmentPage() {
  await loadPortfolioOptions('portfolioId');
  wireAddInvestmentPageForm();
  wireAddInvestmentCustomType();
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
      companyName: String(document.getElementById('companyName')?.value || '').trim(),
      assetType: String(document.getElementById('assetType')?.value || '').trim(),
      customAssetType: String(document.getElementById('customAssetType')?.value || '').trim() || null,
      quantity: toNumber(document.getElementById('quantity')?.value),
      purchasePrice: toNumber(document.getElementById('purchasePrice')?.value),
      purchaseDate: String(document.getElementById('purchaseDate')?.value || '').trim(),
    };

    if (!isValidCreatePayload(payload)) {
      showFormAlert('Please fill all required fields (including Symbol).', 'danger');
      return;
    }

    setButtonLoading(submitBtn, true, 'Adding...');
    try {
      const created = await InvestmentAPI.create(payload);
      console.log('Add page create investment API response:', created);
      showFormAlert('Investment added successfully.', 'success');
      form.reset();
      toggleCustomAssetType('assetType', 'customTypeWrap', 'customAssetType');
      setTimeout(() => {
        window.location.href = 'investments.html';
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
  if (!select) return;
  select.addEventListener('change', () => {
    toggleCustomAssetType('assetType', 'customTypeWrap', 'customAssetType');
  });
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
    document.getElementById('editSymbol').value = investment.symbol || '';
    document.getElementById('editAssetType').value = investment.assetType || '';
    document.getElementById('editCustomAssetType').value = investment.customAssetType || '';
    document.getElementById('editQuantity').value = investment.quantity || '';
    document.getElementById('editPurchasePrice').value = investment.purchasePrice || '';
    document.getElementById('editPurchaseDate').value = formatInputDate(investment.purchaseDate);
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

function isValidCreatePayload(payload) {
  return payload.portfolioId > 0
    && !!payload.symbol
    && !!payload.companyName
    && !!payload.assetType
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
  return toNumber(value) === 0 ? '₹0' : formatINR(value);
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

function sumBy(list, key) {
  return (list || []).reduce((sum, item) => sum + toNumber(item && item[key]), 0);
}

function toNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function formatINR(value) {
  const number = toNumber(value);
  return number === 0
    ? '₹0'
    : `₹${number.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
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

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
