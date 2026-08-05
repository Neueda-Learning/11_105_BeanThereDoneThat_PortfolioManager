"""
generate.py — Generates all HTML layout pages for Portfolio Manager.
Run: python frontend/generate.py
"""
import os

BASE = os.path.dirname(os.path.abspath(__file__))

HEAD = """  <meta charset="UTF-8"/><meta name="viewport" content="width=device-width,initial-scale=1.0"/>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" crossorigin="anonymous"/>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css"/>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet"/>
  <link rel="stylesheet" href="css/style.css"/>"""

SCRIPTS_BASE = """  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script>
  <script src="js/api.js"></script>
  <script src="js/auth.js"></script>"""

SIDEBAR_TOGGLE_JS = """
  <script>
    var sb = document.getElementById('sidebar'), ov = document.getElementById('overlay');
    document.getElementById('hamburger').addEventListener('click', function(){ sb.classList.add('open'); ov.classList.add('active'); });
    ov.addEventListener('click', function(){ sb.classList.remove('open'); ov.classList.remove('active'); });
  </script>"""

def navbar(active_href=""):
    return """  <nav class="pm-navbar">
    <a class="pm-navbar__brand" href="dashboard.html">
      <div class="pm-navbar__brand-icon"><i class="bi bi-graph-up-arrow"></i></div>
      <span class="pm-navbar__brand-name">Portfolio Manager</span>
    </a>
    <div class="pm-navbar__search">
      <i class="bi bi-search"></i>
      <input type="text" placeholder="Search..." aria-label="Search"/>
    </div>
    <div class="pm-navbar__actions">
      <button class="pm-navbar__icon-btn" title="Notifications"><i class="bi bi-bell"></i><span class="pm-navbar__badge"></span></button>
      <button class="pm-navbar__icon-btn" title="Settings"><i class="bi bi-gear"></i></button>
      <div class="pm-navbar__divider"></div>
      <div class="dropdown">
        <button class="pm-navbar__avatar" data-bs-toggle="dropdown" data-initials aria-expanded="false">JD</button>
        <ul class="dropdown-menu dropdown-menu-end" style="border-radius:var(--r-lg);border:1px solid var(--gray-300);min-width:190px;padding:4px;">
          <li><div class="px-3 py-2"><div style="font-weight:700;font-size:.82rem;" data-user-name>Jane Doe</div><div style="font-size:.75rem;color:var(--gray-500);" data-user-email>jane@example.com</div></div></li>
          <li><hr class="dropdown-divider" style="border-color:var(--gray-200);"/></li>
          <li><a class="dropdown-item" href="profile.html" style="font-size:.82rem;"><i class="bi bi-person me-2"></i>Profile</a></li>
          <li><hr class="dropdown-divider" style="border-color:var(--gray-200);"/></li>
          <li><button class="dropdown-item text-danger" style="font-size:.82rem;" data-logout><i class="bi bi-box-arrow-right me-2"></i>Sign Out</button></li>
        </ul>
      </div>
      <button class="pm-navbar__hamburger" id="hamburger" aria-label="Menu"><i class="bi bi-list"></i></button>
    </div>
  </nav>"""

def sidebar(active):
    nav_items = [
        ("dashboard.html",   "bi-grid-1x2-fill",     "Dashboard"),
        ("portfolios.html",  "bi-folder2-open",       "Portfolios"),
        ("investments.html", "bi-bar-chart-fill",     "Investments"),
        ("transactions.html","bi-arrow-left-right",   "Transactions"),
        ("analytics.html",   "bi-pie-chart-fill",     "Analytics"),
        ("profile.html",     "bi-person-fill",        "Profile"),
    ]
    items_html = ""
    for href, icon, label in nav_items:
        cls = ' class="active" aria-current="page"' if href == active else ""
        items_html += f'      <li><a href="{href}"{cls}><i class="bi {icon}"></i><span>{label}</span></a></li>\n'
    return f"""  <aside class="pm-sidebar" id="sidebar">
    <div class="pm-sidebar__section">Menu</div>
    <ul class="pm-sidebar__nav">
{items_html}    </ul>
    <div class="pm-sidebar__divider"></div>
    <div class="pm-sidebar__section">Account</div>
    <ul class="pm-sidebar__nav">
      <li><a href="#" data-logout><i class="bi bi-box-arrow-right"></i><span>Sign Out</span></a></li>
    </ul>
    <div class="pm-sidebar__user">
      <div class="pm-sidebar__user-av" data-initials>JD</div>
      <div>
        <div class="pm-sidebar__user-name" data-user-name>Jane Doe</div>
        <div class="pm-sidebar__user-role">Investor</div>
      </div>
      <button class="pm-sidebar__user-exit" data-logout title="Sign out"><i class="bi bi-box-arrow-right"></i></button>
    </div>
  </aside>"""

def page_wrapper(title, css_extra, active, body_content, js_page="", extra_css_link=""):
    head_extra = f'\n  <link rel="stylesheet" href="{extra_css_link}"/>' if extra_css_link else ""
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <title>{title} - Portfolio Manager</title>
{HEAD}{head_extra}
</head>
<body>
  <div class="pm-overlay" id="overlay"></div>
{navbar(active)}
{sidebar(active)}
  <main class="pm-main" id="main">
{body_content}
  </main>
  <div class="pm-toast-stack" id="toasts"></div>
{SCRIPTS_BASE}
{js_page}
{SIDEBAR_TOGGLE_JS}
</body>
</html>"""

# ─── INDEX (Landing) ──────────────────────────────────────────────────────────

INDEX = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <title>Portfolio Manager - Smart Investment Tracking</title>
{HEAD}
</head>
<body style="background:var(--white);">
  <nav style="height:60px;display:flex;align-items:center;padding:0 32px;border-bottom:1px solid var(--gray-200);gap:12px;">
    <div style="display:flex;align-items:center;gap:9px;">
      <div style="width:34px;height:34px;background:var(--pink-200);border-radius:6px;display:flex;align-items:center;justify-content:center;"><i class="bi bi-graph-up-arrow" style="color:var(--pink-txt);"></i></div>
      <span style="font-weight:800;font-size:.95rem;color:var(--gray-900);">Portfolio Manager</span>
    </div>
    <div style="margin-left:auto;display:flex;gap:8px;align-items:center;">
      <a href="login.html" class="pm-btn pm-btn--outline pm-btn--sm">Sign In</a>
      <a href="signup.html" class="pm-btn pm-btn--primary pm-btn--sm">Get Started</a>
    </div>
  </nav>

  <div class="pm-landing-hero">
    <span class="pm-landing-tag">Enterprise Investment Platform</span>
    <h1 class="pm-landing-h1">Track, Analyse &amp; Grow<br/>Your Investment Portfolio</h1>
    <p class="pm-landing-p">A professional-grade portfolio management platform for individual investors and wealth managers. Monitor stocks, mutual funds, ETFs and more in one unified dashboard.</p>
    <div style="display:flex;gap:10px;justify-content:center;flex-wrap:wrap;">
      <a href="signup.html" class="pm-btn pm-btn--primary pm-btn--lg">Create Free Account</a>
      <a href="login.html" class="pm-btn pm-btn--outline pm-btn--lg">Sign In</a>
    </div>
  </div>

  <div class="pm-features">
    <div class="pm-feature">
      <div class="pm-feature__icon"><i class="bi bi-bar-chart-fill"></i></div>
      <div class="pm-feature__ttl">Portfolio Analytics</div>
      <div class="pm-feature__txt">Real-time P&amp;L tracking, asset allocation charts, and performance benchmarking across all your holdings.</div>
    </div>
    <div class="pm-feature">
      <div class="pm-feature__icon"><i class="bi bi-arrow-left-right"></i></div>
      <div class="pm-feature__ttl">Transaction History</div>
      <div class="pm-feature__txt">Full audit trail of every buy and sell transaction with cost basis tracking and tax report-ready exports.</div>
    </div>
    <div class="pm-feature">
      <div class="pm-feature__icon"><i class="bi bi-folder2-open"></i></div>
      <div class="pm-feature__ttl">Multi-Portfolio</div>
      <div class="pm-feature__txt">Manage multiple portfolios — growth, income, crypto — each with separate performance metrics and goals.</div>
    </div>
    <div class="pm-feature">
      <div class="pm-feature__icon"><i class="bi bi-shield-check-fill"></i></div>
      <div class="pm-feature__ttl">Secure &amp; Private</div>
      <div class="pm-feature__txt">JWT-based authentication, encrypted storage, and zero third-party data sharing. Your data stays yours.</div>
    </div>
    <div class="pm-feature">
      <div class="pm-feature__icon"><i class="bi bi-pie-chart-fill"></i></div>
      <div class="pm-feature__ttl">Asset Diversification</div>
      <div class="pm-feature__txt">Track stocks, mutual funds, ETFs, bonds, and crypto all in one place with unified reporting.</div>
    </div>
    <div class="pm-feature">
      <div class="pm-feature__icon"><i class="bi bi-phone-fill"></i></div>
      <div class="pm-feature__ttl">Mobile Ready</div>
      <div class="pm-feature__txt">Fully responsive interface works seamlessly on desktop, tablet, and mobile browsers.</div>
    </div>
  </div>

  <div style="text-align:center;padding:40px 20px 60px;border-top:1px solid var(--gray-200);color:var(--gray-500);font-size:.8rem;">
    &copy; 2026 Bean There Done That &mdash; Portfolio Manager. All rights reserved.
  </div>
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script>
</body>
</html>"""

