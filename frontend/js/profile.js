/**
 * profile.js — Profile page behavior
 * Handles profile load, edit/save, password update, summary, export, and account deletion.
 */

let profileState = {
  customerId: null,
  customerName: '',
  username: '',
  email: '',
  phoneNumber: '',
};

let isEditMode = false;

document.addEventListener('DOMContentLoaded', () => {
  initializeProfilePage().catch((error) => {
    console.error('[Profile] Initialization failed:', error);
    showPageAlert(error.message || 'Unable to initialize profile page.', 'danger');
  });
});

async function initializeProfilePage() {
  if (typeof requireAuth === 'function') {
    requireAuth();
  }

  if (typeof initNavbarUser === 'function') {
    initNavbarUser();
  }

  wireProfileActions();
  setProfileReadOnly(true);
  clearProfilePlaceholders();
  setSummaryValues({ portfolios: 0, investments: 0, transactions: 0 });

  const [profile, summary] = await Promise.all([
    CustomerAPI.getProfile(),
    CustomerAPI.getSummary(),
  ]);

  hydrateProfileState(profile || {});
  applyProfileData(profileState);
  setSummaryValues(summary || {});
}

function wireProfileActions() {
  const editButton = document.getElementById('editProfileBtn');
  const cancelButton = document.getElementById('cancelEdit');
  const profileForm = document.getElementById('profileForm');
  const passwordForm = document.getElementById('pwdForm');
  const exportButton = document.getElementById('exportDataBtn');
  const deleteButton = document.getElementById('deleteAccountBtn');

  if (editButton) {
    editButton.addEventListener('click', () => enterEditMode());
  }

  if (cancelButton) {
    cancelButton.addEventListener('click', () => cancelEditMode());
  }

  if (profileForm) {
    profileForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      await saveProfileChanges();
    });
  }

  if (passwordForm) {
    passwordForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      await updatePassword();
    });
  }

  if (exportButton) {
    exportButton.addEventListener('click', async () => {
      await exportAllDataCsv();
    });
  }

  if (deleteButton) {
    deleteButton.addEventListener('click', async () => {
      await deleteAccount();
    });
  }
}

function hydrateProfileState(profile) {
  profileState = {
    customerId: profile.customerId || null,
    customerName: String(profile.customerName || '').trim(),
    username: String(profile.username || '').trim(),
    email: String(profile.email || '').trim(),
    phoneNumber: String(profile.phoneNumber || '').trim(),
  };
}

function applyProfileData(profile) {
  const customerName = String(profile.customerName || profile.username || '').trim();
  const email = String(profile.email || '').trim();
  const phoneNumber = String(profile.phoneNumber || '').trim();
  const nameParts = splitName(customerName);
  const initials = getInitials(customerName || profile.username || 'U');

  setInputValue('pFirstName', nameParts.firstName);
  setInputValue('pLastName', nameParts.lastName);
  setInputValue('pEmail', email);
  setInputValue('pPhone', phoneNumber);

  setText('profileDisplayName', customerName || '--');
  setText('profileDisplayEmail', email || '--');
  setText('profileAvatarInitials', initials);

  document.querySelectorAll('[data-user-name]').forEach((element) => {
    element.textContent = customerName || '--';
  });

  document.querySelectorAll('[data-user-email]').forEach((element) => {
    element.textContent = email || '--';
  });

  document.querySelectorAll('[data-initials]').forEach((element) => {
    element.textContent = initials;
  });
}

function enterEditMode() {
  isEditMode = true;
  setProfileReadOnly(false);
  toggleSaveRow(true);
}

function cancelEditMode() {
  isEditMode = false;
  applyProfileData(profileState);
  setProfileReadOnly(true);
  toggleSaveRow(false);
}

