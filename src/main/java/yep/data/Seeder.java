package yep.data;

import yep.model.*;
import yep.service.YepServices;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Loads realistic demo data so every screen has something to show:
 * one Level One programme with four learning tracks, 25 young people from five countries,
 * three Level Two teams with projects, staff accounts and two applications waiting for approval.
 *
 * Every demo account uses the password: Demo@2026
 */
public class Seeder {
    public static final String DEMO_PASSWORD = "Demo@2026";
    public static final String DEMO_CERTIFICATE = "YEP-2026-K7M4Q2XB";

    private final DataStore store;
    private final YepServices services;
    private final Random random = new Random(7);
    private Programme programme;
    private Cohort cohort;
    private final Map<String, Organization> orgs = new HashMap<>();
    private final Map<String, LearningTrack> tracks = new LinkedHashMap<>();

    public Seeder(YepServices services) {
        this.services = services;
        this.store = services.store;
    }

    public void seed() {
        organisations();
        guideline();
        learningContent();
        Facilitator grace = staff();
        participants();
        sessions(grace);
        teamsAndProjects();
        discussion();
        welcomeNotifications();
        store.auditLogs.clear();
        services.audit.record("SYSTEM_START", null, "Demo data loaded");
    }

    // ------------------------------------------------------------------ organisations
    private void organisations() {
        org("Cavendish University Uganda", "University", "Uganda");
        org("University of Nairobi", "University", "Kenya");
        org("University of Dar es Salaam", "University", "Tanzania");
        org("University of Rwanda", "University", "Rwanda");
        org("SafariCode Labs", "Employer", "Kenya");
        org("Kampala Agritech Ltd", "Employer", "Uganda");
        org("Nile Data Works", "Employer", "Uganda");
        org("East Africa Youth Fund", "Partner", "Uganda");
    }

    private void org(String name, String type, String country) {
        Organization o = new Organization(store.newId(), name, type, country);
        store.organizations.add(o);
        orgs.put(name, o);
    }

    private void guideline() {
        store.setGuideline(new AIUsageGuideline("1.2", List.of(
                "Answers must come from YEP learning material and say where they came from.",
                "Every answer is labelled as AI-generated and must be checked with a facilitator.",
                "Personal details such as phone numbers and emails are removed before a question is processed.",
                "The assistant is turned off while a participant has a graded assessment open.",
                "Questions about cheating, hacking or harming others are refused and logged."),
                30, LocalDate.of(2026, 9, 1)));
    }

