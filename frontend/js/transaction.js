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
  wireImportExport();
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
  const fromDate = document.getElementById('transactionFromDate');
  const toDate = document.getElementById('transactionToDate');
  const clearButton = document.getElementById('clearTransactionFiltersBtn');

  if (search) search.addEventListener('input', applyFilters);
  if (type) type.addEventListener('change', applyFilters);
  if (fromDate) fromDate.addEventListener('change', applyFilters);
  if (toDate) toDate.addEventListener('change', applyFilters);
  if (clearButton) {
    clearButton.addEventListener('click', () => {
      clearFilters();
    });
  }
}

function applyFilters() {
  const q = (document.getElementById('transactionSearch')?.value || '').trim().toLowerCase();
  const t = (document.getElementById('transactionTypeFilter')?.value || '').trim().toLowerCase();
  const fromDate = (document.getElementById('transactionFromDate')?.value || '').trim();
  const toDate = (document.getElementById('transactionToDate')?.value || '').trim();

  const fromTime = fromDate ? new Date(fromDate).getTime() : null;
  const toTime = toDate ? new Date(toDate).getTime() : null;

  filteredTransactions = allTransactions.filter((row) => {
    const symbol = String(row.symbol || '').toLowerCase();
    const company = String(row.companyName || '').toLowerCase();
    const type = String(row.transactionType || '').toLowerCase();
    const transactionTime = row.transactionDate ? new Date(row.transactionDate).getTime() : null;

    const matchQuery = !q || symbol.includes(q) || company.includes(q);
    const matchType = !t || type === t;
    const matchFrom = fromTime == null || (transactionTime != null && transactionTime >= fromTime);
    const matchTo = toTime == null || (transactionTime != null && transactionTime <= toTime);
    return matchQuery && matchType && matchFrom && matchTo;
  });

  renderTable(filteredTransactions);
  setText('transactionTableInfo', `${filteredTransactions.length} records`);
  setText('transactionFooterText', `Showing ${filteredTransactions.length} of ${allTransactions.length} records`);
}

function clearFilters() {
  const search = document.getElementById('transactionSearch');
  const type = document.getElementById('transactionTypeFilter');
  const fromDate = document.getElementById('transactionFromDate');
  const toDate = document.getElementById('transactionToDate');

  if (search) search.value = '';
  if (type) type.value = '';
  if (fromDate) fromDate.value = '';
  if (toDate) toDate.value = '';

  filteredTransactions = [...allTransactions];
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

function wireImportExport() {
  const exportButton = document.getElementById('exportTransactionsBtn');
  const templateButton = document.getElementById('downloadTransactionTemplateBtn');
  const importForm = document.getElementById('transactionImportForm');
  const importModal = document.getElementById('transactionImportModal');

  if (exportButton) {
    exportButton.addEventListener('click', async () => {
      setButtonLoading(exportButton, true, 'Exporting...');
      try {
        const blob = await TransactionAPI.exportCsv();
        triggerBrowserDownload(blob, `transactions-customer-${getCustomerId() || 'data'}.csv`);
        showPageAlert('Transaction CSV export generated successfully.', 'success');
      } catch (error) {
        console.error('[Transactions] Export failed:', error);
        showPageAlert(error.message || 'Unable to export transactions.', 'danger');
      } finally {
        setButtonLoading(exportButton, false, '<i class="bi bi-download"></i> Export');
      }
    });
  }

  if (templateButton) {
    templateButton.addEventListener('click', async () => {
      setButtonLoading(templateButton, true, 'Downloading...');
      try {
        const blob = await TransactionAPI.downloadImportTemplate();
        triggerBrowserDownload(blob, 'transaction-import-template.csv');
        showImportAlert('transactionImportAlert', 'Template downloaded successfully.', 'success');
      } catch (error) {
        console.error('[Transactions] Template download failed:', error);
        showImportAlert('transactionImportAlert', error.message || 'Unable to download the import template.', 'danger');
      } finally {
        setButtonLoading(templateButton, false, '<i class="bi bi-file-earmark-arrow-down"></i> Download Import Template');
      }
    });
  }

  if (importModal) {
    importModal.addEventListener('show.bs.modal', resetTransactionImportState);
  }

  if (importForm) {
    importForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      hideImportAlert('transactionImportAlert');

      const fileInput = document.getElementById('transactionImportFile');
      const file = fileInput?.files?.[0] || null;
      if (!file) {
        showImportAlert('transactionImportAlert', 'Please choose a CSV or Excel file to import.', 'danger');
        return;
      }

      const submitButton = document.getElementById('transactionImportSubmitBtn');
      setButtonLoading(submitButton, true, 'Importing...');

      try {
        const summary = await TransactionAPI.importFile(file);
        renderTransactionImportSummary(summary);
        const alertType = (summary.failedCount || 0) > 0 ? 'warning' : 'success';
        showImportAlert('transactionImportAlert', buildImportSummaryMessage(summary, 'transaction', 'transactions'), alertType);
        showPageAlert(buildImportSummaryMessage(summary, 'transaction', 'transactions'), alertType === 'warning' ? 'warning' : 'success');
        await loadTransactions();
      } catch (error) {
        console.error('[Transactions] Import failed:', error);
        showImportAlert('transactionImportAlert', error.message || 'Unable to import transactions.', 'danger');
      } finally {
        setButtonLoading(submitButton, false, '<i class="bi bi-upload"></i> Import');
      }
    });
  }
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

function resetTransactionImportState() {
  hideImportAlert('transactionImportAlert');
  const summary = document.getElementById('transactionImportSummary');
  if (summary) {
    summary.classList.add('d-none');
    summary.innerHTML = '';
  }

  const fileInput = document.getElementById('transactionImportFile');
  if (fileInput) {
    fileInput.value = '';
  }
}

function renderTransactionImportSummary(summary) {
  const container = document.getElementById('transactionImportSummary');
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
