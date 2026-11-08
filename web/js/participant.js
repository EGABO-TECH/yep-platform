// Youth participant screens (wireframes 4 to 8).
import { api, esc, $, $$, go, toast, busy, formData, state, store, journey, icon, chipFor, plural, openSheet, animateJourneys } from "./core.js";

const ring = (p) => `<span class="ring ${p >= 100 ? "full" : ""}" style="--p:${p}" data-label="${p}%" aria-hidden="true"></span>`;
const trackChip = (t) => t.progress >= 100 ? `<span class="chip done">Completed</span>` : t.progress === 0 ? `<span class="chip">Not started</span>` : `<span class="chip progress">${t.progress}%</span>`;

// ------------------------------------------------------------------ dashboard (wireframe 4)
export async function home(main) {
  const d = await api("/api/participant/home");
  const stageProgress = d.stage === "build" ? 0.5 + Math.min(d.team.approved, 3) / 6
    : d.stage === "eligible" ? 0.5 : (d.programme.progress / 100) * 0.5;
  const finalProgress = d.certificate && d.stage === "build" && d.team.approved >= 3 ? 1 : stageProgress;
  const stageLine = {
    learn: `You are ${d.programme.progress}% through Level One. Finish all five assessments to unlock Level Two.`,
    eligible: "You finished Level One. A facilitator will place you in a Level Two team soon.",
    build: `You are in ${d.team?.name} (${d.team?.focusArea}). ${d.team?.approved} of 3 project milestones approved.`,
  }[d.stage];

  const cont = d.continueLessonId
    ? `<a class="btn warm" href="#/lesson/${d.continueLessonId}">Continue learning</a><span class="small" style="color:#B9C6E4">Next: ${esc(d.continueTrack)}</span>`
    : d.nextAssessmentId ? `<a class="btn warm" href="#/quiz/${d.nextAssessmentId}">Take the ${esc(d.nextAssessmentTitle)}</a>`
    : d.team ? `<a class="btn warm" href="#/team">Open team project</a>` : `<a class="btn warm" href="#/portfolio">View my portfolio</a>`;

  main.innerHTML = `<div class="welcome">
    <section class="journey-card">
      <h1>Hello, ${esc(d.firstName)}</h1>
      <p class="sub">${esc(stageLine)}</p>
      ${journey(finalProgress, [["Learn", `Level One, ${d.programme.progress}%`], ["Build", d.team ? d.team.name : "Level Two team"], ["Recognised", d.certificate ? "Certificate earned" : "Certificate"]])}
      <div class="row" style="margin-top:18px">${d.enrolled ? cont : `<button class="btn warm" id="enrol">Enrol in ${esc(d.programme.cohort)}</button>`}</div>
    </section>

    <div class="grid-2 wide-left">
      <section class="panel">
        <div class="panel-title"><h2>My learning tracks</h2><a href="#/learn" class="small">See all lessons</a></div>
        <div class="row between" style="margin-bottom:14px"><span class="muted small">${esc(d.programme.name)}, ${esc(d.programme.cohort)}</span><b>${d.programme.progress}% overall</b></div>
        <div class="bar" style="margin-bottom:16px"><i style="width:${d.programme.progress}%"></i></div>
        <ul class="list">${d.tracks.map((t) => `<li><a class="rowlink" href="#/learn?track=${t.id}">${ring(t.progress)}<span class="grow"><span class="title">${esc(t.title)}</span><br><span class="small muted">${t.lessonsDone} of ${plural(t.lessonCount, "lesson")} done</span></span>${trackChip(t)}</a></li>`).join("")}</ul>
      </section>

      <div class="stack">
        ${d.session ? `<section class="panel">
          <p class="small muted">Next Virtual Exchange session</p>
          <h3 style="margin-top:4px">${esc(d.session.title)}</h3>
          <p class="small" style="margin-top:6px">${esc(d.session.when)}, ${d.session.duration} minutes. Hosted by ${esc(d.session.host)}.</p>
          <p class="tiny muted" style="margin-top:4px">With participants from ${esc(d.session.countries)}</p>
          <a class="btn sm" style="margin-top:12px" href="${esc(d.session.link)}" target="_blank" rel="noopener" data-join>Join session</a>
        </section>` : ""}
        ${d.certificate ? `<section class="panel"><div class="row">${icon.seal}<h3>Certificate earned</h3></div>
          <p class="small" style="margin-top:6px">${esc(d.certificate.programme)}, issued ${esc(d.certificate.issued)}.</p>
          <p class="tiny muted">Code ${esc(d.certificate.code)}</p>
          <a class="btn sm" style="margin-top:10px" href="/api/public/certificate/${esc(d.certificate.code)}" target="_blank" rel="noopener">View certificate</a></section>` : ""}
        <section class="panel">
          <h3>Latest updates</h3>
          ${d.notifications.length ? `<ul class="list" style="margin-top:10px">${d.notifications.map((n) => `<li><a class="rowlink" href="${esc(n.link || "#")}"><span class="grow"><span class="title">${esc(n.text)}</span><br><span class="tiny muted">${esc(n.when)}</span></span></a></li>`).join("")}</ul>`
            : `<p class="muted small" style="margin-top:8px">Nothing new yet.</p>`}
        </section>
      </div>
    </div>
  </div>`;

  $("#enrol")?.addEventListener("click", (e) => busy(e.currentTarget, async () => {
    const r = await api("/api/enrol", { method: "POST", body: { cohortId: d.programme.cohortId } });
    toast(r.message); home(main); animateJourneys(main);
  }));
  $("[data-join]")?.addEventListener("click", (e) => { e.preventDefault(); toast("In the full system this opens the live session. Links are not active in the demo."); });
}