    // ------------------------------------------------------------------ learning content
    private void learningContent() {
        programme = new Programme(store.newId(), "Digital Skills Programme", "One");
        store.programmes.add(programme);
        cohort = new Cohort(store.newId(), programme, "Cohort 2026B",
                LocalDate.of(2026, 9, 7), LocalDate.of(2026, 12, 18));
        programme.getCohorts().add(cohort);
        store.cohorts.add(cohort);

        LearningTrack ai = track("AI Literacy", "Artificial intelligence",
                "Understand what AI can and cannot do, and use it with care.");
        lesson(ai, "What AI is and what it is not", 6, "Intro to AI (video)", 18.4,
                "Artificial intelligence (AI) is software that learns patterns from large amounts of data and uses those patterns to make predictions. A spam filter, a crop disease app and a chatbot are all examples of AI.",
                "AI does not think or understand the way people do. It finds patterns, so when the data it learned from is missing something or is unfair, its answers can be missing something or be unfair too.",
                "A useful rule: treat AI as a fast assistant that still needs a human to check its work, especially for health, money, school grades and legal questions.");
        lesson(ai, "How chatbots produce answers", 7, "Inside a chatbot (video)", 22.1,
                "A chatbot such as ChatGPT predicts the next word again and again, based on text it was trained on. That is why its answers sound fluent and confident.",
                "Sounding confident is not the same as being correct. Chatbots can invent facts, quotes and references. This is often called a hallucination.",
                "To get better answers, give clear context, ask for sources, and ask the chatbot to explain its reasoning. Then check the important parts yourself.");
        lesson(ai, "Spotting AI-generated content", 8, "Spotting fakes (video)", 24.6,
                "AI can now create realistic text, photos, voices and videos. Some are harmless, but others are made to mislead people, for example a fake voice note from a politician or a fake job advert.",
                "Signs of AI-generated images include hands with the wrong number of fingers, blurry or melted text, odd shadows and backgrounds that do not make sense. For text, look for very general statements with no names, dates or sources.",
                "When something looks surprising, pause before sharing. Search for the same story on trusted news websites, check who posted it first, and use a reverse image search to see where a photo came from.");
        lesson(ai, "Using AI responsibly at school and work", 6, null, 0,
                "Using AI to explain an idea, plan your work or check your grammar can save time. Submitting AI-written work as if you wrote it yourself is dishonest and may break your school's rules.",
                "Never paste personal information, passwords or private company data into an AI tool. What you type may be stored and used to train future models.",
                "Always say when you used AI, check every fact it gives you, and keep the final decision with a person.");
        quiz(ai, "AI Literacy Quiz",
                q("What does most AI software do?", List.of("Thinks like a human", "Learns patterns from data to make predictions", "Searches only the internet", "Always gives correct answers"), 1, "AI finds patterns in data and uses them to predict."),
                q("A chatbot gives you a confident answer with a reference. What should you do?", List.of("Trust it because it sounds sure", "Check the reference really exists and says the same thing", "Share it immediately", "Ask the chatbot if it is sure"), 1, "Chatbots can invent references, so check them."),
                q("Which is a common sign of an AI-generated image?", List.of("Clear shadows", "Readable signs and text", "Hands with the wrong number of fingers", "A photographer's name"), 2, "AI images often get hands and text wrong."),
                q("Which of these is safe to paste into a public AI tool?", List.of("Your national ID number", "Your company's customer list", "A paragraph of your own essay to check grammar", "Your bank PIN"), 2, "Never share personal or private data."),
                q("Who should make the final decision when AI is used for a health question?", List.of("The AI", "A qualified person", "The first person who replies online", "Nobody"), 1, "A human should stay in control of important decisions."));

        LearningTrack media = track("Media Literacy", "Media and information",
                "Check information before you believe it or share it.");
        lesson(media, "Why false information spreads", 6, "How rumours travel (video)", 16.0,
                "False information spreads quickly because it is often shocking, emotional or funny. People share it before checking because they want to warn or entertain their friends.",
                "Misinformation is false information shared by mistake. Disinformation is false information shared on purpose to mislead people, for example during elections or health emergencies.",
                "WhatsApp groups and social media make it easy for one message to reach thousands of people in a few hours. That is why one person pausing to check can make a real difference.");
        lesson(media, "Checking a news story in five steps", 7, "Five-step check (video)", 19.3,
                "Step 1: Pause. If a message makes you angry or scared, that is a reason to slow down. Step 2: Check the source. Is it a known news outlet, an official account or an unknown forward?",
                "Step 3: Look for the same story on trusted news websites such as national broadcasters and established newspapers. If no one else is reporting it, be careful.",
                "Step 4: Check the date, because old stories are often shared as if they are new. Step 5: Check the evidence. Real reports name people, places and dates that you can confirm.");
        lesson(media, "Photos and videos can lie too", 6, null, 0,
                "A real photo can be used with a false caption, for example a flood picture from another country presented as local news. A reverse image search on Google Images or TinEye shows where else the photo has appeared.",
                "Videos can be cut to remove context, slowed down to make someone look drunk, or fully generated with AI. Look for the original, longer version before you trust a short clip.",
                "If you cannot confirm a photo or video, do not forward it. You can reply to the sender and explain what you checked.");
        quiz(media, "Media Literacy Quiz",
                q("What is the best first step to check whether a news story shared on WhatsApp is true?", List.of("Share it with friends to ask what they think", "Look for the same story on trusted news websites", "Check how many people have forwarded it", "Trust it if it has a photo"), 1, "Trusted outlets reporting the same story is a strong sign."),
                q("What is disinformation?", List.of("Any news you disagree with", "False information shared by mistake", "False information shared on purpose to mislead", "Old news"), 2, "Disinformation is deliberate."),
                q("A dramatic flood photo is shared as local news. How can you check it?", List.of("Reverse image search", "Count the likes", "Ask the person who forwarded it", "Zoom in"), 0, "Reverse image search shows where it appeared before."),
                q("Why do false stories spread so fast?", List.of("They are usually long", "They are often shocking or emotional", "They come from governments", "They are hard to share"), 1, "Emotion drives sharing."),
                q("You cannot confirm a video. What should you do?", List.of("Forward it with a warning", "Post it publicly", "Do not forward it", "Edit it"), 2, "If you cannot confirm it, do not spread it."));
        written(media, "Media Literacy Written Task",
                "A message says a new government grant pays UGX 500,000 to every young person who registers through a link today. Explain, step by step, how you would check whether this claim is true.");

        LearningTrack safety = track("Digital Safety", "Online safety",
                "Protect your accounts, your money and your personal data.");
        lesson(safety, "Strong passwords and two-step verification", 5, "Password basics (video)", 12.2,
                "A strong password is long and unique. A short sentence of four or more random words is easier to remember and harder to guess than a short word with symbols.",
                "Use a different password for every important account, so that one leak does not open everything. A password manager can remember them for you.",
                "Turn on two-step verification for email, social media and mobile money. Even if someone learns your password, they still need the code sent to your phone.");
        lesson(safety, "Recognising scams and phishing", 6, "Scam stories (video)", 15.8,
                "Phishing messages pretend to be from a bank, mobile money provider or employer and try to make you click a link or share a PIN. They often create urgency: 'your account will be closed today'.",
                "Real companies never ask for your PIN or password by SMS, phone call or WhatsApp. Check the sender's number or email address carefully and call the official customer care line if unsure.",
                "Fake job adverts that ask you to pay a fee before an interview are a common scam targeting young people. A real employer does not charge you to apply.");
        lesson(safety, "Protecting your personal data", 5, null, 0,
                "Personal data includes your name, phone number, ID number, location, photos and school details. Share only what is needed, and check privacy settings on every app.",
                "Uganda's Data Protection and Privacy Act gives you rights over your personal data, including the right to know how it is used and to ask for it to be corrected.",
                "Before you post, ask: would I be comfortable if an employer, a stranger or my family saw this in five years?");
        quiz(safety, "Digital Safety Quiz",
                q("Which password is strongest?", List.of("Kampala1", "P@ss", "green mango bicycle river", "123456789"), 2, "Long passphrases are strongest."),
                q("Someone calls from 'mobile money' and asks for your PIN. What do you do?", List.of("Give it quickly", "Hang up and call the official line", "Send it by SMS instead", "Give half of it"), 1, "Real providers never ask for your PIN."),
                q("What does two-step verification add?", List.of("A second password", "A code sent to your phone or app", "A longer username", "Nothing"), 1, "It adds a second check."),
                q("A job advert asks for a UGX 50,000 'registration fee'. This is most likely:", List.of("Normal", "A scam", "A government rule", "A bonus"), 1, "Real employers do not charge to apply."),
                q("Which law protects personal data in Uganda?", List.of("The Data Protection and Privacy Act", "The Traffic Act", "The Land Act", "No law"), 0, "The Data Protection and Privacy Act, 2019."));

        LearningTrack culture = track("Intercultural Communication", "Virtual Exchange",
                "Work well with young people from other countries and cultures.");
        lesson(culture, "Working with people from other cultures", 6, "Across borders (video)", 20.5,
                "In a Virtual Exchange you will work with young people from Uganda, Kenya, Tanzania, Rwanda, Burundi and beyond. Each person brings different languages, habits and ideas.",
                "Some cultures are direct and say 'no' plainly, while others prefer polite, indirect answers. Neither is wrong. Ask questions instead of assuming what someone meant.",
                "Learn a greeting in your teammates' languages, be patient with accents and check that everyone has understood before moving on.");
        lesson(culture, "Communicating well in online teams", 6, null, 0,
                "Online teams lose the body language we rely on face to face, so write clearly: one topic per message, a clear request and a deadline.",
                "Agree on a meeting time that works across time zones and data budgets. Share notes after every call so that anyone who missed it because of poor connection can catch up.",
                "Use voice notes or short videos for warmth, but keep important decisions in writing so the whole team can find them later.");
        lesson(culture, "Giving and receiving feedback", 5, "Feedback that helps (video)", 14.0,
                "Good feedback is specific, kind and useful. Describe what you saw, explain its effect and suggest one next step.",
                "When you receive feedback, listen first, ask questions to understand, and thank the person. You do not have to agree with everything, but consider it seriously.",
                "Mentors in YEP rate each milestone from 1 to 5 and add a comment. Use those comments to improve your next milestone.");
        quiz(culture, "Intercultural Communication Quiz",
                q("A teammate answers 'maybe' to a deadline. What is the best response?", List.of("Assume yes", "Ask a friendly follow-up question", "Complain to the mentor", "Ignore it"), 1, "Ask instead of assuming."),
                q("Why share notes after a call?", List.of("To look busy", "So people with poor connection can catch up", "It is required by law", "To replace meetings"), 1, "Notes include everyone."),
                q("Which is good feedback?", List.of("This is bad", "Great job!", "The problem statement is clear; add one fact about who is affected", "Redo everything"), 2, "Specific, kind and useful."),
                q("Where should important team decisions be kept?", List.of("Only in voice notes", "In writing where everyone can find them", "In one person's memory", "Nowhere"), 1, "Writing keeps decisions findable."),
                q("Different communication styles across cultures are:", List.of("Wrong", "Something to understand and respect", "Always rude", "Not important"), 1, "Respect and curiosity help teams work."));
    }

