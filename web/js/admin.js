// Administrator screens (wireframe 13).
import { api, esc, $, $$, toast, busy, icon, chipFor, openSheet, plural } from "./core.js";

export async function users(main, { query }) {
  const status = query.status || "PENDING";
  const q = query.q || "";
  const d = await api(`/api/admin/users?status=${status}&q=${encodeURIComponent(q)}`);
  main.innerHTML = `<div class="page-head"><div><h1>User management</h1><p>Approve staff and organisation accounts, unlock accounts and change roles.</p></div></div>
  <div class="tabs" role="tablist" style="margin-bottom:14px">${[["PENDING", "Pending"], ["ALL", "All users"], ["LOCKED", "Locked"]].map(([k, l]) => `<button role="tab" aria-selected="${k === status}" data-status="${k}">${l} (${d.counts[k]})</button>`).join("")}</div>
  <form id="find" class="panel flat" style="margin-bottom:14px"><label class="field"><span>Search by name or email</span><input class="input" name="q" value="${esc(q)}" type="search"></label></form>
  <div id="list">${d.users.length ? d.users.map((u) => card(u, d.roles)).join("") : `<div class="panel empty"><h3>${status === "PENDING" ? "No accounts waiting" : status === "LOCKED" ? "No locked accounts" : "No users found"}</h3><p>${status === "PENDING" ? "New staff and organisation accounts will appear here for approval." : ""}</p></div>`}</div>`;

  $$("[data-status]").forEach((b) => b.addEventListener("click", () => { location.hash = `/admin?status=${b.dataset.status}`; }));
  let t;
  $("#find").q.addEventListener("input", (e) => { clearTimeout(t); t = setTimeout(() => { location.hash = `/admin?status=${status}&q=${encodeURIComponent(e.target.value)}`; }, 400); });
  $("#find").addEventListener("submit", (e) => e.preventDefault());
  $("#list").addEventListener("click", (e) => {
    const b = e.target.closest("[data-act]");
    if (!b) return;
    const id = b.dataset.id;
    if (b.dataset.act === "role") {
      const sel = $(`select[data-role="${id}"]`);
      return busy(b, async () => { const r = await api(`/api/admin/users/${id}/role`, { method: "POST", body: { role: sel.value } }); toast(r.message); users(main, { query }); });
    }
    busy(b, async () => { const r = await api(`/api/admin/users/${id}/${b.dataset.act}`, { method: "POST" }); toast(r.message); users(main, { query }); });
  });
  if (q) { const i = $("#find").q; i.focus(); i.setSelectionRange(q.length, q.length); }
}

const card = (u, roles) => `<div class="user-card"><span class="avatar">${esc(u.initials)}</span><div class="grow">
  <div class="row between"><b>${esc(u.name)}</b><span class="row" style="gap:6px"><span class="chip">${esc(u.roleLabel)}</span>${chipFor(u.status)}</span></div>
  <p class="small muted">${esc(u.organization || "No organisation")}, ${esc(u.country)}. ${esc(u.email)}. Joined ${esc(u.joined)}</p>
  <div class="actions">
    ${u.status === "PENDING" ? `<button class="btn primary sm" data-act="approve" data-id="${u.id}">Approve</button><button class="btn danger sm" data-act="reject" data-id="${u.id}">Reject</button>
      <span class="row" style="gap:6px"><select class="input" style="min-height:36px;padding:4px 10px;width:auto" data-role="${u.id}" aria-label="New role for ${esc(u.name)}">${roles.map((r) => `<option value="${r.value}" ${r.value === u.role ? "selected" : ""}>${esc(r.label)}</option>`).join("")}</select><button class="btn sm" data-act="role" data-id="${u.id}">Change role</button></span>` : ""}
    ${u.status === "LOCKED" ? `<button class="btn primary sm" data-act="unlock" data-id="${u.id}">Unlock account</button>` : ""}
  </div></div></div>`;

