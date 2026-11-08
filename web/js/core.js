// Core helpers shared by every screen: API calls, routing, the app shell and small UI pieces.

export const state = { user: null, unread: 0, meta: null };

// ------------------------------------------------------------------ API
export async function api(path, { method = "GET", body } = {}) {
  let res;
  try {
    res = await fetch(path, {
      method,
      headers: body ? { "Content-Type": "application/json" } : {},
      body: body ? JSON.stringify(body) : undefined,
      credentials: "same-origin",
    });
  } catch (e) {
    const err = new Error("You appear to be offline. Your work is saved on this device and you can try again when the connection returns.");
    err.offline = true;
    throw err;
  }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const err = new Error(data.error || "Something went wrong");
    err.status = res.status;
    if (res.status === 401 && state.user && !path.includes("/auth/")) {
      state.user = null;
      toast("Your session has ended. Please log in again.", true);
      go("/login");
    }
    throw err;
  }
  return data;
}

export async function loadMe() {
  const data = await api("/api/me");
  state.user = data.user;
  state.unread = data.unread || 0;
  return state.user;
}

export async function loadMeta() {
  if (!state.meta) state.meta = await api("/api/meta");
  return state.meta;
}

// ------------------------------------------------------------------ small helpers
export const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
export const $ = (sel, root = document) => root.querySelector(sel);
export const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
export const go = (path) => { if (location.hash !== "#" + path) location.hash = path; else router(); };
export const plural = (n, one, many = one + "s") => `${n} ${n === 1 ? one : many}`;

export const store = {
  get(key, fallback = null) { try { const v = localStorage.getItem("yep." + key); return v === null ? fallback : JSON.parse(v); } catch { return fallback; } },
  set(key, value) { try { localStorage.setItem("yep." + key, JSON.stringify(value)); } catch { /* storage not available */ } },
  remove(key) { try { localStorage.removeItem("yep." + key); } catch { /* ignore */ } },
};

let toastTimer;
export function toast(message, isError = false) {
  const el = $("#toast");
  el.textContent = message;
  el.className = "show" + (isError ? " error" : "");
  el.setAttribute("role", isError ? "alert" : "status");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.className = ""; }, isError ? 5200 : 3200);
}

/** Runs an async action while showing a spinner on the button, and shows errors as a toast. */
export async function busy(button, fn) {
  const original = button ? button.innerHTML : "";
  if (button) { button.disabled = true; button.innerHTML = `<span class="spin" aria-hidden="true"></span>${original}`; }
  try { return await fn(); }
  catch (e) { toast(e.message, true); return undefined; }
  finally { if (button && button.isConnected) { button.disabled = false; button.innerHTML = original; } }
}

export function formData(form) {
  const out = {};
  new FormData(form).forEach((v, k) => { out[k] = v; });
  return out;
}

export const loading = () => `<div class="loading" role="status"><span class="spin"></span><span class="sr-only">Loading</span></div>`;

export function chipFor(status) {
  const map = {
    APPROVED: ["approved", "Approved"], SUBMITTED: ["submitted", "Submitted"], OPEN: ["", "Open"],
    NEEDS_CHANGES: ["changes", "Needs changes"], PASSED: ["passed", "Passed"], NOT_PASSED: ["bad", "Not passed"],
    AWAITING_MARKING: ["awaiting", "Awaiting marking"], NOT_STARTED: ["", "Not started"],
    ACTIVE: ["done", "Active"], PENDING: ["pending", "Pending"], LOCKED: ["locked", "Locked"], REJECTED: ["bad", "Rejected"],
    DRAFT: ["", "In progress"], COMPLETED: ["done", "Completed"],
  };
  const [cls, label] = map[status] || ["", status];
  return `<span class="chip ${cls}">${esc(label)}</span>`;
}

