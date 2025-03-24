package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.TrieNode;
/*
 * This class Use Tries for string Data
 * */
public class StoreDataUsingTrie {
	private TrieNode baseNode;
	private int indexSeries=-1;

	//Constructor
	StoreDataUsingTrie(){
		this.baseNode=new TrieNode();
	}
	
	//Inserts words in Trie And Update its Index
	public int insertWordInTrie(String keyword)  throws Exception{
		TrieNode currentWord=baseNode;
		for(char ch: keyword.toCharArray()) {
			try {
				TrieNode test=currentWord.trieNextElementList[ch-'a'];
			}catch(Exception e) {
				System.out.println("Word is"+ ch);
			}
			TrieNode nextNodeData=currentWord.trieNextElementList[ch-'a'];
			if(nextNodeData==null) {
				TrieNode newWordNode=new TrieNode();
				newWordNode.WordCharacter=ch;
				currentWord.trieNextElementList[ch-'a']=newWordNode;
			}
			currentWord=currentWord.trieNextElementList[ch-'a'];
		}
		currentWord.isEndingWord=true;
		if(currentWord.indexNo==-1) {
			currentWord.indexNo=++indexSeries;
		}
		return currentWord.indexNo;
		
		
	}
	
	//This Method searches of Word in Trie
	public TrieNode searchWordInTrie(String searchWord) throws Exception{
		TrieNode currentWord=baseNode;
		for(char ch: searchWord.toCharArray()) {
			TrieNode nextNodeData=currentWord.trieNextElementList[ch-'a'];
			if(nextNodeData==null) {
				return nextNodeData;
			}
			currentWord=currentWord.trieNextElementList[ch-'a'];
		}
		return currentWord;	
	}
	
	//Returns present Index series going
	public int getCurrentIndexSeries() {
		return indexSeries;
	}
	
	//Returns the Base Root Node of the trie
	public TrieNode getRootNode() {
		return this.baseNode;
	}
	
	
	
}
