package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import java.util.List;

/**
 * Common interface for all course scrapers
 */
public interface CourseScraperService {

    /**
     * Scrapes courses based on the provided query
     *
     * @param query Search query for courses
     * @param limit Maximum number of courses to scrape
     * @return List of scraped courses
     */
    List<Course> scrapeCourses(String query, int limit);

    /**
     * Returns the name of the platform being scraped
     *
     * @return Platform name (e.g., "Coursera", "EDX")
     */
    String getPlatformName();
}