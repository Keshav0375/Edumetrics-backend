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
	public static HashMap<String, ArrayList<String>> getRowDataMap(String CSVfilePath) {
		HashMap<String, ArrayList<String>> rowData = new HashMap<>();

		try (BufferedReader bfReader = new BufferedReader(new FileReader(CSVfilePath))) {
			String headerLine = bfReader.readLine(); // Read the header line
			if (headerLine == null) {
				throw new IOException("CSV file is empty");
			}

			// Map column names to indices
			String[] headers = headerLine.split(",", -1); // Assume no splitting required later
			HashMap<String, Integer> columnIndexMap = mapColumnIndices(headers);

			// Verify required columns exist
			if (!columnIndexMap.containsKey("url") || !columnIndexMap.containsKey("html_text")) {
				throw new IOException("Required columns (url, html_text) not found in CSV header");
			}

			int urlIndex = columnIndexMap.get("url");
			int htmlTextIndex = columnIndexMap.get("html_text");

			String currentLine;
			while ((currentLine = bfReader.readLine()) != null) {
				try {
					// Process the line based on known column indices
					String url = getColumnValue(currentLine, urlIndex, headers.length).trim();
					String fullHtmlBody = getColumnValue(currentLine, htmlTextIndex, headers.length).trim();

					if (!url.isEmpty() && !fullHtmlBody.isEmpty() && url.contains("https://")) {
						System.out.println("Processing URL: " + url);
						System.out.println("Keywords: " + fullHtmlBody);

						ArrayList<String> keyWordsContainer = extractKeywords(fullHtmlBody);
						rowData.put(url, keyWordsContainer);
					}
					bfReader.readLine();
					bfReader.readLine();
				} catch (Exception e) {
					System.out.println("Error while reading a record: " + e);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return rowData;
	}

	private static HashMap<String, Integer> mapColumnIndices(String[] headers) {
		HashMap<String, Integer> columnIndexMap = new HashMap<>();
		for (int i = 0; i < headers.length; i++) {
			columnIndexMap.put(headers[i].trim().toLowerCase(), i);
		}
		return columnIndexMap;
	}

	private static String getColumnValue(String line, int columnIndex, int totalColumns) {
		String[] columns = line.split(",", totalColumns);
		if (columnIndex < columns.length) {
			return columns[columnIndex];
		}
		return "";
	}

	private static ArrayList<String> extractKeywords(String text) {
		ArrayList<String> keyWordsContainer = new ArrayList<>();
		String[] words = text.split("\\s+");

		for (String eachWord : words) {
			String cleanUpKeyword = eachWord.replaceAll("[\"“”.,:\\[\\]?]", "").toLowerCase();
			if (cleanUpKeyword.matches("\\b[a-zA-Z]+\\b")) {
				keyWordsContainer.add(cleanUpKeyword);
			}
		}

		return keyWordsContainer;
	}

//    public static void main(String[] args) {
//        String filePath = "path/to/your/scraped_courses_cleaned.csv";
//        HashMap<String, ArrayList<String>> result = getRowDataMap(filePath);
//        System.out.println("Processed " + result.size() + " records.");
//    }

}
