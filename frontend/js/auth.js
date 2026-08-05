/**
 * auth.js — Authentication and route protection
 */

const CUSTOMER_ID_KEY = 'customerId';
const CUSTOMER_NAME_KEY = 'customerName';
const THEME_KEY = 'pmTheme';

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

function getStoredTheme() {
  const saved = localStorage.getItem(THEME_KEY);
  return saved === 'dark' ? 'dark' : 'light';
}

function applyTheme(theme) {
  const resolvedTheme = theme === 'dark' ? 'dark' : 'light';
  document.documentElement.setAttribute('data-theme', resolvedTheme);
  localStorage.setItem(THEME_KEY, resolvedTheme);

  const toggle = document.getElementById('themeToggleBtn');
  if (toggle) {
    const isDark = resolvedTheme === 'dark';
    toggle.setAttribute('aria-pressed', isDark ? 'true' : 'false');
    toggle.setAttribute('title', isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode');
    toggle.innerHTML = isDark
      ? '<i class="bi bi-sun-fill"></i> Light Mode'
      : '<i class="bi bi-moon-stars-fill"></i> Dark Mode';
  }

  document.dispatchEvent(new CustomEvent('pm:theme-change', {
    detail: { theme: resolvedTheme },
  }));
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'light';
  applyTheme(current === 'dark' ? 'light' : 'dark');
}

function initThemeToggle() {
  const actions = document.querySelector('.pm-navbar__actions');
  if (!actions || document.getElementById('themeToggleBtn')) return;

  const toggle = document.createElement('button');
  toggle.id = 'themeToggleBtn';
  toggle.type = 'button';
  toggle.className = 'pm-btn pm-btn--outline pm-btn--sm pm-theme-toggle';
  toggle.addEventListener('click', toggleTheme);

  actions.insertBefore(toggle, actions.firstChild);
  applyTheme(getStoredTheme());
}

function initializeTheme() {
  applyTheme(getStoredTheme());
  initThemeToggle();
}

function enforcePageProtection() {
  const currentPage = window.location.pathname.split('/').pop().toLowerCase();
  if (PROTECTED_PAGES.includes(currentPage) && !isAuthenticated()) {
    window.location.href = 'login.html';
  }
}

function initNavbarUser() {
  const customerName = getCustomerName();
  initThemeToggle();
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

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeTheme);
} else {
  initializeTheme();
}

window.setAuthSession = setAuthSession;
window.getCustomerId = getCustomerId;
window.getCustomerName = getCustomerName;
window.isAuthenticated = isAuthenticated;
window.requireAuth     = requireAuth;
window.redirectIfAuthenticated = redirectIfAuthenticated;
window.logout          = logout;
window.initNavbarUser  = initNavbarUser;
window.applyTheme = applyTheme;
window.toggleTheme = toggleTheme;
