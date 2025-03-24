package com.Edumetrics.EduApp.service;



import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.stereotype.Service;

import com.Edumetrics.EduApp.model.FrequencyStorageNode;
import com.Edumetrics.EduApp.model.URLFrequencyKeywordNode;
import com.Edumetrics.EduApp.utilities.ReadCourseData;


@Service
public class PageRanking {
	private HashMap<String, FrequencyStorageNode>  rootWordAndURLRecord;
	
	// Constructor
	public PageRanking(){
		this.rootWordAndURLRecord=new HashMap<String, FrequencyStorageNode>();
	}
	
	//This method stores Data in AVL Tree Data Structure
	private void StoreDataInAStructure(String url, ArrayList<String> urlData) {
		if(url==null || urlData==null || url.isBlank() || urlData.size()==0) {
			System.out.println("String received in StoreDataInAStructure is Empty as "+ url +" "+ urlData);
			return;
		}
		
		StoreDataUsingAVL storageStructure= new StoreDataUsingAVL();
		for(String data : urlData) {
			try {
			    storageStructure.addWordInStructure(data, 1);
			}catch(Exception e) {
				System.out.println("Exception while adding word to AVL tree in StoreDataInAStructure() as "+ e);
				continue;
			}
		}
		FrequencyStorageNode structureRootKeyword=storageStructure.getRootKeywood();
//		storageStructure.printKeywordTree();
		//System.out.println("Toal words in Tree"+storageStructure.getTotalKeywords());
		if(structureRootKeyword!=null) {
			this.rootWordAndURLRecord.put(url, structureRootKeyword);
		}else {
			this.rootWordAndURLRecord.put(url, null);
		}
		return;
	}

	//It Reads from CSV file and calls the store Method
	private void readAndStoreCourseData(String CSVfilePath) {
		HashMap<String, ArrayList<String>> rowData;
		try {
			rowData=ReadCourseData.getRowDataMap(CSVfilePath);
		}catch(Exception e) {
			System.out.println("Exception arised while writing reading CSV in readAndStoreCourseData() as "+e);
			return;
		}
		for(String url : rowData.keySet()) {
			try {
				StoreDataInAStructure(url, rowData.get(url));
			}catch(Exception e) {
				System.out.println("Exception arised while Storing data for url "+url + "as "+e);
				continue;
			}
		}
	}
	
	//It returns the sorted Page Information
	public ArrayList<URLFrequencyKeywordNode> getSortedRankedPageInformation(String wordToBeSearched){
		ArrayList<URLFrequencyKeywordNode> answer=new ArrayList<URLFrequencyKeywordNode>();
		if(wordToBeSearched==null || wordToBeSearched.isBlank()) {
			return answer;
		}
		SortUsingHeap sortingObject= new SortUsingHeap(wordToBeSearched.toLowerCase());
		
		for(String urlName : this.rootWordAndURLRecord.keySet()) {
			FrequencyStorageNode rootKeyWord=rootWordAndURLRecord.get(urlName);
			try {
				sortingObject.fetchKeywordFrequency(rootKeyWord, urlName);
			}catch(Exception e) {
				System.out.println("Exception arised while fetching Keyword Frequency in getSortedRankedPageInformation() as "+ e);
			}
		}
		try {
			sortingObject.sortPages();
		}catch(Exception e) {
			System.out.println("Exception arised while sorting in getSortedRankedPageInformation() as "+ e);
		}
		answer= sortingObject.getSortedPages();
		return answer;
		
	}
	//Main method for Ranking Pages using Heap Sort
	public ArrayList<URLFrequencyKeywordNode> rankPage(String searchWord) {
		String filePath="C:\\Users\\DELL\\OneDrive - University of Windsor\\Desktop\\EduApp\\scraped_courses.csv";
		ArrayList<URLFrequencyKeywordNode> answer= new ArrayList<URLFrequencyKeywordNode>();
		try {
			readAndStoreCourseData(filePath);
		}catch(Exception e) {
			System.out.println("Exception in reading And Stroing CSV data in rankPage as "+e);
		}
		try {
			answer =getSortedRankedPageInformation(searchWord);
		}catch(Exception e) {
			System.out.println("Exception in rankPage for ranked Page reterival as "+ e);
		}
//		System.out.println("Size of list "+j.size());
//		for(URLFrequencyKeywordNode node: answer) {
//			System.out.println("URL ::"+node.urlLink + "Word::"+ node.word+" Frequency::"+node.frequency);
//		}
		return answer;
	}
	
}