async function saveProfileChanges() {
  const firstName = String(getInputValue('pFirstName')).trim();
  const lastName = String(getInputValue('pLastName')).trim();
  const emailInput = String(getInputValue('pEmail')).trim();
  const phoneNumber = String(getInputValue('pPhone')).trim();

  if (!firstName) {
    showPageAlert('First name is required.', 'danger');
    return;
  }

  if (!emailInput || !isValidEmail(emailInput)) {
    showPageAlert('Please enter a valid email address.', 'danger');
    return;
  }

  const phoneDigits = (phoneNumber.match(/\d/g) || []).length;
  if (!/^[+()\-\s0-9]+$/.test(phoneNumber) || phoneDigits < 7 || phoneDigits > 15) {
    showPageAlert('Phone number must be 7 to 15 digits and may include +, spaces, hyphens, or parentheses.', 'danger');
    return;
  }

  const customerName = [firstName, lastName].filter(Boolean).join(' ').trim();

  const payload = {
    customerName,
    username: emailInput,
    email: emailInput,
    phoneNumber,
  };

  try {
    setButtonLoading('editProfileBtn', true, '<span class="spinner-border spinner-border-sm me-2"></span>Saving...');
    const updated = await CustomerAPI.update(payload);
    hidePageAlert();

    hydrateProfileState(updated || payload);
    applyProfileData(profileState);

    const customerId = profileState.customerId || (typeof getCustomerId === 'function' ? getCustomerId() : null);
    if (typeof setAuthSession === 'function' && customerId) {
      setAuthSession(customerId, profileState.customerName || profileState.username || 'Customer');
    }

    cancelEditMode();
    showPageAlert('Profile updated successfully.', 'success');
  } catch (error) {
    console.error('[Profile] Update failed:', error);
    showPageAlert(error.message || 'Unable to update profile.', 'danger');
  } finally {
    setButtonLoading('editProfileBtn', false, '<i class="bi bi-pencil-fill"></i> Edit');
  }
}

async function updatePassword() {
  const currentPassword = String(getInputValue('currentPassword'));
  const newPassword = String(getInputValue('newPassword'));
  const confirmPassword = String(getInputValue('confirmNewPassword'));

  if (!currentPassword || !newPassword || !confirmPassword) {
    showInlineAlert('pwdAlert', 'All password fields are required.', 'danger');
    return;
  }

  if (newPassword.length < 8) {
    showInlineAlert('pwdAlert', 'New password must be at least 8 characters long.', 'danger');
    return;
  }

  if (newPassword !== confirmPassword) {
    showInlineAlert('pwdAlert', 'New password and confirmation do not match.', 'danger');
    return;
  }

  try {
    setButtonLoading('pwdSubmitBtn', true, '<span class="spinner-border spinner-border-sm me-2"></span>Updating...');
    await CustomerAPI.changePassword({ currentPassword, newPassword });
    showInlineAlert('pwdAlert', 'Password updated successfully.', 'success');

    const pwdForm = document.getElementById('pwdForm');
    if (pwdForm) {
      pwdForm.reset();
    }
  } catch (error) {
    console.error('[Profile] Password update failed:', error);
    showInlineAlert('pwdAlert', error.message || 'Unable to update password.', 'danger');
  } finally {
    setButtonLoading('pwdSubmitBtn', false, '<i class="bi bi-shield-lock-fill"></i> Update Password');
  }
}

async function exportAllDataCsv() {
  try {
    setButtonLoading('exportDataBtn', true, '<span class="spinner-border spinner-border-sm me-2"></span>Exporting...');
    const blob = await CustomerAPI.exportCsv();
    const customerId = typeof getCustomerId === 'function' ? getCustomerId() : 'account';
    const fileName = `portfolio-data-customer-${customerId}.csv`;
    triggerBrowserDownload(blob, fileName);
    showPageAlert('CSV export generated successfully.', 'success');
  } catch (error) {
    console.error('[Profile] CSV export failed:', error);
    showPageAlert(error.message || 'Unable to export data.', 'danger');
  } finally {
    setButtonLoading('exportDataBtn', false, '<i class="bi bi-download"></i> Export CSV');
  }
}

