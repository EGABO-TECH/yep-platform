// Facilitator and mentor screens (wireframes 9 and 10).
import { api, esc, $, $$, go, toast, busy, formData, icon, chipFor, plural } from "./core.js";

const TABS = [["", "Grading"], ["content", "Content"], ["teams", "Teams"], ["comments", "Comments"]];

export async function facilitator(main, { params }) {
  const tab = params.tab || "";
  const d = await api("/api/facilitator/overview");
  const head = `<div class="page-head"><div><h1>${{ "": "Submissions to grade", content: "Add learning content", teams: "Form Level Two teams", comments: "Reported comments" }[tab]}</h1>
    <p>${{ "": "Mark written tasks. Participants are notified as soon as you save.", content: "Publish a lesson to a learning track. Text loads first to save data.", teams: "Participants who passed Level One and are not yet in a team.", comments: "Comments reported by participants. Hide them or put them back." }[tab]}</p></div></div>
  <div class="stats" style="margin-bottom:18px">
    <div class="stat"><span>Active this week</span><b>${d.stats.active}</b></div>
    <div class="stat ${d.stats.toGrade ? "hot" : ""}"><span>To grade</span><b>${d.stats.toGrade}</b></div>
    <div class="stat ${d.stats.flagged ? "hot" : ""}"><span>Reported comments</span><b>${d.stats.flagged}</b></div>
  </div>`;

  if (tab === "") {
    main.innerHTML = head + `<div class="grid-2 wide-left"><div id="grade-list" class="stack" style="--gap:14px">${d.toGrade.length ? d.toGrade.map(gradeCard).join("") : `<div class="panel empty"><h3>All caught up</h3><p>No written work is waiting to be marked.</p></div>`}</div>
      <aside class="panel"><h3>Track completion</h3><p class="small muted" style="margin-top:4px">Share of the ${d.stats.enrolled} enrolled participants who finished each track.</p>
        <div class="chart" style="margin-top:14px">${Object.entries(d.trackCompletion).map(([k, v]) => `<div class="bar-row" style="grid-template-columns:1fr 46px"><span class="small"><b>${esc(k)}</b><span class="bar" style="display:block;margin-top:6px"><i style="width:${v}%"></i></span></span><span class="val">${v}%</span></div>`).join("")}</div>
        <p class="small" style="margin-top:14px">${d.stats.completed} of ${d.stats.enrolled} have completed Level One.</p></aside></div>`;
    $$(".grade-form").forEach((form) => {
      const score = form.score, result = form.querySelector(".result"), max = +form.dataset.max;
      score.addEventListener("input", () => {
        const v = parseFloat(score.value);
        result.innerHTML = isNaN(v) ? "" : v < 0 || v > max ? `<span class="chip bad">Enter 0 to ${max}</span>` : v / max >= 0.5 ? `<span class="chip passed">Pass</span>` : `<span class="chip bad">Not passed</span>`;
      });
      form.addEventListener("submit", (e) => {
        e.preventDefault();
        busy(form.querySelector("button[type=submit]"), async () => {
          const r = await api(`/api/submissions/${form.dataset.id}/grade`, { method: "POST", body: { score: parseFloat(score.value), feedback: form.feedback.value } });
          toast(r.message);
          facilitator(main, { params });
        });
      });
    });
  } else if (tab === "content") {
    main.innerHTML = head + `<form class="panel stack" id="content-form" style="max-width:760px">
      <label class="field"><span>Learning track</span><select class="input" name="track">${d.tracks.map((t) => `<option value="${t.id}">${esc(t.title)} (${plural(t.lessons, "lesson")})</option>`).join("")}</select></label>
      <label class="field"><span>Lesson title</span><input class="input" name="title" required maxlength="90"></label>
      <label class="field"><span>Lesson text</span><textarea class="input" name="body" style="min-height:220px" placeholder="Write short paragraphs. Leave an empty line between paragraphs." required></textarea><small>Short paragraphs are easier to read on a phone.</small></label>
      <label class="field"><span>Optional video size in MB</span><input class="input" name="videoSizeMb" type="number" min="0" max="200" step="0.1" value="0"><small>Learners see the size before they choose to play it.</small></label>
      <button class="btn primary" type="submit">Publish lesson</button></form>`;
    $("#content-form").addEventListener("submit", (e) => {
      e.preventDefault();
      const f = formData(e.target);
      busy(e.target.querySelector("button"), async () => {
        const r = await api(`/api/tracks/${f.track}/lessons`, { method: "POST", body: { title: f.title, body: f.body, videoSizeMb: parseFloat(f.videoSizeMb) || 0 } });
        toast(r.message + ". Enrolled participants were notified.");
        e.target.reset();
      });
    });
  } else if (tab === "teams") {
    main.innerHTML = head + `<div class="grid-2 wide-left">
      <form class="panel stack" id="team-form">
        <div class="row between"><h3>Eligible participants (${d.eligible.length})</h3>${d.eligible.length ? `<button type="button" class="btn ghost sm" id="all">Select all</button>` : ""}</div>
        ${d.eligible.length ? `<div class="picks">${d.eligible.map((p) => `<label class="pick"><input type="checkbox" name="p" value="${p.id}"><span class="avatar" style="width:34px;height:34px;font-size:13px">${esc(p.initials)}</span><span class="grow"><b>${esc(p.name)}</b><br><span class="tiny muted">${esc(p.country)}${p.skills.length ? ", " + esc(p.skills.slice(0, 2).join(", ")) : ""}</span></span></label>`).join("")}</div>
        <label class="field"><span>Focus area</span><select class="input" name="focusArea">${d.focusAreas.map((f) => `<option>${esc(f)}</option>`).join("")}</select></label>
        <p class="small muted" id="team-preview">Select at least 4 participants. Teams have up to 6 members, mixed by country. The first member placed in each team becomes its lead.</p>
        <button class="btn primary" type="submit">Form teams</button>`
        : `<div class="empty"><h3>No one is waiting</h3><p>Participants appear here after they pass all Level One assessments.</p></div>`}
      </form>
      <aside class="panel"><h3>Current teams</h3><ul class="list" style="margin-top:10px">${d.teams.map((t) => `<li><span class="grow"><b>${esc(t.name)}</b>, ${esc(t.focusArea)}<br><span class="small muted">${plural(t.members, "member")}. ${t.mentor ? "Mentor: " + esc(t.mentor) : "No mentor yet"}</span></span></li>`).join("")}</ul></aside></div>`;
    const form = $("#team-form");
    const preview = () => {
      const n = $$("input[name=p]:checked", form).length;
      const teams = Math.ceil(n / 6);
      $("#team-preview").textContent = n < 4 ? `${n} selected. Select at least 4 participants.` : `${n} selected. This will form ${plural(teams, "team")} of about ${Math.round(n / teams)} people, mixed by country.`;
    };
    $$("input[name=p]", form).forEach((c) => c.addEventListener("change", preview));
    $("#all")?.addEventListener("click", () => { $$("input[name=p]", form).forEach((c) => { c.checked = true; }); preview(); });
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      const ids = $$("input[name=p]:checked", form).map((c) => +c.value);
      busy(form.querySelector("button[type=submit]"), async () => {
        const r = await api("/api/teams/form", { method: "POST", body: { participantIds: ids, focusArea: form.focusArea.value } });
        toast(r.message + ". Members have been notified.");
        facilitator(main, { params });
      });
    });
  } else {
    main.innerHTML = head + `<div class="stack" style="--gap:12px;max-width:760px">${d.flagged.length ? d.flagged.map((p) => `<div class="panel">
      <div class="row"><span class="avatar">${esc(p.initials)}</span><div class="grow"><b>${esc(p.author)}</b>, ${esc(p.country)}<br><span class="small muted">${esc(p.trackTitle)}: ${esc(p.lessonTitle)}, ${esc(p.when)}</span></div><span class="chip alert">Reported</span></div>
      <p class="answer-box" style="margin-top:12px">${esc(p.body)}</p>
      <div class="row" style="margin-top:12px"><button class="btn danger" data-mod="${p.id}" data-hide="true">Hide comment</button><button class="btn" data-mod="${p.id}" data-hide="false">Keep it</button></div></div>`).join("")
      : `<div class="panel empty"><h3>No reported comments</h3><p>When a participant reports a comment it will appear here.</p></div>`}</div>`;
    $$("[data-mod]").forEach((b) => b.addEventListener("click", () => busy(b, async () => {
      const r = await api(`/api/posts/${b.dataset.mod}/moderate`, { method: "POST", body: { hide: b.dataset.hide === "true" } });
      toast(r.message); facilitator(main, { params });
    })));
  }
}

