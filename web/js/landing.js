// Landing page: one screen. The offer, one action, and a working certificate check over
// photographs of young people in Lagos, Accra and Nairobi. Cohort details come from the running system.
import { api, esc, $, go, state, homePath, loadMeta } from "./core.js";
import { SLIDES, photoBackdrop, photoCredit, runSlideshow } from "./photos.js";


export async function landing(root) {
  if (state.user) return go(homePath(state.user));
  const [d, meta] = await Promise.all([api("/api/public/programme"), loadMeta().catch(() => null)]);
  const totalLessons = d.tracks.reduce((n, t) => n + t.lessons, 0);

  root.innerHTML = `<div class="lp photo-screen">
  ${photoBackdrop(0)}

  <header class="lp-nav">
    <a class="lp-brand" href="#/" aria-label="YEP Youth Empowerment Project, home"><span class="lp-mark"><img src="img/yep-symbol.png" alt="" width="240" height="140"></span>YEP</a>
    <nav class="lp-auth" aria-label="Account"><a href="#/login" class="lp-login">Log in</a><a href="#/register" class="btn warm sm">Create account</a></nav>
  </header>

  <main class="lp-body">
    <p class="lp-for">Youth Empowerment Project &middot; ages 18 to 35, Uganda and East Africa</p>
    <h1 class="lp-title" aria-label="Learn. Connect. Grow.">
      ${SLIDES.map((s, i) => `<span class="lp-word${i === 0 ? " is-on" : ""}">${s.word}<i class="lp-dot"></i></span>`).join("")}
    </h1>
    <p class="lp-lead">Free short courses on AI, media literacy and online safety, then a real project with young people from other countries, guided by a mentor.</p>
    <div class="lp-cta">
      <a class="btn warm lg" href="#/register">Create a free account</a>
      <a class="btn lg lp-ghost" href="#/login">Log in</a>
    </div>

    <form id="lp-verify" class="lp-verify" novalidate>
      <label for="lp-code">Check a YEP certificate</label>
      <div class="lp-verify-row">
        <input class="input" id="lp-code" name="code" placeholder="YEP-2026-XXXXXXXX" spellcheck="false" autocapitalize="characters" autocomplete="off">
        <button class="btn" type="submit">Verify</button>
      </div>
      <div id="lp-verify-result" aria-live="polite">${meta?.demoCertificate ? `<p class="lp-hint">Try the demo code <button type="button" id="lp-try">${esc(meta.demoCertificate)}</button></p>` : ""}</div>
    </form>
  </main>

  <footer class="lp-foot">
    <p class="lp-facts">${esc(d.cohort.name)}: ${esc(d.cohort.start)} to ${esc(d.cohort.end)} &middot; ${d.tracks.length} tracks, ${totalLessons} short lessons</p>
    <p class="lp-credit" id="lp-credit"></p>
  </footer>
</div>`;

  // ---- certificate check
  const out = $("#lp-verify-result");
  const form = $("#lp-verify");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const code = form.code.value.trim();
    if (!code) { out.innerHTML = `<p class="lp-result no">Enter the code printed on the certificate.</p>`; form.code.focus(); return; }
    try {
      const r = await api("/api/public/verify?code=" + encodeURIComponent(code));
      out.innerHTML = r.valid
        ? `<p class="lp-result ok"><b>Valid.</b> ${esc(r.name)}, ${esc(r.country)}. ${esc(r.programme)}, issued ${esc(r.issued)}. <a href="/api/public/certificate/${esc(r.code)}" target="_blank" rel="noopener">View certificate</a></p>`
        : `<p class="lp-result no"><b>Not valid.</b> No YEP certificate has the code ${esc(r.code)}. Check it for typing mistakes.</p>`;
    } catch (ex) {
      out.innerHTML = `<p class="lp-result no">${esc(ex.message)}</p>`;
    }
  });
  $("#lp-try")?.addEventListener("click", () => { form.code.value = meta.demoCertificate; form.requestSubmit(); });

  // ---- background photographs: the matching word of the tagline lights up
  const words = [...root.querySelectorAll(".lp-word")];
  const credit = $("#lp-credit");
  runSlideshow(root.querySelector(".lp"), {
    onChange: (i, s) => {
      words.forEach((el, k) => el.classList.toggle("is-on", k === i));
      credit.innerHTML = photoCredit(s);
    },
  });
}
