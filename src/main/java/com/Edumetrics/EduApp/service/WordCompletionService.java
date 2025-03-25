package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.TrieStructure;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

@Service
public class WordCompletionService {

    private TrieStructure trie = new TrieStructure();

    /**
     * Initialize the trie by loading the CSV file from resources and inserting
     * each course title (from the “title” column).
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
            // Insert each title into the trie.
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length > titleIndex) {
                    String title = fields[titleIndex];
                    trie.insert(title);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading CSV file: " + e.getMessage(), e);
        }
    }

    /**
     * Returns up to 3 completion suggestions for a given query.
     */
    public List<String> getSuggestions(String query) {
        if (query == null || query.isEmpty()) {
            return List.of();
        }
        return trie.getSuggestions(query, 3);
    }
}