// ------------------------------------------------------------------ icons (simple line icons)
const P = (d) => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${d}</svg>`;
export const icon = {
  home: P('<path d="M3 11l9-7 9 7"/><path d="M5 10v10h14V10"/><path d="M10 20v-6h4v6"/>'),
  book: P('<path d="M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2z"/><path d="M4 19V5"/><path d="M9 7h6"/>'),
  team: P('<circle cx="9" cy="8" r="3.2"/><path d="M3 20c.6-3.4 3-5.4 6-5.4s5.4 2 6 5.4"/><circle cx="17.5" cy="9" r="2.4"/><path d="M16.5 14.4c2.4.2 4 1.8 4.5 4.6"/>'),
  card: P('<rect x="3" y="4" width="18" height="16" rx="2.5"/><circle cx="9" cy="11" r="2.4"/><path d="M5.8 16.6c.6-1.6 1.8-2.4 3.2-2.4s2.6.8 3.2 2.4"/><path d="M14.5 10h4M14.5 13.5h3"/>'),
  bell: P('<path d="M6 16V11a6 6 0 1 1 12 0v5l1.5 2h-15z"/><path d="M10 20.5a2.2 2.2 0 0 0 4 0"/>'),
  check: P('<path d="M4 12.5l5 5L20 6.5"/>'),
  clipboard: P('<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4.5h6v3H9z"/><path d="M8.5 12l2 2 4-4"/><path d="M8.5 17h7"/>'),
  plus: P('<path d="M12 5v14M5 12h14"/>'),
  chat: P('<path d="M4 5h16v11H9l-5 4z"/>'),
  search: P('<circle cx="11" cy="11" r="6.5"/><path d="M20 20l-4.2-4.2"/>'),
  shield: P('<path d="M12 3l8 3v6c0 4.6-3.4 8-8 9-4.6-1-8-4.4-8-9V6z"/><path d="M8.5 12l2.5 2.5 4.5-5"/>'),
  chart: P('<path d="M4 20V4"/><path d="M4 20h16"/><rect x="7" y="11" width="3" height="6"/><rect x="12" y="7" width="3" height="10"/><rect x="17" y="13" width="3" height="4"/>'),
  users: P('<circle cx="12" cy="8" r="3.4"/><path d="M5 20c.8-3.8 3.6-6 7-6s6.2 2.2 7 6"/>'),
  spark: P('<path d="M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5L18 18M6 18l2.5-2.5M15.5 8.5L18 6"/>'),
  log: P('<path d="M6 3h9l4 4v14H6z"/><path d="M14 3v5h5"/><path d="M9 12h7M9 16h7"/>'),
  play: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5l11 7-11 7z" fill="currentColor"/></svg>',
  back: P('<path d="M15 5l-7 7 7 7"/>'),
  download: P('<path d="M12 4v11"/><path d="M7 11l5 5 5-5"/><path d="M5 20h14"/>'),
  logout: P('<path d="M15 4h4v16h-4"/><path d="M10 8l-4 4 4 4"/><path d="M6 12h10"/>'),
  close: P('<path d="M6 6l12 12M18 6L6 18"/>'),
  seal: P('<circle cx="12" cy="10" r="6"/><path d="M9 15.5L8 21l4-2 4 2-1-5.5"/><path d="M9.5 10l1.8 1.8L14.8 8"/>'),
  link: P('<path d="M10 14a4 4 0 0 0 5.6 0l3-3a4 4 0 0 0-5.6-5.6l-1 1"/><path d="M14 10a4 4 0 0 0-5.6 0l-3 3a4 4 0 0 0 5.6 5.6l1-1"/>'),
};

export const wordmark = (href = "#/") => `<a class="wordmark" href="${href}" aria-label="YEP Youth Empowerment Project, home">
  <span class="mark"><img src="img/yep-symbol.png" alt="" width="240" height="140"></span>YEP</a>`;

// ------------------------------------------------------------------ the journey path
/** The orange path from the logo, showing Learn, Build and Recognised. progress is 0 to 1. */
export function journey(progress, labels) {
  const id = "j" + Math.random().toString(36).slice(2, 8);
  const reached = (i) => progress >= i / 2 - 0.001;
  const current = progress >= 1 ? 3 : Math.floor(progress * 2 + 0.0001);
  return `<div class="journey" data-progress="${progress}" id="${id}">
    <svg viewBox="0 0 600 110" role="img" aria-label="Progress: ${Math.round(progress * 100)} percent of the YEP journey">
      <path class="track" d="M14 92 C 150 92, 190 30, 300 40 S 470 22, 586 18"/>
      <path class="trail" d="M14 92 C 150 92, 190 30, 300 40 S 470 22, 586 18"/>
      ${[0, 1, 2].map((i) => `<circle class="stop ${reached(i) ? (i === current ? "current" : "reached") : ""}" r="11"/>`).join("")}
    </svg>
    <div class="labels">${labels.map((l) => `<div><b>${esc(l[0])}</b>${esc(l[1])}</div>`).join("")}</div>
  </div>`;
}

/** Places the three stops on the curve and animates the orange trail. Call after the HTML is on the page. */
export function animateJourneys(root = document) {
  $$(".journey", root).forEach((el) => {
    const trail = $(".trail", el);
    if (!trail) return;
    const len = trail.getTotalLength();
    const stops = $$(".stop", el);
    [0, 0.5, 1].forEach((f, i) => {
      const pt = trail.getPointAtLength(len * f);
      stops[i].setAttribute("cx", pt.x);
      stops[i].setAttribute("cy", pt.y);
    });
    const p = Math.max(0, Math.min(1, parseFloat(el.dataset.progress) || 0));
    trail.style.strokeDasharray = len;
    trail.style.strokeDashoffset = len;
    requestAnimationFrame(() => requestAnimationFrame(() => { trail.style.strokeDashoffset = len * (1 - p); }));
  });
}

// ------------------------------------------------------------------ app shell
const NAV = {
  YouthParticipant: [["/home", "Home", "home"], ["/learn", "Learn", "book"], ["/team", "Team", "team"], ["/portfolio", "Portfolio", "card"]],
  Facilitator: [["/facilitator", "Grading", "clipboard"], ["/facilitator/content", "Content", "plus"], ["/facilitator/teams", "Teams", "team"], ["/facilitator/comments", "Comments", "chat"]],
  Mentor: [["/mentor", "My teams", "team"], ["/verify", "Verify", "shield"]],
  Employer: [["/talent", "Find talent", "search"], ["/verify", "Verify", "shield"]],
  UniversityRep: [["/reports", "Reports", "chart"], ["/verify", "Verify", "shield"]],
  Partner: [["/reports", "Reports", "chart"], ["/verify", "Verify", "shield"]],
  Administrator: [["/admin", "Users", "users"], ["/admin/mentors", "Mentors", "team"], ["/admin/ai", "AI rules", "spark"], ["/admin/audit", "Audit log", "log"], ["/reports", "Reports", "chart"]],
};

export function homePath(user) {
  return { home: "/home", facilitator: "/facilitator", mentor: "/mentor", talent: "/talent", reports: "/reports", admin: "/admin" }[user.home] || "/home";
}

export function shell(path, content) {
  const u = state.user;
  const items = NAV[u.role] || [];
  const isCurrent = (p) => path === p || (p !== "/admin" && p !== "/facilitator" && path.startsWith(p + "/")) ? ' aria-current="page"' : "";
  const links = items.map(([p, label, ic]) => `<a href="#${p}"${isCurrent(p)}>${icon[ic]}<span>${label}</span></a>`).join("");
  return `<div class="shell with-rail">
    <aside class="rail" aria-label="Main">
      ${wordmark("#" + homePath(u))}
      <nav>${links}</nav>
      <div class="me"><span class="avatar">${esc(u.initials)}</span><div><b>${esc(u.name)}</b><span>${esc(u.roleLabel)}</span></div>
        <button data-action="logout" type="button" aria-label="Log out" title="Log out">${icon.logout}</button></div>
    </aside>
    <header class="topbar">
      <span class="brand-mobile">${wordmark("#" + homePath(u))}</span>
      <span class="who">${esc(u.roleLabel)}${u.organization ? ", " + esc(u.organization) : ""}</span>
      <div class="row" style="gap:8px">
        <button class="iconbtn" data-action="notifications" type="button" aria-label="Notifications${state.unread ? ", " + state.unread + " unread" : ""}">${icon.bell}${state.unread ? `<span class="dot">${state.unread}</span>` : ""}</button>
        <button class="iconbtn mobile-only" data-action="logout" type="button" aria-label="Log out">${icon.logout}</button>
      </div>
    </header>
    <main class="main" id="main" tabindex="-1">${content}</main>
    <nav class="tabbar" aria-label="Main">${links}</nav>
  </div>`;
}

export async function openNotifications() {
  const scrim = document.createElement("div");
  scrim.className = "scrim";
  const drawer = document.createElement("aside");
  drawer.className = "drawer";
  drawer.setAttribute("aria-label", "Notifications");
  drawer.innerHTML = `<header><h2>Notifications</h2><button class="iconbtn" aria-label="Close" data-close>${icon.close}</button></header><div class="items">${loading()}</div>`;
  document.body.append(scrim, drawer);
  requestAnimationFrame(() => drawer.classList.add("open"));
  const close = () => { drawer.classList.remove("open"); setTimeout(() => { drawer.remove(); scrim.remove(); }, 250); document.removeEventListener("keydown", onKey); };
  const onKey = (e) => { if (e.key === "Escape") close(); };
  document.addEventListener("keydown", onKey);
  scrim.onclick = close;
  $("[data-close]", drawer).onclick = close;
  $("[data-close]", drawer).focus();
  try {
    const data = await api("/api/notifications");
    $(".items", drawer).innerHTML = data.items.length
      ? data.items.map((n) => `<a class="item ${n.read ? "" : "unread"}" href="${esc(n.link || "#")}">${esc(n.text)}<br><small>${esc(n.when)}</small></a>`).join("")
      : `<div class="empty"><h3>No notifications yet</h3><p>Updates about your lessons, team and certificates will appear here.</p></div>`;
    $$(".item", drawer).forEach((a) => a.addEventListener("click", close));
    if (data.unread) {
      await api("/api/notifications/read", { method: "POST" });
      state.unread = 0;
      $$(".iconbtn .dot").forEach((d) => d.remove());
    }
  } catch (e) {
    $(".items", drawer).innerHTML = `<p class="notice error">${esc(e.message)}</p>`;
  }
}

