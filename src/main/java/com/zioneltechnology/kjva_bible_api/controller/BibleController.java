package com.zioneltechnology.kjva_bible_api.controller;

import com.zioneltechnology.kjva_bible_api.exception.ResourceNotFoundException;
import com.zioneltechnology.kjva_bible_api.model.Verse;
import com.zioneltechnology.kjva_bible_api.repository.VerseRepository;
import com.zioneltechnology.kjva_bible_api.taxonomy.BookCategoryResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bible")
public class BibleController {

    private final VerseRepository verseRepo;

    public BibleController(VerseRepository verseRepo) {
        this.verseRepo = verseRepo;
    }

    @GetMapping("/section/{sectionName}")
    public ResponseEntity<Page<Verse>> getBySection(
            @PathVariable String sectionName,
            @PageableDefault(size = 25, sort = "uid", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Verse> result = verseRepo.findBySectionIgnoreCase(sectionName, pageable);
        if (result.isEmpty()) {
            throw new ResourceNotFoundException("Section not found: " + sectionName);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Use a stored category (e.g. TORAH, GOSPELS), {@code tanakh} for the full OT Hebrew-bible
     * split in this corpus, or {@code prophets} for major + minor prophets together.
     */
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<Page<Verse>> getByCategory(
            @PathVariable String categoryName,
            @PageableDefault(size = 25, sort = "uid", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Verse> result;
        if ("tanakh".equalsIgnoreCase(categoryName)) {
            result = verseRepo.findByCategoryIgnoreCaseIn(BookCategoryResolver.TANAKH_CATEGORIES, pageable);
        } else if ("prophets".equalsIgnoreCase(categoryName)) {
            result = verseRepo.findByCategoryIgnoreCaseIn(BookCategoryResolver.PROPHETS_CATEGORIES, pageable);
        } else {
            result = verseRepo.findByCategoryIgnoreCase(categoryName, pageable);
        }
        if (result.isEmpty()) {
            throw new ResourceNotFoundException("Category not found: " + categoryName);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{book}/{chapter}/{verse}")
    public ResponseEntity<Verse> getVerseText(
            @PathVariable String book,
            @PathVariable int chapter,
            @PathVariable int verse) {
        return verseRepo.findByBookIgnoreCaseAndChapterAndVerse(book, chapter, verse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Verse not found: " + book + " " + chapter + ":" + verse));
    }

    @GetMapping("/{book}/{chapter}")
    public ResponseEntity<List<Verse>> getChapter(
            @PathVariable String book,
            @PathVariable int chapter) {
        List<Verse> result = verseRepo.findByBookIgnoreCaseAndChapter(book, chapter);
        if (result.isEmpty()) {
            throw new ResourceNotFoundException("Chapter not found: " + book + " " + chapter);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{bookName}")
    public ResponseEntity<Page<Verse>> getVersesByBook(
            @PathVariable String bookName,
            @PageableDefault(size = 25, sort = "uid", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Verse> result = verseRepo.findByBookIgnoreCase(bookName, pageable);
        if (result.isEmpty()) {
            throw new ResourceNotFoundException("Book not found: " + bookName);
        }
        return ResponseEntity.ok(result);
    }
}
