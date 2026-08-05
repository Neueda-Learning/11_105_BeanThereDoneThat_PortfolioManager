/**
 * transaction.js — Transactions API integration
 */

let allTransactions = [];
let filteredTransactions = [];
let deleteTransactionId = null;

document.addEventListener('DOMContentLoaded', () => {
  if (typeof requireAuth === 'function') {
    requireAuth();
  }
  if (typeof initNavbarUser === 'function') {
    initNavbarUser();
  }

  wireFilters();
  wireCreateTransactionForm();
  wireDeleteTransactionConfirm();
  loadTransactions();
});

async function loadTransactions() {
  const tbody = document.querySelector('#transactionTable tbody');
  if (!tbody) return;

  tbody.innerHTML = '<tr><td colspan="10" style="padding:24px;text-align:center;color:var(--gray-500);"><span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Loading transactions...</td></tr>';

  try {
    const response = await TransactionAPI.getAll();
    console.log('Transactions API response:', response);
    allTransactions = Array.isArray(response) ? response : [];
    filteredTransactions = [...allTransactions];

    renderTable(filteredTransactions);
    updateSummary(allTransactions);
    setText('transactionTableInfo', `${filteredTransactions.length} records`);
    setText('transactionFooterText', `Showing ${filteredTransactions.length} of ${allTransactions.length} records`);
    hideInlineAlert();
  } catch (error) {
    console.error('[Transactions] Failed to load:', error);
    renderTable([]);
    updateSummary([]);
    setText('transactionTableInfo', '0 records');
    setText('transactionFooterText', 'Showing 0 of 0 records');
    showInlineAlert(error.message || 'Unable to load transactions.', 'danger');
  }
}

function renderTable(rows) {
  const tbody = document.querySelector('#transactionTable tbody');
  if (!tbody) return;

  if (!rows.length) {
    tbody.innerHTML = '<tr><td colspan="10" style="padding:24px;text-align:center;color:var(--gray-500);">No transactions found.</td></tr>';
    return;
  }

  tbody.innerHTML = rows.map((tx) => {
    const txId = toNumber(tx.transactionId);
    const type = String(tx.transactionType || 'N/A').toUpperCase();
    const isBuy = type === 'BUY';
    const badgeClass = isBuy ? 'pm-badge--green' : 'pm-badge--red';
    const icon = isBuy ? 'arrow-down-circle-fill' : 'arrow-up-circle-fill';

    return `
      <tr>
        <td style="color:var(--gray-400);">${escapeHtml(String(txId || 0))}</td>
        <td>${escapeHtml(tx.companyName || 'N/A')}</td>
        <td>${escapeHtml(tx.symbol || 'N/A')}</td>
        <td>${escapeHtml(tx.assetType || 'N/A')}</td>
        <td><span class="pm-badge ${badgeClass}"><i class="bi bi-${icon}"></i> ${escapeHtml(type)}</span></td>
        <td>${formatNumber(tx.quantity)}</td>
        <td>${formatINR(tx.transactionPrice)}</td>
        <td style="font-weight:700;">${formatINR(tx.transactionAmount)}</td>
        <td style="color:var(--gray-500);font-size:.78rem;">${formatDate(tx.transactionDate)}</td>
        <td style="text-align:center;">
          <button class="pm-icon-btn pm-icon-btn--del" title="Delete" onclick="openTransactionDelete(${txId}, '${escapeHtml(tx.symbol || tx.companyName || 'transaction')}')"><i class="bi bi-trash3-fill"></i></button>
        </td>
      </tr>
    `;
  }).join('');
}

function updateSummary(rows) {
  const total = rows.length;
  const buys = rows.filter((r) => String(r.transactionType || '').toUpperCase() === 'BUY').length;
  const sells = rows.filter((r) => String(r.transactionType || '').toUpperCase() === 'SELL').length;
  const volume = sumBy(rows, 'transactionAmount');

  setText('summaryTotalTxns', String(total));
  setText('summaryBuyOrders', String(buys));
  setText('summarySellOrders', String(sells));
  setText('summaryTotalVolume', formatINR(volume));
}

function wireFilters() {
  const search = document.getElementById('transactionSearch');
  const type = document.getElementById('transactionTypeFilter');

  if (search) search.addEventListener('input', applyFilters);
  if (type) type.addEventListener('change', applyFilters);
}

function applyFilters() {
  const q = (document.getElementById('transactionSearch')?.value || '').trim().toLowerCase();
  const t = (document.getElementById('transactionTypeFilter')?.value || '').trim().toLowerCase();

  filteredTransactions = allTransactions.filter((row) => {
    const symbol = String(row.symbol || '').toLowerCase();
    const company = String(row.companyName || '').toLowerCase();
    const type = String(row.transactionType || '').toLowerCase();

    const matchQuery = !q || symbol.includes(q) || company.includes(q);
    const matchType = !t || type === t;
    return matchQuery && matchType;
  });

  renderTable(filteredTransactions);
  setText('transactionTableInfo', `${filteredTransactions.length} records`);
  setText('transactionFooterText', `Showing ${filteredTransactions.length} of ${allTransactions.length} records`);
}

