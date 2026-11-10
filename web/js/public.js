// Public pages: landing, log in, register and certificate check (wireframes 1 to 3 and 11).
import { api, esc, $, $$, go, toast, busy, formData, loadMe, loadMeta, state, store, wordmark, homePath, icon } from "./core.js";
import { photoBackdrop, photoCredit, runSlideshow } from "./photos.js";

function publicLayout(content, { dark = false } = {}) {
  const head = `<header class="pub-head">${wordmark("#/")}
    <nav class="row" style="gap:6px">${state.user
      ? `<a class="btn sm" href="#${homePath(state.user)}">Go to my dashboard</a>`
      : `<a class="btn ghost sm" href="#/verify">Verify a certificate</a><a class="btn sm" href="#/login">Log in</a>`}</nav></header>`;
  const foot = `<footer class="pub-foot"><div class="inner"><span>Youth Empowerment Project. Learn, connect, grow.</span>
    <span>Semester assignment demo, Cavendish University Uganda, 2026</span></div></footer>`;
  return dark ? content.replace("<!--HEAD-->", head) + foot : `<div class="public">${head}${content}${foot}</div>`;
}

/**
 * Log in and create account: the same photographs as the landing page, with the form on a card.
 * aside = { title, lead, switchText, switchHref, switchLabel, start }
 */
function authScreen(card, a) {
  return `<div class="auth-screen photo-screen">
    ${photoBackdrop(a.start)}
    <header class="auth-nav">
      <a class="lp-brand" href="#/" aria-label="YEP Youth Empowerment Project, home"><span class="lp-mark"><img src="img/yep-symbol.png" alt="" width="240" height="140"></span>YEP</a>
      <p class="auth-switch">${a.switchText} <a href="${a.switchHref}">${a.switchLabel}</a></p>
    </header>
    <div class="auth-body">
      <div class="auth-intro">
        <p class="auth-title">${a.title}</p>
        <p class="auth-lead">${a.lead}</p>
      </div>
      <div class="auth-card">${card}</div>
    </div>
    <footer class="auth-foot"><span>Youth Empowerment Project. Learn, connect, grow.</span><span class="auth-credit"></span></footer>
  </div>`;
}

function startAuthPhotos(root, start) {
  const credit = root.querySelector(".auth-credit");
  runSlideshow(root.querySelector(".auth-screen"), { start, onChange: (i, s) => { credit.innerHTML = photoCredit(s); } });
}

export async function login(root) {
  if (state.user) return go(homePath(state.user));
  const meta = await loadMeta();
  root.innerHTML = authScreen(`
    <div class="panel stack">
      <div class="auth-head"><div><h1>Log in</h1><p>Welcome back to YEP</p></div></div>
      <div id="login-error" class="notice error" role="alert" hidden></div>
      <form id="login-form" class="stack" novalidate>
        <label class="field"><span>Email</span><input class="input" name="email" type="email" autocomplete="username" required></label>
        <label class="field"><span>Password</span><input class="input" name="password" type="password" autocomplete="current-password" required></label>
        <button class="btn primary block" type="submit">Log in</button>
        <p class="tiny muted" style="text-align:center">The account is locked after 5 wrong attempts.</p>
      </form>
    </div>
    <div class="panel flat" style="margin-top:14px">
      <h3>Try a demo account</h3>
      <p class="small muted">Tap one to fill in the form. Every demo password is <b>${esc(meta.demoPassword)}</b>.</p>
      <div class="demo-accounts">${meta.demoAccounts.map((a) => `<button type="button" data-email="${esc(a.email)}"><b>${esc(a.name)}</b><span>${esc(a.role)}. ${esc(a.note)}</span></button>`).join("")}</div>
    </div>`, {
    title: "Pick up where you left off.",
    lead: "Your lessons, your team and your milestones are waiting. Everything you finish counts towards your certificate.",
    switchText: "New to YEP?", switchHref: "#/register", switchLabel: "Create an account", start: 1,
  });
  startAuthPhotos(root, 1);

  const form = $("#login-form");
  $$(".demo-accounts button").forEach((b) => b.addEventListener("click", () => {
    form.email.value = b.dataset.email;
    form.password.value = meta.demoPassword;
    form.querySelector("button[type=submit]").focus();
  }));
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const err = $("#login-error");
    err.hidden = true;
    const data = formData(form);
    if (!data.email || !data.password) { err.textContent = "Enter your email and password."; err.hidden = false; return; }
    const btn = form.querySelector("button[type=submit]");
    btn.disabled = true;
    try {
      await api("/api/auth/login", { method: "POST", body: data });
      await loadMe();
      toast("Welcome back, " + state.user.name.split(" ")[0]);
      const next = store.get("afterLogin");
      store.remove("afterLogin");
      go(next && next !== "/login" ? next : homePath(state.user));
    } catch (ex) {
      err.textContent = ex.message;
      err.hidden = false;
      form.password.value = "";
      form.password.focus();
    } finally { btn.disabled = false; }
  });
}

