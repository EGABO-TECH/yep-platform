// Entry point: registers every screen with the router and starts the app.
import { route, router, loadMe } from "./core.js";
import * as pub from "./public.js";
import { landing } from "./landing.js";
import * as youth from "./participant.js";
import * as staff from "./staff.js";
import * as org from "./org.js";
import * as admin from "./admin.js";

const Y = ["YouthParticipant"];
const ADMIN = ["Administrator"];

route("/", landing, { auth: false, publicLayout: true });
route("/login", pub.login, { auth: false, publicLayout: true });
route("/register", pub.register, { auth: false, publicLayout: true });
route("/verify", pub.verify, { auth: false });

route("/home", youth.home, { roles: Y });
route("/learn", youth.learn, { roles: Y });
route("/lesson/:id", youth.lesson, { roles: Y });
route("/quiz/:id", youth.quiz, { roles: Y });
route("/team", youth.team, { roles: Y });
route("/portfolio", youth.portfolio, { roles: Y });

route("/facilitator", staff.facilitator, { roles: ["Facilitator"] });
route("/facilitator/:tab", staff.facilitator, { roles: ["Facilitator"] });
route("/mentor", staff.mentor, { roles: ["Mentor"] });

route("/talent", org.talent, { roles: ["Employer"] });
route("/reports", org.reports, { roles: ["UniversityRep", "Partner", "Administrator"] });

route("/admin", admin.users, { roles: ADMIN });
route("/admin/mentors", admin.mentors, { roles: ADMIN });
route("/admin/ai", admin.ai, { roles: ADMIN });
route("/admin/audit", admin.audit, { roles: ADMIN });

window.addEventListener("hashchange", router);
loadMe().catch(() => null).then(router);
