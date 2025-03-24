package com.Edumetrics.EduApp.model;

import java.util.Map;

//Class to store word frequency data
public class WordFrequencyData {
  private int count;
  private Map<String, Integer> topWords;

  public WordFrequencyData(int count, Map<String, Integer> topWords) {
      this.count = count;
      this.topWords = topWords;
  }

  public int getCount() {
      return count;
  }

  public Map<String, Integer> getTopWords() {
      return topWords;
  }
}