function wireCreateTransactionForm() {
  const form = document.getElementById('createTransactionForm');
  const modal = document.getElementById('createTransactionModal');
  if (!form || !modal) return;

  modal.addEventListener('shown.bs.modal', async () => {
    await loadInvestmentOptions();
  });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideModalAlert('createTransactionAlert');

    const payload = {
      investmentId: toNumber(document.getElementById('createInvestmentId')?.value),
      transactionType: String(document.getElementById('createTransactionType')?.value || '').trim().toUpperCase(),
      quantity: toNumber(document.getElementById('createQuantity')?.value),
      transactionPrice: toNumber(document.getElementById('createTransactionPrice')?.value),
      transactionDate: String(document.getElementById('createTransactionDate')?.value || '').trim(),
    };

    if (!payload.investmentId || !payload.transactionType || payload.quantity <= 0 || payload.transactionPrice <= 0 || !payload.transactionDate) {
      showModalAlert('createTransactionAlert', 'Please fill all required fields.', 'danger');
      return;
    }

    const submitBtn = document.getElementById('createTransactionSubmitBtn');
    setButtonLoading(submitBtn, true, 'Creating...');

    try {
      const created = await TransactionAPI.create(payload);
      console.log('Create transaction API response:', created);
      showPageAlert('Transaction created successfully.', 'success');
      closeModal('createTransactionModal');
      form.reset();
      await loadTransactions();
    } catch (error) {
      console.error('[Transactions] Create failed:', error);
      showModalAlert('createTransactionAlert', error.message || 'Failed to create transaction.', 'danger');
    } finally {
      setButtonLoading(submitBtn, false, '<i class="bi bi-check-lg"></i> Create');
    }
  });
}

async function loadInvestmentOptions() {
  const select = document.getElementById('createInvestmentId');
  if (!select) return;

  try {
    const response = await InvestmentAPI.getAll();
    console.log('Investments API response for transaction form:', response);
    const list = Array.isArray(response) ? response : [];

    if (!list.length) {
      select.innerHTML = '<option value="">No investments available</option>';
      return;
    }

    select.innerHTML = '<option value="">Select investment...</option>' + list.map((investment) => (
      `<option value="${escapeHtml(String(investment.investmentId || ''))}">${escapeHtml(investment.symbol || investment.companyName || `Investment ${investment.investmentId}`)}</option>`
    )).join('');
  } catch (error) {
    console.error('[Transactions] Failed to load investments for form:', error);
    select.innerHTML = '<option value="">Unable to load investments</option>';
  }
}

function openTransactionDelete(id, label) {
  deleteTransactionId = id;
  const text = document.getElementById('deleteTransactionName');
  if (text) text.textContent = label || 'this transaction';
  openModal('deleteTransactionModal');
}

window.openTransactionDelete = openTransactionDelete;

function wireDeleteTransactionConfirm() {
  const button = document.getElementById('confirmDeleteTransactionBtn');
  if (!button) return;

  button.addEventListener('click', async () => {
    if (!deleteTransactionId) return;

    setButtonLoading(button, true, 'Deleting...');
    try {
      const deleted = await TransactionAPI.delete(deleteTransactionId);
      console.log('Delete transaction API response:', deleted);
      showPageAlert('Transaction deleted successfully.', 'success');
      closeModal('deleteTransactionModal');
      deleteTransactionId = null;
      await loadTransactions();
    } catch (error) {
      console.error('[Transactions] Delete failed:', error);
      showPageAlert(error.message || 'Failed to delete transaction.', 'danger');
    } finally {
      setButtonLoading(button, false, 'Delete');
    }
  });
}

function showPageAlert(message, type) {
  const main = document.querySelector('main.pm-main');
  if (!main) return;

  const old = document.getElementById('transactionPageAlert');
  if (old) old.remove();

  const alert = document.createElement('div');
  alert.id = 'transactionPageAlert';
  alert.className = `alert alert-${type} alert-dismissible fade show`;
  alert.setAttribute('role', 'alert');
  alert.innerHTML = `${escapeHtml(message)}<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>`;
  main.prepend(alert);
}

function showInlineAlert(message, type) {
  const alert = document.getElementById('transactionInlineAlert');
  if (!alert) return;
  alert.className = `alert alert-${type} m-3`;
  alert.textContent = message;
  alert.classList.remove('d-none');
}

function hideInlineAlert() {
  const alert = document.getElementById('transactionInlineAlert');
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

function openModal(id) {
  const el = document.getElementById(id);
  if (!el || typeof bootstrap === 'undefined') return;
  bootstrap.Modal.getOrCreateInstance(el).show();
}

function closeModal(id) {
  const el = document.getElementById(id);
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

function sumBy(list, key) {
  return (list || []).reduce((sum, item) => sum + toNumber(item && item[key]), 0);
}

function toNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function formatINR(value) {
  const n = toNumber(value);
  return n === 0
    ? '₹0'
    : `₹${n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
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

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