# ─── LOGIN ────────────────────────────────────────────────────────────────────

LOGIN = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <title>Sign In - Portfolio Manager</title>
{HEAD}
</head>
<body>
<div class="pm-auth-page">
  <div class="pm-auth-card">
    <div class="pm-auth-card__top">
      <div class="pm-auth-logo"><i class="bi bi-graph-up-arrow"></i></div>
      <div class="pm-auth-card__ttl">Portfolio Manager</div>
      <div class="pm-auth-card__sub">Sign in to your investment dashboard</div>
    </div>
    <div class="pm-auth-card__bd">
      <div id="loginAlert" class="pm-alert pm-alert--danger mb-3 d-none"><i class="bi bi-exclamation-circle-fill"></i><span id="alertMsg">Invalid credentials.</span></div>
      <form id="loginForm" novalidate>
        <div class="mb-3">
          <label class="pm-label" for="email">Email address <span class="req">*</span></label>
          <input type="email" id="email" class="pm-input" placeholder="you@example.com" required autocomplete="email"/>
        </div>
        <div class="mb-4">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;">
            <label class="pm-label mb-0" for="password">Password <span class="req">*</span></label>
            <a href="#" style="font-size:.73rem;color:var(--gray-500);">Forgot password?</a>
          </div>
          <div class="pm-input-group">
            <input type="password" id="password" class="pm-input" placeholder="Enter your password" required autocomplete="current-password" style="border-radius:var(--r-sm) 0 0 var(--r-sm);"/>
            <button type="button" id="togglePwd" class="pm-icon-btn" style="border-radius:0 var(--r-sm) var(--r-sm) 0;width:38px;height:36px;border-left:none;" aria-label="Toggle"><i class="bi bi-eye" id="eyeIcon"></i></button>
          </div>
        </div>
        <div style="display:flex;align-items:center;gap:7px;margin-bottom:18px;font-size:.79rem;color:var(--gray-600);">
          <input type="checkbox" id="remember" style="accent-color:var(--pink-400);width:14px;height:14px;"/>
          <label for="remember" style="cursor:pointer;margin:0;">Keep me signed in</label>
        </div>
        <button type="submit" id="loginBtn" class="pm-btn pm-btn--primary pm-btn--full pm-btn--lg"><i class="bi bi-box-arrow-in-right"></i> Sign In</button>
      </form>
      <hr style="border-color:var(--gray-200);margin:16px 0;"/>
      <p style="text-align:center;font-size:.8rem;color:var(--gray-600);">No account? <a href="signup.html" style="font-weight:700;color:var(--pink-txt);">Create one free</a></p>
      <p style="text-align:center;font-size:.72rem;color:var(--gray-400);margin-top:10px;">&copy; 2026 Bean There Done That. All rights reserved.</p>
    </div>
  </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script>
<script src="js/api.js"></script><script src="js/auth.js"></script>
<script>
  redirectIfAuthenticated();
  document.getElementById("togglePwd").addEventListener("click", function() {{
    var p = document.getElementById("password"), i = document.getElementById("eyeIcon");
    p.type = p.type === "password" ? "text" : "password";
    i.className = p.type === "password" ? "bi bi-eye" : "bi bi-eye-slash";
  }});
  document.getElementById("loginForm").addEventListener("submit", async function(e) {{
    e.preventDefault();
    var btn = document.getElementById("loginBtn"), alrt = document.getElementById("loginAlert");
    alrt.classList.add("d-none"); btn.disabled = true;
    btn.innerHTML = "<span class=\\"spinner-border spinner-border-sm me-2\\"></span>Signing in...";
    try {{
      var res = await AuthAPI.login({{email: document.getElementById("email").value, password: document.getElementById("password").value}});
      saveToken(res.token); saveUser({{email: res.email, firstName: "Jane", lastName: "Doe", customerId: 1}});
      window.location.href = "dashboard.html";
    }} catch(err) {{
      document.getElementById("alertMsg").textContent = err.message || "Sign in failed.";
      alrt.classList.remove("d-none"); btn.disabled = false;
      btn.innerHTML = "<i class=\\"bi bi-box-arrow-in-right\\"></i> Sign In";
    }}
  }});
</script>
</body></html>"""

# ─── SIGNUP ───────────────────────────────────────────────────────────────────

SIGNUP = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <title>Create Account - Portfolio Manager</title>
{HEAD}
</head>
<body>
<div class="pm-auth-page">
  <div class="pm-auth-card" style="max-width:460px;">
    <div class="pm-auth-card__top">
      <div class="pm-auth-logo"><i class="bi bi-graph-up-arrow"></i></div>
      <div class="pm-auth-card__ttl">Create Account</div>
      <div class="pm-auth-card__sub">Start managing your portfolio today</div>
    </div>
    <div class="pm-auth-card__bd">
      <div id="signupAlert" class="pm-alert pm-alert--danger mb-3 d-none"><i class="bi bi-exclamation-circle-fill"></i><span id="signupAlertMsg">Error.</span></div>
      <div id="signupSuccess" class="pm-alert pm-alert--success mb-3 d-none"><i class="bi bi-check-circle-fill"></i>Account created! <a href="login.html">Sign in now</a></div>
      <form id="signupForm" novalidate>
        <div class="row g-3 mb-3">
          <div class="col-6">
            <label class="pm-label" for="firstName">First Name <span class="req">*</span></label>
            <input type="text" id="firstName" class="pm-input" placeholder="Jane" required/>
          </div>
          <div class="col-6">
            <label class="pm-label" for="lastName">Last Name <span class="req">*</span></label>
            <input type="text" id="lastName" class="pm-input" placeholder="Doe" required/>
          </div>
        </div>
        <div class="mb-3">
          <label class="pm-label" for="signupEmail">Email address <span class="req">*</span></label>
          <input type="email" id="signupEmail" class="pm-input" placeholder="you@example.com" required autocomplete="email"/>
        </div>
        <div class="mb-3">
          <label class="pm-label" for="phone">Phone Number</label>
          <input type="tel" id="phone" class="pm-input" placeholder="+91 98765 43210"/>
        </div>
        <div class="mb-4">
          <label class="pm-label" for="signupPwd">Password <span class="req">*</span></label>
          <input type="password" id="signupPwd" class="pm-input" placeholder="Min. 8 characters" required minlength="8" autocomplete="new-password"/>
          <div class="pm-hint">Use at least 8 characters with a number and symbol.</div>
        </div>
        <div class="mb-4">
          <label class="pm-label" for="confirmPwd">Confirm Password <span class="req">*</span></label>
          <input type="password" id="confirmPwd" class="pm-input" placeholder="Repeat your password" required autocomplete="new-password"/>
        </div>
        <div style="display:flex;align-items:flex-start;gap:7px;margin-bottom:18px;font-size:.79rem;color:var(--gray-600);">
          <input type="checkbox" id="terms" style="accent-color:var(--pink-400);width:14px;height:14px;flex-shrink:0;margin-top:2px;" required/>
          <label for="terms" style="cursor:pointer;margin:0;">I agree to the <a href="#" style="color:var(--pink-txt);">Terms of Service</a> and <a href="#" style="color:var(--pink-txt);">Privacy Policy</a></label>
        </div>
        <button type="submit" id="signupBtn" class="pm-btn pm-btn--primary pm-btn--full pm-btn--lg"><i class="bi bi-person-plus-fill"></i> Create Account</button>
      </form>
      <hr style="border-color:var(--gray-200);margin:16px 0;"/>
      <p style="text-align:center;font-size:.8rem;color:var(--gray-600);">Already have an account? <a href="login.html" style="font-weight:700;color:var(--pink-txt);">Sign in</a></p>
    </div>
  </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script>
<script src="js/api.js"></script><script src="js/auth.js"></script>
<script>
  redirectIfAuthenticated();
  document.getElementById("signupForm").addEventListener("submit", async function(e) {{
    e.preventDefault();
    var p = document.getElementById("signupPwd").value, c = document.getElementById("confirmPwd").value;
    var alrt = document.getElementById("signupAlert"), ok = document.getElementById("signupSuccess");
    alrt.classList.add("d-none"); ok.classList.add("d-none");
    if (p !== c) {{ document.getElementById("signupAlertMsg").textContent = "Passwords do not match."; alrt.classList.remove("d-none"); return; }}
    var btn = document.getElementById("signupBtn");
    btn.disabled = true; btn.innerHTML = "<span class=\\"spinner-border spinner-border-sm me-2\\"></span>Creating...";
    try {{
      await AuthAPI.register({{firstName: document.getElementById("firstName").value, lastName: document.getElementById("lastName").value,
        email: document.getElementById("signupEmail").value, password: p, phoneNumber: document.getElementById("phone").value}});
      ok.classList.remove("d-none"); e.target.reset();
    }} catch(err) {{
      document.getElementById("signupAlertMsg").textContent = err.message || "Registration failed.";
      alrt.classList.remove("d-none");
    }} finally {{ btn.disabled = false; btn.innerHTML = "<i class=\\"bi bi-person-plus-fill\\"></i> Create Account"; }}
  }});
</script>
</body></html>"""

