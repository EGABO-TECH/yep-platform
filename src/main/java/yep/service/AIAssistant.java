package yep.service;

import yep.data.DataStore;
import yep.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Pseudo-code: CLASS AIAssistant, method ask(participant, question).
 *
 * For this demo the "AI_MODEL" is an offline helper that searches the lesson library and
 * answers from it, so the system works without internet and never sends data outside.
 * All the safeguards from the pseudo-code are applied exactly as written.
 */
public class AIAssistant {
    private static final String DISCLAIMER = "AI-generated: confirm with your facilitator.";
    private static final List<String> HARMFUL = List.of("hack into", "steal", "password of", "make a weapon",
            "bomb", "cheat on", "answers to the quiz", "answers to the assessment", "kill");
    private static final Set<String> STOP_WORDS = Set.of("the", "a", "an", "is", "are", "what", "how", "why",
            "do", "does", "i", "to", "of", "in", "on", "and", "or", "can", "it", "for", "my", "me", "with", "about",
            "this", "that", "be", "you", "your", "should", "when", "which", "who", "if", "we", "they", "there", "lesson", "made", "make", "was", "were", "has", "have", "get", "use");

    private final DataStore store;
    private final AuthService auth;
    private final AssessmentService assessments;
    private final AuditService audit;

    public AIAssistant(DataStore store, AuthService auth, AssessmentService assessments, AuditService audit) {
        this.store = store;
        this.auth = auth;
        this.assessments = assessments;
        this.audit = audit;
    }

    public record Answer(String text, String source, Integer sourceLessonId, boolean blocked, int questionsLeft) {
    }

    public Answer ask(User participant, String rawQuestion, Integer lessonId) {
        auth.checkPermission(participant, Permission.USE_AI);
        int limit = store.getGuideline().getDailyLimit();
        long usedToday = store.aiUsageLogs.stream()
                .filter(l -> l.getUser().getUserId() == participant.getUserId()
                        && l.getAskedAt().toLocalDate().equals(LocalDate.now()))
                .count();
        if (usedToday >= limit) {
            return new Answer("Daily limit reached. You can ask " + limit + " questions a day. Try again tomorrow.", null, null, true, 0);
        }
        if (rawQuestion == null || rawQuestion.isBlank()) {
            return new Answer("Type a question about the lesson first.", null, null, true, (int) (limit - usedToday));
        }

        String question = removePersonalData(rawQuestion);
        boolean hadPersonalData = !question.equals(rawQuestion);

        if (isHarmful(question)) {
            audit.record("AI_MISUSE_FLAGGED", participant, "Question refused");
            return new Answer("This question cannot be answered. The assistant only helps with learning.", null, null, true, (int) (limit - usedToday));
        }
        if (assessments.hasOpenAssessment(participant)) {
            return new Answer("The assistant is not available during an assessment.", null, null, true, (int) (limit - usedToday));
        }

        Match best = findBestMatch(question, lessonId);
        StringBuilder text = new StringBuilder();
        if (hadPersonalData) {
            text.append("I removed personal details from your question to protect your privacy. ");
        }
        String source = null;
        Integer sourceId = null;
        if (best == null || best.score == 0) {
            text.append("I could not find this in the YEP lessons yet. Try asking with words from the lesson, ")
                .append("or post your question in the discussion so your facilitator and classmates can help.");
        } else {
            text.append(best.paragraph);
            source = best.lesson.getTrack().getTitle() + ": " + best.lesson.getTitle();
            sourceId = best.lesson.getContentId();
        }
        text.append(" (").append(DISCLAIMER).append(")");

        store.aiUsageLogs.add(new AIUsageLog(store.newId(), participant, question, LocalDateTime.now()));
        return new Answer(text.toString(), source, sourceId, false, (int) (limit - usedToday - 1));
    }

    /** REMOVE_PERSONAL_DATA(question): hides email addresses and phone numbers. */
    static String removePersonalData(String question) {
        return question
                .replaceAll("[\\w.+-]+@[\\w-]+\\.[\\w.]+", "[email removed]")
                .replaceAll("(\\+?\\d[\\d\\s-]{8,}\\d)", "[number removed]");
    }

    /** IS_HARMFUL(question). */
    static boolean isHarmful(String question) {
        String lower = question.toLowerCase(Locale.ROOT);
        return HARMFUL.stream().anyMatch(lower::contains);
    }

    private record Match(Lesson lesson, String paragraph, int score) {
    }

    /** Finds the lesson paragraph that shares the most key words with the question. */
    private Match findBestMatch(String question, Integer currentLessonId) {
        Set<String> words = keywords(question);
        Match best = null;
        for (Lesson lesson : store.lessons) {
            for (String paragraph : lesson.getParagraphs()) {
                Set<String> pWords = keywords(paragraph + " " + lesson.getTitle());
                int score = 0;
                for (String w : words) {
                    if (pWords.contains(w)) {
                        score += 2;
                    } else if (pWords.stream().anyMatch(p -> p.startsWith(w) || w.startsWith(p))) {
                        score += 1;
                    }
                }
                if (currentLessonId != null && lesson.getContentId() == currentLessonId && score > 0) {
                    score += 1;
                }
                if (best == null || score > best.score) {
                    best = new Match(lesson, paragraph, score);
                }
            }
        }
        return best;
    }

    /** Words that mean the same thing in a question, so "photo" also finds lessons about "images". */
    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("photo", "image"), Map.entry("picture", "image"), Map.entry("pic", "image"),
            Map.entry("tell", "sign"), Map.entry("spot", "sign"), Map.entry("spotting", "sign"), Map.entry("detect", "sign"), Map.entry("know", "sign"),
            Map.entry("fake", "false"), Map.entry("rumour", "false"), Map.entry("rumor", "false"), Map.entry("lie", "false"),
            Map.entry("chatgpt", "chatbot"), Map.entry("bot", "chatbot"),
            Map.entry("pin", "password"), Map.entry("hacked", "password"), Map.entry("account", "password"),
            Map.entry("fraud", "scam"), Map.entry("con", "scam"),
            Map.entry("teammate", "team"), Map.entry("group", "team"));

    private static Set<String> keywords(String text) {
        Set<String> out = new HashSet<>();
        for (String w : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (w.length() > 2 && !STOP_WORDS.contains(w)) {
                String base = w.endsWith("s") && w.length() > 4 ? w.substring(0, w.length() - 1) : w;
                out.add(SYNONYMS.getOrDefault(base, base));
            }
        }
        return out;
    }
}
