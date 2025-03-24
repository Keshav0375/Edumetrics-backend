package com.Edumetrics.EduApp.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.Edumetrics.EduApp.model.WordFrequencyData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class WordFrequencyService {

    String filePath="C:\\Users\\DELL\\OneDrive - University of Windsor\\Desktop\\EduApp\\scraped_courses.csv";


    public WordFrequencyData countWordOccurrences(String word) {
        word = word.toLowerCase(); // Convert search word to lowercase for case-insensitive search
        Map<Character, Integer> badChar = buildBadCharTable(word);
        int count = countWordOccurrencesInFile(filePath, word, badChar);

        // Get top 5 frequent words
        Map<String, Integer> topWords = findTopFrequentWords(5);

        return new WordFrequencyData(count, topWords);
    }

    public Map<String, Integer> findTopFrequentWords(int topCount) {
        Map<String, Integer> wordFrequencies = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Splitting the line into words (considering spaces, commas, etc. as separators)
                String[] words = line.toLowerCase().split("[\\s,;.\"\\(\\)\\[\\]\\{\\}]+");

                for (String word : words) {
                    // Skip empty strings and very short words (likely not meaningful)
                    if (word.length() <= 1 || isCommonStopWord(word)) {
                        continue;
                    }

                    // Increment word count
                    wordFrequencies.put(word, wordFrequencies.getOrDefault(word, 0) + 1);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + e.getMessage(), e);
        }

        // Sort by frequency (descending) and get top N words
        return wordFrequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topCount)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    // Helper method to filter out common stop words that don't add meaning
    private boolean isCommonStopWord(String word) {
        Set<String> stopWords = Set.of("the", "and", "a", "to", "of", "in", "for", "is", "on", "that", "by", "this", "with", "i", "you", "it");
        return stopWords.contains(word);
    }

    private Map<Character, Integer> buildBadCharTable(String pattern) {
        int m = pattern.length();
        Map<Character, Integer> badChar = new HashMap<>();
        for (int i = 0; i < m; i++) {
            badChar.put(pattern.charAt(i), i);
        }
        return badChar;
    }

    private int countWordOccurrencesInFile(String filePath, String word, Map<Character, Integer> badChar) {
        int totalCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalCount += countOccurrences(line.toLowerCase(), word, badChar);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + e.getMessage(), e);
        }
        return totalCount;
    }

    private int countOccurrences(String text, String pattern, Map<Character, Integer> badChar) {
        int n = text.length();
        int m = pattern.length();
        int count = 0;
        int s = 0;

        while (s <= n - m) {
            int j = m - 1;
            while (j >= 0 && pattern.charAt(j) == text.charAt(s + j)) {
                j--;
            }
            if (j < 0) {
                count++;
                s += m;
            } else {
                char badCharInText = text.charAt(s + j);
                int shift = badChar.getOrDefault(badCharInText, -1);
                s += Math.max(1, j - shift);
            }
        }
        return count;
    }
}