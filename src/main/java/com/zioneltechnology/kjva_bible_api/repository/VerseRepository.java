package com.zioneltechnology.kjva_bible_api.repository;

import com.zioneltechnology.kjva_bible_api.model.Verse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

import java.util.Optional;

public interface VerseRepository extends JpaRepository<Verse, Long> {
    Page<Verse> findBySectionIgnoreCase(String section, Pageable pageable);
    Page<Verse> findByCategoryIgnoreCase(String category, Pageable pageable);
    Page<Verse> findByCategoryIgnoreCaseIn(Collection<String> categories, Pageable pageable);
    Page<Verse> findByBookIgnoreCase(String book, Pageable pageable);
    List<Verse> findByBookIgnoreCaseAndChapter(String book, int chapter);
    Optional<Verse> findByBookIgnoreCaseAndChapterAndVerse(String book, int chapter, int verse);
}