// ------------------------------------------------------------------ learn: tracks and sessions
export async function learn(main, { query }) {
  const d = await api("/api/learn");
  const open = parseInt(query.track || "0", 10);
  main.innerHTML = `<div class="page-head"><div><h1>Learn</h1><p>${esc(d.programme)}. Each track has short lessons and an assessment you need to pass.</p></div></div>
  <div class="grid-2 wide-left">
    <div class="stack" style="--gap:12px">${d.tracks.map((t) => `
      <details class="panel" ${open === t.id || (!open && t.progress > 0 && t.progress < 100) ? "open" : ""}>
        <summary class="track-row" style="cursor:pointer;list-style:none">${ring(t.progress)}<span class="grow"><h3>${esc(t.title)}</h3><span class="small muted">${esc(t.summary)}</span></span>${trackChip(t)}</summary>
        <ul class="list" style="margin-top:14px">
          ${t.lessons.map((l, i) => `<li><a class="rowlink" href="#/lesson/${l.id}"><span class="chip ${l.done ? "done" : ""}" style="min-width:34px;justify-content:center">${l.done ? "✓" : i + 1}</span><span class="grow"><span class="title">${esc(l.title)}</span><br><span class="tiny muted">${l.minutes} min read${l.hasVideo ? ", optional video" : ""}</span></span></a></li>`).join("")}
          ${t.assessments.map((a) => `<li><a class="rowlink" href="#/quiz/${a.id}"><span class="avatar blue" style="width:34px;height:34px">${icon.clipboard}</span><span class="grow"><span class="title">${esc(a.title)}</span><br><span class="tiny muted">${a.type === "WRITTEN" ? "Written task, marked by a facilitator" : `${a.questionCount} questions, ${a.timeLimit} minutes`}, ${a.maxScore} marks${a.score != null ? `. You scored ${a.score}` : ""}</span></span>${chipFor(a.state)}</a></li>`).join("")}
        </ul>
      </details>`).join("")}
    </div>
    <aside class="stack" style="--gap:12px">
      <section class="panel"><h3>Live Virtual Exchange</h3>
        <ul class="list" style="margin-top:10px">${d.sessions.map((s) => `<li><span class="grow"><span class="title">${esc(s.title)}</span><br><span class="small muted">${esc(s.when)}, ${s.duration} min<br>${esc(s.track)}, hosted by ${esc(s.host)}</span></span></li>`).join("")}</ul>
      </section>
      <section class="panel flat"><h3>Saving data</h3><p class="small" style="margin-top:6px">Lessons open as text first. Videos load only when you tap play, and show their size before you do.</p></section>
    </aside>
  </div>`;
}

