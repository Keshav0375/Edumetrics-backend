package com.Edumetrics.EduApp.service;

import java.util.ArrayList;

import com.Edumetrics.EduApp.model.FrequencyStorageNode;
import com.Edumetrics.EduApp.model.URLFrequencyKeywordNode;

/*
 * This Class uses Heap And Heap Sort
 * */
public class SortUsingHeap {
	public ArrayList<URLFrequencyKeywordNode> listOfKeywordPageData;
	public String keywordtoBeSearched;
	public URLFrequencyKeywordNode pageData[];
	public int LastKeywordPointer;
	
	//Constructor
	SortUsingHeap(String keywordtoBeSearched){
			this.keywordtoBeSearched=keywordtoBeSearched;
			this.listOfKeywordPageData= new ArrayList<URLFrequencyKeywordNode>();
		}
	
	//From AVL Tree previously created searches for KeyWord Node and adds in List
	public void fetchKeywordFrequency(FrequencyStorageNode rootKeyword, String url) {
		try {
			StoreDataUsingAVL keyWordoperationsobject= new StoreDataUsingAVL();
			FrequencyStorageNode isKeywordPresent= keyWordoperationsobject.searchKeyword(this.keywordtoBeSearched, rootKeyword);
			if(isKeywordPresent!=null && !isKeywordPresent.word.isBlank()) {
				URLFrequencyKeywordNode keyWordPageNode=new URLFrequencyKeywordNode(keywordtoBeSearched, isKeywordPresent.frequency, url);
				listOfKeywordPageData.add(keyWordPageNode);
			}
			return;
		}catch(Exception e) {
			System.out.println("Exception arised in fetchKeywordFrequency as "+ e);
			return;
		}
	}
	
	//Converts the List  to Array
	private URLFrequencyKeywordNode[] convertToArray(ArrayList<URLFrequencyKeywordNode> toBeSortedPageList) {
		if(toBeSortedPageList==null) {
			return null;
		}
		URLFrequencyKeywordNode arr[]= new URLFrequencyKeywordNode[toBeSortedPageList.size()];
		for(int i=0;i<toBeSortedPageList.size();i++) {
			arr[i]=toBeSortedPageList.get(i);
		}
		return arr;
	}
	//Creates a Heap Of found Words based on Frequency
	private void createPageRankingHeap(URLFrequencyKeywordNode pageData[], int limit, int keywordPageIndex) throws Exception{
		int largestKeywordIndex=keywordPageIndex;
		int leftChildIndex=2*keywordPageIndex+1;
		int rightChildIndex=2*keywordPageIndex+2;
		
		if(leftChildIndex<limit && pageData[leftChildIndex].frequency>pageData[largestKeywordIndex].frequency) {
			largestKeywordIndex=leftChildIndex;
		}
		if(rightChildIndex<limit && pageData[rightChildIndex].frequency>pageData[largestKeywordIndex].frequency) {
			largestKeywordIndex=rightChildIndex;
		}
		
		if(largestKeywordIndex!=keywordPageIndex) {
			URLFrequencyKeywordNode temp=pageData[largestKeywordIndex];
			pageData[largestKeywordIndex]=pageData[keywordPageIndex];
			pageData[keywordPageIndex]=temp;
			createPageRankingHeap(pageData, limit, largestKeywordIndex);
		}
		return;
	}
	
	//Uses Heap Sort to sort the Heap created
	public void sortPages() {
		try {
		this.pageData= convertToArray(listOfKeywordPageData);
		if(pageData==null||pageData.length==0) {
			return;
		}
		this.LastKeywordPointer=pageData.length-1;
		int totalPageData= pageData.length;
		for(int i=totalPageData/2 -1;i>=0;i--) {
			createPageRankingHeap(pageData, totalPageData, i);
		}
		for(int i=this.pageData.length-1;i>0;i--) {
			URLFrequencyKeywordNode temp=pageData[0];
			pageData[0]=pageData[i];
			pageData[i]=temp;
			createPageRankingHeap(pageData, i, 0);
		}
		}catch(Exception e) {
			System.out.println("Exception arised in sortPage as "+ e);
			return;
		}
	}
	
	public ArrayList<URLFrequencyKeywordNode> getSortedPages() {
		ArrayList<URLFrequencyKeywordNode> finalSortedList=new ArrayList<URLFrequencyKeywordNode>();
		try {
		for(int index=this.pageData.length-1;index>=0;index--) {
			finalSortedList.add(pageData[index]);
		}
		}catch(Exception e) {
			System.out.println("Exception arised in getSortedPages() as "+ e);
		}
		return finalSortedList;
	}	
	
	
}