export function openSheet(title, html) {
  const dlg = document.createElement("dialog");
  dlg.className = "sheet";
  dlg.innerHTML = `<div class="sheet-head"><h2>${esc(title)}</h2><button class="iconbtn" aria-label="Close" data-close>${icon.close}</button></div><div class="sheet-body">${html}</div>`;
  document.body.append(dlg);
  dlg.showModal();
  $("[data-close]", dlg).onclick = () => dlg.close();
  dlg.addEventListener("close", () => dlg.remove());
  dlg.addEventListener("click", (e) => { if (e.target === dlg) dlg.close(); });
  return dlg;
}

// ------------------------------------------------------------------ router
const routes = [];
export function route(pattern, view, { auth = true, roles = null, publicLayout = false } = {}) {
  const keys = [];
  const re = new RegExp("^" + pattern.replace(/:(\w+)/g, (_, k) => { keys.push(k); return "([^/]+)"; }) + "$");
  routes.push({ re, keys, view, auth, roles, publicLayout });
}

export async function router() {
  const raw = location.hash.replace(/^#/, "") || "/";
  const [path, queryString] = raw.split("?");
  const query = Object.fromEntries(new URLSearchParams(queryString || ""));
  const app = $("#app");
  for (const r of routes) {
    const m = path.match(r.re);
    if (!m) continue;
    const params = Object.fromEntries(r.keys.map((k, i) => [k, decodeURIComponent(m[i + 1])]));
    if (r.auth && !state.user) { store.set("afterLogin", raw); return go("/login"); }
    if (r.roles && state.user && !r.roles.includes(state.user.role)) return go(homePath(state.user));
    const ctx = { path, params, query };
    if (state.user && !r.publicLayout) {
      app.innerHTML = shell(path, loading());
      const main = $("#main");
      try { await r.view(main, ctx); } catch (e) { main.innerHTML = `<div class="panel"><p class="notice error">${esc(e.message)}</p></div>`; }
      animateJourneys(main);
    } else {
      try { await r.view(app, ctx); } catch (e) { app.innerHTML = `<p class="notice error" style="margin:20px">${esc(e.message)}</p>`; }
      animateJourneys(app);
    }
    window.scrollTo(0, 0);
    const h = $("h1", app);
    if (h && document.activeElement === document.body) { h.setAttribute("tabindex", "-1"); h.focus({ preventScroll: true }); }
    document.title = (h ? h.textContent.trim() + " | " : "") + "YEP Youth Empowerment Project";
    return;
  }
  go(state.user ? homePath(state.user) : "/");
}

// Global actions available on every page
document.addEventListener("click", async (e) => {
  const el = e.target.closest("[data-action]");
  if (!el) return;
  if (el.dataset.action === "logout") {
    e.preventDefault();
    await api("/api/auth/logout", { method: "POST" }).catch(() => {});
    state.user = null;
    toast("Logged out");
    go("/");
  } else if (el.dataset.action === "notifications") {
    e.preventDefault();
    openNotifications();
  }
});