// ------------------------------------------------------------------ lesson with discussion and AI assistant (wireframe 5)
export async function lesson(main, { params }) {
  const l = await api("/api/lessons/" + params.id);
  const saver = store.get("dataSaver", true);
  const videoBlock = !l.videoTitle ? "" : saver
    ? `<div class="video saver" id="video"><div><p><b>${esc(l.videoTitle)}</b></p><p class="small muted">Data saver is on. The video loads only when you tap play.</p>
       <button class="btn sm" style="margin-top:10px" data-play>${icon.play} Play video (${l.videoSizeMb} MB)</button></div></div>`
    : `<div class="video" id="video"><div><div class="play">${icon.play}</div><b>${esc(l.videoTitle)}</b><p class="small" style="color:#B9C6E4">${l.videoSizeMb} MB</p></div></div>`;

  main.innerHTML = `<a class="back" href="#/learn?track=${l.trackId}">${icon.back} ${esc(l.trackTitle)}</a>
  <div class="page-head"><div><p class="small" style="margin:0">Lesson ${l.position} of ${l.total}, ${l.minutes} min read</p><h1 style="margin-top:4px">${esc(l.title)}</h1></div>
    <label class="switch panel flat" style="padding:10px 14px"><span class="small"><b>Data saver</b><br><span class="muted">Text first, no autoplay</span></span><input type="checkbox" id="saver" ${saver ? "checked" : ""} aria-label="Data saver"></label></div>
  <div class="grid-2 wide-left">
    <div class="stack">
      ${videoBlock}
      <article class="panel lesson-body">${l.paragraphs.map((p) => `<p>${esc(p)}</p>`).join("")}</article>
      <div class="row between">
        <button class="btn" id="download">${icon.download} Download text</button>
        <button class="btn ${l.done ? "" : "primary"}" id="done" ${l.done ? "disabled" : ""}>${l.done ? `${icon.check} Done` : "Mark as done"}</button>
      </div>
      <div class="row between">
        ${l.prevId ? `<a class="btn ghost" href="#/lesson/${l.prevId}">${icon.back} Previous lesson</a>` : "<span></span>"}
        ${l.nextId ? `<a class="btn" href="#/lesson/${l.nextId}">Next lesson</a>` : l.assessments[0] ? `<a class="btn warm" href="#/quiz/${l.assessments[0].id}">Take the ${esc(l.assessments[0].title)}</a>` : ""}
      </div>
    </div>
    <div class="stack">
      <section class="ai-box">
        <h3>Ask the AI assistant</h3>
        <form id="ai-form" class="row" style="margin-top:12px;flex-wrap:nowrap">
          <label class="sr-only" for="ai-q">Your question</label>
          <input class="input grow" id="ai-q" name="question" placeholder="Ask about this lesson" maxlength="300" autocomplete="off">
          <button class="btn primary" type="submit">Ask</button>
        </form>
        <p class="tiny muted" style="margin-top:8px">AI answers can be wrong. Check important facts with your facilitator. Do not share personal details.</p>
        <div id="ai-answer" aria-live="polite"></div>
      </section>
      <section class="panel">
        <h3>Discussion (<span id="post-count">${l.posts.length}</span>)</h3>
        <div id="posts" style="margin-top:8px">${l.posts.map(postHtml).join("") || `<p class="small muted" id="no-posts">No comments yet. Start the conversation.</p>`}</div>
        <form id="post-form" class="row" style="margin-top:12px;flex-wrap:nowrap">
          <label class="sr-only" for="post-body">Write a comment</label>
          <input class="input grow" id="post-body" name="body" placeholder="Write a comment" maxlength="600" autocomplete="off">
          <button class="btn primary" type="submit">Post</button>
        </form>
      </section>
    </div>
  </div>`;

  $("#saver").addEventListener("change", (e) => { store.set("dataSaver", e.target.checked); lesson(main, { params }); });
  $("[data-play]")?.addEventListener("click", () => {
    $("#video").outerHTML = `<div class="video" id="video"><div><div class="play">${icon.play}</div><b>${esc(l.videoTitle)}</b><p class="small" style="color:#B9C6E4">Video files are not included in this demo. In the full system the ${l.videoSizeMb} MB video would stream here.</p></div></div>`;
  });
  $("#download").addEventListener("click", () => {
    const blob = new Blob([l.title + "\n\n" + l.paragraphs.join("\n\n")], { type: "text/plain" });
    const a = Object.assign(document.createElement("a"), { href: URL.createObjectURL(blob), download: l.title.replace(/[^a-z0-9]+/gi, "-").toLowerCase() + ".txt" });
    a.click(); URL.revokeObjectURL(a.href);
  });
  $("#done").addEventListener("click", (e) => busy(e.currentTarget, async () => {
    const r = await api(`/api/lessons/${l.id}/done`, { method: "POST" });
    toast(`Lesson done. ${l.trackTitle} is now ${r.trackProgress}% complete.`);
    if (l.nextId) go("/lesson/" + l.nextId); else lesson(main, { params });
  }));
  $("#ai-form").addEventListener("submit", (e) => {
    e.preventDefault();
    const q = e.target.question.value.trim();
    if (!q) return;
    busy(e.target.querySelector("button"), async () => {
      const r = await api("/api/ai/ask", { method: "POST", body: { question: q, lessonId: l.id } });
      $("#ai-answer").innerHTML = `<div class="ai-answer ${r.blocked ? "blocked" : ""}"><p class="small muted" style="margin-bottom:6px">You asked: ${esc(q)}</p><p>${esc(r.answer)}</p>
        ${r.source ? `<p class="src">Source: <a href="#/lesson/${r.sourceLessonId}">${esc(r.source)}</a></p>` : ""}
        <p class="src">${r.left} questions left today. Guidelines version ${esc(r.guideline)}.</p></div>`;
      e.target.question.value = "";
    });
  });
  $("#post-form").addEventListener("submit", (e) => {
    e.preventDefault();
    const body = e.target.body.value.trim();
    if (!body) { toast("Message cannot be empty", true); return; }
    busy(e.target.querySelector("button"), async () => {
      const p = await api(`/api/lessons/${l.id}/posts`, { method: "POST", body: { body } });
      $("#no-posts")?.remove();
      $("#posts").insertAdjacentHTML("beforeend", postHtml(p));
      $("#post-count").textContent = $$("#posts .post").length;
      e.target.body.value = "";
    });
  });
  $("#posts").addEventListener("click", (e) => {
    const b = e.target.closest("[data-flag]");
    if (!b) return;
    busy(b, async () => { const r = await api(`/api/posts/${b.dataset.flag}/flag`, { method: "POST" }); toast(r.message); b.replaceWith(Object.assign(document.createElement("span"), { className: "tiny muted", textContent: "Reported" })); });
  });
}