async function deleteAccount() {
  const confirmed = window.confirm('Are you sure you want to permanently delete your account? This action cannot be undone.');
  if (!confirmed) {
    return;
  }

  try {
    setButtonLoading('deleteAccountBtn', true, '<span class="spinner-border spinner-border-sm me-2"></span>Deleting...');
    await CustomerAPI.deleteAccount();
    if (typeof logout === 'function') {
      logout();
      return;
    }
    window.location.href = 'login.html';
  } catch (error) {
    console.error('[Profile] Delete account failed:', error);
    showPageAlert(error.message || 'Unable to delete account.', 'danger');
  } finally {
    setButtonLoading('deleteAccountBtn', false, '<i class="bi bi-trash3-fill"></i> Delete');
  }
}

function setSummaryValues(summary) {
  setText('summaryPortfolios', String(toNumber(summary.portfolios)));
  setText('summaryInvestments', String(toNumber(summary.investments)));
  setText('summaryTransactions', String(toNumber(summary.transactions)));
}

function clearProfilePlaceholders() {
  setInputValue('pFirstName', '');
  setInputValue('pLastName', '');
  setInputValue('pEmail', '');
  setInputValue('pPhone', '');

  setText('profileDisplayName', '--');
  setText('profileDisplayEmail', '--');
  setText('profileAvatarInitials', '--');
}

function showPageAlert(message, type) {
  const safeMessage = String(message || 'Something went wrong.');
  const main = document.querySelector('main.pm-main');
  if (!main) return;

  const existing = document.getElementById('profileApiAlert');
  if (existing) {
    existing.remove();
  }

  const alert = document.createElement('div');
  alert.id = 'profileApiAlert';
  alert.className = `alert alert-${type || 'danger'}`;
  alert.setAttribute('role', 'alert');
  alert.style.marginBottom = '16px';
  alert.textContent = safeMessage;

  const firstElement = main.firstElementChild;
  if (firstElement) {
    main.insertBefore(alert, firstElement.nextSibling);
  } else {
    main.appendChild(alert);
  }
}

function hidePageAlert() {
  const existing = document.getElementById('profileApiAlert');
  if (existing) {
    existing.remove();
  }
}

function showInlineAlert(elementId, message, type) {
  const alert = document.getElementById(elementId);
  if (!alert) return;

  alert.className = `alert alert-${type || 'danger'}`;
  alert.textContent = String(message || 'Something went wrong.');
  alert.classList.remove('d-none');
}

function setProfileReadOnly(readOnly) {
  ['pFirstName', 'pLastName', 'pEmail', 'pPhone'].forEach((id) => {
    const input = document.getElementById(id);
    if (input) {
      input.readOnly = !!readOnly;
    }
  });
}

function toggleSaveRow(show) {
  const saveRow = document.getElementById('saveRow');
  if (!saveRow) return;
  saveRow.classList.toggle('d-none', !show);
}

function setButtonLoading(buttonId, loading, html) {
  const button = document.getElementById(buttonId);
  if (!button) return;

  if (loading) {
    button.disabled = true;
    button.dataset.originalHtml = button.innerHTML;
    button.innerHTML = html;
    return;
  }

  button.disabled = false;
  if (button.dataset.originalHtml) {
    button.innerHTML = button.dataset.originalHtml;
    delete button.dataset.originalHtml;
  } else {
    button.innerHTML = html;
  }
}

function triggerBrowserDownload(blob, fileName) {
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = objectUrl;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(objectUrl);
}

function splitName(fullName) {
  const tokens = String(fullName || '').trim().split(/\s+/).filter(Boolean);
  if (tokens.length === 0) {
    return { firstName: '', lastName: '' };
  }
  if (tokens.length === 1) {
    return { firstName: tokens[0], lastName: '' };
  }

  return {
    firstName: tokens[0],
    lastName: tokens.slice(1).join(' '),
  };
}

function getInitials(value) {
  const text = String(value || '').trim();
  if (!text) return 'U';

  const parts = text.split(/\s+/);
  if (parts.length > 1) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  return text.substring(0, 2).toUpperCase();
}

function setInputValue(id, value) {
  const input = document.getElementById(id);
  if (input) {
    input.value = value || '';
  }
}

function getInputValue(id) {
  const input = document.getElementById(id);
  return input ? input.value : '';
}

function setText(id, value) {
  const element = document.getElementById(id);
  if (element) {
    element.textContent = value;
  }
}

function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || '').trim());
}

function toNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}
