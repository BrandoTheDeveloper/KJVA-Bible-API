package com.zioneltechnology.kjva_bible_api.service;

import com.zioneltechnology.kjva_bible_api.dto.ModernizationPreview;
import com.zioneltechnology.kjva_bible_api.dto.ModernizationResponse;
import com.zioneltechnology.kjva_bible_api.model.Verse;
import com.zioneltechnology.kjva_bible_api.repository.VerseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies 1611→modern rules to produce {@link Verse#getModernText() modernText} from {@link Verse#getOriginalText()
 * originalText}: static whole-word dictionary first, then pattern-based U/V, silent-e stripping, and {@code nesse}
 * suffix handling. {@code originalText} is never modified.
 * <p>
 * Note: It is expected that the input text has already been pre-processed by
 * {@link DataCleaningService#cleanText(String)} to remove HTML entities before modernization begins.
 */
@Service
public class ModernizationService {

    private static final int PREVIEW_MAX_LEN = 200;
    private static final int DRY_RUN_SAMPLE_LIMIT = 10;

    /** Double consonant + final {@code e} (e.g. sonne → son). */
    private static final Pattern DOUBLE_CONSONANT_FINAL_E =
            Pattern.compile("\\b(\\w+)([bdfglmnprst])\\2e\\b", Pattern.CASE_INSENSITIVE);

    /** Suffix {@code nesse} → {@code ness} (e.g. darknesse → darkness). */
    private static final Pattern NESSE_SUFFIX = Pattern.compile("\\b(\\w+)nesse\\b", Pattern.CASE_INSENSITIVE);

    /** Word-initial {@code v} + consonant → {@code u} (e.g. vnto → unto). */
    private static final Pattern LEADING_V_PLUS_CONSONANT =
            Pattern.compile("\\b([vV])([bcdfghjklmnpqrstwxzBCDFGHJKLMNPQRSTWXZ])");

    /** {@code u} between vowels → {@code v} (e.g. haue → have). */
    private static final Pattern U_BETWEEN_VOWELS =
            Pattern.compile("(?<=[aeiouyAEIOUY])([uU])(?=[aeiouyAEIOUY])");

    /**
     * 1611-style spellings and archaisms → modern English. Keys are matched with {@code \b} word boundaries
     * (case-insensitive); values are canonical replacements; {@link #matchCase(String, String)} preserves casing.
     */
    private static final Map<String, String> REPLACEMENT_RULES = new LinkedHashMap<>();

    static {
        // --- Theological & Proper Names ---
        REPLACEMENT_RULES.put("Iesus", "Jesus");
        REPLACEMENT_RULES.put("Leuites", "Levites");
        REPLACEMENT_RULES.put("Leui", "Levi");
        REPLACEMENT_RULES.put("Iechonias", "Jechonias");
        REPLACEMENT_RULES.put("Iudah", "Judah");
        REPLACEMENT_RULES.put("Ierusalem", "Jerusalem");
        REPLACEMENT_RULES.put("Iordan", "Jordan");
        REPLACEMENT_RULES.put("Iordane", "Jordan");
        REPLACEMENT_RULES.put("Iames", "James");
        REPLACEMENT_RULES.put("Iohn", "John");
        REPLACEMENT_RULES.put("Iudea", "Judea");
        REPLACEMENT_RULES.put("Ieremie", "Jeremiah");
        REPLACEMENT_RULES.put("Iudges", "Judges");
        REPLACEMENT_RULES.put("Iust", "Just");
        REPLACEMENT_RULES.put("Iustice", "Justice");
        REPLACEMENT_RULES.put("Ioy", "Joy");

        // --- Pronouns & Archaisms ---
        REPLACEMENT_RULES.put("thee", "you");
        REPLACEMENT_RULES.put("thou", "you");
        REPLACEMENT_RULES.put("thy", "your");
        REPLACEMENT_RULES.put("thine", "yours");
        REPLACEMENT_RULES.put("ye", "you");
        REPLACEMENT_RULES.put("hee", "he");
        REPLACEMENT_RULES.put("shee", "she");
        REPLACEMENT_RULES.put("selfe", "self");
        REPLACEMENT_RULES.put("themselues", "themselves");
        REPLACEMENT_RULES.put("owne", "own");
        REPLACEMENT_RULES.put("euery", "every");
        REPLACEMENT_RULES.put("euil", "evil");
        REPLACEMENT_RULES.put("whiles", "while");
        REPLACEMENT_RULES.put("amongst", "among");

        // --- Common 1611 Verb Conjugations ---
        REPLACEMENT_RULES.put("hath", "has");
        REPLACEMENT_RULES.put("doth", "does");
        REPLACEMENT_RULES.put("doeth", "does");
        REPLACEMENT_RULES.put("shalt", "shall");
        REPLACEMENT_RULES.put("shal", "shall");
        REPLACEMENT_RULES.put("wilt", "will");
        REPLACEMENT_RULES.put("wil", "will");
        REPLACEMENT_RULES.put("canst", "can");
        REPLACEMENT_RULES.put("couldst", "could");
        REPLACEMENT_RULES.put("wouldst", "would");
        REPLACEMENT_RULES.put("shouldst", "should");
        REPLACEMENT_RULES.put("mightest", "might");
        REPLACEMENT_RULES.put("gavest", "gave");
        REPLACEMENT_RULES.put("knowest", "know");
        REPLACEMENT_RULES.put("seest", "see");
        REPLACEMENT_RULES.put("hearest", "hear");
        REPLACEMENT_RULES.put("goest", "go");
        REPLACEMENT_RULES.put("camest", "came");
        REPLACEMENT_RULES.put("saidst", "said");
        REPLACEMENT_RULES.put("saith", "says");
        REPLACEMENT_RULES.put("sayeth", "says");
        REPLACEMENT_RULES.put("dwelleth", "dwells");
        REPLACEMENT_RULES.put("falleth", "falls");
        REPLACEMENT_RULES.put("calleth", "calls");
        REPLACEMENT_RULES.put("commeth", "comes");
        REPLACEMENT_RULES.put("cometh", "comes");
        REPLACEMENT_RULES.put("passeth", "passes");
        REPLACEMENT_RULES.put("crosseth", "crosses");
        REPLACEMENT_RULES.put("hast", "have"); // "Thou hast" -> "You have"
        REPLACEMENT_RULES.put("art", "are");   // "Thou art" -> "You are"
        REPLACEMENT_RULES.put("wert", "were");
        REPLACEMENT_RULES.put("becommeth", "becomes");
        REPLACEMENT_RULES.put("bringeth", "brings");
        REPLACEMENT_RULES.put("beleeveth", "believes");

        // --- Static U/V and I/J Fixes ---
        REPLACEMENT_RULES.put("vnto", "unto");
        REPLACEMENT_RULES.put("vpon", "upon");
        REPLACEMENT_RULES.put("vp", "up");
        REPLACEMENT_RULES.put("vnder", "under");
        REPLACEMENT_RULES.put("vntill", "until");
        REPLACEMENT_RULES.put("vs", "us");
        REPLACEMENT_RULES.put("vse", "use");
        REPLACEMENT_RULES.put("vsed", "used");
        REPLACEMENT_RULES.put("vtter", "utter");
        REPLACEMENT_RULES.put("haue", "have");
        REPLACEMENT_RULES.put("loue", "love");
        REPLACEMENT_RULES.put("liue", "live");
        REPLACEMENT_RULES.put("euer", "ever");
        REPLACEMENT_RULES.put("neuer", "never");
        REPLACEMENT_RULES.put("giue", "give");
        REPLACEMENT_RULES.put("giuen", "given");

        // --- Spelling Normalization ---
        REPLACEMENT_RULES.put("mercie", "mercy");
        REPLACEMENT_RULES.put("sonne", "son");
        REPLACEMENT_RULES.put("sinne", "sin");
        REPLACEMENT_RULES.put("sinnes", "sins");
        REPLACEMENT_RULES.put("heauen", "heaven");
        REPLACEMENT_RULES.put("foorth", "forth");
        REPLACEMENT_RULES.put("worde", "word");
        REPLACEMENT_RULES.put("kingdome", "kingdom");
        REPLACEMENT_RULES.put("wildernesse", "wilderness");
        REPLACEMENT_RULES.put("riuer", "river");
        REPLACEMENT_RULES.put("baptisme", "baptism");
        REPLACEMENT_RULES.put("haire", "hair");
        REPLACEMENT_RULES.put("booke", "book");
        REPLACEMENT_RULES.put("marke", "mark");
        REPLACEMENT_RULES.put("darknesse", "darkness");
        REPLACEMENT_RULES.put("lightnesse", "lightness");
        REPLACEMENT_RULES.put("witnesse", "witness");
        REPLACEMENT_RULES.put("goodnesse", "goodness");
        REPLACEMENT_RULES.put("mountaine", "mountain");
        REPLACEMENT_RULES.put("captaine", "captain");
        REPLACEMENT_RULES.put("certaine", "certain");
        REPLACEMENT_RULES.put("honie", "honey");
        REPLACEMENT_RULES.put("passe", "pass");
        REPLACEMENT_RULES.put("comming", "coming");
        REPLACEMENT_RULES.put("crosse", "cross");
        REPLACEMENT_RULES.put("fortie", "forty");
        REPLACEMENT_RULES.put("thirtie", "thirty");
        REPLACEMENT_RULES.put("twentie", "twenty");
        REPLACEMENT_RULES.put("seruants", "servants");
        REPLACEMENT_RULES.put("seruant", "servant");
        REPLACEMENT_RULES.put("vncleane", "unclean");
        REPLACEMENT_RULES.put("vncleannesse", "uncleanness");
        REPLACEMENT_RULES.put("passeouer", "passover");
        REPLACEMENT_RULES.put("passeouers", "passovers");
        REPLACEMENT_RULES.put("onely", "only");
        REPLACEMENT_RULES.put("citye", "city");
        REPLACEMENT_RULES.put("cityes", "cities");
        REPLACEMENT_RULES.put("countreys", "countries");
        REPLACEMENT_RULES.put("enimies", "enemies");
        REPLACEMENT_RULES.put("waies", "ways");
        REPLACEMENT_RULES.put("daies", "days");
        REPLACEMENT_RULES.put("wisedome", "wisdom");
        REPLACEMENT_RULES.put("trueth", "truth");
        REPLACEMENT_RULES.put("voyd", "void");
        REPLACEMENT_RULES.put("forme", "form");
        REPLACEMENT_RULES.put("saluation", "salvation");
        REPLACEMENT_RULES.put("commaundements", "commmandments");
    }

    private final VerseRepository verseRepository;

    public ModernizationService(VerseRepository verseRepository) {
        this.verseRepository = verseRepository;
    }

    /**
     * Recomputes {@code modernText} from {@code originalText} via {@link #modernizeText(String)} (dictionary + pattern
     * rules). {@code originalText} on each entity is left unchanged. When {@code dryRun} is {@code true}, no database
     * writes occur and up to
     * {@value #DRY_RUN_SAMPLE_LIMIT} sample rows are returned.
     */
    @Transactional
    public ModernizationResponse modernizeAll(boolean dryRun) {
        List<Verse> all = verseRepository.findAll();
        List<ModernizationPreview> samples = new ArrayList<>();
        List<Verse> toSave = new ArrayList<>();
        int changed = 0;

        for (Verse v : all) {
            String original = v.getOriginalText() != null ? v.getOriginalText() : "";
            String proposed = modernizeText(original);
            String currentModern = v.getModernText() != null ? v.getModernText() : "";

            if (proposed.equals(currentModern)) {
                continue;
            }
            changed++;
            if (dryRun && samples.size() < DRY_RUN_SAMPLE_LIMIT) {
                samples.add(new ModernizationPreview(
                        v.getUid(),
                        reference(v),
                        truncate(currentModern),
                        truncate(proposed)));
            }
            if (!dryRun) {
                v.setModernText(proposed);
                toSave.add(v);
            }
        }

        if (!dryRun && !toSave.isEmpty()) {
            verseRepository.saveAll(toSave);
        }

        return new ModernizationResponse(dryRun, all.size(), changed, List.copyOf(samples));
    }

    /**
     * Full pipeline: static dictionary (whole-word) then pattern-based U/V, silent-e strip, and {@code nesse}.
     */
    public String modernizeText(String text) {
        if (text == null) {
            return null;
        }
        if (text.isEmpty()) {
            return "";
        }
        return applyPatternRules(applyReplacementRules(text));
    }

    /** Applies static dictionary rules only (whole-word {@code \\b}). */
    public String applyReplacementRules(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? null : "";
        }
        String result = text;
        for (Map.Entry<String, String> e : REPLACEMENT_RULES.entrySet()) {
            result = replaceWholeWord(result, e.getKey(), e.getValue());
        }
        return result;
    }

    /**
     * Pattern stage after dictionary: (1) leading v→u before consonant, (2) u→v between vowels, (3) double consonant
     * + final e stripped, (4) {@code nesse}→{@code ness}. Capitalization follows {@link #matchCase(String, String)}.
     */
    public String applyPatternRules(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? null : "";
        }
        String s = applyLeadingVToU(text);
        s = applyUBetweenVowelsToV(s);
        s = applyDoubleConsonantStripE(s);
        s = applyNesseToNess(s);
        return s;
    }

    /**
     * 1611 orthography often used 'v' at the beginning of words where modern English uses 'u'.
     * This rule finds a leading 'v' followed by a consonant (e.g. "vnto") and replaces it with 'u' ("unto").
     */
    private static String applyLeadingVToU(String text) {
        Matcher m = LEADING_V_PLUS_CONSONANT.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String vLetter = m.group(1);
            String consonant = m.group(2);
            String repl = ("V".equals(vLetter) ? "U" : "u") + consonant;
            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 1611 orthography often used 'u' in the middle of words where modern English uses 'v'.
     * This rule targets a 'u' surrounded by vowels (e.g. "haue", "loue") and replaces it with 'v' ("have", "love").
     * Note: Words like "house" or "count" are naturally ignored because the 'u' is adjacent to a consonant.
     */
    private static String applyUBetweenVowelsToV(String text) {
        Matcher m = U_BETWEEN_VOWELS.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String uLetter = m.group(1);
            String repl = "U".equals(uLetter) ? "V" : "v";
            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 1611 orthography often doubled consonants and appended a silent 'e' to the end of words.
     * This rule strips the final 'e' and reduces the double consonant to a single one (e.g. "sonne" -> "son").
     */
    private static String applyDoubleConsonantStripE(String text) {
        Matcher m = DOUBLE_CONSONANT_FINAL_E.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String full = m.group(0);
            String canonical = (m.group(1) + m.group(2)).toLowerCase(Locale.ROOT);
            m.appendReplacement(sb, Matcher.quoteReplacement(matchCase(full, canonical)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 1611 orthography often spelled the "-ness" suffix as "-nesse".
     * This rule strips the trailing 'e' from words ending in "nesse" (e.g. "darknesse" -> "darkness").
     */
    private static String applyNesseToNess(String text) {
        Matcher m = NESSE_SUFFIX.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String full = m.group(0);
            String canonical = m.group(1).toLowerCase(Locale.ROOT) + "ness";
            m.appendReplacement(sb, Matcher.quoteReplacement(matchCase(full, canonical)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String replaceWholeWord(String text, String fromWord, String toWord) {
        Pattern p = Pattern.compile("\\b" + Pattern.quote(fromWord) + "\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String matched = m.group();
            String replacement = matchCase(matched, toWord);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Mirrors ALL CAPS, Title Case, or lower casing of the matched token onto {@code replacement}.
     * This is critical for the "Modern Text" UI so that capitalized words in the 1611 text (like "Vnto")
     * remain capitalized when modernized (like "Unto").
     */
    static String matchCase(String matched, String replacement) {
        if (matched.isEmpty() || replacement.isEmpty()) {
            return replacement;
        }
        if (matched.length() > 1 && matched.equals(matched.toUpperCase(Locale.ROOT))) {
            return replacement.toUpperCase(Locale.ROOT);
        }
        if (Character.isUpperCase(matched.charAt(0))) {
            String restMatched = matched.substring(1);
            boolean restLower = restMatched.equals(restMatched.toLowerCase(Locale.ROOT));
            if (restLower) {
                return Character.toUpperCase(replacement.charAt(0))
                        + replacement.substring(1).toLowerCase(Locale.ROOT);
            }
            return Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
        }
        return replacement.toLowerCase(Locale.ROOT);
    }

    private static String reference(Verse v) {
        return v.getBook() + " " + v.getChapter() + ":" + v.getVerse();
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        if (s.length() <= PREVIEW_MAX_LEN) {
            return s;
        }
        return s.substring(0, PREVIEW_MAX_LEN) + "…";
    }
}
