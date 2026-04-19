package com.zioneltechnology.kjva_bible_api.dto;

import java.util.List;

public record ModernizationResponse(
        boolean dryRun,
        int versesScanned,
        int versesChanged,
        List<ModernizationPreview> sampleChanges) {
}