# ─── DASHBOARD ────────────────────────────────────────────────────────────────

DASHBOARD_BODY = """    <div style="display:flex;align-items:flex-start;justify-content:space-between;flex-wrap:wrap;gap:12px;margin-bottom:20px;">
      <div>
        <div style="font-size:.75rem;color:var(--gray-500);margin-bottom:2px;">Dashboard</div>
        <h1 class="pm-page-title">Good morning, Jane</h1>
        <p class="pm-page-sub">Here is your investment overview for today.</p>
      </div>
      <div style="display:flex;gap:8px;flex-wrap:wrap;">
        <a href="add-investment.html" class="pm-btn pm-btn--primary"><i class="bi bi-plus-lg"></i> Add Investment</a>
        <a href="portfolios.html" class="pm-btn pm-btn--outline"><i class="bi bi-folder2-open"></i> Portfolios</a>
      </div>
    </div>

    <!-- Stat Cards -->
    <div class="row g-3 mb-4">
      <div class="col-6 col-xl-3">
        <div class="pm-stat">
          <div class="pm-stat__icon pm-stat__icon--pink"><i class="bi bi-collection-fill"></i></div>
          <div class="pm-stat__lbl">Total Investments</div>
          <div class="pm-stat__val" id="statCount">5</div>
          <div class="pm-stat__dlt delta-flat"><i class="bi bi-dash"></i> Active holdings</div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="pm-stat">
          <div class="pm-stat__icon pm-stat__icon--blue"><i class="bi bi-currency-rupee"></i></div>
          <div class="pm-stat__lbl">Current Value</div>
          <div class="pm-stat__val" id="statValue">&#8377;20,38,800</div>
          <div class="pm-stat__dlt delta-flat"><i class="bi bi-dash"></i> Portfolio value</div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="pm-stat">
          <div class="pm-stat__icon pm-stat__icon--green"><i class="bi bi-graph-up-arrow"></i></div>
          <div class="pm-stat__lbl">Total P&amp;L</div>
          <div class="pm-stat__val" id="statPL" style="color:var(--success);">+&#8377;2,59,300</div>
          <div class="pm-stat__dlt delta-up"><i class="bi bi-arrow-up-short"></i> +14.6% overall</div>
        </div>
      </div>
      <div class="col-6 col-xl-3">
        <div class="pm-stat">
          <div class="pm-stat__icon pm-stat__icon--amber"><i class="bi bi-folder2-open"></i></div>
          <div class="pm-stat__lbl">Portfolios</div>
          <div class="pm-stat__val" id="statPortfolios">3</div>
          <div class="pm-stat__dlt delta-flat"><i class="bi bi-dash"></i> Managed</div>
        </div>
      </div>
    </div>

    <!-- Charts Row -->
    <div class="row g-3 mb-4">
      <div class="col-12 col-xl-8">
        <div class="pm-card">
          <div class="pm-card__hd">
            <span class="pm-card__title"><i class="bi bi-activity"></i> Portfolio Growth</span>
            <div style="display:flex;gap:4px;">
              <button class="pm-btn pm-btn--outline pm-btn--sm active-period" style="border-radius:4px;">1M</button>
              <button class="pm-btn pm-btn--outline pm-btn--sm" style="border-radius:4px;">3M</button>
              <button class="pm-btn pm-btn--outline pm-btn--sm" style="border-radius:4px;">1Y</button>
            </div>
          </div>
          <div class="pm-card__bd">
            <div style="height:240px;display:flex;align-items:center;justify-content:center;background:var(--gray-50);border-radius:var(--r-sm);border:1px dashed var(--gray-300);">
              <canvas id="growthChart"></canvas>
            </div>
          </div>
        </div>
      </div>
      <div class="col-12 col-xl-4">
        <div class="pm-card h-100">
          <div class="pm-card__hd"><span class="pm-card__title"><i class="bi bi-pie-chart-fill"></i> Allocation</span></div>
          <div class="pm-card__bd">
            <div style="height:160px;display:flex;align-items:center;justify-content:center;background:var(--gray-50);border-radius:var(--r-sm);border:1px dashed var(--gray-300);margin-bottom:14px;">
              <canvas id="allocChart"></canvas>
            </div>
            <ul style="list-style:none;padding:0;display:flex;flex-direction:column;gap:6px;">
              <li style="display:flex;align-items:center;gap:7px;font-size:.79rem;"><span style="width:9px;height:9px;border-radius:50%;background:var(--pink-300);flex-shrink:0;"></span><span style="flex:1;color:var(--gray-600);">Stocks</span><span style="font-weight:700;">61%</span></li>
              <li style="display:flex;align-items:center;gap:7px;font-size:.79rem;"><span style="width:9px;height:9px;border-radius:50%;background:var(--info);flex-shrink:0;"></span><span style="flex:1;color:var(--gray-600);">Mutual Funds</span><span style="font-weight:700;">15%</span></li>
              <li style="display:flex;align-items:center;gap:7px;font-size:.79rem;"><span style="width:9px;height:9px;border-radius:50%;background:var(--warning);flex-shrink:0;"></span><span style="flex:1;color:var(--gray-600);">Cryptocurrency</span><span style="font-weight:700;">24%</span></li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="pm-card mb-4">
      <div class="pm-card__hd"><span class="pm-card__title"><i class="bi bi-lightning-fill"></i> Quick Actions</span></div>
      <div class="pm-card__bd">
        <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:10px;">
          <a href="add-investment.html" style="display:flex;flex-direction:column;align-items:center;gap:6px;padding:14px 8px;background:var(--white);border:1px solid var(--gray-300);border-radius:var(--r-md);cursor:pointer;transition:var(--tr);text-decoration:none;" onmouseover="this.style.background='var(--pink-100)';this.style.borderColor='var(--pink-200)'" onmouseout="this.style.background='var(--white)';this.style.borderColor='var(--gray-300)'">
            <div style="width:36px;height:36px;border-radius:6px;background:var(--pink-100);display:flex;align-items:center;justify-content:center;font-size:1rem;color:var(--pink-txt);"><i class="bi bi-plus-circle-fill"></i></div>
            <span style="font-size:.72rem;font-weight:600;color:var(--gray-700);text-align:center;">Add Investment</span>
          </a>
          <a href="transactions.html" style="display:flex;flex-direction:column;align-items:center;gap:6px;padding:14px 8px;background:var(--white);border:1px solid var(--gray-300);border-radius:var(--r-md);cursor:pointer;transition:var(--tr);text-decoration:none;" onmouseover="this.style.background='var(--pink-100)';this.style.borderColor='var(--pink-200)'" onmouseout="this.style.background='var(--white)';this.style.borderColor='var(--gray-300)'">
            <div style="width:36px;height:36px;border-radius:6px;background:var(--info-bg);display:flex;align-items:center;justify-content:center;font-size:1rem;color:var(--info);"><i class="bi bi-arrow-left-right"></i></div>
            <span style="font-size:.72rem;font-weight:600;color:var(--gray-700);text-align:center;">Transactions</span>
          </a>
          <a href="analytics.html" style="display:flex;flex-direction:column;align-items:center;gap:6px;padding:14px 8px;background:var(--white);border:1px solid var(--gray-300);border-radius:var(--r-md);cursor:pointer;transition:var(--tr);text-decoration:none;" onmouseover="this.style.background='var(--pink-100)';this.style.borderColor='var(--pink-200)'" onmouseout="this.style.background='var(--white)';this.style.borderColor='var(--gray-300)'">
            <div style="width:36px;height:36px;border-radius:6px;background:var(--success-bg);display:flex;align-items:center;justify-content:center;font-size:1rem;color:var(--success);"><i class="bi bi-pie-chart-fill"></i></div>
            <span style="font-size:.72rem;font-weight:600;color:var(--gray-700);text-align:center;">Analytics</span>
          </a>
          <a href="portfolios.html" style="display:flex;flex-direction:column;align-items:center;gap:6px;padding:14px 8px;background:var(--white);border:1px solid var(--gray-300);border-radius:var(--r-md);cursor:pointer;transition:var(--tr);text-decoration:none;" onmouseover="this.style.background='var(--pink-100)';this.style.borderColor='var(--pink-200)'" onmouseout="this.style.background='var(--white)';this.style.borderColor='var(--gray-300)'">
            <div style="width:36px;height:36px;border-radius:6px;background:var(--warning-bg);display:flex;align-items:center;justify-content:center;font-size:1rem;color:var(--warning);"><i class="bi bi-folder2-open"></i></div>
            <span style="font-size:.72rem;font-weight:600;color:var(--gray-700);text-align:center;">Portfolios</span>
          </a>
        </div>
      </div>
    </div>

    <!-- Recent Transactions -->
    <div class="row g-3">
      <div class="col-12 col-xl-7">
        <div class="pm-card">
          <div class="pm-card__hd">
            <span class="pm-card__title"><i class="bi bi-arrow-left-right"></i> Recent Transactions</span>
            <a href="transactions.html" style="font-size:.78rem;color:var(--pink-txt);">View all</a>
          </div>
          <div class="pm-card__bd--bare">
            <div class="pm-tbl-wrap">
              <table class="pm-tbl">
                <thead><tr><th>Asset</th><th>Type</th><th>Qty</th><th>Amount</th><th>Date</th></tr></thead>
                <tbody>
                  <tr><td><div style="font-weight:600;">TCS</div><div style="font-size:.74rem;color:var(--gray-500);">Tata Consultancy</div></td><td><span class="pm-badge pm-badge--green"><i class="bi bi-arrow-down-circle-fill"></i>BUY</span></td><td>50</td><td style="font-weight:700;">&#8377;1,60,000</td><td style="color:var(--gray-500);font-size:.78rem;">15 Jun 2023</td></tr>
                  <tr><td><div style="font-weight:600;">INFY</div><div style="font-size:.74rem;color:var(--gray-500);">Infosys Ltd</div></td><td><span class="pm-badge pm-badge--green"><i class="bi bi-arrow-down-circle-fill"></i>BUY</span></td><td>80</td><td style="font-weight:700;">&#8377;1,16,000</td><td style="color:var(--gray-500);font-size:.78rem;">20 Sep 2023</td></tr>
                  <tr><td><div style="font-weight:600;">TCS</div><div style="font-size:.74rem;color:var(--gray-500);">Tata Consultancy</div></td><td><span class="pm-badge pm-badge--red"><i class="bi bi-arrow-up-circle-fill"></i>SELL</span></td><td>10</td><td style="font-weight:700;">&#8377;37,500</td><td style="color:var(--gray-500);font-size:.78rem;">01 Mar 2024</td></tr>
                  <tr><td><div style="font-weight:600;">BTC</div><div style="font-size:.74rem;color:var(--gray-500);">Bitcoin</div></td><td><span class="pm-badge pm-badge--green"><i class="bi bi-arrow-down-circle-fill"></i>BUY</span></td><td>0.5</td><td style="font-weight:700;">&#8377;14,00,000</td><td style="color:var(--gray-500);font-size:.78rem;">10 Jan 2025</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
      <div class="col-12 col-xl-5">
        <div class="pm-card">
          <div class="pm-card__hd">
            <span class="pm-card__title"><i class="bi bi-folder2-open"></i> Portfolio Summary</span>
            <a href="portfolios.html" style="font-size:.78rem;color:var(--pink-txt);">Manage</a>
          </div>
          <div class="pm-card__bd">
            <div style="margin-bottom:14px;">
              <div style="display:flex;justify-content:space-between;margin-bottom:4px;font-size:.79rem;"><span style="color:var(--gray-700);font-weight:500;">Growth Portfolio</span><span style="color:var(--gray-600);">&#8377;5,80,000</span></div>
              <div style="height:6px;background:var(--gray-200);border-radius:99px;overflow:hidden;"><div style="height:100%;background:var(--pink-300);border-radius:99px;width:58%;"></div></div>
            </div>
            <div style="margin-bottom:14px;">
              <div style="display:flex;justify-content:space-between;margin-bottom:4px;font-size:.79rem;"><span style="color:var(--gray-700);font-weight:500;">Income Portfolio</span><span style="color:var(--gray-600);">&#8377;3,20,000</span></div>
              <div style="height:6px;background:var(--gray-200);border-radius:99px;overflow:hidden;"><div style="height:100%;background:var(--info);border-radius:99px;width:32%;"></div></div>
            </div>
            <div>
              <div style="display:flex;justify-content:space-between;margin-bottom:4px;font-size:.79rem;"><span style="color:var(--gray-700);font-weight:500;">Crypto Basket</span><span style="color:var(--gray-600);">&#8377;95,000</span></div>
              <div style="height:6px;background:var(--gray-200);border-radius:99px;overflow:hidden;"><div style="height:100%;background:var(--warning);border-radius:99px;width:9.5%;"></div></div>
            </div>
          </div>
        </div>
      </div>
    </div>"""

