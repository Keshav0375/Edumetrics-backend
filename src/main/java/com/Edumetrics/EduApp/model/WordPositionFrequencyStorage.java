package com.Edumetrics.EduApp.model;

import java.util.*;

/**
 * This class stores the url, position of the word
 * */
public class WordPositionFrequencyStorage {
	public String url;
	public int frequency;
	public ArrayList<Integer> positionList;
	
	//Constructor
	public WordPositionFrequencyStorage(String url, int frequency, int position){
		this.frequency=frequency;
		this.url=url;
		this.positionList=new ArrayList<Integer>();
		this.positionList.add(position);
	}
	
}