const postHtml = (p) => `<div class="post"><span class="avatar" style="width:34px;height:34px;font-size:13px">${esc(p.initials)}</span>
  <div class="grow"><p class="meta"><b>${esc(p.author)}</b>, ${esc(p.country)}, ${esc(p.when)}</p><p>${esc(p.body)}</p>
  ${p.mine ? "" : `<button class="btn ghost sm" style="padding:0;min-height:28px;margin-top:2px" data-flag="${p.id}">Report</button>`}</div></div>`;

// ------------------------------------------------------------------ assessment (wireframe 6)
export async function quiz(main, { params }) {
  let a;
  try {
    a = await api(`/api/assessments/${params.id}/start`, { method: "POST" });
  } catch (e) {
    main.innerHTML = `<a class="back" href="#/learn">${icon.back} Learn</a><div class="panel stack"><h1>Assessment</h1><p class="notice warn">${esc(e.message)}</p><a class="btn" href="#/learn">Back to Learn</a></div>`;
    return;
  }
  const key = "quiz." + a.id;
  const saved = store.get(key, { answers: [], index: 0, written: "", startedAt: Date.now() });
  const leave = () => api(`/api/assessments/${a.id}/leave`, { method: "POST" }).catch(() => {});
  window.addEventListener("hashchange", leave, { once: true });

  if (a.type === "WRITTEN") {
    main.innerHTML = `<a class="back" href="#/learn?track=${a.trackId}">${icon.back} ${esc(a.track)}</a>
    <div class="panel stack" style="max-width:760px">
      <div class="row between"><h1 style="font-size:30px">${esc(a.title)}</h1><span class="chip">${a.maxScore} marks</span></div>
      <div class="notice"><b>Task:</b> ${esc(a.prompt)}</div>
      <label class="field"><span>Your answer</span><textarea class="input" id="written" style="min-height:220px">${esc(saved.written)}</textarea><small id="wc"></small></label>
      <p class="notice dashed small">Your answer is saved on this device as you type. A facilitator will mark it and you will get a notification.</p>
      <p class="notice dashed small">The AI assistant is turned off while this task is open.</p>
      <button class="btn primary" id="submit-written">Submit for marking</button>
    </div>`;
    const ta = $("#written");
    const count = () => { $("#wc").textContent = ta.value.trim().split(/\s+/).filter(Boolean).length + " words"; };
    count();
    ta.addEventListener("input", () => { saved.written = ta.value; store.set(key, saved); count(); });
    $("#submit-written").addEventListener("click", (e) => busy(e.currentTarget, async () => {
      await api(`/api/assessments/${a.id}/submit`, { method: "POST", body: { writtenAnswer: ta.value } });
      store.remove(key);
      main.innerHTML = `<div class="panel stack" style="max-width:640px;text-align:center"><div class="avatar lg" style="margin:0 auto;background:var(--green-soft)">${icon.check}</div>
        <h1 style="font-size:30px">Submitted for marking</h1><p>Your facilitator will mark your answer. You will get a notification with your score and feedback.</p>
        <a class="btn primary" href="#/learn?track=${a.trackId}">Back to ${esc(a.track)}</a></div>`;
    }));
    return;
  }

  const total = a.questions.length;
  const deadline = saved.startedAt + a.timeLimit * 60 * 1000;
  let timer;

  const render = () => {
    const i = saved.index;
    const q = a.questions[i];
    const answered = saved.answers.filter((x) => x !== null && x !== undefined).length;
    main.innerHTML = `<div class="row between" style="margin-bottom:10px"><button class="btn ghost" id="exit">${icon.back} Exit</button><span class="chip">${a.maxScore} marks</span></div>
    <div class="panel stack" style="max-width:760px">
      <div><h1 style="font-size:20px">${esc(a.title)}</h1>
        <div class="row between small" style="margin-top:8px"><span>Question ${i + 1} of ${total}</span><span id="time" aria-live="off"></span></div>
        <div class="bar blue" style="margin-top:8px"><i style="width:${((i + 1) / total) * 100}%"></i></div></div>
      <fieldset style="border:0;padding:0;margin:0"><legend class="quiz-q">${esc(q.text)}</legend>
        <div style="margin-top:16px">${q.options.map((o, k) => `<label class="quiz-option ${saved.answers[i] === k ? "selected" : ""}"><input type="radio" name="opt" value="${k}" ${saved.answers[i] === k ? "checked" : ""}><span>${esc(o)}</span></label>`).join("")}</div>
      </fieldset>
      <div class="grid-2" style="grid-template-columns:1fr 1fr"><button class="btn" id="prev" ${i === 0 ? "disabled" : ""}>Previous</button>
        ${i < total - 1 ? `<button class="btn primary" id="next">Next</button>` : `<button class="btn primary" id="review">Review and submit</button>`}</div>
      <p class="notice dashed small">Answers are saved on this device. If the connection drops, they are sent when you are back online.</p>
      <p class="notice dashed small">The AI assistant is turned off during assessments.</p>
      <p class="small muted">${answered} of ${total} answered</p>
    </div>`;
    $$("input[name=opt]").forEach((r) => r.addEventListener("change", () => { saved.answers[i] = parseInt(r.value, 10); store.set(key, saved); render(); $("input[name=opt]:checked")?.focus(); }));
    $("#prev").addEventListener("click", () => { saved.index--; store.set(key, saved); render(); });
    $("#next")?.addEventListener("click", () => { saved.index++; store.set(key, saved); render(); });
    $("#review")?.addEventListener("click", () => confirmSubmit());
    $("#exit").addEventListener("click", () => { leave(); go("/learn?track=" + a.trackId); });
    tick();
  };

  const tick = () => {
    clearInterval(timer);
    const el = () => $("#time");
    const update = () => {
      const left = Math.max(0, deadline - Date.now());
      if (!el()) { clearInterval(timer); return; }
      const m = Math.floor(left / 60000), s = Math.floor((left % 60000) / 1000);
      el().textContent = `Time left ${m}:${String(s).padStart(2, "0")}`;
      if (left === 0) { clearInterval(timer); toast("Time is up. Your answers have been submitted."); submit(); }
    };
    update();
    timer = setInterval(update, 1000);
  };

  const confirmSubmit = () => {
    const missing = a.questions.map((_, k) => k).filter((k) => saved.answers[k] === undefined || saved.answers[k] === null);
    if (missing.length) {
      const dlg = openSheet("Submit with unanswered questions?", `<p>You have not answered ${plural(missing.length, "question")}: ${missing.map((k) => k + 1).join(", ")}.</p>
        <div class="row" style="margin-top:16px"><button class="btn" data-close-x>Go back</button><button class="btn primary" data-submit>Submit anyway</button></div>`);
      $("[data-close-x]", dlg).onclick = () => dlg.close();
      $("[data-submit]", dlg).onclick = () => { dlg.close(); submit(); };
    } else submit();
  };

  const submit = async () => {
    clearInterval(timer);
    const answers = a.questions.map((_, k) => (saved.answers[k] ?? null));
    try {
      const r = await api(`/api/assessments/${a.id}/submit`, { method: "POST", body: { answers } });
      store.remove(key);
      showResult(r);
    } catch (e) { toast(e.message, true); }
  };

  const showResult = (r) => {
    main.innerHTML = `<div class="grid-2" style="align-items:start">
      <section class="panel stack" style="text-align:center">
        <p class="muted">${esc(a.title)}</p>
        <div class="result-score">${r.score}<span style="font-size:28px;color:var(--muted)"> / ${r.maxScore}</span></div>
        <div>${r.passed ? `<span class="chip passed">Passed, ${r.percentage}%</span>` : `<span class="chip bad">Not passed, ${r.percentage}%. The pass mark is 50%</span>`}</div>
        <p>Programme progress is now <b>${r.programmeProgress}%</b>.</p>
        ${r.completed ? `<p class="notice ok">You completed Level One. Your certificate is ready in your portfolio.</p>` : ""}
        <div class="row" style="justify-content:center"><a class="btn primary" href="#/learn?track=${r.trackId}">Back to Learn</a>${r.completed ? `<a class="btn" href="#/portfolio">See certificate</a>` : ""}</div>
      </section>
      <section class="panel"><h2>Review your answers</h2>${r.review.map((q, k) => `<div class="review-item"><p><b>${k + 1}. ${esc(q.text)}</b></p>
        <p class="small">${q.correct ? `<span class="ok">Correct:</span> ${esc(q.yours)}` : `<span class="no">Your answer:</span> ${esc(q.yours ?? "Not answered")}<br><span class="ok">Correct answer:</span> ${esc(q.correctAnswer)}`}</p>
        <p class="tiny muted">${esc(q.why)}</p></div>`).join("")}</section></div>`;
  };

  render();
}