DASHBOARD = page_wrapper("Dashboard", "", "dashboard.html", DASHBOARD_BODY,
  """  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js"></script>
  <script src="js/dashboard.js"></script>""", "css/dashboard.css")

# ─── PORTFOLIOS ───────────────────────────────────────────────────────────────

PORTFOLIOS_BODY = """    <div class="pm-page-header">
      <div>
        <div class="pm-breadcrumb"><a href="dashboard.html">Dashboard</a><span class="sep">/</span><span class="current">Portfolios</span></div>
        <h1 class="pm-page-title">Portfolios</h1>
        <p class="pm-page-sub">Manage your investment portfolios.</p>
      </div>
      <button class="pm-btn pm-btn--primary" data-bs-toggle="modal" data-bs-target="#createModal"><i class="bi bi-plus-lg"></i> New Portfolio</button>
    </div>

    <!-- Stats -->
    <div class="row g-3 mb-4">
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--pink"><i class="bi bi-folder2-open"></i></div><div class="pm-stat__lbl">Total</div><div class="pm-stat__val">3</div></div></div>
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--blue"><i class="bi bi-currency-rupee"></i></div><div class="pm-stat__lbl">Combined Value</div><div class="pm-stat__val">&#8377;9,95,000</div></div></div>
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--green"><i class="bi bi-graph-up-arrow"></i></div><div class="pm-stat__lbl">Best Performer</div><div class="pm-stat__val">Growth</div></div></div>
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--amber"><i class="bi bi-bar-chart-fill"></i></div><div class="pm-stat__lbl">Holdings</div><div class="pm-stat__val">5</div></div></div>
    </div>

    <!-- Portfolio Cards -->
    <div class="row g-3">
      <div class="col-12 col-md-6 col-xl-4">
        <div class="pm-card">
          <div class="pm-card__hd">
            <span class="pm-card__title"><i class="bi bi-folder2-open"></i> Growth Portfolio</span>
            <div style="display:flex;gap:4px;">
              <button class="pm-icon-btn" title="Edit"><i class="bi bi-pencil-fill"></i></button>
              <button class="pm-icon-btn pm-icon-btn--del" title="Delete"><i class="bi bi-trash3-fill"></i></button>
            </div>
          </div>
          <div class="pm-card__bd">
            <div style="font-size:.78rem;color:var(--gray-500);margin-bottom:12px;">Long-term equity growth</div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:14px;">
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">Current Value</div><div style="font-weight:700;font-size:.95rem;">&#8377;5,80,000</div></div>
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">Holdings</div><div style="font-weight:700;font-size:.95rem;">3</div></div>
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">P&amp;L</div><div style="font-weight:700;font-size:.95rem;color:var(--success);">+&#8377;54,800</div></div>
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">Created</div><div style="font-weight:700;font-size:.95rem;">10 Jan 2024</div></div>
            </div>
            <a href="investments.html" class="pm-btn pm-btn--outline pm-btn--sm pm-btn--full"><i class="bi bi-bar-chart-fill"></i> View Investments</a>
          </div>
        </div>
      </div>
      <div class="col-12 col-md-6 col-xl-4">
        <div class="pm-card">
          <div class="pm-card__hd">
            <span class="pm-card__title"><i class="bi bi-folder2-open"></i> Income Portfolio</span>
            <div style="display:flex;gap:4px;">
              <button class="pm-icon-btn" title="Edit"><i class="bi bi-pencil-fill"></i></button>
              <button class="pm-icon-btn pm-icon-btn--del" title="Delete"><i class="bi bi-trash3-fill"></i></button>
            </div>
          </div>
          <div class="pm-card__bd">
            <div style="font-size:.78rem;color:var(--gray-500);margin-bottom:12px;">Dividend income &amp; bonds</div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:14px;">
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">Current Value</div><div style="font-weight:700;font-size:.95rem;">&#8377;3,20,000</div></div>
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">Holdings</div><div style="font-weight:700;font-size:.95rem;">1</div></div>
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">P&amp;L</div><div style="font-weight:700;font-size:.95rem;color:var(--success);">+&#8377;4,500</div></div>
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">Created</div><div style="font-weight:700;font-size:.95rem;">22 Mar 2024</div></div>
            </div>
            <a href="investments.html" class="pm-btn pm-btn--outline pm-btn--sm pm-btn--full"><i class="bi bi-bar-chart-fill"></i> View Investments</a>
          </div>
        </div>
      </div>
      <div class="col-12 col-md-6 col-xl-4">
        <div class="pm-card">
          <div class="pm-card__hd">
            <span class="pm-card__title"><i class="bi bi-folder2-open"></i> Crypto Basket</span>
            <div style="display:flex;gap:4px;">
              <button class="pm-icon-btn" title="Edit"><i class="bi bi-pencil-fill"></i></button>
              <button class="pm-icon-btn pm-icon-btn--del" title="Delete"><i class="bi bi-trash3-fill"></i></button>
            </div>
          </div>
          <div class="pm-card__bd">
            <div style="font-size:.78rem;color:var(--gray-500);margin-bottom:12px;">Digital assets only</div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:14px;">
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">Current Value</div><div style="font-weight:700;font-size:.95rem;">&#8377;95,000</div></div>
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">Holdings</div><div style="font-weight:700;font-size:.95rem;">1</div></div>
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">P&amp;L</div><div style="font-weight:700;font-size:.95rem;color:var(--success);">+&#8377;2,00,000</div></div>
              <div><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:2px;">Created</div><div style="font-weight:700;font-size:.95rem;">05 Jan 2025</div></div>
            </div>
            <a href="investments.html" class="pm-btn pm-btn--outline pm-btn--sm pm-btn--full"><i class="bi bi-bar-chart-fill"></i> View Investments</a>
          </div>
        </div>
      </div>
    </div>

  <!-- Create Portfolio Modal -->
  <div class="modal fade" id="createModal" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content">
        <div class="modal-header"><h5 class="modal-title"><i class="bi bi-folder-plus me-2" style="color:var(--pink-400);"></i>New Portfolio</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <form>
          <div class="modal-body">
            <div class="mb-3"><label class="pm-label" for="pName">Portfolio Name <span class="req">*</span></label><input type="text" id="pName" class="pm-input" placeholder="e.g. Growth Portfolio" required/></div>
            <div class="mb-3"><label class="pm-label" for="pDesc">Description</label><textarea id="pDesc" class="pm-input pm-textarea" placeholder="Short description..." rows="3"></textarea></div>
          </div>
          <div class="modal-footer"><button type="button" class="pm-btn pm-btn--outline" data-bs-dismiss="modal">Cancel</button><button type="submit" class="pm-btn pm-btn--primary"><i class="bi bi-check-lg"></i> Create</button></div>
        </form>
      </div>
    </div>
  </div>"""