export async function register(root, { query }) {
  if (state.user) return go(homePath(state.user));
  const meta = await loadMeta();
  const role = query.role || "YouthParticipant";
  root.innerHTML = authScreen(`
    <div class="panel stack">
      <div class="auth-head"><div><h1>Create an account</h1><p>Youth accounts open immediately. Other accounts are checked by an administrator first.</p></div></div>
      <div id="reg-msg" role="alert" hidden></div>
      <form id="reg-form" class="stack" novalidate>
        <label class="field"><span>Full name</span><input class="input" name="fullName" autocomplete="name" required></label>
        <label class="field"><span>Email</span><input class="input" name="email" type="email" autocomplete="email" required></label>
        <label class="field"><span>Password</span><input class="input" name="password" type="password" autocomplete="new-password" minlength="8" required><small>At least 8 characters. A short sentence works well.</small></label>
        <div class="grid-2" style="grid-template-columns:1fr 1fr">
          <label class="field"><span>Age</span><input class="input" name="age" type="number" inputmode="numeric" min="16" max="99" required></label>
          <label class="field"><span>Country</span><select class="input" name="country">${meta.countries.map((c) => `<option>${esc(c)}</option>`).join("")}</select></label>
        </div>
        <label class="field"><span>I am a</span><select class="input" name="role">${meta.roles.map((r) => `<option value="${r.value}" ${r.value === role ? "selected" : ""}>${esc(r.label)}</option>`).join("")}</select></label>
        <label class="field"><span>Organisation <span class="muted" style="font-weight:400">(optional for youth)</span></span>
          <input class="input" name="organization" list="orgs" placeholder="University, company or organisation"><datalist id="orgs">${meta.organizations.map((o) => `<option value="${esc(o.name)}">`).join("")}</datalist></label>
        <label class="check"><input type="checkbox" name="agree" value="yes"><span>I agree to the privacy policy. YEP collects only what it needs to run the programme and never shows my email to employers.</span></label>
        <button class="btn primary block" type="submit">Create account</button>
      </form>
    </div>`, {
    title: "Your first lesson is a few minutes away.",
    lead: "Short lessons on AI, media literacy and online safety, a real project with young people from other countries, and a certificate employers can check.",
    switchText: "Already registered?", switchHref: "#/login", switchLabel: "Log in", start: 0,
  });
  startAuthPhotos(root, 0);

  const form = $("#reg-form");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const msg = $("#reg-msg");
    const d = formData(form);
    const body = { ...d, age: parseInt(d.age, 10), agree: !!d.agree };
    const problem = !d.fullName || !d.email || !d.password ? "Please fill in all required fields"
      : d.password.length < 8 ? "Password must have at least 8 characters"
      : d.role === "YouthParticipant" && (body.age < 18 || body.age > 35) ? "Only youth aged 18 to 35 can register as participants"
      : !body.agree ? "Please agree to the privacy policy to continue" : null;
    if (problem) { msg.className = "notice error"; msg.textContent = problem; msg.hidden = false; return; }
    await busy(form.querySelector("button[type=submit]"), async () => {
      try {
        const res = await api("/api/auth/register", { method: "POST", body });
        if (res.active) {
          await api("/api/auth/login", { method: "POST", body: { email: d.email, password: d.password } });
          await loadMe();
          toast(res.message);
          go("/home");
        } else {
          root.querySelector(".auth-card .panel").innerHTML = `<div class="stack" style="text-align:center">
            <div class="avatar lg" style="margin:0 auto;background:var(--green-soft)">${icon.check}</div>
            <h1 style="font-size:28px">Application received</h1><p>${esc(res.message)}</p><p class="muted small">You will get a notification as soon as it is approved.</p>
            <a class="btn" href="#/">Back to the home page</a></div>`;
        }
      } catch (ex) {
        msg.className = "notice error"; msg.textContent = ex.message; msg.hidden = false; msg.scrollIntoView({ block: "center" });
      }
    });
  });
}

/** Certificate check. Public, and also inside the app for employers and staff. */
export async function verify(root, { query }) {
  const meta = await loadMeta();
  const inner = `<div class="${state.user ? "" : "auth-wrap"}"><div class="${state.user ? "" : "auth-card"}" style="${state.user ? "max-width:620px" : ""}">
    <div class="panel stack">
      <div><h1 style="font-size:30px">Verify a certificate</h1><p class="muted" style="margin-top:6px">Enter the code printed on a YEP certificate to confirm it is genuine.</p></div>
      <form id="verify-form" class="row" style="align-items:flex-end">
        <label class="field grow"><span>Certificate code</span><input class="input" name="code" placeholder="YEP-2026-XXXXXXXX" value="${esc(query.code || "")}" autocapitalize="characters" spellcheck="false"></label>
        <button class="btn primary" type="submit">Verify</button>
      </form>
      <div id="verify-result" aria-live="polite"></div>
      <p class="tiny muted">Demo: try <button class="btn ghost sm" type="button" id="try-demo" style="padding:0 4px;min-height:0">${esc(meta.demoCertificate)}</button></p>
    </div></div></div>`;
  root.innerHTML = state.user ? inner : publicLayout(inner);
  const form = $("#verify-form");
  const check = async () => {
    const code = form.code.value.trim();
    if (!code) { $("#verify-result").innerHTML = `<p class="notice error">Enter a certificate code.</p>`; return; }
    const r = await api("/api/public/verify?code=" + encodeURIComponent(code));
    $("#verify-result").innerHTML = r.valid
      ? `<div class="verify-result ok"><div class="row">${icon.seal}<b>Valid certificate</b></div>
         <p style="margin-top:8px"><b>${esc(r.name)}</b>, ${esc(r.country)}<br>${esc(r.programme)}, ${esc(r.cohort)}<br>Issued ${esc(r.issued)}</p>
         <a class="btn sm" style="margin-top:10px" href="/api/public/certificate/${esc(r.code)}" target="_blank" rel="noopener">View certificate</a></div>`
      : `<div class="verify-result no"><b>Certificate not valid.</b><p>No YEP certificate has the code ${esc(r.code)}. Check the code for typing mistakes.</p></div>`;
  };
  form.addEventListener("submit", (e) => { e.preventDefault(); check().catch((ex) => toast(ex.message, true)); });
  $("#try-demo").addEventListener("click", () => { form.code.value = meta.demoCertificate; check(); });
  if (query.code) check();
}
