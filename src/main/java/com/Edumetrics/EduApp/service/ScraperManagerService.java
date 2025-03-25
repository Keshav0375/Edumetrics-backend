package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ScraperManagerService {

    private final List<CourseScraperService> scraperServices;
    private final CsvDataService csvDataService;

    @Autowired
    public ScraperManagerService(
            List<CourseScraperService> scraperServices,
            CsvDataService csvDataService) {
        this.scraperServices = scraperServices;
        this.csvDataService = csvDataService;
    }

    /**
     * Scrapes courses from a specific platform
     *
     * @param platformName Name of the platform to scrape from
     * @param query Search query for courses
     * @param limit Maximum number of courses to scrape
     * @param saveToCSV Whether to save scraped courses to CSV
     * @return List of scraped courses
     */
    public List<Course> scrapePlatform(String platformName, String query, int limit, boolean saveToCSV) {

        CourseScraperService service = findScraperByName(platformName);
        if (service == null) {
            throw new IllegalArgumentException("Unsupported platform: " + platformName);
        }

        List<Course> courses = service.scrapeCourses(query, limit);

        if (saveToCSV && !courses.isEmpty()) {
            csvDataService.saveCourses(courses);
        }

        return courses;
    }

    /**
     * Scrapes courses from all supported platforms
     *
     * @param query Search query for courses
     * @param limit Maximum number of courses to scrape per platform
     * @param saveToCSV Whether to save scraped courses to CSV
     * @return Map of platform names to lists of scraped courses
     */
    public Map<String, List<Course>> scrapeAllPlatforms(String query, int limit, boolean saveToCSV) {
        Map<String, List<Course>> results = new HashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(scraperServices.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (CourseScraperService service : scraperServices) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                String platformName = service.getPlatformName();
                List<Course> courses = service.scrapeCourses(query, limit);

                if (saveToCSV && !courses.isEmpty()) {
                    csvDataService.saveCourses(courses);
                }

                synchronized (results) {
                    results.put(platformName, courses);
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all scrapers to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        return results;
    }

    /**
     * Find a scraper service by platform name
     */
    private CourseScraperService findScraperByName(String platformName) {
        for (CourseScraperService service : scraperServices) {
            System.out.println("service.getPlatformName() = " + service.getPlatformName());
            if (service.getPlatformName().equalsIgnoreCase(platformName)) {
                return service;
            }
        }
        return null;
    }

    /**
     * Get a list of all supported platforms
     */
    public List<String> getSupportedPlatforms() {
        List<String> platforms = new ArrayList<>();
        for (CourseScraperService service : scraperServices) {
            platforms.add(service.getPlatformName());
        }
        return platforms;
    }
}