    private LearningTrack track(String title, String topic, String summary) {
        LearningTrack t = new LearningTrack(store.newId(), programme, title, topic, summary);
        programme.getTracks().add(t);
        store.tracks.add(t);
        tracks.put(title, t);
        return t;
    }

    private void lesson(LearningTrack track, String title, int minutes, String video, double mb, String... paragraphs) {
        Lesson l = new Lesson(store.newId(), track, title, List.of(paragraphs), video, mb, minutes);
        track.addLesson(l);
        store.lessons.add(l);
    }

    private Question q(String text, List<String> options, int correct, String why) {
        return new Question(text, options, correct, why);
    }

    private void quiz(LearningTrack track, String title, Question... questions) {
        Assessment a = new Assessment(store.newId(), track, title, Assessment.MULTIPLE_CHOICE, 20, 15, null);
        for (Question q : questions) {
            a.addQuestion(q);
        }
        track.addAssessment(a);
        store.assessments.add(a);
    }

    private void written(LearningTrack track, String title, String prompt) {
        Assessment a = new Assessment(store.newId(), track, title, Assessment.WRITTEN, 20, 0, prompt);
        track.addAssessment(a);
        store.assessments.add(a);
    }

    // ------------------------------------------------------------------ users
    private <T extends User> T add(T user, AccountStatus status, String orgName) {
        user.setPassword(DEMO_PASSWORD);
        user.setStatus(status);
        if (orgName != null) {
            user.setOrganization(orgs.get(orgName));
        }
        store.users.add(user);
        return user;
    }