// ------------------------------------------------------------------ team project (wireframe 7)
export async function team(main) {
  const d = await api("/api/team");
  if (!d.team) {
    main.innerHTML = `<div class="page-head"><div><h1>Team</h1></div></div>
    <div class="panel empty stack" style="max-width:640px;margin:0 auto">
      <div class="avatar lg blue" style="margin:0 auto">${icon.team}</div>
      ${d.eligible ? `<h3>You are ready for Level Two</h3><p>A facilitator will place you in a team of up to six young people from different countries. You will get a notification when it happens.</p>`
        : `<h3>Teams open after Level One</h3><p>You are ${d.progress}% through Level One. Pass all five assessments and a facilitator will place you in a Level Two team.</p><div class="bar" style="max-width:320px;margin:12px auto 0"><i style="width:${d.progress}%"></i></div><a class="btn primary" href="#/learn" style="margin-top:12px">Keep learning</a>`}
    </div>`;
    return;
  }
  const t = d.team;
  const p = t.project;
  const lead = t.members.find((m) => m.role === "Lead");
  const current = p?.milestones.find((m) => m.canSubmit);
  const submittedWaiting = p?.milestones.find((m) => m.status === "SUBMITTED");
  const draftKey = current ? "draft." + current.id : null;
  const localDraft = draftKey ? store.get(draftKey) : null;

  main.innerHTML = `<div class="page-head"><div><p class="small muted" style="margin:0">Level Two impact project</p><h1 style="margin-top:4px">${esc(t.name)}, ${esc(t.focusArea)}</h1></div>
    <button class="btn" id="chat">${icon.chat} Team chat</button></div>
  ${!p ? `<section class="panel stack" style="max-width:760px"><h2>Start your project</h2><p class="muted">Agree as a team on the problem you will work on, then one member creates the project. Three milestones are added automatically: Planning, Prototype and Final.</p>
      <form id="create-project" class="stack"><label class="field"><span>Project title</span><input class="input" name="title" required maxlength="90"></label>
      <label class="field"><span>The problem and your idea</span><textarea class="input" name="description" required></textarea></label>
      <button class="btn primary" type="submit">Create project</button></form></section>` : `
  <div class="grid-2 wide-left">
    <div class="stack">
      <section class="panel"><h2>${esc(p.title)}</h2><p style="margin-top:8px">${esc(p.description)}</p></section>
      <section class="panel"><div class="panel-title"><h3>Milestones</h3><span class="small muted">${p.approved} of ${p.milestones.length} approved</span></div>
        <ol class="milestones">${p.milestones.map((m) => `<li class="${m.status}"><span><b>${esc(m.title)}</b><br><span class="small muted">Due ${esc(m.due)}${m.submittedBy ? `. Submitted by ${esc(m.submittedBy)}` : ""}</span></span>${chipFor(m.status)}</li>`).join("")}</ol>
      </section>
      ${current ? `<section class="panel stack"><h3>Submit: ${esc(current.title)}</h3>
        ${current.status === "NEEDS_CHANGES" ? `<p class="notice warn small">Your mentor asked for changes. Read the latest feedback, update your work and submit again.</p>` : ""}
        <label class="field"><span>What did your team do?</span><textarea class="input" id="ms-note" placeholder="Describe the work, what you tested and what you learned">${esc(localDraft ?? current.draft ?? "")}</textarea></label>
        <label class="field"><span>Link to your work</span><input class="input" id="ms-link" placeholder="Document, video, prototype or repository link" inputmode="url"></label>
        <div class="row"><button class="btn primary grow" id="ms-submit">Submit milestone</button><button class="btn" id="ms-draft">Save draft</button></div>
        <p class="tiny muted" id="draft-state">Drafts are saved on this device until the connection returns.</p></section>`
        : submittedWaiting ? `<p class="notice">${esc(submittedWaiting.title)} is waiting for your mentor's review.</p>`
        : p.status === "COMPLETED" ? `<p class="notice ok">Project complete. Score ${p.score}%. It has been added to every member's portfolio.</p>` : ""}
    </div>
    <div class="stack">
      <section class="panel"><div class="panel-title"><h3>Team members</h3></div>
        <p class="small">${t.mentor ? `Mentor: <b>${esc(t.mentor.name)}</b>, ${esc(t.mentor.expertise)}` : `<span class="chip alert">Mentor not assigned yet</span>`}</p>
        <ul class="list" style="margin-top:10px">${t.members.map((m) => `<li><span class="avatar">${esc(m.initials)}</span><span class="grow"><b>${esc(m.name)}</b>${m.you ? " (you)" : ""}<br><span class="small muted">${esc(m.country)}</span></span>${m.role === "Lead" ? `<span class="chip dark">Lead</span>` : ""}</li>`).join("")}</ul>
        <p class="tiny muted" style="margin-top:10px">Countries: ${esc(t.countries.join(", "))}</p>
      </section>
      <section class="panel"><h3>Feedback</h3>${p.feedback.length ? p.feedback.map((f) => `<div class="review-item"><div class="row between"><b>${esc(f.milestone || "Project")}</b><span class="chip">Rating ${f.rating} / 5</span></div>
        <p style="margin-top:6px">${esc(f.comment)}</p><p class="tiny muted">${esc(f.author)}, ${esc(f.when)}</p></div>`).join("") : `<p class="small muted" style="margin-top:8px">Your mentor's feedback will appear here.</p>`}</section>
    </div>
  </div>`}`;

  $("#chat").addEventListener("click", () => {
    openSheet(`${t.name} chat`, `<p class="notice dashed">Team chat is part of the full system. In this demo, use the milestone notes and the lesson discussions to share updates.</p><p class="small muted" style="margin-top:10px">Team lead: ${esc(lead?.name || "")}</p>`);
  });
  $("#create-project")?.addEventListener("submit", (e) => {
    e.preventDefault();
    const body = formData(e.target);
    busy(e.target.querySelector("button"), async () => { await api(`/api/teams/${t.id}/project`, { method: "POST", body }); toast("Project created with three milestones"); team(main); });
  });
  if (current) {
    const note = $("#ms-note");
    note.addEventListener("input", () => { store.set(draftKey, note.value); $("#draft-state").textContent = "Draft saved on this device."; });
    $("#ms-draft").addEventListener("click", (e) => busy(e.currentTarget, async () => {
      store.set(draftKey, note.value);
      try { await api(`/api/milestones/${current.id}/draft`, { method: "POST", body: { work: note.value } }); store.remove(draftKey); toast("Draft saved to your team's project"); }
      catch (ex) { if (ex.offline) toast("Offline. Draft kept on this device and will be sent when you are back online."); else throw ex; }
    }));
    $("#ms-submit").addEventListener("click", (e) => busy(e.currentTarget, async () => {
      const r = await api(`/api/milestones/${current.id}/submit`, { method: "POST", body: { note: note.value, link: $("#ms-link").value } });
      store.remove(draftKey);
      toast(r.message);
      team(main);
    }));
  }
}

