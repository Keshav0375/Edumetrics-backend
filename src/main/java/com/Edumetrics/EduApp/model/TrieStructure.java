package com.Edumetrics.EduApp.model;

import java.util.*;

public class TrieStructure {

    private TrieNode root;

    public TrieStructure() {
        root = new TrieNode();
    }

    public static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord;
    }

    /**
     * Inserts a word into the trie.
     */
    public void insert(String word) {
        if (word == null) return;
        TrieNode current = root;
        for (char ch : word.toLowerCase().toCharArray()) {
            current.children.putIfAbsent(ch, new TrieNode());
            current = current.children.get(ch);
        }
        current.isEndOfWord = true;
    }

    /**
     * Returns true if the word exists in the trie.
     */
    public boolean search(String word) {
        TrieNode node = searchNode(word);
        return node != null && node.isEndOfWord;
    }

    private TrieNode searchNode(String word) {
        TrieNode current = root;
        for (char ch : word.toLowerCase().toCharArray()) {
            current = current.children.get(ch);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * Gets suggestions for a given prefix up to a specified limit.
     */
    public List<String> getSuggestions(String prefix, int limit) {
        List<String> results = new ArrayList<>();
        TrieNode node = searchNode(prefix);
        if (node == null) return results;
        dfs(node, new StringBuilder(prefix), results, limit);
        return results;
    }

    private void dfs(TrieNode node, StringBuilder currentWord, List<String> results, int limit) {
        if (results.size() >= limit) {
            return;
        }
        if (node.isEndOfWord) {
            results.add(currentWord.toString());
        }
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            currentWord.append(entry.getKey());
            dfs(entry.getValue(), currentWord, results, limit);
            currentWord.deleteCharAt(currentWord.length() - 1);
        }
    }

    /**
     * Retrieves all words stored in the trie.
     */
    public List<String> getAllWords() {
        List<String> words = new ArrayList<>();
        dfs(root, new StringBuilder(), words, Integer.MAX_VALUE);
        return words;
    }
}