    private String mail(String name) {
        String[] p = name.toLowerCase().replace(".", "").replace("-", "").split(" ");
        return p[0] + "@yep.demo";
    }

    private Facilitator staff() {
        Facilitator grace = add(new Facilitator(store.newId(), "Grace Atim", "grace@yep.demo", 34, "Uganda", "Media and AI literacy"), AccountStatus.ACTIVE, null);
        add(new Mentor(store.newId(), "Daniel Okello", "daniel@yep.demo", 38, "Uganda", "Agritech and product design"), AccountStatus.ACTIVE, "Kampala Agritech Ltd");
        add(new Mentor(store.newId(), "Wanjiru Kamau", "wanjiru@yep.demo", 41, "Kenya", "Public health technology"), AccountStatus.ACTIVE, "University of Nairobi");
        add(new EmployerUser(store.newId(), "Allan Mugisha", "allan@yep.demo", 36, "Uganda"), AccountStatus.ACTIVE, "SafariCode Labs");
        add(new UniversityRep(store.newId(), "Dr. Ruth Nansubuga", "ruth@yep.demo", 45, "Uganda"), AccountStatus.ACTIVE, "Cavendish University Uganda");
        add(new PartnerUser(store.newId(), "Esther Wairimu", "esther@yep.demo", 39, "Kenya"), AccountStatus.ACTIVE, "East Africa Youth Fund");
        add(new Administrator(store.newId(), "Paul Okot", "admin@yep.demo", 40, "Uganda", 1), AccountStatus.ACTIVE, null);
        add(new Mentor(store.newId(), "Samuel Kiprono", "samuel@yep.demo", 33, "Kenya", "Data science"), AccountStatus.PENDING, "Nile Data Works");
        add(new EmployerUser(store.newId(), "Lydia Namugga", "lydia@yep.demo", 31, "Uganda"), AccountStatus.PENDING, "Kampala Agritech Ltd");
        return grace;
    }