PORTFOLIOS = page_wrapper("Portfolios", "", "portfolios.html", PORTFOLIOS_BODY,
  "  <script src=\"js/portfolio.js\"></script>")

# ─── INVESTMENTS ──────────────────────────────────────────────────────────────

INVESTMENTS_BODY = """    <div class="pm-page-header">
      <div>
        <div class="pm-breadcrumb"><a href="dashboard.html">Dashboard</a><span class="sep">/</span><span class="current">Investments</span></div>
        <h1 class="pm-page-title">Investments</h1>
        <p class="pm-page-sub">All your investment holdings.</p>
      </div>
      <div style="display:flex;gap:8px;">
        <button class="pm-btn pm-btn--primary" data-bs-toggle="modal" data-bs-target="#addModal"><i class="bi bi-plus-lg"></i> Add Investment</button>
        <button class="pm-btn pm-btn--outline"><i class="bi bi-download"></i> Export</button>
      </div>
    </div>

    <!-- Summary -->
    <div class="row g-3 mb-4">
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--pink"><i class="bi bi-collection-fill"></i></div><div class="pm-stat__lbl">Holdings</div><div class="pm-stat__val">5</div></div></div>
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--blue"><i class="bi bi-currency-rupee"></i></div><div class="pm-stat__lbl">Invested</div><div class="pm-stat__val">&#8377;17,80,000</div></div></div>
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--green"><i class="bi bi-graph-up-arrow"></i></div><div class="pm-stat__lbl">Current Value</div><div class="pm-stat__val">&#8377;20,38,800</div></div></div>
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--amber"><i class="bi bi-percent"></i></div><div class="pm-stat__lbl">Total P&amp;L</div><div class="pm-stat__val" style="color:var(--success);">+14.6%</div></div></div>
    </div>

    <!-- Table -->
    <div class="pm-card">
      <div class="pm-card__hd" style="flex-wrap:wrap;gap:8px;">
        <span class="pm-card__title"><i class="bi bi-bar-chart-fill"></i> Holdings</span>
        <div style="display:flex;gap:8px;margin-left:auto;flex-wrap:wrap;">
          <div style="position:relative;"><i class="bi bi-search" style="position:absolute;left:9px;top:50%;transform:translateY(-50%);color:var(--gray-500);font-size:.8rem;pointer-events:none;"></i><input type="text" id="searchInput" class="pm-input" placeholder="Search..." style="padding-left:28px;width:180px;height:34px;"/></div>
          <select id="typeFilter" class="pm-input" style="width:130px;height:34px;"><option value="">All Types</option><option>Stock</option><option>Mutual Fund</option><option>ETF</option><option>Cryptocurrency</option></select>
        </div>
      </div>
      <div id="investAlert" class="pm-alert pm-alert--danger m-3 d-none"><i class="bi bi-exclamation-circle-fill"></i>Error loading data.</div>
      <div class="pm-card__bd--bare">
        <div class="pm-tbl-wrap">
          <table class="pm-tbl" id="investTable">
            <thead><tr><th>#</th><th>Company / Asset</th><th>Type</th><th>Qty</th><th>Buy Price</th><th>Cur. Price</th><th>Value</th><th>P&amp;L</th><th>Date</th><th style="text-align:center;">Actions</th></tr></thead>
            <tbody>
              <tr><td style="color:var(--gray-400);">1</td><td><div style="font-weight:600;">Tata Consultancy Services</div><div style="font-size:.74rem;color:var(--gray-500);">TCS</div></td><td><span class="pm-badge pm-badge--blue">Stock</span></td><td>50</td><td>&#8377;3,200</td><td>&#8377;3,850</td><td style="font-weight:700;">&#8377;1,92,500</td><td style="color:var(--success);font-weight:600;">+&#8377;32,500</td><td style="color:var(--gray-500);font-size:.78rem;">15 Jun 2023</td><td style="text-align:center;"><div style="display:flex;gap:4px;justify-content:center;"><button class="pm-icon-btn" title="Edit"><i class="bi bi-pencil-fill"></i></button><button class="pm-icon-btn pm-icon-btn--del" title="Delete"><i class="bi bi-trash3-fill"></i></button></div></td></tr>
              <tr><td style="color:var(--gray-400);">2</td><td><div style="font-weight:600;">Infosys Ltd</div><div style="font-size:.74rem;color:var(--gray-500);">INFY</div></td><td><span class="pm-badge pm-badge--blue">Stock</span></td><td>80</td><td>&#8377;1,450</td><td>&#8377;1,620</td><td style="font-weight:700;">&#8377;1,29,600</td><td style="color:var(--success);font-weight:600;">+&#8377;13,600</td><td style="color:var(--gray-500);font-size:.78rem;">20 Sep 2023</td><td style="text-align:center;"><div style="display:flex;gap:4px;justify-content:center;"><button class="pm-icon-btn" title="Edit"><i class="bi bi-pencil-fill"></i></button><button class="pm-icon-btn pm-icon-btn--del" title="Delete"><i class="bi bi-trash3-fill"></i></button></div></td></tr>
              <tr><td style="color:var(--gray-400);">3</td><td><div style="font-weight:600;">Reliance Industries</div><div style="font-size:.74rem;color:var(--gray-500);">RELIANCE</div></td><td><span class="pm-badge pm-badge--blue">Stock</span></td><td>30</td><td>&#8377;2,600</td><td>&#8377;2,890</td><td style="font-weight:700;">&#8377;86,700</td><td style="color:var(--success);font-weight:600;">+&#8377;8,700</td><td style="color:var(--gray-500);font-size:.78rem;">08 Jan 2024</td><td style="text-align:center;"><div style="display:flex;gap:4px;justify-content:center;"><button class="pm-icon-btn" title="Edit"><i class="bi bi-pencil-fill"></i></button><button class="pm-icon-btn pm-icon-btn--del" title="Delete"><i class="bi bi-trash3-fill"></i></button></div></td></tr>
              <tr><td style="color:var(--gray-400);">4</td><td><div style="font-weight:600;">SBI Bluechip Fund</div><div style="font-size:.74rem;color:var(--gray-500);">SBIBCF</div></td><td><span class="pm-badge pm-badge--pink">Mutual Fund</span></td><td>500</td><td>&#8377;52</td><td>&#8377;61</td><td style="font-weight:700;">&#8377;30,500</td><td style="color:var(--success);font-weight:600;">+&#8377;4,500</td><td style="color:var(--gray-500);font-size:.78rem;">14 Feb 2024</td><td style="text-align:center;"><div style="display:flex;gap:4px;justify-content:center;"><button class="pm-icon-btn" title="Edit"><i class="bi bi-pencil-fill"></i></button><button class="pm-icon-btn pm-icon-btn--del" title="Delete"><i class="bi bi-trash3-fill"></i></button></div></td></tr>
              <tr><td style="color:var(--gray-400);">5</td><td><div style="font-weight:600;">Bitcoin</div><div style="font-size:.74rem;color:var(--gray-500);">BTC</div></td><td><span class="pm-badge pm-badge--amber">Crypto</span></td><td>0.5</td><td>&#8377;28,00,000</td><td>&#8377;32,00,000</td><td style="font-weight:700;">&#8377;16,00,000</td><td style="color:var(--success);font-weight:600;">+&#8377;2,00,000</td><td style="color:var(--gray-500);font-size:.78rem;">10 Jan 2025</td><td style="text-align:center;"><div style="display:flex;gap:4px;justify-content:center;"><button class="pm-icon-btn" title="Edit"><i class="bi bi-pencil-fill"></i></button><button class="pm-icon-btn pm-icon-btn--del" title="Delete"><i class="bi bi-trash3-fill"></i></button></div></td></tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="pm-card__ft" style="display:flex;align-items:center;justify-content:space-between;">
        <span style="font-size:.78rem;color:var(--gray-500);">Showing 5 of 5 records</span>
        <div class="pm-pagination"><button class="pm-pg-btn" disabled><i class="bi bi-chevron-left"></i></button><button class="pm-pg-btn active">1</button><button class="pm-pg-btn" disabled><i class="bi bi-chevron-right"></i></button></div>
      </div>
    </div>

  <!-- Add Investment Modal -->
  <div class="modal fade" id="addModal" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content">
        <div class="modal-header"><h5 class="modal-title"><i class="bi bi-plus-circle-fill me-2" style="color:var(--pink-400);"></i>Add Investment</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <form>
          <div class="modal-body">
            <div class="row g-3">
              <div class="col-12"><label class="pm-label">Portfolio <span class="req">*</span></label><select class="pm-select"><option value="">Select portfolio...</option><option>Growth Portfolio</option><option>Income Portfolio</option><option>Crypto Basket</option></select></div>
              <div class="col-md-8"><label class="pm-label">Company / Asset Name <span class="req">*</span></label><input type="text" class="pm-input" placeholder="e.g. Tata Consultancy Services" required/></div>
              <div class="col-md-4"><label class="pm-label">Symbol</label><input type="text" class="pm-input" placeholder="e.g. TCS"/></div>
              <div class="col-md-6"><label class="pm-label">Asset Type <span class="req">*</span></label><select class="pm-select" required><option value="">Select type...</option><option>Stock</option><option>Mutual Fund</option><option>ETF</option><option>Cryptocurrency</option><option>Bond</option><option>Custom</option></select></div>
              <div class="col-md-6"><label class="pm-label">Purchase Date <span class="req">*</span></label><input type="date" class="pm-input" required/></div>
              <div class="col-md-4"><label class="pm-label">Quantity <span class="req">*</span></label><input type="number" class="pm-input" placeholder="0" min="0" step="any" required/></div>
              <div class="col-md-4"><label class="pm-label">Purchase Price (&#8377;) <span class="req">*</span></label><input type="number" class="pm-input" placeholder="0.00" min="0" step="0.01" required/></div>
              <div class="col-md-4"><label class="pm-label">Current Price (&#8377;)</label><input type="number" class="pm-input" placeholder="0.00" min="0" step="0.01"/></div>
            </div>
          </div>
          <div class="modal-footer"><button type="button" class="pm-btn pm-btn--outline" data-bs-dismiss="modal">Cancel</button><button type="submit" class="pm-btn pm-btn--primary"><i class="bi bi-check-lg"></i> Add</button></div>
        </form>
      </div>
    </div>
  </div>

  <!-- Edit Modal -->
  <div class="modal fade" id="editModal" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content">
        <div class="modal-header"><h5 class="modal-title"><i class="bi bi-pencil-fill me-2" style="color:var(--pink-400);"></i>Edit Investment</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body"><p style="font-size:.85rem;color:var(--gray-600);">Edit form will be populated dynamically.</p></div>
        <div class="modal-footer"><button type="button" class="pm-btn pm-btn--outline" data-bs-dismiss="modal">Cancel</button><button type="submit" class="pm-btn pm-btn--primary"><i class="bi bi-check-lg"></i> Save</button></div>
      </div>
    </div>
  </div>

  <!-- Delete Modal -->
  <div class="modal fade" id="deleteModal" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered modal-sm">
      <div class="modal-content">
        <div class="modal-header"><h5 class="modal-title text-danger"><i class="bi bi-exclamation-triangle me-2"></i>Confirm Delete</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body text-center"><i class="bi bi-trash3" style="font-size:2rem;color:var(--gray-300);display:block;margin-bottom:8px;"></i><p style="font-size:.85rem;">Are you sure you want to delete this investment? This cannot be undone.</p></div>
        <div class="modal-footer justify-content-center gap-2"><button type="button" class="pm-btn pm-btn--outline" data-bs-dismiss="modal">Cancel</button><button type="button" class="pm-btn pm-btn--danger">Delete</button></div>
      </div>
    </div>
  </div>"""