// ------------------------------------------------------------------ portfolio and certificate (wireframe 8)
export async function portfolio(main) {
  const p = await api("/api/portfolio");
  let skills = [...p.skills];
  const vis = { PRIVATE: "Only me", EMPLOYERS: "Employers on YEP", PUBLIC: "Anyone with the link" };
  main.innerHTML = `<div class="page-head"><div><h1>My portfolio</h1><p>Your verified skills and projects. You decide who can see them.</p></div>
    <button class="btn" id="preview">Preview as employer</button></div>
  <div class="grid-2 wide-left">
    <form class="panel stack" id="pf-form">
      <div class="row"><span class="avatar lg">${esc(p.initials)}</span><div><h2>${esc(p.name)}</h2><p class="muted">${esc(p.country)}</p></div></div>
      <label class="field"><span>Headline</span><input class="input" name="headline" maxlength="90" value="${esc(p.headline)}" placeholder="For example: Agri-tech builder and data enthusiast"></label>
      <label class="field"><span>About me</span><textarea class="input" name="bio" placeholder="What you care about, what you have built, what you want to do next">${esc(p.bio)}</textarea></label>
      <div class="field"><span style="display:block;font-weight:700;font-size:15px;margin-bottom:6px">Skills</span>
        <div class="skill-input" id="skills"><input id="skill-new" placeholder="Type a skill and press Enter" aria-label="Add a skill"></div>
        <small class="muted tiny">Employers search by skill. Up to 12.</small></div>
      <label class="field"><span>Who can see my portfolio</span><select class="input" name="visibility">${Object.entries(vis).map(([k, v]) => `<option value="${k}" ${p.visibility === k ? "selected" : ""}>${v}</option>`).join("")}</select>
        <small>Your email and phone number are never shown, whatever you choose.</small></label>
      <button class="btn primary" type="submit">Save changes</button>
    </form>
    <div class="stack">
      <section class="panel"><h3>Credentials</h3><div style="margin-top:6px">${p.credentials.length ? p.credentials.map((c) => `<div class="cred">
        <span class="seal ${c.verified ? "" : "off"}">${icon.seal}</span>
        <div class="grow"><b>${esc(c.title)}</b><br><span class="small muted">${esc(c.issuer)}</span>${c.code ? `<br><span class="tiny muted">Code ${esc(c.code)}</span>` : ""}
        ${c.code ? `<div class="row" style="margin-top:8px;gap:8px"><a class="btn sm" href="/api/public/certificate/${esc(c.code)}" target="_blank" rel="noopener">${icon.download} Certificate</a><button type="button" class="btn sm" data-copy="${esc(c.code)}">${icon.link} Copy verify link</button></div>` : ""}</div>
        ${c.verified ? `<span class="chip verified">Verified</span>` : `<span class="chip">Not verified</span>`}</div>`).join("")
        : `<p class="small muted">Finish Level One to earn your first verified credential.</p>`}</div></section>
      <section class="panel"><h3>Projects</h3>${p.projects.length ? `<ul class="list" style="margin-top:10px">${p.projects.map((pr) => `<li><span class="grow"><b>${esc(pr.title)}</b><br><span class="small muted">${esc(pr.team)}, ${esc(pr.focus)}. ${pr.approved} of 3 milestones approved</span></span>${chipFor(pr.status)}</li>`).join("")}</ul>` : `<p class="small muted" style="margin-top:8px">Projects from your Level Two team will appear here.</p>`}</section>
    </div>
  </div>`;

  const box = $("#skills");
  const drawSkills = () => {
    $$(".chip", box).forEach((c) => c.remove());
    skills.forEach((s, i) => box.insertBefore(Object.assign(document.createElement("span"), { className: "chip progress", innerHTML: `${esc(s)}<button type="button" aria-label="Remove ${esc(s)}" data-i="${i}">×</button>` }), $("#skill-new")));
  };
  drawSkills();
  box.addEventListener("click", (e) => { const b = e.target.closest("button[data-i]"); if (b) { skills.splice(+b.dataset.i, 1); drawSkills(); $("#skill-new").focus(); } });
  $("#skill-new").addEventListener("keydown", (e) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      const v = e.target.value.trim().replace(/,$/, "");
      if (v && skills.length < 12 && !skills.includes(v)) { skills.push(v); drawSkills(); }
      e.target.value = "";
    }
  });
  $("#pf-form").addEventListener("submit", (e) => {
    e.preventDefault();
    const d = formData(e.target);
    busy(e.target.querySelector("button[type=submit]"), async () => { const r = await api("/api/portfolio", { method: "PUT", body: { ...d, skills } }); toast(r.message); });
  });
  $$("[data-copy]").forEach((b) => b.addEventListener("click", async () => {
    const url = location.origin + "/#/verify?code=" + b.dataset.copy;
    try { await navigator.clipboard.writeText(url); toast("Verify link copied"); } catch { toast(url); }
  }));
  $("#preview").addEventListener("click", async () => {
    const v = await api("/api/portfolio/preview");
    openSheet("What employers see", profileHtml(v) + `<p class="notice dashed small" style="margin-top:14px">Visibility is set to "${vis[p.visibility]}". ${p.visibility === "PRIVATE" ? "Employers cannot find you in search until you change it." : "Employers on YEP can find you in talent search."}</p>`);
  });
}

