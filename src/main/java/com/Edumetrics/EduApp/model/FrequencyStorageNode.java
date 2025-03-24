package com.Edumetrics.EduApp.model;

/**
 * This class stores details for AVL Tree Node
 * It stores word, frequency and height associated with the subtree
 * */
public class FrequencyStorageNode {
	public String word;
	public int frequency;
	public int height;
	public FrequencyStorageNode rightKeywordTree;
	public FrequencyStorageNode leftKeywordTree;
	public FrequencyStorageNode(String keyword, int frequency){
		this.word=keyword;
		this.frequency=frequency;
		this.height=1;
		this.rightKeywordTree=null;
		this.leftKeywordTree=null;
	}
	
	
}