INVESTMENTS = page_wrapper("Investments", "", "investments.html", INVESTMENTS_BODY,
  "  <script src=\"js/investment.js\"></script>")

# ─── TRANSACTIONS ─────────────────────────────────────────────────────────────

TRANSACTIONS_BODY = """    <div class="pm-page-header">
      <div>
        <div class="pm-breadcrumb"><a href="dashboard.html">Dashboard</a><span class="sep">/</span><span class="current">Transactions</span></div>
        <h1 class="pm-page-title">Transactions</h1>
        <p class="pm-page-sub">Complete transaction history across all portfolios.</p>
      </div>
      <div style="display:flex;gap:8px;">
        <button class="pm-btn pm-btn--outline"><i class="bi bi-funnel-fill"></i> Filter</button>
        <button class="pm-btn pm-btn--outline"><i class="bi bi-download"></i> Export</button>
      </div>
    </div>

    <!-- Summary -->
    <div class="row g-3 mb-4">
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--pink"><i class="bi bi-receipt"></i></div><div class="pm-stat__lbl">Total Txns</div><div class="pm-stat__val">5</div></div></div>
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--green"><i class="bi bi-arrow-down-circle-fill"></i></div><div class="pm-stat__lbl">Buy Orders</div><div class="pm-stat__val">4</div></div></div>
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--red"><i class="bi bi-arrow-up-circle-fill"></i></div><div class="pm-stat__lbl">Sell Orders</div><div class="pm-stat__val">1</div></div></div>
      <div class="col-6 col-md-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--blue"><i class="bi bi-currency-rupee"></i></div><div class="pm-stat__lbl">Total Volume</div><div class="pm-stat__val">&#8377;28.9L</div></div></div>
    </div>

    <!-- Filters -->
    <div class="pm-card mb-3">
      <div class="pm-card__bd" style="padding:12px 16px;">
        <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
          <div style="position:relative;"><i class="bi bi-search" style="position:absolute;left:9px;top:50%;transform:translateY(-50%);color:var(--gray-500);font-size:.8rem;pointer-events:none;"></i><input type="text" class="pm-input" placeholder="Search symbol..." style="padding-left:28px;width:160px;height:34px;"/></div>
          <select class="pm-input" style="width:130px;height:34px;"><option value="">All Types</option><option>BUY</option><option>SELL</option></select>
          <input type="date" class="pm-input" style="width:140px;height:34px;" title="From date"/>
          <input type="date" class="pm-input" style="width:140px;height:34px;" title="To date"/>
          <button class="pm-btn pm-btn--outline pm-btn--sm"><i class="bi bi-x-lg"></i> Clear</button>
        </div>
      </div>
    </div>

    <!-- Transactions Table -->
    <div class="pm-card">
      <div class="pm-card__hd"><span class="pm-card__title"><i class="bi bi-arrow-left-right"></i> Transaction History</span><span style="font-size:.78rem;color:var(--gray-500);">5 records</span></div>
      <div class="pm-card__bd--bare">
        <div class="pm-tbl-wrap">
          <table class="pm-tbl">
            <thead><tr><th>#</th><th>Asset</th><th>Type</th><th>Quantity</th><th>Price</th><th>Total Amount</th><th>Date</th></tr></thead>
            <tbody>
              <tr><td style="color:var(--gray-400);">1</td><td><div style="font-weight:600;">TCS</div><div style="font-size:.74rem;color:var(--gray-500);">Tata Consultancy Services</div></td><td><span class="pm-badge pm-badge--green"><i class="bi bi-arrow-down-circle-fill"></i> BUY</span></td><td>50</td><td>&#8377;3,200</td><td style="font-weight:700;">&#8377;1,60,000</td><td style="color:var(--gray-500);font-size:.78rem;">15 Jun 2023</td></tr>
              <tr><td style="color:var(--gray-400);">2</td><td><div style="font-weight:600;">INFY</div><div style="font-size:.74rem;color:var(--gray-500);">Infosys Ltd</div></td><td><span class="pm-badge pm-badge--green"><i class="bi bi-arrow-down-circle-fill"></i> BUY</span></td><td>80</td><td>&#8377;1,450</td><td style="font-weight:700;">&#8377;1,16,000</td><td style="color:var(--gray-500);font-size:.78rem;">20 Sep 2023</td></tr>
              <tr><td style="color:var(--gray-400);">3</td><td><div style="font-weight:600;">TCS</div><div style="font-size:.74rem;color:var(--gray-500);">Tata Consultancy Services</div></td><td><span class="pm-badge pm-badge--red"><i class="bi bi-arrow-up-circle-fill"></i> SELL</span></td><td>10</td><td>&#8377;3,750</td><td style="font-weight:700;">&#8377;37,500</td><td style="color:var(--gray-500);font-size:.78rem;">01 Mar 2024</td></tr>
              <tr><td style="color:var(--gray-400);">4</td><td><div style="font-weight:600;">RELIANCE</div><div style="font-size:.74rem;color:var(--gray-500);">Reliance Industries</div></td><td><span class="pm-badge pm-badge--green"><i class="bi bi-arrow-down-circle-fill"></i> BUY</span></td><td>30</td><td>&#8377;2,600</td><td style="font-weight:700;">&#8377;78,000</td><td style="color:var(--gray-500);font-size:.78rem;">08 Jan 2024</td></tr>
              <tr><td style="color:var(--gray-400);">5</td><td><div style="font-weight:600;">BTC</div><div style="font-size:.74rem;color:var(--gray-500);">Bitcoin</div></td><td><span class="pm-badge pm-badge--green"><i class="bi bi-arrow-down-circle-fill"></i> BUY</span></td><td>0.5</td><td>&#8377;28,00,000</td><td style="font-weight:700;">&#8377;14,00,000</td><td style="color:var(--gray-500);font-size:.78rem;">10 Jan 2025</td></tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="pm-card__ft" style="display:flex;align-items:center;justify-content:space-between;">
        <span style="font-size:.78rem;color:var(--gray-500);">Showing 5 of 5 records</span>
        <div class="pm-pagination"><button class="pm-pg-btn" disabled><i class="bi bi-chevron-left"></i></button><button class="pm-pg-btn active">1</button><button class="pm-pg-btn" disabled><i class="bi bi-chevron-right"></i></button></div>
      </div>
    </div>"""

