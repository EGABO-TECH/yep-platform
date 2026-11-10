// Employer talent search (wireframe 11) and reports for universities, partners and admins (wireframe 12).
import { api, esc, $, $$, toast, busy, state, icon, openSheet, loadMeta, plural } from "./core.js";
import { profileHtml } from "./participant.js";

export async function talent(main, { query }) {
  const meta = await loadMeta();
  main.innerHTML = `<div class="page-head"><div><h1>Find talent</h1><p>Young people who chose to share their portfolio with employers. Verified credentials are listed first.</p></div></div>
  <div class="grid-2 wide-left" style="align-items:start">
    <div class="stack">
      <form class="panel" id="search">
        <div class="grid-2" style="grid-template-columns:1.4fr 1fr">
          <label class="field"><span>Skill</span><input class="input" name="skill" placeholder="For example: data analysis" value="${esc(query.skill || "")}"></label>
          <label class="field"><span>Country</span><select class="input" name="country"><option value="">All</option>${meta.countries.map((c) => `<option ${query.country === c ? "selected" : ""}>${esc(c)}</option>`).join("")}</select></label>
        </div>
        <button class="btn primary block" style="margin-top:14px" type="submit">${icon.search} Search</button>
      </form>
      <p class="small muted" id="count" aria-live="polite"></p>
      <div id="results"></div>
    </div>
    <aside class="panel stack">
      <h3>Verify a certificate</h3><p class="small muted">Enter the code from a candidate's certificate.</p>
      <form id="vf" class="row" style="flex-wrap:nowrap"><input class="input grow" name="code" placeholder="YEP-2026-XXXXXXXX" aria-label="Certificate code" spellcheck="false"><button class="btn">Verify</button></form>
      <div id="vr" aria-live="polite"></div>
      <p class="tiny muted">Demo code: ${esc(meta.demoCertificate)}</p>
    </aside>
  </div>`;

  const run = async () => {
    const f = $("#search");
    const qs = new URLSearchParams({ skill: f.skill.value, country: f.country.value });
    const r = await api("/api/talent?" + qs);
    $("#count").textContent = `${plural(r.count, "portfolio")} found, most verified credentials first`;
    $("#results").innerHTML = r.results.length ? r.results.map((p) => `<div class="talent">
      <span class="avatar lg">${esc(p.initials)}</span>
      <div class="grow"><b>${esc(p.name)}</b><div class="meta">${esc(p.country)}${p.headline ? ". " + esc(p.headline) : ""}</div>
      <div class="meta">${plural(p.credentials.length, "verified credential")}${p.projects.length ? ", " + plural(p.projects.length, "project") : ""}</div></div>
      <button class="btn sm" data-view="${p.id}">View</button></div>`).join("")
      : `<div class="panel empty"><h3>No matches</h3><p>Try a broader skill, such as "design" or "data", or search all countries.</p></div>`;
  };
  $("#search").addEventListener("submit", (e) => { e.preventDefault(); busy(e.submitter, run); });
  $("#results").addEventListener("click", async (e) => {
    const b = e.target.closest("[data-view]");
    if (!b) return;
    await busy(b, async () => { const v = await api("/api/talent/" + b.dataset.view); openSheet(v.name, profileHtml(v)); });
  });
  $("#vf").addEventListener("submit", (e) => {
    e.preventDefault();
    busy(e.submitter, async () => {
      const code = e.target.code.value.trim();
      if (!code) throw new Error("Enter a certificate code");
      const r = await api("/api/public/verify?code=" + encodeURIComponent(code));
      $("#vr").innerHTML = r.valid ? `<div class="verify-result ok"><b>Valid.</b> ${esc(r.name)}, ${esc(r.programme)}, issued ${esc(r.issued)}</div>`
        : `<div class="verify-result no"><b>Not valid.</b> No certificate has this code.</div>`;
    });
  });
  run().catch((e) => toast(e.message, true));
}

