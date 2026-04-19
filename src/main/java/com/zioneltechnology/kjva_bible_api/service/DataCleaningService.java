package com.zioneltechnology.kjva_bible_api.service;

import org.springframework.stereotype.Service;

@Service
public class DataCleaningService {

    /**
     * Replaces HTML entities found in the 1611 KJV source with their standard characters.
     */
    public String cleanText(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("&thorn;", "th")
                   .replace("&amp;", "&")
                   .replace("&quot;", "\"")
                   .replace("&apos;", "'");
    }
}