    private record P(String name, String country, int age, String org, String headline, String skills) {
    }

    private void participants() {
        List<P> completed = List.of(
                new P("Faith Achieng", "Kenya", 23, "University of Nairobi", "Agri-tech builder and data enthusiast", "data analysis, SMS apps, agriculture, Python"),
                new P("Joseph Tumusiime", "Uganda", 25, "Cavendish University Uganda", "Mobile developer focused on farmers", "Android, Java, agriculture"),
                new P("Neema Mwakyusa", "Tanzania", 21, "University of Dar es Salaam", "Designer who tests ideas with real users", "UI design, user research, Figma"),
                new P("Eric Habimana", "Rwanda", 27, "University of Rwanda", "Field researcher and storyteller", "research, writing, Kinyarwanda, French"),
                new P("Peter Ochieng", "Kenya", 24, "University of Nairobi", "Health tech product lead", "product management, health, data analysis"),
                new P("Sarah Nabirye", "Uganda", 22, "Cavendish University Uganda", "Software engineering student building for clinics", "Java, web development, SQL"),
                new P("Claudine Uwase", "Rwanda", 26, "University of Rwanda", "Community health communicator", "public health, communication, French"),
                new P("Hassan Juma", "Tanzania", 29, "University of Dar es Salaam", "Climate data volunteer", "GIS, data analysis, climate"),
                new P("Ritah Akello", "Uganda", 20, "Cavendish University Uganda", "Front-end developer and tree planter", "HTML, CSS, JavaScript, climate"),
                new P("Jean-Paul Ndayishimiye", "Burundi", 28, null, "Teacher using digital tools in class", "teaching, French, digital literacy"),
                new P("Mercy Wanjiku", "Kenya", 23, "University of Nairobi", "Data analyst who loves clean spreadsheets", "Excel, data analysis, Power BI"),
                new P("Brian Ssali", "Uganda", 26, "Cavendish University Uganda", "Back-end developer learning cloud", "Java, Spring, databases"),
                new P("Aline Mukamana", "Rwanda", 24, "University of Rwanda", "Marketing and social media for small businesses", "digital marketing, content, Canva"),
                new P("Baraka Mollel", "Tanzania", 25, "University of Dar es Salaam", "Solar and energy tinkerer", "electronics, IoT, Arduino"),
                new P("Diane Iradukunda", "Burundi", 22, null, "Fact-checker and campus journalist", "fact-checking, writing, media literacy"));
        for (int i = 0; i < completed.size(); i++) {
            P p = completed.get(i);
            YouthParticipant u = participant(p);
            Enrollment e = enrol(u, 7);
            markAllLessons(e);
            for (Assessment a : programme.getAllAssessments()) {
                gradedSubmission(u, a, 13 + random.nextInt(7), 20);
            }
            e.complete();
            if (i == 0) {
                fixedCertificate(e);
            } else {
                services.certificates.issueCertificate(e);
            }
            Portfolio pf = store.portfolioOf(u).orElseThrow();
            pf.setHeadline(p.headline());
            pf.setBio(bioFor(p));
            pf.setSkills(Arrays.asList(p.skills().split(",\\s*")));
            pf.setVisibility(i % 5 == 4 ? "PRIVATE" : (i % 2 == 0 ? "EMPLOYERS" : "PUBLIC"));
        }
        // Self-added, not yet verified credential to show the difference
        store.portfolioOf(byName("Faith Achieng")).ifPresent(pf ->
                pf.addCredential(new Credential(store.newId(), "Google Data Analytics (self-reported)", "Self-added", false, null)));

        // Participants still working through Level One
        YouthParticipant amina = participant(new P("Amina Nakato", "Uganda", 22, "Cavendish University Uganda", "", ""));
        Enrollment ae = enrol(amina, 20);
        LearningTrack ai = tracks.get("AI Literacy");
        LearningTrack media = tracks.get("Media Literacy");
        ai.getLessons().forEach(l -> ae.getCompletedLessonIds().add(l.getContentId()));
        ae.getCompletedLessonIds().add(media.getLessons().get(0).getContentId());
        ae.getCompletedLessonIds().add(media.getLessons().get(1).getContentId());
        gradedSubmission(amina, ai.getAssessments().get(0), 16, 20);
        gradedSubmission(amina, media.getAssessments().get(0), 14, 20);
        writtenSubmission(amina, media.getAssessments().get(1),
                "First I would not click the link. I would search for the grant on the official government website and on trusted news sites like NTV or Daily Monitor. I would check if the message has a date and an official contact. Real government programmes do not ask you to register through a random link with a deadline of today, so I would warn the group that it is probably a scam.", 2);
        recalc(amina);

        Object[][] inProgress = {
                {"Kevin Otieno", "Kenya", 19, "University of Nairobi", 3},
                {"Zawadi Kimaro", "Tanzania", 30, "University of Dar es Salaam", 2},
                {"Patrick Mugabo", "Rwanda", 31, "University of Rwanda", 1},
                {"Moses Lubega", "Uganda", 27, "Cavendish University Uganda", 2},
                {"Ivy Chebet", "Kenya", 21, "University of Nairobi", 1},
                {"Rehema Said", "Tanzania", 24, null, 0},
                {"Ronald Kiggundu", "Uganda", 33, "Cavendish University Uganda", 1},
                {"Olivier Niyonzima", "Rwanda", 20, "University of Rwanda", 0},
                {"Esther Mutoni", "Rwanda", 25, "University of Rwanda", 1},
                {"Halima Bakari", "Tanzania", 26, null, 0}};
        for (Object[] row : inProgress) {
            YouthParticipant u = participant(new P((String) row[0], (String) row[1], (Integer) row[2], (String) row[3], "", ""));
            int quizzesDone = (Integer) row[4];
            Enrollment e = enrol(u, 30);
            List<LearningTrack> list = programme.getTracks();
            for (int t = 0; t < quizzesDone && t < list.size(); t++) {
                list.get(t).getLessons().forEach(l -> e.getCompletedLessonIds().add(l.getContentId()));
                gradedSubmission(u, list.get(t).getAssessments().get(0), 11 + random.nextInt(8), 20);
            }
            if (quizzesDone >= 2) {
                writtenSubmission(u, media.getAssessments().get(1), switch ((String) row[0]) {
                    case "Kevin Otieno" -> "I would check if the story is on the Kenyan government website and on Nation or Standard. If not, it is fake. I would also ask the person where they got it and tell them not to click the link.";
                    case "Zawadi Kimaro" -> "Pause first. Then look at who sent it and whether the link is an official .go.ug site. Search the grant name on Google News. Grants that need you to register today are suspicious so I would report it.";
                    default -> "I would check the source, look for the same story on trusted websites and check the date before sharing it with anyone.";
                }, 1);
            }
            recalc(u);
            if ("Rehema Said".equals(row[0])) {
                u.setStatus(AccountStatus.LOCKED);
                for (int k = 0; k < 5; k++) {
                    u.recordFailedLogin();
                }
            }
        }
    }

