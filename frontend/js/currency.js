/**
 * currency.js — Shared display-only currency conversion utility
 * Reuses backend exchange-rate endpoint through InvestmentAPI.getExchangeRate.
 */

(function bootstrapCurrencyModule(global) {
  const SUPPORTED_CURRENCIES = [
    'INR',
    'USD',
    'EUR',
    'GBP',
    'JPY',
    'AUD',
    'CAD',
    'CHF',
    'SGD',
    'AED',
  ];

  const exchangeRateCache = {};

  function toNumber(value) {
    const n = Number(value);
    return Number.isFinite(n) ? n : 0;
  }

  function normalizeCurrencyCode(currency) {
    const normalized = String(currency || '').trim().toUpperCase();
    if (!normalized) return null;
    return SUPPORTED_CURRENCIES.includes(normalized) ? normalized : normalized;
  }

  function getCurrencyLocale(currency) {
    return normalizeCurrencyCode(currency) === 'INR' ? 'en-IN' : 'en-US';
  }

  function formatMoney(value, currency) {
    const resolvedCurrency = normalizeCurrencyCode(currency) || 'INR';
    return new Intl.NumberFormat(getCurrencyLocale(resolvedCurrency), {
      style: 'currency',
      currency: resolvedCurrency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(toNumber(value));
  }

  function getCacheKey(fromCurrency, toCurrency) {
    return `${fromCurrency}->${toCurrency}`;
  }

  function getCachedRate(fromCurrency, toCurrency) {
    const from = normalizeCurrencyCode(fromCurrency) || 'INR';
    const to = normalizeCurrencyCode(toCurrency) || 'INR';
    if (from === to) return 1;
    return toNumber(exchangeRateCache[getCacheKey(from, to)]);
  }

  async function fetchRate(fromCurrency, toCurrency) {
    const from = normalizeCurrencyCode(fromCurrency) || 'INR';
    const to = normalizeCurrencyCode(toCurrency) || 'INR';

    if (from === to) {
      return 1;
    }

    const cacheKey = getCacheKey(from, to);
    const cached = toNumber(exchangeRateCache[cacheKey]);
    if (cached > 0) {
      return cached;
    }

    if (!global.InvestmentAPI || typeof global.InvestmentAPI.getExchangeRate !== 'function') {
      return 0;
    }

    try {
      const payload = await global.InvestmentAPI.getExchangeRate(from, to);
      const rate = toNumber(payload && payload.rate);
      if (rate > 0) {
        exchangeRateCache[cacheKey] = rate;
        return rate;
      }
    } catch (error) {
      console.warn(`[Currency] Failed to fetch rate ${cacheKey}:`, error);
    }

    return 0;
  }

  async function preloadRates(sourceCurrencies, targetCurrency) {
    const to = normalizeCurrencyCode(targetCurrency) || 'INR';
    const uniqueSources = [...new Set((sourceCurrencies || [])
      .map((code) => normalizeCurrencyCode(code))
      .filter(Boolean))];

    const lookups = uniqueSources
      .filter((from) => from !== to)
      .map((from) => fetchRate(from, to));

    await Promise.all(lookups);
  }

  async function preloadRatesFromItems(items, targetCurrency, currencySelector) {
    const selector = typeof currencySelector === 'function'
      ? currencySelector
      : (item) => item && item.currency;

    const sources = (items || []).map((item) => selector(item));
    await preloadRates(sources, targetCurrency);
  }

  function convertAmount(value, fromCurrency, toCurrency) {
    const amount = toNumber(value);
    const from = normalizeCurrencyCode(fromCurrency) || 'INR';
    const to = normalizeCurrencyCode(toCurrency) || 'INR';

    if (!amount || from === to) {
      return amount;
    }

    const rate = getCachedRate(from, to);
    if (rate <= 0) {
      return amount;
    }

    return amount * rate;
  }

  function populateCurrencySelect(selectOrId, selectedCurrency) {
    const select = typeof selectOrId === 'string'
      ? document.getElementById(selectOrId)
      : selectOrId;

    if (!select) return;

    const selected = normalizeCurrencyCode(selectedCurrency) || 'INR';
    select.innerHTML = SUPPORTED_CURRENCIES.map((currency) => (
      `<option value="${currency}"${currency === selected ? ' selected' : ''}>${currency}</option>`
    )).join('');
  }

  function createDisplayController(initialCurrency) {
    let displayCurrency = normalizeCurrencyCode(initialCurrency) || 'INR';

    return {
      getCurrency() {
        return displayCurrency;
      },
      setCurrency(nextCurrency) {
        displayCurrency = normalizeCurrencyCode(nextCurrency) || 'INR';
        return displayCurrency;
      },
      format(value) {
        return formatMoney(value, displayCurrency);
      },
      convert(value, sourceCurrency) {
        return convertAmount(value, sourceCurrency, displayCurrency);
      },
      async preloadFromItems(items, currencySelector) {
        await preloadRatesFromItems(items, displayCurrency, currencySelector);
      },
      async preloadFromCurrencies(sourceCurrencies) {
        await preloadRates(sourceCurrencies, displayCurrency);
      },
      sumConverted(items, valueSelector, currencySelector) {
        const getValue = typeof valueSelector === 'function'
          ? valueSelector
          : (item) => item && item[valueSelector];
        const getCurrency = typeof currencySelector === 'function'
          ? currencySelector
          : (item) => item && item.currency;

        return (items || []).reduce((sum, item) => {
          const originalAmount = toNumber(getValue(item));
          const sourceCurrency = getCurrency(item);
          return sum + convertAmount(originalAmount, sourceCurrency, displayCurrency);
        }, 0);
      },
      syncSelect(selectOrId) {
        populateCurrencySelect(selectOrId, displayCurrency);
      },
    };
  }

  global.PMCurrency = {
    SUPPORTED_CURRENCIES,
    normalizeCurrencyCode,
    formatMoney,
    getCachedRate,
    fetchRate,
    preloadRates,
    preloadRatesFromItems,
    convertAmount,
    populateCurrencySelect,
    createDisplayController,
  };
}(window));
