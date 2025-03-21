package com.Edumetrics.EduApp.controller;

import com.Edumetrics.EduApp.model.Course;
import com.Edumetrics.EduApp.model.ScrapeResponse;
import com.Edumetrics.EduApp.service.CsvDataService;
import com.Edumetrics.EduApp.service.ScraperManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final ScraperManagerService scraperManager;
    private final CsvDataService csvDataService;

    @Autowired
    public CourseController(
            ScraperManagerService scraperManager,
            CsvDataService csvDataService) {
        this.scraperManager = scraperManager;
        this.csvDataService = csvDataService;
    }

    @GetMapping("/platforms")
    public ResponseEntity<List<String>> getSupportedPlatforms() {
        List<String> platforms = scraperManager.getSupportedPlatforms();
        return ResponseEntity.ok(platforms);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Course>> getAllCourses() {
        List<Course> courses = csvDataService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/platform/{platformName}")
    public ResponseEntity<List<Course>> getCoursesByPlatform(@PathVariable String platformName) {
        List<Course> courses = csvDataService.getCoursesByPlatform(platformName);
        return ResponseEntity.ok(courses);
    }

    @PostMapping("/scrape/{platformName}")
    public ResponseEntity<ScrapeResponse> scrapePlatform(
            @PathVariable String platformName,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "5") int limit,
            @RequestParam(required = false, defaultValue = "true") boolean saveToCSV) {

        long startTime = System.currentTimeMillis();
        ScrapeResponse response = new ScrapeResponse();

        try {
            List<Course> courses = scraperManager.scrapePlatform(platformName, query, limit, saveToCSV);

            response.setSuccess(true);
            response.setMessage("Successfully scraped courses from " + platformName);
            response.setCourses(courses);
            response.setTotalCourses(courses.size());

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error scraping courses: " + e.getMessage());
        }

        response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/scrape-edumetrics")
    public ResponseEntity<ScrapeResponse> scrapeAllPlatforms(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "5") int limit,
            @RequestParam(required = false, defaultValue = "true") boolean saveToCSV) {

        long startTime = System.currentTimeMillis();
        ScrapeResponse response = new ScrapeResponse();

        try {
            Map<String, List<Course>> coursesByPlatform = scraperManager.scrapeAllPlatforms(query, limit, saveToCSV);

            // Count total courses
            long totalCourses = coursesByPlatform.values().stream()
                    .mapToLong(List::size)
                    .sum();

            response.setSuccess(true);
            response.setMessage("Successfully scraped courses from all platforms");
            response.setCoursesByPlatform(coursesByPlatform);
            response.setTotalCourses(totalCourses);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error scraping courses: " + e.getMessage());
        }

        response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return ResponseEntity.ok(response);
    }
}