    private String bioFor(P p) {
        return "I am a " + p.age() + "-year-old from " + p.country() + ". I completed the YEP Digital Skills Programme and enjoy working in teams across East Africa. "
                + "I am interested in " + p.skills().split(",")[0].trim() + " and building tools that help my community.";
    }

    private YouthParticipant participant(P p) {
        YouthParticipant u = add(new YouthParticipant(store.newId(), p.name(), mail(p.name()), p.age(), p.country()),
                AccountStatus.ACTIVE, p.org());
        store.portfolios.add(new Portfolio(store.newId(), u));
        return u;
    }

    private Enrollment enrol(YouthParticipant u, int daysAgoMax) {
        Enrollment e = new Enrollment(store.newId(), u, cohort, cohort.getStartDate().plusDays(random.nextInt(daysAgoMax)));
        store.enrollments.add(e);
        return e;
    }

    private void markAllLessons(Enrollment e) {
        programme.getTracks().forEach(t -> t.getLessons().forEach(l -> e.getCompletedLessonIds().add(l.getContentId())));
    }

    private void gradedSubmission(YouthParticipant u, Assessment a, int score, int max) {
        List<Integer> answers = new ArrayList<>();
        for (Question q : a.getQuestions()) {
            answers.add(q.getCorrectIndex());
        }
        Submission s = new Submission(store.newId(), a, u, answers,
                a.isMultipleChoice() ? null : "Completed written task.", LocalDateTime.now().minusDays(10 + random.nextInt(30)));
        s.setScore((double) Math.min(score, max));
        store.submissions.add(s);
    }

