package com.zioneltechnology.kjva_bible_api.dto;

/**
 * One verse that would be updated by {@link com.zioneltechnology.kjva_bible_api.service.ModernizationService}.
 */
public record ModernizationPreview(
        Long verseUid,
        String reference,
        String modernTextBefore,
        String modernTextAfter) {
}
