package com.Edumetrics.EduApp.model;

/*
 * This stores the url with frequency count and word
 * */
public class URLFrequencyKeywordNode {
	public String word;
	public int frequency;
	public String urlLink;
	public URLFrequencyKeywordNode(String word, int frequency, String urlLink) {
		this.word = word;
		this.frequency = frequency;
		this.urlLink = urlLink;
	}
}
