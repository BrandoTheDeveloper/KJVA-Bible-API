package com.zioneltechnology.kjva_bible_api.taxonomy;

import java.util.Set;

/**
 * Maps each book to a stable category code. {@code section} is OT, AP, or NT (folder-derived).
 */
public final class BookCategoryResolver {

    public static final Set<String> TANAKH_CATEGORIES = Set.of(
            "TORAH",
            "HISTORY",
            "WISDOM",
            "MAJOR_PROPHETS",
            "MINOR_PROPHETS");

    /** Major + minor prophets (OT prophetic books in this corpus). */
    public static final Set<String> PROPHETS_CATEGORIES = Set.of(
            "MAJOR_PROPHETS",
            "MINOR_PROPHETS");

    private BookCategoryResolver() {
    }

    public static String resolve(String section, String book) {
        if (section == null || book == null) {
            return "UNKNOWN";
        }
        String s = section.trim().toUpperCase();
        String b = book.trim();
        return switch (s) {
            case "AP" -> "APOCRYPHA";
            case "NT" -> categoryForNewTestament(b);
            case "OT" -> categoryForOldTestament(b);
            default -> "UNKNOWN";
        };
    }

    private static String categoryForOldTestament(String book) {
        return switch (book) {
            case "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy" -> "TORAH";
            case "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel", "1 Kings", "2 Kings",
                    "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah", "Esther" -> "HISTORY";
            case "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon" -> "WISDOM";
            case "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel" -> "MAJOR_PROPHETS";
            case "Hosea", "Joel", "Amos", "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk",
                    "Zephaniah", "Haggai", "Zechariah", "Malachi" -> "MINOR_PROPHETS";
            default -> "UNKNOWN";
        };
    }

    private static String categoryForNewTestament(String book) {
        return switch (book) {
            case "Matthew", "Mark", "Luke", "John" -> "GOSPELS";
            case "Acts" -> "ACTS";
            case "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
                    "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
                    "1 Timothy", "2 Timothy", "Titus", "Philemon" -> "PAULINE_EPISTLES";
            case "Hebrews", "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude" ->
                    "GENERAL_EPISTLES";
            case "Revelation" -> "REVELATION";
            default -> "UNKNOWN";
        };
    }
}
