package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.TrieStructure;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpellCheckerService {

    private TrieStructure trie = new TrieStructure();

    /**
     * Initialize the trie by loading the CSV file from the absolute file path and inserting
     * each distinct word from the course title (using the “title” column).
     */
    @PostConstruct
    public void init() {
        try {
            InputStream inputStream = new FileInputStream("C:\\Users\\DELL\\OneDrive - University of Windsor\\Desktop\\EduApp\\scraped_courses.csv");
            if (inputStream == null) {
                throw new RuntimeException("scraping_courses.csv not found in resources");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            // Read header and determine the index of the title column.
            String header = reader.readLine();
            int titleIndex = -1;
            if (header != null) {
                String[] columns = header.split(",");
                for (int i = 0; i < columns.length; i++) {
                    if (columns[i].trim().equalsIgnoreCase("title")) {
                        titleIndex = i;
                        break;
                    }
                }
            }
            if (titleIndex == -1) {
                throw new RuntimeException("Title column not found in CSV");
            }
            // Use a set to avoid duplicate words.
            Set<String> words = new HashSet<>();
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length > titleIndex) {
                    String title = fields[titleIndex];
                    // Split title into individual words based on non-alphanumeric characters.
                    String[] titleWords = title.split("[^a-zA-Z0-9]+");
                    for (String word : titleWords) {
                        if (!word.isEmpty()) {
                            words.add(word.toLowerCase());
                        }
                    }
                }
            }
            // Insert each distinct word into the trie.
            for (String word : words) {
                trie.insert(word);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading CSV file: " + e.getMessage(), e);
        }
    }

    /**
     * Returns up to 3 corrections for the input query based on edit distance.
     */
    public List<String> getCorrections(String query) {
        if (query == null || query.isEmpty()) {
            return List.of();
        }
        List<String> allWords = trie.getAllWords();
        return allWords.stream()
                .sorted((w1, w2) -> Integer.compare(editDistance(query.toLowerCase(), w1.toLowerCase()),
                        editDistance(query.toLowerCase(), w2.toLowerCase())))
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * Calculates the edit distance between two strings.
     */
    private int editDistance(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }
        return dp[m][n];
    }
}
