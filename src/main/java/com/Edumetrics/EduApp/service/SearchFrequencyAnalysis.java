package com.Edumetrics.EduApp.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class SearchFrequencyAnalysis {

    private final Map<String, Integer> termOccurrence = new HashMap<>();

    /**
     * Processes a CSV file and counts occurrences of the specified term
     *
     * @param sourceFile The path to the CSV file
     * @param searchTerm The term to search for
     * @return The total count of occurrences
     * @throws IOException If there's an error reading the file
     */
    public int processFileAndCountTerm(String sourceFile, String searchTerm) throws IOException {
        // Clear previous counts
        termOccurrence.clear();

        // Process the file
        processFileForTermCount(sourceFile, searchTerm);

        // Return the count
        return getTermOccurrence(searchTerm);
    }

    /**
     * Processes a CSV file, tallying the occurrences of a specific term within the "text" column,
     * employing an efficient pattern-matching approach.
     *
     * @param sourceFile The path to the CSV file.
     * @param searchTerm The specific term to locate within each text entry.
     * @throws IOException If a problem arises during file reading.
     */
    private void processFileForTermCount(String sourceFile, String searchTerm) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            String dataRow;
            boolean ignoreHeader = true;
            while ((dataRow = reader.readLine()) != null) {
                if (ignoreHeader) {
                    ignoreHeader = false;
                    continue;
                }
                String dataEntry = dataRow.trim();
                updateTermOccurrence(dataEntry, searchTerm);
            }
        }
    }

    /**
     * Updates the tracked count of a specific term within a given text segment, using a fast search method.
     *
     * @param textChunk The text segment under analysis.
     * @param targetTerm The term whose occurrences we're counting.
     * @return The updated total count for the term.
     */
    private int updateTermOccurrence(String textChunk, String targetTerm) {
        int count = rapidSearch(textChunk, targetTerm);
        termOccurrence.put(targetTerm, termOccurrence.getOrDefault(targetTerm, 0) + count);
        return termOccurrence.get(targetTerm);
    }

    /**
     * Obtains the accumulated count for a particular term that has been analyzed.
     *
     * @param queryTerm The term being queried.
     * @return The total number of times the term was encountered, or 0 if it hasn't been analyzed.
     */
    private int getTermOccurrence(String queryTerm) {
        return termOccurrence.getOrDefault(queryTerm, 0);
    }

    /**
     * Executes a quick string search (resembling KMP) for efficient pattern identification.
     *
     * @param fullText    The string in which to search.
     * @param searchText The pattern to find.
     * @return The number of times the pattern is present in the string.
     */
    private int rapidSearch(String fullText, String searchText) {
        if (fullText == null || searchText == null || fullText.isEmpty() || searchText.isEmpty()) {
            return 0;
        }
        int[] borderTable = generateBorderTable(searchText);
        int textIndex = 0;
        int patternIndex = 0;
        int textLength = fullText.length();
        int patternLength = searchText.length();
        int foundCount = 0;

        while (textIndex < textLength) {
            if (searchText.charAt(patternIndex) == fullText.charAt(textIndex)) {
                patternIndex++;
                textIndex++;
            }

            if (patternIndex == patternLength) {
                foundCount++;
                patternIndex = borderTable[patternIndex - 1];
            } else if (textIndex < textLength && searchText.charAt(patternIndex) != fullText.charAt(textIndex)) {
                if (patternIndex != 0) {
                    patternIndex = borderTable[patternIndex - 1];
                } else {
                    textIndex++;
                }
            }
        }

        return foundCount;
    }

    /**
     * Creates a border table (akin to the LPS array in KMP) to speed up the search process.
     *
     * @param searchPattern The pattern for which to construct the border table.
     * @return The generated border table.
     */
    private int[] generateBorderTable(String searchPattern) {
        int patternLength = searchPattern.length();
        int[] borderTable = new int[patternLength];
        borderTable[0] = 0;

        int k = 0;
        int i = 1;

        while (i < patternLength) {
            if (searchPattern.charAt(i) == searchPattern.charAt(k)) {
                k++;
                borderTable[i] = k;
                i++;
            } else {
                if (k != 0) {
                    k = borderTable[k - 1];
                } else {
                    borderTable[i] = 0;
                    i++;
                }
            }
        }

        return borderTable;
    }
}
