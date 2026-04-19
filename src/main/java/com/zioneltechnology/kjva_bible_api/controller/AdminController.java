package com.zioneltechnology.kjva_bible_api.controller;

import com.zioneltechnology.kjva_bible_api.dto.ModernizationResponse;
import com.zioneltechnology.kjva_bible_api.service.ModernizationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ModernizationService modernizationService;

    public AdminController(ModernizationService modernizationService) {
        this.modernizationService = modernizationService;
    }

    /**
     * Bulk-refresh {@code modernText} from {@code originalText} using 1611→modern word rules.
     *
     * @param dryRun when {@code true}, no saves; response includes up to 10 sample verses that would change.
     */
    @PostMapping("/modernize")
    public ModernizationResponse modernize(@RequestParam(defaultValue = "false") boolean dryRun) {
        return modernizationService.modernizeAll(dryRun);
    }
}