export async function reports(main, { query }) {
  const u = state.user;
  const defaultType = u.role === "UniversityRep" ? "institution" : u.role === "Administrator" ? "system" : "outcome";
  const type = query.type || defaultType;
  const qs = new URLSearchParams({ type, from: query.from || "", to: query.to || "" });
  const r = await api("/api/reports?" + qs);
  const tabs = u.role === "Administrator" ? [["system", "System usage"], ["outcome", "Programme outcomes"], ["institution", "Institutions"]]
    : u.role === "UniversityRep" ? [["institution", "My institution"], ["outcome", "Programme outcomes"]] : [["outcome", "Programme outcomes"]];

  const isOutcome = r.columns[0] === "Country";
  const chart = isOutcome ? `<div class="panel"><h3>Completion rate by country</h3><div class="chart" style="margin-top:14px">${r.rows.map((row) => {
      const hidden = row[2] === "-";
      const pct = hidden ? 0 : parseInt(row[2], 10);
      return `<div class="bar-row"><span>${esc(row[0])}</span><span class="bar ${hidden ? "hidden-data" : ""}" role="img" aria-label="${esc(row[0])}: ${hidden ? "hidden, fewer than 5 enrolled" : pct + " percent completed"}"><i style="width:${pct}%"></i></span><span class="val">${hidden ? "Hidden" : pct + "%"}</span></div>`;
    }).join("")}</div><p class="tiny muted" style="margin-top:12px">Countries with fewer than 5 enrolled participants are hidden to protect privacy.</p></div>` : "";

  main.innerHTML = `<div class="page-head"><div><h1>${esc(r.title)}</h1><p>Generated ${esc(r.generated)}${r.organization && type === "institution" ? `. ${esc(r.organization)}` : ""}</p></div>
    <div class="row no-print"><a class="btn" href="/api/reports/export?${qs}">${icon.download} Export CSV</a><button class="btn" onclick="window.print()">Export PDF</button></div></div>
  ${tabs.length > 1 ? `<div class="tabs no-print" role="tablist" style="margin-bottom:16px">${tabs.map(([k, l]) => `<button role="tab" aria-selected="${k === type}" data-type="${k}">${l}</button>`).join("")}</div>` : ""}
  <form class="panel no-print" id="range" style="margin-bottom:16px"><div class="row" style="align-items:flex-end">
    <label class="field grow"><span>From</span><input class="input" type="date" name="from" value="${esc(query.from || "")}"></label>
    <label class="field grow"><span>To</span><input class="input" type="date" name="to" value="${esc(query.to || "")}"></label>
    <button class="btn primary" type="submit">Update</button></div><p class="tiny muted" style="margin-top:8px">Dates filter by cohort start date. Leave empty to include all cohorts.</p></form>
  <div class="stats four" style="margin-bottom:16px">${Object.entries(r.totals).map(([k, v]) => `<div class="stat"><span>${esc(k)}</span><b>${esc(v)}</b></div>`).join("")}</div>
  <div class="${isOutcome ? "grid-2" : ""}" style="align-items:start">
    ${chart}
    <div class="table-wrap" style="background:var(--surface)"><table class="data"><thead><tr>${r.columns.map((c, i) => `<th class="${i > 0 && isOutcome ? "num" : ""}">${esc(c)}</th>`).join("")}</tr></thead>
      <tbody>${r.rows.length ? r.rows.map((row) => `<tr>${row.map((c, i) => `<td class="${i > 0 && isOutcome ? "num" : ""}">${r.columns[i] === "Status" ? statusChip(c) : esc(c)}</td>`).join("")}</tr>`).join("") : `<tr><td colspan="${r.columns.length}" class="muted">No records in this date range.</td></tr>`}</tbody></table></div>
  </div>`;

  const nav = (t, from, to) => { location.hash = "/reports?" + new URLSearchParams({ type: t, from: from || "", to: to || "" }); };
  $$("[data-type]").forEach((b) => b.addEventListener("click", () => nav(b.dataset.type, query.from, query.to)));
  $("#range").addEventListener("submit", (e) => { e.preventDefault(); nav(type, e.target.from.value, e.target.to.value); });
}

/** Status text in report tables shown as a coloured chip. */
function statusChip(text) {
  const t = String(text ?? "");
  const cls = /complet|pass|approved/i.test(t) ? "done" : /progress|enrol/i.test(t) ? "progress" : /withdr|fail|not/i.test(t) ? "bad" : "";
  return `<span class="chip ${cls}">${esc(t)}</span>`;
}
