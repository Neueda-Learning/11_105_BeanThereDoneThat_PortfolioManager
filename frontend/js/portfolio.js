/**
 * portfolio.js — Portfolios page API integration
 */

let portfolioDeleteTargetId = null;

document.addEventListener('DOMContentLoaded', () => {
  if (typeof requireAuth === 'function') {
    requireAuth();
  }
  if (typeof initNavbarUser === 'function') {
    initNavbarUser();
  }

  wireCreatePortfolioForm();
  wireEditPortfolioForm();
  wireDeletePortfolioConfirm();
  loadPortfolios();
});

async function loadPortfolios() {
  const tbody = document.querySelector('#portfolioTable tbody');
  if (!tbody) return;

  tbody.innerHTML = '<tr><td colspan="4" style="padding:24px;text-align:center;color:var(--gray-500);"><span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Loading portfolios...</td></tr>';

  try {
    const response = await PortfolioAPI.getAll();
    console.log('Portfolios API response:', response);
    const portfolios = Array.isArray(response) ? response : [];
    renderPortfolioTable(portfolios);
    setText('portfolioCount', String(portfolios.length));
    setText('portfolioFooterText', `Showing ${portfolios.length} records`);
    hideInlineAlert();
  } catch (error) {
    console.error('[Portfolios] Failed to load portfolios:', error);
    renderPortfolioTable([]);
    setText('portfolioCount', '0');
    setText('portfolioFooterText', 'Showing 0 records');
    showInlineAlert(error.message || 'Unable to load portfolios.', 'danger');
  }
}

