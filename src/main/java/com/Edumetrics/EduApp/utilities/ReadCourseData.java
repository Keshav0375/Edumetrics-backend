package com.Edumetrics.EduApp.utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * This class performs method to Read Data From CSV File
 * */
public class ReadCourseData {
	public static HashMap<String, ArrayList<String>> getRowDataMap(String CSVfilePath){
		 HashMap<String, ArrayList<String>> rowData=new HashMap<String, ArrayList<String>>();
		try (BufferedReader bfReader = new BufferedReader(new FileReader(CSVfilePath))) {
	        String currentLine;
	        boolean initialLine = true;
	        int count = 0;
	        rowData=new HashMap<String, ArrayList<String>>();
	        while ((currentLine = bfReader.readLine()) != null) {
	        	try {
	            
	        	if (initialLine) {
	                initialLine = false;
	                continue; // Skip the header row
	            }
	
	            // Handle quoted fields and splitting manually
	            String[] columns = currentLine.split(",",-1);
	
	            if (columns.length > 5) {
	                String url = columns[2].trim(); // Ensure this is the URL
	                String fullHtmlBody = columns[6]; // Full HTML content
	
	                if (!url.isEmpty() && !fullHtmlBody.isEmpty()) {
	                	if(url.contains("humanities/university-of-pennsylvania-the-science-of-generosity")) {
	                		System.out.println("Processing URL: " + url);
	                		System.out.println("Keywords: " + fullHtmlBody);
	                	}
	                	ArrayList<String> keyWordsContainer = extractKeywords(fullHtmlBody);
	                	
	                	
	                	// Store the data in your structure or database
	                    rowData.put(url, keyWordsContainer);
	//                	StoreDataInAStructure(url, keyWordsContainer);
	                    count++;
	                }
	            }
	        }catch(Exception e) {
	        	System.out.println("Error while reading a line as "+e);
	        	continue;
	        }
	        }
	        return rowData;
	    } catch (IOException e) {
	        e.printStackTrace();
	        return rowData;
	    }
	}
	
	//This private Method extract keywords
	private static ArrayList<String> extractKeywords(String text) throws Exception{
		ArrayList<String> keyWordsContainer = new ArrayList<>();
        String[] words = text.split("\\s+");

        for (String eachWord : words) {
//        	System.out.println("WORDS IS"+eachWord);
            String cleanUpKeyword = eachWord.replaceAll("[\"“”.,:\\[\\]?]", "").toLowerCase();
            if (cleanUpKeyword.matches("\\b[a-zA-Z]+\\b")) {
                keyWordsContainer.add(cleanUpKeyword);
            }
        }

        return keyWordsContainer;
    }
}
