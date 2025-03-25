package com.Edumetrics.EduApp.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SearchFrequencyService {
    private Map<String, Integer> searchFrequency;
    private List<String> csvContent;
    private static final String CSV_FILE_PATH = "C:\\Users\\HP\\Desktop\\University of Windsor\\Sem 2\\ACC\\Web-Scraping\\combined_website_data.csv";

    public SearchFrequencyService() {
        searchFrequency = new HashMap<>();
        csvContent = new ArrayList<>();
    }

    public void loadCSVFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                csvContent.add(line);
            }
            System.out.println("CSV file loaded successfully. " + csvContent.size() + " lines loaded.");
        } catch (IOException e) {
            System.err.println("Error loading CSV file: " + e.getMessage());
        }
    }

    public int search(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            System.out.println("Please provide a valid search pattern.");
            return 0;
        }

        // Update search frequency
        searchFrequency.put(pattern, searchFrequency.getOrDefault(pattern, 0) + 1);

        int occurrences = 0;
        for (String line : csvContent) {
            occurrences += countOccurrencesWithKMP(line, pattern);
        }

        return occurrences;
    }

    private int countOccurrencesWithKMP(String text, String pattern) {
        int count = 0;
        int[] lps = computeLPSArray(pattern);
        int i = 0; // index for text
        int j = 0; // index for pattern

        while (i < text.length()) {
            if (pattern.charAt(j) == text.charAt(i)) {
                i++;
                j++;
            }

            if (j == pattern.length()) {
                // Found a match
                count++;
                j = lps[j - 1]; // Look for the next match
            } else if (i < text.length() && pattern.charAt(j) != text.charAt(i)) {
                // Mismatch after j matches
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }

        return count;
    }

    private int[] computeLPSArray(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0;
        int i = 1;

        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }

        return lps;
    }

    public List<Map.Entry<String, Integer>> getTopSearches(int n) {
        List<Map.Entry<String, Integer>> sortedSearches = new ArrayList<>(searchFrequency.entrySet());

        // Sort by frequency (descending) and then by word (alphabetically) if frequencies are equal
        sortedSearches.sort((a, b) -> {
            int freqComparison = b.getValue().compareTo(a.getValue());
            if (freqComparison == 0) {
                return a.getKey().compareTo(b.getKey());
            }
            return freqComparison;
        });

        // Return top n searches or all if n is greater than the size
        return sortedSearches.subList(0, Math.min(n, sortedSearches.size()));
    }

    public static void main(String[] args) {
        SearchFrequencyService tracker = new SearchFrequencyService();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Loading CSV file from: " + CSV_FILE_PATH);
        tracker.loadCSVFile();

        while (true) {
            System.out.println("\n1. Search for a word");
            System.out.println("2. View top searched words");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.print("Enter the word to search: ");
                    String pattern = scanner.nextLine();
                    int occurrences = tracker.search(pattern);
                    System.out.println("Number of occurrences of '" + pattern + "': " + occurrences);
                    System.out.println("Search frequency for '" + pattern + "': " +
                            tracker.searchFrequency.get(pattern));
                    break;

                case 2:
                    System.out.print("Enter the number of top searches to display: ");
                    try {
                        int n = Integer.parseInt(scanner.nextLine());
                        List<Map.Entry<String, Integer>> topSearches = tracker.getTopSearches(n);
                        System.out.println("\nTop " + topSearches.size() + " searched words:");
                        for (int i = 0; i < topSearches.size(); i++) {
                            Map.Entry<String, Integer> entry = topSearches.get(i);
                            System.out.println((i + 1) + ". '" + entry.getKey() + "' - searched " +
                                    entry.getValue() + " times");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid number.");
                    }
                    break;

                case 3:
                    System.out.println("Exiting the program. Goodbye!");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}