    private void writtenSubmission(YouthParticipant u, Assessment a, String text, int daysAgo) {
        store.submissions.add(new Submission(store.newId(), a, u, null, text, LocalDateTime.now().minusDays(daysAgo).minusHours(3)));
    }

    private void recalc(YouthParticipant u) {
        services.assessments.updateProgress(u, programme.getAllAssessments().get(0));
    }

    private void fixedCertificate(Enrollment e) {
        Certificate c = new Certificate(store.newId(), e, LocalDate.of(2026, 10, 24), DEMO_CERTIFICATE);
        store.certificates.add(c);
        store.portfolioOf(e.getUser()).ifPresent(p -> p.addCredential(
                new Credential(store.newId(), c.getTitle(), "Youth Empowerment Project", true, DEMO_CERTIFICATE)));
    }

    private YouthParticipant byName(String name) {
        return (YouthParticipant) DataStore.find(store.users, u -> u.getFullName().equals(name)).orElseThrow();
    }

    private Mentor mentor(String name) {
        return (Mentor) DataStore.find(store.users, u -> u.getFullName().equals(name)).orElseThrow();
    }

    // ------------------------------------------------------------------ Level Two
    private void sessions(Facilitator host) {
        LocalDate thursday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
        store.sessions.add(new VirtualSession(store.newId(), tracks.get("Intercultural Communication"),
                "Working across borders: live exchange", LocalDateTime.of(thursday, LocalTime.of(16, 0)), 60,
                "https://meet.yep.demo/exchange-12", host, "Uganda, Kenya, Tanzania, Rwanda, Burundi"));
        store.sessions.add(new VirtualSession(store.newId(), tracks.get("Media Literacy"),
                "Fact-check clinic: bring a rumour", LocalDateTime.of(thursday.plusDays(7), LocalTime.of(17, 0)), 45,
                "https://meet.yep.demo/factcheck-3", host, "Open to all countries"));
    }

    private Team team(String name, String focus, String... members) {
        Team t = new Team(store.newId(), name, focus, LocalDateTime.now().minusDays(24));
        for (int i = 0; i < members.length; i++) {
            t.addMember(new TeamMembership(store.newId(), t, byName(members[i]), i == 0 ? "Lead" : "Member", t.getFormedAt()));
        }
        store.teams.add(t);
        return t;
    }

    private ImpactProject project(Team t, String title, String description) {
        ImpactProject p = new ImpactProject(store.newId(), t, title, description);
        LocalDate start = LocalDate.now().minusDays(20);
        services.projects.addMilestone(p, "Planning", start.plusDays(14));
        services.projects.addMilestone(p, "Prototype", start.plusDays(42));
        services.projects.addMilestone(p, "Final", start.plusDays(70));
        t.getProjects().add(p);
        store.projects.add(p);
        return p;
    }

