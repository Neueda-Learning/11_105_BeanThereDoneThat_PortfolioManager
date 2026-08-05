/**
 * auth.js — Authentication and route protection
 */

const CUSTOMER_ID_KEY = 'customerId';
const CUSTOMER_NAME_KEY = 'customerName';

const PROTECTED_PAGES = [
  'dashboard.html',
  'portfolios.html',
  'investments.html',
  'transactions.html',
  'analytics.html',
  'profile.html',
];

function setAuthSession(customerId, customerName) {
  localStorage.setItem(CUSTOMER_ID_KEY, String(customerId));
  localStorage.setItem(CUSTOMER_NAME_KEY, customerName || 'Customer');
}

function getCustomerId() {
  return localStorage.getItem(CUSTOMER_ID_KEY);
}

function getCustomerName() {
  return localStorage.getItem(CUSTOMER_NAME_KEY) || '';
}

function isAuthenticated() {
  return !!getCustomerId();
}

function requireAuth() {
  if (!isAuthenticated()) {
    window.location.href = 'login.html';
  }
}

function redirectIfAuthenticated() {
  if (isAuthenticated()) {
    window.location.href = 'dashboard.html';
  }
}

function logout() {
  localStorage.clear();
  window.location.href = 'login.html';
}

function enforcePageProtection() {
  const currentPage = window.location.pathname.split('/').pop().toLowerCase();
  if (PROTECTED_PAGES.includes(currentPage) && !isAuthenticated()) {
    window.location.href = 'login.html';
  }
}

function initNavbarUser() {
  const customerName = getCustomerName();
  if (!customerName) return;

  const initials = _initials(customerName);
  document.querySelectorAll('[data-initials]').forEach(el => el.textContent = initials);
  document.querySelectorAll('[data-user-name]').forEach(el => el.textContent = customerName);
  document.querySelectorAll('[data-user-email]').forEach(el => el.textContent = '');
  document.querySelectorAll('[data-logout], [data-logout-btn]').forEach(el => el.addEventListener('click', logout));
}

function _initials(s) {
  if (!s) return 'U';
  const p = s.trim().split(/\s+/);
  return p.length > 1 ? (p[0][0] + p[1][0]).toUpperCase() : s.substring(0,2).toUpperCase();
}

enforcePageProtection();

window.setAuthSession = setAuthSession;
window.getCustomerId = getCustomerId;
window.getCustomerName = getCustomerName;
window.isAuthenticated = isAuthenticated;
window.requireAuth     = requireAuth;
window.redirectIfAuthenticated = redirectIfAuthenticated;
window.logout          = logout;
window.initNavbarUser  = initNavbarUser;