const gradeCard = (s) => `<form class="panel stack grade-form" data-id="${s.id}" data-max="${s.maxScore}">
  <div class="row"><span class="avatar">${esc(s.initials)}</span><div class="grow"><b>${esc(s.participant)}</b>, ${esc(s.country)}<br><span class="small muted">${esc(s.assessment)}, submitted ${esc(s.submitted)}</span></div></div>
  <p class="small"><b>Task:</b> ${esc(s.prompt)}</p>
  <div class="answer-box">${esc(s.answer)}</div>
  <div class="grid-2" style="grid-template-columns:1fr 1fr;align-items:end">
    <label class="field"><span>Score (out of ${s.maxScore})</span><input class="input" name="score" type="number" min="0" max="${s.maxScore}" step="0.5" required></label>
    <div class="result" aria-live="polite" style="padding-bottom:12px"></div></div>
  <label class="field"><span>Feedback</span><textarea class="input" name="feedback" placeholder="What was good, and one thing to improve"></textarea></label>
  <button class="btn primary" type="submit">Save grade</button></form>`;

// ------------------------------------------------------------------ mentor (wireframe 10)
export async function mentor(main, { query }) {
  const d = await api("/api/mentor/overview");
  const pick = d.teams.find((t) => String(t.id) === query.team) || d.teams.find((t) => t.waiting > 0) || d.teams[0];
  const ms = pick?.project?.milestones.find((m) => m.status === "SUBMITTED");
  main.innerHTML = `<div class="page-head"><div><h1>My teams (${d.teams.length} of ${d.max})</h1><p>${esc(d.expertise)}. ${d.toReview ? `${plural(d.toReview, "milestone")} waiting for your review.` : "Nothing waiting for review."}</p></div></div>
  ${!d.teams.length ? `<div class="panel empty"><h3>No teams yet</h3><p>The administrator will assign teams to you. You can mentor up to ${d.max} at a time.</p></div>` : `
  <div class="grid-2 mentor-grid">
    <section class="panel"><ul class="list">${d.teams.map((t) => `<li><a class="rowlink" href="#/mentor?team=${t.id}"><span class="grow"><span class="title">${esc(t.name)}, ${esc(t.focusArea)}</span><br><span class="small muted">${t.project ? esc(t.project.title) : "No project yet"}</span></span>
      <span class="chip ${t.waiting ? "dark" : t.badge === "Overdue" ? "bad" : ""}">${esc(t.badge)}</span></a></li>`).join("")}</ul></section>
    <section class="stack">
      <div class="panel"><h2>${esc(pick.name)}, ${esc(pick.focusArea)}</h2>
        ${pick.project ? `<h3 style="margin-top:10px">${esc(pick.project.title)}</h3><p class="small" style="margin-top:6px">${esc(pick.project.description)}</p>` : `<p class="muted">The team has not created its project yet.</p>`}
        <div class="row" style="margin-top:12px"><span class="avatars">${pick.members.map((m) => `<span class="avatar" title="${esc(m.name)}, ${esc(m.country)}">${esc(m.initials)}</span>`).join("")}</span><span class="small muted">${esc(pick.countries.join(", "))}</span></div>
        ${pick.project ? `<ol class="milestones" style="margin-top:14px">${pick.project.milestones.map((m) => `<li class="${m.status}"><span><b>${esc(m.title)}</b><br><span class="small muted">Due ${esc(m.due)}</span></span>${chipFor(m.status)}</li>`).join("")}</ol>` : ""}
      </div>
      ${ms ? `<form class="panel stack" id="review-form">
        <h2>Review: ${esc(ms.title)}</h2>
        <div class="dropzone small"><p class="muted">Submitted ${esc(ms.submittedOn)} by ${esc(ms.submittedBy)}</p><p style="margin-top:6px">${esc(ms.note)}</p>
          ${ms.link ? `<p style="margin-top:6px"><a href="${esc(ms.link)}" target="_blank" rel="noopener">${esc(ms.link)}</a></p>` : ""}</div>
        <fieldset style="border:0;padding:0;margin:0"><legend style="font-weight:700;margin-bottom:6px">Rating</legend>
          <div class="stars">${[1, 2, 3, 4, 5].map((n) => `<label><input type="radio" name="rating" value="${n}" ${n === 4 ? "checked" : ""}><span>${n}</span></label>`).join("")}</div></fieldset>
        <label class="field"><span>Comment to the team</span><textarea class="input" name="comment" placeholder="What works, and the one most important thing to improve" required></textarea></label>
        <div class="grid-2" style="grid-template-columns:1fr 1fr"><button class="btn" type="button" data-approve="false">Request changes</button><button class="btn primary" type="button" data-approve="true">Approve</button></div>
        <p class="tiny muted">The team is notified as soon as you save.</p></form>`
      : pick.project ? `<p class="notice">No milestone is waiting for review in ${esc(pick.name)}.</p>` : ""}
      ${pick.project?.feedback.length ? `<div class="panel"><h3>Previous feedback</h3>${pick.project.feedback.map((f) => `<div class="review-item"><div class="row between"><b>${esc(f.milestone || "Project")}</b><span class="chip">Rating ${f.rating} / 5</span></div><p class="small" style="margin-top:4px">${esc(f.comment)}</p></div>`).join("")}</div>` : ""}
    </section>
  </div>`}`;

  $$("#review-form [data-approve]").forEach((b) => b.addEventListener("click", () => {
    const form = $("#review-form");
    const comment = form.comment.value.trim();
    if (!comment) { toast("Write a short comment for the team", true); form.comment.focus(); return; }
    busy(b, async () => {
      const r = await api(`/api/milestones/${ms.id}/review`, { method: "POST", body: { approved: b.dataset.approve === "true", comment, rating: +form.rating.value } });
      toast(r.message);
      mentor(main, { query: { team: String(pick.id) } });
    });
  }));
}