    private void teamsAndProjects() {
        Mentor daniel = mentor("Daniel Okello");

        Team agri = team("Team 1", "Agriculture", "Faith Achieng", "Joseph Tumusiime", "Neema Mwakyusa", "Eric Habimana");
        ImpactProject soko = project(agri, "SokoSMS: crop prices by text message",
                "Small farmers often sell to middlemen without knowing the fair market price. SokoSMS sends the day's maize, beans and coffee prices from nearby markets by SMS, so farmers can negotiate with facts. It works on any phone, with no data needed.");
        store.mentorships.add(new Mentorship(store.newId(), daniel, agri, LocalDateTime.now().minusDays(20)));
        Milestone planning = soko.getMilestones().get(0);
        planning.submit(byName("Faith Achieng"), "Problem statement, interviews with 12 farmers in Mbale and Kisumu, and a one-page plan.", "https://docs.yep.demo/sokosms-plan");
        planning.markApproved();
        store.feedback.add(new Feedback(store.newId(), soko, planning, daniel,
                "Clear problem statement and good farmer interviews. Next, decide which three markets you will start with and how prices will be collected each morning.", 4, LocalDateTime.now().minusDays(6)));
        store.milestoneDrafts.put(soko.getMilestones().get(1).getMilestoneId(),
                "SMS flow is working for two markets. Testing with 5 farmers this week.");

        Team health = team("Team 2", "Health", "Peter Ochieng", "Sarah Nabirye", "Claudine Uwase");
        ImpactProject afya = project(health, "Afya Reminder: clinic visit reminders for new mothers",
                "Many mothers miss post-natal and immunisation visits. Afya Reminder sends friendly SMS and voice reminders in Luganda, Swahili and Kinyarwanda before each visit, and lets clinics see who may need a follow-up call.");
        store.mentorships.add(new Mentorship(store.newId(), daniel, health, LocalDateTime.now().minusDays(20)));
        Milestone hp = afya.getMilestones().get(0);
        hp.submit(byName("Peter Ochieng"), "Plan agreed with two health centres in Kampala and Nairobi.", "https://docs.yep.demo/afya-plan");
        hp.markApproved();
        store.feedback.add(new Feedback(store.newId(), afya, hp, daniel,
                "Strong partnership with the clinics. Keep the reminder messages short and test them with mothers who use basic phones.", 5, LocalDateTime.now().minusDays(9)));
        Milestone proto = afya.getMilestones().get(1);
        proto.submit(byName("Sarah Nabirye"), "Working prototype: clinic web page to add mothers and visit dates, scheduled SMS in three languages, and a test with 20 numbers.", "https://demo.yep.demo/afya-prototype");

        Team climate = team("Team 3", "Climate", "Hassan Juma", "Ritah Akello", "Jean-Paul Ndayishimiye");
        ImpactProject tree = project(climate, "TreeTrack: school tree-planting tracker",
                "Schools plant trees but rarely know how many survive. TreeTrack lets students log each tree with a photo and GPS point and check it every term, so schools can report survival rates.");
        tree.getMilestones().get(0).submit(byName("Hassan Juma"), "Plan and pilot schools in Dar es Salaam and Gulu.", "https://docs.yep.demo/treetrack-plan");

        Mentor wanjiru = mentor("Wanjiru Kamau");
        services.notifications.send(wanjiru, "Welcome, Wanjiru. Teams will appear here once the administrator assigns you.", "#/mentor");
    }

    private void discussion() {
        Lesson spotting = tracks.get("AI Literacy").getLessons().get(2);
        post(spotting, "Kevin Otieno", "I saw an AI video of a Kenyan musician 'endorsing' a betting app last week. The voice was almost perfect, but the lips did not match the words.", 3);
        post(spotting, "Neema Mwakyusa", "Reverse image search helped me find that a 'flood in Dar' photo was actually from 2019 in another country.", 2);
        post(spotting, "Amina Nakato", "Does the trick about counting fingers still work with newer image tools?", 1);
        DiscussionPost bad = post(tracks.get("Media Literacy").getLessons().get(0), "Ronald Kiggundu",
                "Join my group for quick money, click this link to double your mobile money in 24 hours!!!", 1);
        bad.flag();
    }

    private DiscussionPost post(Lesson lesson, String author, String text, int daysAgo) {
        DiscussionPost p = new DiscussionPost(store.newId(), lesson, byName(author), text, LocalDateTime.now().minusDays(daysAgo).minusHours(random.nextInt(8)));
        store.posts.add(p);
        return p;
    }

    private void welcomeNotifications() {
        User amina = byName("Amina Nakato");
        services.notifications.send(amina, "Welcome to Cohort 2026B. Your first lesson is ready.", "#/learn");
        services.notifications.send(amina, "Your Media Literacy Quiz has been marked: 14 out of 20.", "#/learn");
        User faith = byName("Faith Achieng");
        services.notifications.send(faith, "Daniel Okello approved your Planning milestone.", "#/team");
        services.notifications.send(faith, "Your certificate for Digital Skills Programme is ready.", "#/portfolio");
        store.notifications.forEach(n -> {
            if (n.getUser().getUserId() != amina.getUserId() && n.getUser().getUserId() != faith.getUserId()) {
                n.markRead();
            }
        });
        store.notifications.stream().filter(n -> n.getContent().startsWith("Welcome to Cohort")).forEach(Notification::markRead);
    }
}