function renderPortfolioTable(portfolios) {
  const tbody = document.querySelector('#portfolioTable tbody');
  if (!tbody) return;

  if (!portfolios.length) {
    tbody.innerHTML = '<tr><td colspan="4" style="padding:24px;text-align:center;color:var(--gray-500);">No portfolios found.</td></tr>';
    return;
  }

  tbody.innerHTML = portfolios.map((portfolio) => {
    const portfolioId = toNumber(portfolio.portfolioId);
    const customerId = toNumber(portfolio.customerId);

    return `
      <tr>
        <td>${escapeHtml(String(portfolioId || 0))}</td>
        <td>${escapeHtml(String(customerId || 0))}</td>
        <td style="font-weight:600;">${escapeHtml(portfolio.portfolioName || 'N/A')}</td>
        <td style="text-align:center;">
          <div style="display:flex;gap:4px;justify-content:center;">
            <button class="pm-icon-btn" title="Edit" onclick="openPortfolioEdit(${portfolioId})"><i class="bi bi-pencil-fill"></i></button>
            <button class="pm-icon-btn pm-icon-btn--del" title="Delete" onclick="openPortfolioDelete(${portfolioId}, '${escapeHtml(portfolio.portfolioName || 'Portfolio')}')"><i class="bi bi-trash3-fill"></i></button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function wireCreatePortfolioForm() {
  const form = document.getElementById('createPortfolioForm');
  if (!form) return;

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideModalAlert('createModalAlert');

    const payload = {
      customerId: toNumber(document.getElementById('createCustomerId')?.value),
      portfolioName: String(document.getElementById('createPortfolioName')?.value || '').trim(),
    };

    if (!payload.customerId || !payload.portfolioName) {
      showModalAlert('createModalAlert', 'Customer ID and Portfolio Name are required.', 'danger');
      return;
    }

    const submitBtn = document.getElementById('createPortfolioSubmitBtn');
    setButtonLoading(submitBtn, true, 'Creating...');

    try {
      const created = await PortfolioAPI.create(payload);
      console.log('Create portfolio API response:', created);
      showPageAlert('Portfolio created successfully.', 'success');
      closeModal('createModal');
      form.reset();
      await loadPortfolios();
    } catch (error) {
      console.error('[Portfolios] Create failed:', error);
      showModalAlert('createModalAlert', error.message || 'Failed to create portfolio.', 'danger');
    } finally {
      setButtonLoading(submitBtn, false, '<i class="bi bi-check-lg"></i> Create');
    }
  });
}

async function openPortfolioEdit(id) {
  try {
    hideModalAlert('editModalAlert');
    const portfolio = await PortfolioAPI.getById(id);
    console.log('Get portfolio by id API response:', portfolio);

    document.getElementById('editPortfolioId').value = portfolio.portfolioId || '';
    document.getElementById('editCustomerId').value = portfolio.customerId || '';
    document.getElementById('editPortfolioName').value = portfolio.portfolioName || '';
    openModal('editModal');
  } catch (error) {
    console.error('[Portfolios] Failed to load portfolio by id:', error);
    showPageAlert(error.message || 'Failed to fetch portfolio details.', 'danger');
  }
}

window.openPortfolioEdit = openPortfolioEdit;

function wireEditPortfolioForm() {
  const form = document.getElementById('editPortfolioForm');
  if (!form) return;

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideModalAlert('editModalAlert');

    const portfolioId = toNumber(document.getElementById('editPortfolioId')?.value);
    const payload = {
      portfolioName: String(document.getElementById('editPortfolioName')?.value || '').trim(),
    };

    if (!portfolioId || !payload.portfolioName) {
      showModalAlert('editModalAlert', 'Portfolio Name is required.', 'danger');
      return;
    }

    const submitBtn = document.getElementById('editPortfolioSubmitBtn');
    setButtonLoading(submitBtn, true, 'Saving...');

    try {
      const updated = await PortfolioAPI.update(portfolioId, payload);
      console.log('Update portfolio API response:', updated);
      showPageAlert('Portfolio updated successfully.', 'success');
      closeModal('editModal');
      await loadPortfolios();
    } catch (error) {
      console.error('[Portfolios] Update failed:', error);
      showModalAlert('editModalAlert', error.message || 'Failed to update portfolio.', 'danger');
    } finally {
      setButtonLoading(submitBtn, false, '<i class="bi bi-check-lg"></i> Save');
    }
  });
}

function openPortfolioDelete(id, name) {
  portfolioDeleteTargetId = id;
  const label = document.getElementById('deletePortfolioName');
  if (label) {
    label.textContent = name || 'this portfolio';
  }
  openModal('deleteModal');
}

window.openPortfolioDelete = openPortfolioDelete;

function wireDeletePortfolioConfirm() {
  const button = document.getElementById('confirmDeletePortfolioBtn');
  if (!button) return;

  button.addEventListener('click', async () => {
    if (!portfolioDeleteTargetId) return;

    setButtonLoading(button, true, 'Deleting...');

    try {
      const deleted = await PortfolioAPI.delete(portfolioDeleteTargetId);
      console.log('Delete portfolio API response:', deleted);
      showPageAlert('Portfolio deleted successfully.', 'success');
      closeModal('deleteModal');
      portfolioDeleteTargetId = null;
      await loadPortfolios();
    } catch (error) {
      console.error('[Portfolios] Delete failed:', error);
      showPageAlert(error.message || 'Failed to delete portfolio.', 'danger');
    } finally {
      setButtonLoading(button, false, 'Delete');
    }
  });
}

function showPageAlert(message, type) {
  const main = document.querySelector('main.pm-main');
  if (!main) return;

  const oldAlert = document.getElementById('portfolioPageAlert');
  if (oldAlert) oldAlert.remove();

  const alert = document.createElement('div');
  alert.id = 'portfolioPageAlert';
  alert.className = `alert alert-${type} alert-dismissible fade show`;
  alert.setAttribute('role', 'alert');
  alert.innerHTML = `${escapeHtml(message)}<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>`;
  main.prepend(alert);
}

function showInlineAlert(message, type) {
  const alert = document.getElementById('portfolioAlert');
  if (!alert) return;
  alert.className = `alert alert-${type} m-3`;
  alert.textContent = message;
  alert.classList.remove('d-none');
}

function hideInlineAlert() {
  const alert = document.getElementById('portfolioAlert');
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

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

function toNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