/** The employer view of a portfolio. Also used in the employer screens. */
export function profileHtml(v) {
  return `<div class="stack">
    <div class="row"><span class="avatar lg">${esc(v.initials)}</span><div><h2>${esc(v.name)}</h2><p class="muted">${esc(v.country)}${v.headline ? ". " + esc(v.headline) : ""}</p></div></div>
    ${v.bio ? `<p>${esc(v.bio)}</p>` : ""}
    ${v.skills.length ? `<div class="row" style="gap:6px">${v.skills.map((s) => `<span class="chip progress">${esc(s)}</span>`).join("")}</div>` : ""}
    <div><h3>Verified credentials</h3>${v.credentials.length ? v.credentials.map((c) => `<div class="cred"><span class="seal">${icon.seal}</span><div class="grow"><b>${esc(c.title)}</b><br><span class="small muted">${esc(c.issuer)}${c.code ? `, code ${esc(c.code)}` : ""}</span></div></div>`).join("") : `<p class="small muted">None yet.</p>`}</div>
    <div><h3>Projects</h3>${v.projects.length ? v.projects.map((p) => `<div class="review-item"><b>${esc(p.title)}</b> <span class="small muted">${esc(p.team)}, ${esc(p.focus)}</span><p class="small" style="margin-top:4px">${esc(p.description)}</p><p class="tiny muted">${p.approved} of 3 milestones approved by a mentor</p></div>`).join("") : `<p class="small muted">None yet.</p>`}</div>
  </div>`;
}
