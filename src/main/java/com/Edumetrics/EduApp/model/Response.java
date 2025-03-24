package com.Edumetrics.EduApp.model;
import java.util.*;

/**
 * This class stores the response to the request
 * */
public class Response <T>{
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public ArrayList<T> getData() {
		return data;
	}
	public void setData(ArrayList<T> data) {
		this.data = data;
	}
	int statusCode=0;
	String message;
	ArrayList<T> data;
	
}