TRANSACTIONS = page_wrapper("Transactions", "", "transactions.html", TRANSACTIONS_BODY,
  "  <script src=\"js/transaction.js\"></script>")

# ─── ANALYTICS ────────────────────────────────────────────────────────────────

ANALYTICS_BODY = """    <div class="pm-page-header">
      <div>
        <div class="pm-breadcrumb"><a href="dashboard.html">Dashboard</a><span class="sep">/</span><span class="current">Analytics</span></div>
        <h1 class="pm-page-title">Analytics</h1>
        <p class="pm-page-sub">In-depth portfolio performance analysis.</p>
      </div>
      <div style="display:flex;gap:8px;">
        <select class="pm-input" style="width:160px;height:34px;"><option>All Portfolios</option><option>Growth Portfolio</option><option>Income Portfolio</option><option>Crypto Basket</option></select>
        <select class="pm-input" style="width:120px;height:34px;"><option>1 Year</option><option>6 Months</option><option>3 Months</option><option>1 Month</option></select>
      </div>
    </div>

    <!-- KPIs -->
    <div class="row g-3 mb-4">
      <div class="col-6 col-xl-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--pink"><i class="bi bi-currency-rupee"></i></div><div class="pm-stat__lbl">Total Invested</div><div class="pm-stat__val">&#8377;17,80,000</div></div></div>
      <div class="col-6 col-xl-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--green"><i class="bi bi-graph-up-arrow"></i></div><div class="pm-stat__lbl">Current Value</div><div class="pm-stat__val">&#8377;20,38,800</div></div></div>
      <div class="col-6 col-xl-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--blue"><i class="bi bi-arrow-up-right-circle-fill"></i></div><div class="pm-stat__lbl">Absolute P&amp;L</div><div class="pm-stat__val" style="color:var(--success);">+&#8377;2,58,800</div></div></div>
      <div class="col-6 col-xl-3"><div class="pm-stat"><div class="pm-stat__icon pm-stat__icon--amber"><i class="bi bi-percent"></i></div><div class="pm-stat__lbl">XIRR</div><div class="pm-stat__val" style="color:var(--success);">+14.5%</div></div></div>
    </div>

    <!-- Charts -->
    <div class="row g-3 mb-4">
      <div class="col-12 col-xl-8">
        <div class="pm-card">
          <div class="pm-card__hd"><span class="pm-card__title"><i class="bi bi-activity"></i> Portfolio Growth Over Time</span></div>
          <div class="pm-card__bd">
            <div style="height:260px;background:var(--gray-50);border-radius:var(--r-sm);border:1px dashed var(--gray-300);display:flex;align-items:center;justify-content:center;">
              <canvas id="growthChart"></canvas>
            </div>
          </div>
        </div>
      </div>
      <div class="col-12 col-xl-4">
        <div class="pm-card">
          <div class="pm-card__hd"><span class="pm-card__title"><i class="bi bi-pie-chart-fill"></i> Asset Allocation</span></div>
          <div class="pm-card__bd">
            <div style="height:180px;background:var(--gray-50);border-radius:var(--r-sm);border:1px dashed var(--gray-300);display:flex;align-items:center;justify-content:center;margin-bottom:14px;">
              <canvas id="allocChart"></canvas>
            </div>
            <ul style="list-style:none;padding:0;display:flex;flex-direction:column;gap:7px;">
              <li style="display:flex;align-items:center;gap:7px;font-size:.79rem;"><span style="width:9px;height:9px;border-radius:50%;background:var(--pink-300);flex-shrink:0;"></span><span style="flex:1;color:var(--gray-600);">Stocks (3)</span><span style="font-weight:700;">40.6%</span></li>
              <li style="display:flex;align-items:center;gap:7px;font-size:.79rem;"><span style="width:9px;height:9px;border-radius:50%;background:var(--info);flex-shrink:0;"></span><span style="flex:1;color:var(--gray-600);">Mutual Funds (1)</span><span style="font-weight:700;">1.5%</span></li>
              <li style="display:flex;align-items:center;gap:7px;font-size:.79rem;"><span style="width:9px;height:9px;border-radius:50%;background:var(--warning);flex-shrink:0;"></span><span style="flex:1;color:var(--gray-600);">Cryptocurrency (1)</span><span style="font-weight:700;">57.9%</span></li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- P&L + Holdings Table -->
    <div class="row g-3">
      <div class="col-12 col-xl-6">
        <div class="pm-card">
          <div class="pm-card__hd"><span class="pm-card__title"><i class="bi bi-bar-chart-fill"></i> P&amp;L by Asset</span></div>
          <div class="pm-card__bd">
            <div style="height:200px;background:var(--gray-50);border-radius:var(--r-sm);border:1px dashed var(--gray-300);display:flex;align-items:center;justify-content:center;">
              <canvas id="plChart"></canvas>
            </div>
          </div>
        </div>
      </div>
      <div class="col-12 col-xl-6">
        <div class="pm-card">
          <div class="pm-card__hd"><span class="pm-card__title"><i class="bi bi-table"></i> Top Holdings</span></div>
          <div class="pm-card__bd--bare">
            <div class="pm-tbl-wrap">
              <table class="pm-tbl">
                <thead><tr><th>Asset</th><th>Value</th><th>Weight</th><th>P&amp;L</th></tr></thead>
                <tbody>
                  <tr><td><div style="font-weight:600;">Bitcoin (BTC)</div></td><td>&#8377;16,00,000</td><td><div style="display:flex;align-items:center;gap:6px;"><div style="flex:1;height:5px;background:var(--gray-200);border-radius:99px;overflow:hidden;"><div style="width:78%;height:100%;background:var(--warning);border-radius:99px;"></div></div><span style="font-size:.75rem;">78%</span></div></td><td style="color:var(--success);font-weight:600;">+&#8377;2,00,000</td></tr>
                  <tr><td><div style="font-weight:600;">TCS</div></td><td>&#8377;1,92,500</td><td><div style="display:flex;align-items:center;gap:6px;"><div style="flex:1;height:5px;background:var(--gray-200);border-radius:99px;overflow:hidden;"><div style="width:9%;height:100%;background:var(--pink-300);border-radius:99px;"></div></div><span style="font-size:.75rem;">9%</span></div></td><td style="color:var(--success);font-weight:600;">+&#8377;32,500</td></tr>
                  <tr><td><div style="font-weight:600;">Infosys</div></td><td>&#8377;1,29,600</td><td><div style="display:flex;align-items:center;gap:6px;"><div style="flex:1;height:5px;background:var(--gray-200);border-radius:99px;overflow:hidden;"><div style="width:6%;height:100%;background:var(--pink-300);border-radius:99px;"></div></div><span style="font-size:.75rem;">6%</span></div></td><td style="color:var(--success);font-weight:600;">+&#8377;13,600</td></tr>
                  <tr><td><div style="font-weight:600;">Reliance</div></td><td>&#8377;86,700</td><td><div style="display:flex;align-items:center;gap:6px;"><div style="flex:1;height:5px;background:var(--gray-200);border-radius:99px;overflow:hidden;"><div style="width:4%;height:100%;background:var(--pink-300);border-radius:99px;"></div></div><span style="font-size:.75rem;">4%</span></div></td><td style="color:var(--success);font-weight:600;">+&#8377;8,700</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>"""

