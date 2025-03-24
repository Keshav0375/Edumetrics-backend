package com.Edumetrics.EduApp.service;

import java.util.ArrayList;

import com.Edumetrics.EduApp.model.FrequencyStorageNode;

/*
 * This class performs AVL Tree operations 
 **/
class StoreDataUsingAVL{
	private ArrayList<FrequencyStorageNode> totalWordsList;
	private  FrequencyStorageNode rootwordNode;
	
	//Constructor
	StoreDataUsingAVL(){
		this.totalWordsList=new ArrayList<>();
		this.rootwordNode=null;
	}
	//Method returns the height of the sub-tree
	private int fetchHeight(FrequencyStorageNode node) {
		if(node==null) {
			return 0;
		}
		return node.height;
	}
	
	//This Method adds Keyword Node in Tree, balances, rotates the tree 
	private FrequencyStorageNode insertWord(String keyword, int frequency, FrequencyStorageNode rootNode) throws Exception{
			if(rootNode==null) {
				FrequencyStorageNode newRootNode= new FrequencyStorageNode(keyword, frequency);
				totalWordsList.add(newRootNode);
				return newRootNode;
			}
			if (rootNode.word.equals(keyword)) {
				rootNode.frequency += frequency;
		        return rootNode;
		    }
			if(rootNode.word.compareTo(keyword)<0) {
				rootNode.rightKeywordTree= insertWord(keyword, frequency, rootNode.rightKeywordTree);
			}else {
				rootNode.leftKeywordTree= insertWord(keyword, frequency, rootNode.leftKeywordTree);
			}
			rootNode.height= 1+ Math.max(fetchHeight( rootNode.rightKeywordTree), fetchHeight(rootNode.leftKeywordTree));
			
			int balanceAmount=fetchBalanceAmount(rootNode); 
			
			if(balanceAmount>1 && keyword.compareTo(rootNode.leftKeywordTree.word)<0) {
				return rightRotateData(rootNode);
			}
			if(balanceAmount<-1 && keyword.compareTo(rootNode.rightKeywordTree.word)>0) {
				return leftRotateData(rootNode);
			}
			if(balanceAmount>1 && keyword.compareTo(rootNode.leftKeywordTree.word)>0) {
				rootNode.leftKeywordTree= leftRotateData(rootNode.leftKeywordTree);
				return rightRotateData(rootNode);
			}
			if(balanceAmount<-1 && keyword.compareTo(rootNode.rightKeywordTree.word)<0) {
				rootNode.rightKeywordTree= rightRotateData(rootNode.rightKeywordTree);
				return leftRotateData(rootNode);
			}
			return rootNode;
		
	}
	
	
	//Searches for keyword and if not found inserts it
	public void addWordInStructure(String keyword, int frequency) {
		try {
			if(keyword==null||keyword.isBlank()) {
				System.out.println("Keyword received is Empty or null "+keyword);
				return;
			}
			
			FrequencyStorageNode iskeywordExist=searchKeyword(keyword, this.rootwordNode);
			
			if(iskeywordExist!=null) {
				searchAndUpdateKeyword(keyword, 1, rootwordNode);
				return;
			}
			this.rootwordNode=insertWord(keyword, frequency, this.rootwordNode);
			 		
		}catch(Exception e) {
			System.out.println("Exception arised while adding word "+ keyword+" as "+e);
		}
		return;
	}
	
	
	//Searches and updates the frequency of keyword
	public boolean searchAndUpdateKeyword(String keyword, int updatedFrequency,FrequencyStorageNode node) throws Exception{
		if(keyword==null || keyword.isBlank() || node==null) {
			return false;
		}
		
		if(node.word.equals(keyword)) {
			node.frequency+=updatedFrequency;
			return true;
		}
		if(keyword.compareTo(node.word)>=0) {
			return searchAndUpdateKeyword(keyword, updatedFrequency, node.rightKeywordTree);
		}else {
			return searchAndUpdateKeyword(keyword, updatedFrequency, node.leftKeywordTree);
		}
		
	}
	
	
	//It searches the keyword on BST tree conditions
	public FrequencyStorageNode searchKeyword(String keyword,FrequencyStorageNode node) throws Exception{
		if(node==null) {
			return null;
		}
		if(node.word.equals(keyword)) {
			return node;
		}
		if(keyword.compareTo(node.word)>=0) {
			return searchKeyword(keyword, node.rightKeywordTree);
		}else {
			return searchKeyword(keyword, node.leftKeywordTree);
		}
		
	}
	
	
	//It returns the balance factor
	private int fetchBalanceAmount(FrequencyStorageNode node) {
		if(node==null) {
			return 0;
		}
		return fetchHeight(node.leftKeywordTree)- fetchHeight(node.rightKeywordTree);	
		
	}
	
	//Performs Left Rotation on the given pivot Node
	private FrequencyStorageNode leftRotateData(FrequencyStorageNode pivotNode) throws Exception{
		FrequencyStorageNode rightChild= pivotNode.rightKeywordTree;
		FrequencyStorageNode rightChildeLeftChild = rightChild.leftKeywordTree;
		
		rightChild.leftKeywordTree=pivotNode;
		pivotNode.rightKeywordTree=rightChildeLeftChild;
		
		pivotNode.height=Math.max(fetchHeight( pivotNode.leftKeywordTree),fetchHeight(pivotNode.rightKeywordTree))+1;
		rightChild.height= Math.max(fetchHeight(rightChild.rightKeywordTree), fetchHeight(rightChild.leftKeywordTree))+1;
		return rightChild;
	}
	
	
	//Performs Right Rotation on the given pivot Node
	private FrequencyStorageNode rightRotateData(FrequencyStorageNode pivotNode) throws Exception {
		FrequencyStorageNode leftChild=pivotNode.leftKeywordTree;
		FrequencyStorageNode leftChildRightChild= leftChild.rightKeywordTree;
		
		leftChild.rightKeywordTree=pivotNode;
		pivotNode.leftKeywordTree=leftChildRightChild;
		
		pivotNode.height=Math.max(fetchHeight( pivotNode.leftKeywordTree),fetchHeight(pivotNode.rightKeywordTree))+1;
		leftChild.height= Math.max(fetchHeight(leftChild.rightKeywordTree), fetchHeight(leftChild.leftKeywordTree))+1;
		return leftChild;
		
	}
	
	//Returns the Root Node of Tree
	public FrequencyStorageNode getRootKeywood() {
		return this.rootwordNode;
	}
	
	
	//Returns the total Keywords stored in the tree
	public int getTotalKeywords() {
		return this.totalWordsList.size();
	}
	
	
	//Returns the list of Keyword Node
	public ArrayList<FrequencyStorageNode> getKeywordsDataList(){
		return this.totalWordsList;
	}
	
	
	//Calls printInOrderWise method
	public void printKeywordTree() {
		printInOrderWise(this.rootwordNode);
		return;
	}
	
	
	//Prints Tree in In-order Traversal
	private void printInOrderWise(FrequencyStorageNode keyword) {
		if(keyword==null) {
			return;
		}
		printInOrderWise(keyword.leftKeywordTree);
		System.out.println("Keyword is "+ keyword.word+" with Frequency "+ keyword.frequency);
			printInOrderWise(keyword.rightKeywordTree);
			return;
		}
}