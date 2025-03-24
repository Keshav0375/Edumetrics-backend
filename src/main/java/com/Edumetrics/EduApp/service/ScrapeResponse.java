package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;

import java.util.List;
import java.util.Map;

public class ScrapeResponse {
    private boolean success;
    private String message;
    private Map<String, List<Course>> coursesByPlatform;
    private List<Course> courses;
    private long totalCourses;
    private long executionTimeMs;

    // Constructors
    public ScrapeResponse() {
    }

    public ScrapeResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, List<Course>> getCoursesByPlatform() {
        return coursesByPlatform;
    }

    public void setCoursesByPlatform(Map<String, List<Course>> coursesByPlatform) {
        this.coursesByPlatform = coursesByPlatform;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }

    public long getTotalCourses() {
        return totalCourses;
    }

    public void setTotalCourses(long totalCourses) {
        this.totalCourses = totalCourses;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}