export async function mentors(main) {
  const d = await api("/api/admin/mentoring");
  main.innerHTML = `<div class="page-head"><div><h1>Assign mentors</h1><p>Each mentor can guide up to 3 teams at a time.</p></div></div>
  <div class="grid-2" style="align-items:start">
    <form class="panel stack" id="assign">
      <h3>Assign a mentor to a team</h3>
      <label class="field"><span>Mentor</span><select class="input" name="mentorId">${d.mentors.map((m) => `<option value="${m.id}" ${m.active >= m.max ? "disabled" : ""}>${esc(m.name)} (${m.active} of ${m.max})${m.active >= m.max ? ", full" : ""}</option>`).join("")}</select></label>
      <label class="field"><span>Team</span><select class="input" name="teamId">${d.teams.map((t) => `<option value="${t.id}">${esc(t.name)}, ${esc(t.focusArea)}${t.mentor ? ` (now ${esc(t.mentor)})` : ", no mentor"}</option>`).join("")}</select></label>
      <button class="btn primary" type="submit">Assign</button>
      <p class="tiny muted">The mentor and every team member are notified.</p>
    </form>
    <div class="stack">
      <section class="panel"><h3>Mentors</h3><ul class="list" style="margin-top:10px">${d.mentors.map((m) => `<li><span class="grow"><b>${esc(m.name)}</b><br><span class="small muted">${esc(m.expertise)}</span></span><span class="chip ${m.active >= m.max ? "alert" : ""}">${m.active} of ${m.max} teams</span></li>`).join("")}</ul></section>
      <section class="panel"><h3>Teams</h3><ul class="list" style="margin-top:10px">${d.teams.map((t) => `<li><span class="grow"><b>${esc(t.name)}</b>, ${esc(t.focusArea)}<br><span class="small muted">${plural(t.members, "member")}</span></span>${t.mentor ? `<span class="chip done">${esc(t.mentor)}</span>` : `<span class="chip alert">No mentor</span>`}</li>`).join("")}</ul></section>
    </div></div>`;
  const noMentor = d.teams.find((t) => !t.mentor);
  if (noMentor) $("#assign").teamId.value = noMentor.id;
  $("#assign").addEventListener("submit", (e) => {
    e.preventDefault();
    busy(e.submitter, async () => { const r = await api("/api/admin/mentorships", { method: "POST", body: { mentorId: +e.target.mentorId.value, teamId: +e.target.teamId.value } }); toast(r.message); mentors(main); });
  });
}

export async function ai(main) {
  const g = await api("/api/admin/ai-guidelines");
  main.innerHTML = `<div class="page-head"><div><h1>AI usage guidelines</h1><p>Version ${esc(g.version)}, in force since ${esc(g.effectiveFrom)}. ${plural(g.questionsToday, "question")} asked today.</p></div></div>
  <form class="panel stack" id="g" style="max-width:820px">
    <p class="small muted">The assistant follows these rules for every question. Publishing creates a new version and is recorded in the audit log.</p>
    <div id="rules" class="stack" style="--gap:10px">${g.rules.map((r, i) => rule(r, i)).join("")}</div>
    <button type="button" class="btn ghost" id="add" style="justify-self:start">${icon.plus} Add a rule</button>
    <label class="field" style="max-width:260px"><span>Questions per participant per day</span><input class="input" type="number" min="1" max="200" name="dailyLimit" value="${g.dailyLimit}"></label>
    <button class="btn primary" type="submit">Publish new version</button>
  </form>`;
  $("#add").addEventListener("click", () => { $("#rules").insertAdjacentHTML("beforeend", rule("", $$("#rules textarea").length)); $$("#rules textarea").at(-1).focus(); });
  $("#rules").addEventListener("click", (e) => { const b = e.target.closest("[data-remove]"); if (b) b.closest(".row").remove(); });
  $("#g").addEventListener("submit", (e) => {
    e.preventDefault();
    const rules = $$("#rules textarea").map((t) => t.value);
    busy(e.submitter, async () => { const r = await api("/api/admin/ai-guidelines", { method: "PUT", body: { rules, dailyLimit: +e.target.dailyLimit.value } }); toast(r.message); ai(main); });
  });
}
const rule = (text, i) => `<div class="row" style="flex-wrap:nowrap;align-items:flex-start"><span class="chip" style="margin-top:12px">${i + 1}</span><textarea class="input grow" style="min-height:56px" aria-label="Rule ${i + 1}">${esc(text)}</textarea><button type="button" class="iconbtn" data-remove aria-label="Remove rule ${i + 1}" style="margin-top:4px">${icon.close}</button></div>`;

export async function audit(main) {
  const d = await api("/api/admin/audit");
  main.innerHTML = `<div class="page-head"><div><h1>Audit log</h1><p>Important account, security and grading actions, newest first.</p></div></div>
  <div class="table-wrap" style="background:var(--surface)"><table class="data log"><thead><tr><th>When</th><th>Action</th><th>By</th><th>Details</th></tr></thead>
  <tbody>${d.items.map((l) => `<tr><td>${esc(l.when)}</td><td><code>${esc(l.action)}</code></td><td>${esc(l.user)}</td><td>${esc(l.detail)}</td></tr>`).join("")}</tbody></table></div>`;
}
