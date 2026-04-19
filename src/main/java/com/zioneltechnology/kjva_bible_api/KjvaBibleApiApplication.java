package com.zioneltechnology.kjva_bible_api;

import com.zioneltechnology.kjva_bible_api.model.Verse;
import com.zioneltechnology.kjva_bible_api.repository.VerseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zioneltechnology.kjva_bible_api.dto.*;
import com.zioneltechnology.kjva_bible_api.service.DataCleaningService;
import com.zioneltechnology.kjva_bible_api.service.ModernizationService;
import com.zioneltechnology.kjva_bible_api.taxonomy.BookCategoryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class KjvaBibleApiApplication {

    private static final Logger log = LoggerFactory.getLogger(KjvaBibleApiApplication.class);

    private static final List<String> BOOK_ORDER = List.of(
            // OT
            "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
            "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel",
            "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles",
            "Ezra", "Nehemiah", "Esther", "Job", "Psalms",
            "Proverbs", "Ecclesiastes", "Song of Solomon", "Isaiah",
            "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea",
            "Joel", "Amos", "Obadiah", "Jonah", "Micah", "Nahum",
            "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi",
            // AP
            "1 Esdras", "2 Esdras", "Tobit", "Judith", "Rest of Esther",
            "Wisdom of Solomon", "Sirach", "Baruch", "Letter of Jeremiah",
            "Prayer of Azariah", "Susanna", "Bel and the Dragon",
            "Prayer of Manasseh", "1 Maccabees", "2 Maccabees",
            // NT
            "Matthew", "Mark", "Luke", "John", "Acts", "Romans",
            "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
            "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians",
            "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews",
            "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John",
            "Jude", "Revelation"
    );

    private int getBookRank(String bookName) {
        int index = BOOK_ORDER.indexOf(bookName);
        return index != -1 ? index : 999;
    }

    private int getSectionRank(Resource resource) {
        try {
            String path = resource.getURL().getPath();
            if (path.contains("/OT/")) return 1;
            if (path.contains("/AP/")) return 2;
            if (path.contains("/NT/")) return 3;
        } catch (Exception e) {
            // ignore
        }
        return 99;
    }

	public static void main(String[] args) {
		SpringApplication.run(KjvaBibleApiApplication.class, args);
	}

	@Bean
CommandLineRunner init(VerseRepository verseRepo, ModernizationService modernizationService, DataCleaningService dataCleaningService) {
    return args -> {
        if (verseRepo.count() > 0) {
            log.info("Database already populated.");
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        
        // We look for any JSON file inside any subfolder of bible-data
        Resource[] resources = resolver.getResources("classpath:bible-data-kjv1611/**/*.json");

        Arrays.sort(resources, (r1, r2) -> {
            int sec1 = getSectionRank(r1);
            int sec2 = getSectionRank(r2);
            if (sec1 != sec2) {
                return Integer.compare(sec1, sec2);
            }
            
            String book1 = r1.getFilename() != null ? r1.getFilename().replace(".json", "") : "";
            String book2 = r2.getFilename() != null ? r2.getFilename().replace(".json", "") : "";
            
            return Integer.compare(getBookRank(book1), getBookRank(book2));
        });

        for (Resource resource : resources) {
            // This gets the name of the folder the file is in (OT, AP, or NT)
            String path = resource.getURL().getPath();
            String sectionCode = "OT";
            if (path.contains("/AP/")) {
                sectionCode = "AP";
            } else if (path.contains("/NT/")) {
                sectionCode = "NT";
            } else if (path.contains("/OT/")) {
                sectionCode = "OT";
            }

            BookDTO bookData = mapper.readValue(resource.getInputStream(), BookDTO.class);
            String category = BookCategoryResolver.resolve(sectionCode, bookData.book);
            List<Verse> flatVerses = new ArrayList<>();

            for (ChapterDTO chapterData : bookData.chapters) {
                for (VerseDTO verseData : chapterData.verses) {
                    Verse v = new Verse();
                    v.setSection(sectionCode);
                    v.setCategory(category);
                    v.setBook(bookData.book);
                    v.setChapter(chapterData.chapter);
                    v.setVerse(verseData.verse);
                    v.setOriginalText(verseData.text);
                    v.setModernText(modernizationService.modernizeText(dataCleaningService.cleanText(verseData.text)));
                    flatVerses.add(v);
                }
            }
            verseRepo.saveAll(flatVerses);
            log.info("Imported {} ({}) into {}", bookData.book, category, sectionCode);
        }
    };
}
}
