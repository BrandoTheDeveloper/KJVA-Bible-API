package com.zioneltechnology.kjva_bible_api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name = "verses")
public class Verse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long uid;
    /** Canon part: OT, AP, or NT. */
    private String section;
    /** e.g. TORAH, HISTORY, WISDOM, GOSPELS, APOCRYPHA — see {@code BookCategoryResolver}. */
    private String category;
    private String book;
    private int chapter;
    private int verse;
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String modernText;
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String originalText;

    // Constructors
    // No Args
    public Verse() {}

    // Paramaterized
    public Verse(String section, String category, String book, int chapter, int verse, String modernText, String originalText) {
        this.section = section;
        this.category = category;
        this.book = book;
        this.chapter = chapter;
        this.verse = verse;
        this.modernText = modernText;
        this.originalText = originalText;
    }

    // Getters
    public long getUid() {
        return uid;
    }
    public void setUid(long uid) {
        this.uid = uid;
    }
    public String getSection() {
        return section;
    }
    public void setSection(String section) {
        this.section = section;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public String getBook() {
        return book;
    }
    public void setBook(String book) {
        this.book = book;
    }
    public int getChapter() {
        return chapter;
    }
    public void setChapter(int chapter) {
        this.chapter = chapter;
    }
    public int getVerse() {
        return verse;
    }
    public void setVerse(int verse) {
        this.verse = verse;
    }
    public String getModernText() {
        return modernText;
    }
    public void setModernText(String modernText) {
        this.modernText = modernText;
    }
    public String getOriginalText() {
        return originalText;
    }
    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }


}
