package com.Edumetrics.EduApp.service;

import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.stereotype.Service;

import com.Edumetrics.EduApp.model.TrieNode;
import com.Edumetrics.EduApp.model.WordPositionFrequencyStorage;
import com.Edumetrics.EduApp.utilities.ReadCourseData;

/**
 * This service interacts with controller
 * It performs inverted indexing using trie and HashMap
 * */
@Service
public class InvertedIndexing {
	//HashMap to store Index and List of Pages,Private to not be accessible outside Class
	private HashMap<Integer,ArrayList<WordPositionFrequencyStorage>> indexData;
	StoreDataUsingTrie trieImp;
	
	//Constructor
	public InvertedIndexing(){
		this.indexData=new HashMap<Integer, ArrayList<WordPositionFrequencyStorage>>();
		this.trieImp=new StoreDataUsingTrie();
	}
	
	//This method stores data in trie after reading From CSV file
	private void readAndStoreDataInTrie(String filePath) {
		HashMap<String, ArrayList<String>> rowData=ReadCourseData.getRowDataMap(filePath);
		for(String url: rowData.keySet()) {
			try {
				ArrayList<String> urlContent= rowData.get(url);
				int index=-1;
				for(String eachWord: urlContent) {
					int indexNumber=trieImp.insertWordInTrie(eachWord);
					updateIndexData(indexNumber, url, index+=1);
				}
		  }catch(Exception e) {
			  System.out.println("Exception in adding content to Trie in readAndStoreDataInTrie() as "+e);
			  continue;
		  }
		}
		
	}
	
	//This Method returns List of Pages of Particular Search Word
	public ArrayList<WordPositionFrequencyStorage> getWordData(String searchWord) {
		ArrayList<WordPositionFrequencyStorage> finalList= new ArrayList<WordPositionFrequencyStorage>();
		try {
			TrieNode foundWordData=this.trieImp.searchWordInTrie(searchWord);
			if(foundWordData==null|| foundWordData.indexNo==-1) {
				return null;
			}
			finalList=this.indexData.get(foundWordData.indexNo);
		}catch(Exception e) {
			System.out.println("Exception arised in getWordData() as "+ e);
		}
		return finalList;
	}
	
	//Returns the Searched Word Data
	public ArrayList<WordPositionFrequencyStorage> getInvertedIndexInformation(String searchWord) {
		String filePath="C:\\Users\\DELL\\OneDrive - University of Windsor\\Desktop\\EduApp\\scraped_courses.csv";
		readAndStoreDataInTrie(filePath);
		return getWordData(searchWord.toLowerCase());
	}
	
	//This private method updates index value and its data in HashMap
	private void updateIndexData(int indexNumber, String url, int position) throws Exception{
		if(!indexData.containsKey(indexNumber)) {
			ArrayList<WordPositionFrequencyStorage> list=new ArrayList<WordPositionFrequencyStorage>();
			list.add(new WordPositionFrequencyStorage(url,1,position));
			indexData.put(indexNumber, list);
			return;
		}
		
		ArrayList<WordPositionFrequencyStorage> alreadyPresent=indexData.get(indexNumber);
		boolean updatedValue=false;
		for(WordPositionFrequencyStorage storage:alreadyPresent) {
			if(storage.url.equals(url)) {
				storage.frequency++;
				storage.positionList.add(position);
				updatedValue=true;
				break;
			}
		}
		if(!updatedValue) {
			alreadyPresent.add(new WordPositionFrequencyStorage(url,1,position));
		}
		indexData.put(indexNumber, alreadyPresent);
		
		
	}
	
	
}