ANALYTICS = page_wrapper("Analytics", "", "analytics.html", ANALYTICS_BODY,
  """  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js"></script>
  <script src="js/analytics.js"></script>""")

# ─── PROFILE ──────────────────────────────────────────────────────────────────

PROFILE_BODY = """    <div class="pm-page-header">
      <div>
        <div class="pm-breadcrumb"><a href="dashboard.html">Dashboard</a><span class="sep">/</span><span class="current">Profile</span></div>
        <h1 class="pm-page-title">Profile</h1>
        <p class="pm-page-sub">Manage your account details and preferences.</p>
      </div>
    </div>

    <!-- Profile Header -->
    <div class="pm-profile-hd mb-4">
      <div class="pm-profile-av">JD</div>
      <div style="flex:1;">
        <div style="font-size:1.1rem;font-weight:800;color:var(--gray-900);">Jane Doe</div>
        <div style="font-size:.83rem;color:var(--gray-500);margin-bottom:8px;">jane@example.com &bull; Joined January 2024</div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;">
          <span class="pm-badge pm-badge--pink"><i class="bi bi-person-check-fill"></i> Active Account</span>
          <span class="pm-badge pm-badge--blue"><i class="bi bi-shield-check-fill"></i> Verified</span>
        </div>
      </div>
      <button class="pm-btn pm-btn--outline" data-bs-toggle="modal" data-bs-target="#avatarModal"><i class="bi bi-camera-fill"></i> Change Photo</button>
    </div>

    <div class="row g-3">
      <!-- Personal Info -->
      <div class="col-12 col-xl-6">
        <div class="pm-card">
          <div class="pm-card__hd">
            <span class="pm-card__title"><i class="bi bi-person-fill"></i> Personal Information</span>
            <button class="pm-btn pm-btn--outline pm-btn--sm" id="editProfileBtn"><i class="bi bi-pencil-fill"></i> Edit</button>
          </div>
          <div class="pm-card__bd">
            <form id="profileForm">
              <div class="row g-3">
                <div class="col-6">
                  <label class="pm-label">First Name</label>
                  <input type="text" class="pm-input" value="Jane" id="pFirstName" readonly/>
                </div>
                <div class="col-6">
                  <label class="pm-label">Last Name</label>
                  <input type="text" class="pm-input" value="Doe" id="pLastName" readonly/>
                </div>
                <div class="col-12">
                  <label class="pm-label">Email Address</label>
                  <input type="email" class="pm-input" value="jane@example.com" id="pEmail" readonly/>
                </div>
                <div class="col-12">
                  <label class="pm-label">Phone Number</label>
                  <input type="tel" class="pm-input" value="+91 98765 43210" id="pPhone" readonly/>
                </div>
              </div>
              <div class="mt-3 d-none" id="saveRow">
                <button type="submit" class="pm-btn pm-btn--primary"><i class="bi bi-check-lg"></i> Save Changes</button>
                <button type="button" class="pm-btn pm-btn--outline ms-2" id="cancelEdit">Cancel</button>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- Change Password -->
      <div class="col-12 col-xl-6">
        <div class="pm-card">
          <div class="pm-card__hd"><span class="pm-card__title"><i class="bi bi-lock-fill"></i> Change Password</span></div>
          <div class="pm-card__bd">
            <form id="pwdForm">
              <div class="mb-3"><label class="pm-label">Current Password <span class="req">*</span></label><input type="password" class="pm-input" placeholder="Enter current password" required/></div>
              <div class="mb-3"><label class="pm-label">New Password <span class="req">*</span></label><input type="password" class="pm-input" placeholder="Min. 8 characters" required minlength="8"/></div>
              <div class="mb-4"><label class="pm-label">Confirm New Password <span class="req">*</span></label><input type="password" class="pm-input" placeholder="Repeat new password" required/></div>
              <button type="submit" class="pm-btn pm-btn--primary"><i class="bi bi-shield-lock-fill"></i> Update Password</button>
            </form>
          </div>
        </div>
      </div>

      <!-- Account Stats -->
      <div class="col-12 col-xl-6">
        <div class="pm-card">
          <div class="pm-card__hd"><span class="pm-card__title"><i class="bi bi-bar-chart-fill"></i> Account Summary</span></div>
          <div class="pm-card__bd">
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
              <div style="background:var(--gray-50);border:1px solid var(--gray-200);border-radius:var(--r-sm);padding:12px;"><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:3px;">Portfolios</div><div style="font-weight:800;font-size:1.2rem;">3</div></div>
              <div style="background:var(--gray-50);border:1px solid var(--gray-200);border-radius:var(--r-sm);padding:12px;"><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:3px;">Investments</div><div style="font-weight:800;font-size:1.2rem;">5</div></div>
              <div style="background:var(--gray-50);border:1px solid var(--gray-200);border-radius:var(--r-sm);padding:12px;"><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:3px;">Transactions</div><div style="font-weight:800;font-size:1.2rem;">5</div></div>
              <div style="background:var(--gray-50);border:1px solid var(--gray-200);border-radius:var(--r-sm);padding:12px;"><div style="font-size:.7rem;color:var(--gray-500);text-transform:uppercase;letter-spacing:.7px;margin-bottom:3px;">Member Since</div><div style="font-weight:800;font-size:.9rem;">Jan 2024</div></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Danger Zone -->
      <div class="col-12 col-xl-6">
        <div class="pm-card" style="border-color:#FFCDD2;">
          <div class="pm-card__hd" style="background:#FFF8F8;"><span class="pm-card__title" style="color:var(--danger);"><i class="bi bi-exclamation-triangle-fill" style="color:var(--danger);"></i> Danger Zone</span></div>
          <div class="pm-card__bd">
            <p style="font-size:.83rem;color:var(--gray-600);margin-bottom:14px;">These actions are irreversible. Please proceed with caution.</p>
            <div style="display:flex;flex-direction:column;gap:10px;">
              <div style="display:flex;align-items:center;justify-content:space-between;padding:12px;border:1px solid var(--gray-300);border-radius:var(--r-sm);">
                <div><div style="font-size:.83rem;font-weight:600;color:var(--gray-800);">Export All Data</div><div style="font-size:.75rem;color:var(--gray-500);">Download all your portfolio data as CSV</div></div>
                <button class="pm-btn pm-btn--outline pm-btn--sm"><i class="bi bi-download"></i> Export</button>
              </div>
              <div style="display:flex;align-items:center;justify-content:space-between;padding:12px;border:1px solid #FFCDD2;border-radius:var(--r-sm);background:#FFF8F8;">
                <div><div style="font-size:.83rem;font-weight:600;color:var(--danger);">Delete Account</div><div style="font-size:.75rem;color:var(--gray-500);">Permanently delete your account and all data</div></div>
                <button class="pm-btn pm-btn--danger pm-btn--sm"><i class="bi bi-trash3-fill"></i> Delete</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>"""

PROFILE = page_wrapper("Profile", "", "profile.html", PROFILE_BODY, "")

# ─── WRITE ALL FILES ──────────────────────────────────────────────────────────

files = {
    "index.html":        INDEX,
    "login.html":        LOGIN,
    "signup.html":       SIGNUP,
    "dashboard.html":    DASHBOARD,
    "portfolios.html":   PORTFOLIOS,
    "investments.html":  INVESTMENTS,
    "transactions.html": TRANSACTIONS,
    "analytics.html":    ANALYTICS,
    "profile.html":      PROFILE,
}

for filename, content in files.items():
    path = os.path.join(BASE, filename)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print(f"  written: {filename}")

print(f"\nAll {len(files)} HTML files generated successfully.")
