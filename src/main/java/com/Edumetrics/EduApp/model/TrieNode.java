package com.Edumetrics.EduApp.model;

/**
 * This class stores the Trie Node for the Tree
 * */
public class TrieNode {
	public boolean isEndingWord=false;
	public TrieNode trieNextElementList[]=new TrieNode[26];
	public char WordCharacter;
	public int indexNo=-1;
}
