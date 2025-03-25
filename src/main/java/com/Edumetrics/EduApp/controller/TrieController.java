package com.Edumetrics.EduApp.controller;

import com.Edumetrics.EduApp.service.WordCompletionService;
import com.Edumetrics.EduApp.service.SpellCheckerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trie")
public class TrieController {

    private final WordCompletionService wordCompletionService;
    private final SpellCheckerService spellCheckerService;

    @Autowired
    public TrieController(WordCompletionService wordCompletionService, SpellCheckerService spellCheckerService) {
        this.wordCompletionService = wordCompletionService;
        this.spellCheckerService = spellCheckerService;
    }

    /**
     * Endpoint for word completions.
     * Returns up to 3 suggestions based on the query.
     */
    @GetMapping("/completions")
    public ResponseEntity<?> getCompletions(@RequestParam("query") String query) {
        try {
            return ResponseEntity.ok(wordCompletionService.getSuggestions(query));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving suggestions: " + e.getMessage());
        }
    }

    /**
     * Endpoint for spell checking.
     * Returns up to 3 corrected words based on the query.
     */
    @GetMapping("/spellcheck")
    public ResponseEntity<?> getCorrections(@RequestParam("query") String query) {
        try {
            return ResponseEntity.ok(spellCheckerService.getCorrections(query));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving corrections: " + e.getMessage());
        }
    }
}
