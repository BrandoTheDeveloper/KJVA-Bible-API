package com.zioneltechnology.kjva_bible_api.dto;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BookDTO {
    public String book;

    @JsonProperty("chapter-count") // This maps the hyphenated JSON key to Java
    public String chapterCount;

    public List<ChapterDTO> chapters;
}