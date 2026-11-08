// Landing page. Everything shown here comes from the running system: real tracks, cohort dates and sessions.
import { api, esc, $, go, state, homePath, icon, plural } from "./core.js";

const mark = (cls = "") => `<a class="lp-brand ${cls}" href="#/" aria-label="YEP Youth Empowerment Project, home">
  <img src="img/yep-symbol.png" alt="" width="240" height="140"><span>YEP</span></a>`;

export async function landing(root) {
  if (state.user) return go(homePath(state.user));
  const d = await api("/api/public/programme");
  const totalLessons = d.tracks.reduce((n, t) => n + t.lessons, 0);

  root.innerHTML = `<div class="lp">
  <header class="lp-nav">
    ${mark()}
    <nav aria-label="Main" class="lp-links">
      <a href="#learn" data-scroll>What you learn</a>
      <a href="#levels" data-scroll>How it works</a>
      <a href="#organisations" data-scroll>Organisations</a>
      <a href="#verify" data-scroll>Verify a certificate</a>
    </nav>
    <div class="lp-auth"><a href="#/login" class="lp-login">Log in</a><a href="#/register" class="btn primary sm">Create account</a></div>
  </header>

  <section class="lp-hero" aria-labelledby="lp-title">
    <div class="lp-hero-text">
      <p class="lp-for">For young people aged 18 to 35 in Uganda and across East Africa</p>
      <h1 id="lp-title" class="lp-title">
        <span class="lp-line"><span class="lp-word">Learn<i class="lp-dot"></i></span><span class="lp-note">Level One courses</span></span>
        <span class="lp-line"><span class="lp-word">Connect<i class="lp-dot"></i></span><span class="lp-note">A team and a mentor</span></span>
        <span class="lp-line"><span class="lp-word">Grow<i class="lp-dot"></i></span><span class="lp-note">A certificate employers can check</span></span>
      </h1>
      <p class="lp-lead">Short lessons on AI, media literacy and online safety. Then a real project with young people from other countries, guided by someone who has built things before.</p>
      <div class="lp-cta"><a class="btn primary" href="#/register">Create a free account</a><a class="btn" href="#/login">Log in</a></div>
      <p class="lp-small">${esc(d.cohort.name)} runs from ${esc(d.cohort.start)} to ${esc(d.cohort.end)}.</p>
    </div>
    <figure class="lp-hero-art" aria-hidden="true">
      <img src="img/yep-symbol.png" alt="" width="240" height="140">
    </figure>
  </section>

  ${d.session ? `<aside class="lp-next" aria-label="Next live session">
    <span class="lp-live" aria-hidden="true"></span>
    <p><b>Next live Virtual Exchange:</b> ${esc(d.session.title)}, ${esc(d.session.when)}. ${d.session.duration} minutes, with participants from ${esc(d.session.countries)}.</p>
  </aside>` : ""}

  <section class="lp-section" id="learn" aria-labelledby="learn-title">
    <div class="lp-head">
      <h2 id="learn-title">What you learn in Level One</h2>
      <p>${plural(d.tracks.length, "track")}, ${totalLessons} short lessons. Pass each track's assessment to finish Level One.</p>
    </div>
    <div class="lp-tracks">${d.tracks.map((t) => `
      <article class="lp-track">
        <p class="lp-topic">${esc(t.topic)}</p>
        <h3>${esc(t.title)}</h3>
        <p>${esc(t.summary)}</p>
        <ul>${t.lessonTitles.map((l) => `<li>${esc(l)}</li>`).join("")}</ul>
        <p class="lp-meta">${plural(t.lessons, "lesson")}${t.assessments.map((a) => a.type === "WRITTEN" ? ", a written task" : `, a ${a.minutes}-minute quiz`).join("")}</p>
      </article>`).join("")}
    </div>
  </section>

  <section class="lp-band" id="levels" aria-labelledby="levels-title">
    <div class="lp-band-inner">
      <div class="lp-head light">
        <h2 id="levels-title">From your first lesson to a finished project</h2>
        <p>Every step is recorded, so what you achieve can be checked by anyone you choose to show it to.</p>
      </div>
      <ol class="lp-steps">
        <li><span class="lp-step-dot" aria-hidden="true"></span><h3>Finish Level One</h3><p>Work through the four tracks on your phone, at your own pace, and pass each assessment.</p></li>
        <li><span class="lp-step-dot" aria-hidden="true"></span><h3>Join a team</h3><p>A facilitator places you with up to five others from different countries, around a focus area such as health, agriculture or climate.</p></li>
        <li><span class="lp-step-dot" aria-hidden="true"></span><h3>Build with a mentor</h3><p>Your team submits ${d.milestones.map(esc).join(", ").replace(/, ([^,]*)$/, " and $1")} milestones. A mentor rates each one and tells you what to improve.</p></li>
        <li><span class="lp-step-dot" aria-hidden="true"></span><h3>Show what you did</h3><p>Your certificate has a code anyone can verify, and your portfolio lists the work your mentor approved.</p></li>
      </ol>
    </div>
  </section>

  <section class="lp-section lp-split" aria-labelledby="data-title">
    <div class="lp-head">
      <h2 id="data-title">Made for phones and small data bundles</h2>
      <p>Most young people in Africa go online on a phone, and data is expensive. YEP is built around that.</p>
    </div>
    <ul class="lp-facts">
      <li>${icon.check}<div><b>Lessons open as text first</b><span>Videos only load when you tap play, and you see the size in MB before you do.</span></div></li>
      <li>${icon.check}<div><b>Your work is kept if the network drops</b><span>Quiz answers and project drafts are saved on your phone until you are back online.</span></div></li>
      <li>${icon.check}<div><b>An assistant that knows the lessons</b><span>Ask questions about any lesson. Answers show where they came from, and personal details are removed first.</span></div></li>
      <li>${icon.check}<div><b>You decide who sees your portfolio</b><span>Keep it private, share it with employers on YEP, or make it public. Your email is never shown.</span></div></li>
    </ul>
  </section>

  <section class="lp-section" id="organisations" aria-labelledby="org-title">
    <div class="lp-head">
      <h2 id="org-title">For organisations</h2>
      <p>Staff and organisation accounts are checked by an administrator before they open.</p>
    </div>
    <div class="lp-orgs">
      <a href="#/register?role=Mentor"><b>Mentors</b><span>Guide up to three teams and review their milestones.</span><em>Apply as a mentor</em></a>
      <a href="#/register?role=Employer"><b>Employers</b><span>Search portfolios by skill and country, and verify certificates.</span><em>Register as an employer</em></a>
      <a href="#/register?role=UniversityRep"><b>Universities</b><span>See your students' progress in an institution report.</span><em>Register your university</em></a>
      <a href="#/register?role=Partner"><b>Partners</b><span>Follow completion by country, with small groups kept private.</span><em>Register as a partner</em></a>
    </div>
  </section>

  <section class="lp-section lp-verify" id="verify" aria-labelledby="verify-title">
    <div class="lp-head">
      <h2 id="verify-title">Check a YEP certificate</h2>
      <p>Enter the code printed on the certificate.</p>
    </div>
    <div>
      <form id="lp-verify" class="lp-verify-form">
        <label class="sr-only" for="lp-code">Certificate code</label>
        <input class="input" id="lp-code" name="code" placeholder="YEP-2026-XXXXXXXX" spellcheck="false" autocapitalize="characters">
        <button class="btn primary" type="submit">Verify</button>
      </form>
      <div id="lp-verify-result" aria-live="polite"></div>
    </div>
  </section>

  <section class="lp-final" aria-labelledby="final-title">
    <img src="img/yep-logo.jpg" alt="" class="lp-final-glow" width="960" height="640" loading="lazy">
    <div class="lp-final-inner">
      <h2 id="final-title">Your first lesson takes about ${d.firstLessonMinutes} minutes.</h2>
      <p>Free for young people aged 18 to 35. All you need is a phone and an email address.</p>
      <a class="btn warm" href="#/register">Create a free account</a>
    </div>
  </section>

  <footer class="lp-foot">
    <div class="lp-foot-inner">
      <div>${mark("small")}<p class="tagline" style="margin-top:10px">Learn <i></i> Connect <i></i> Grow</p></div>
      <nav aria-label="Footer"><a href="#/login">Log in</a><a href="#/register">Create account</a><a href="#/verify">Verify a certificate</a></nav>
      <p class="lp-credit">Semester assignment prototype, Cavendish University Uganda, 2026</p>
    </div>
  </footer>
</div>`;

  root.querySelectorAll("[data-scroll]").forEach((a) => a.addEventListener("click", (e) => {
    e.preventDefault();
    document.querySelector(a.getAttribute("href"))?.scrollIntoView({ behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" });
  }));

  $("#lp-verify").addEventListener("submit", async (e) => {
    e.preventDefault();
    const out = $("#lp-verify-result");
    const code = e.target.code.value.trim();
    if (!code) { out.innerHTML = `<p class="notice error" style="margin-top:12px">Enter a certificate code.</p>`; return; }
    const r = await api("/api/public/verify?code=" + encodeURIComponent(code));
    out.innerHTML = r.valid
      ? `<div class="verify-result ok"><b>Valid certificate.</b> ${esc(r.name)}, ${esc(r.country)}. ${esc(r.programme)}, issued ${esc(r.issued)}. <a href="/api/public/certificate/${esc(r.code)}" target="_blank" rel="noopener">View certificate</a></div>`
      : `<div class="verify-result no"><b>Not valid.</b> No YEP certificate has the code ${esc(r.code)}. Check for typing mistakes.</div>`;
  });

  requestAnimationFrame(() => root.querySelector(".lp")?.classList.add("is-in"));
}
