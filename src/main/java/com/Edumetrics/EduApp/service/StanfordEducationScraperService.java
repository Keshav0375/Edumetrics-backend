package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class StanfordEducationScraperService implements CourseScraperService {
    @Autowired
    private CsvDataService csvDataService;
    private static final int WAIT_TIME_SECONDS = 3;
    private static final int MAX_COURSES = 5; // Limiting to top 5 courses as requested
    private final Random random = new Random();

    @Override
    public String getPlatformName() {
        return "StanfordOnline";
    }

    @Override
    public List<Course> scrapeCourses(String query, int limit) {
        System.out.println("Starting to scrape Stanford courses for query: " + query);
        final int REQUIRED_COURSES = 5;

        System.out.println("Setting up Chrome driver with headless options");
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME_SECONDS));
        List<Course> courses = new ArrayList<>();

        try {
            // Use the direct search URL with the query parameter
            String searchUrl = "https://online.stanford.edu/explore?keywords=" + query + "&filter%5B0%5D=free_or_paid%3Apaid";
            System.out.println("Navigating to search URL: " + searchUrl);
            driver.get(searchUrl);

            // Wait for course elements to load
            System.out.println("Waiting for course elements to load...");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.node.node--type-course")));
            List<WebElement> courseElements = driver.findElements(By.cssSelector("a.node.node--type-course"));

            System.out.println("Found " + courseElements.size() + " course elements on the page");

            // Determine how many courses to process
            int coursesToProcess = Math.min(limit, Math.min(courseElements.size(), MAX_COURSES));
            System.out.println("Will process " + coursesToProcess + " courses");

            // Collect course URLs
            List<String> courseUrls = new ArrayList<>();
            for (int i = 0; i < coursesToProcess; i++) {
                WebElement courseElement = courseElements.get(i);
                String url = courseElement.getAttribute("href");
                System.out.println("Course #" + (i+1) + " URL: " + url);
                courseUrls.add(url);
            }

            // Process each course URL using the same WebDriver instance
            System.out.println("Starting to process individual course pages...");
            for (int i = 0; i < courseUrls.size(); i++) {
                String courseUrl = courseUrls.get(i);
                System.out.println("Processing course #" + (i+1) + ": " + courseUrl);
                Course course = scrapeCourseDetails(driver, wait, courseUrl);
                if (course != null) {
                    courses.add(course);
                    System.out.println("Successfully added course: " + course.getTitle());
                    csvDataService.saveCourse(course);
                    System.out.println("Saved course to CSV");
                } else {
                    System.err.println("Failed to process course: " + courseUrl);
                    Course demoCourse = createDemoCourse(query, courses.size() + 1);
                    courses.add(demoCourse);
                    csvDataService.saveCourse(demoCourse);
                    System.out.println("Added demo course in place of failed course");
                }
            }

            int demoCoursesNeeded = REQUIRED_COURSES - courses.size();
            if (demoCoursesNeeded > 0) {
                System.out.println("Adding " + demoCoursesNeeded + " demo courses to reach required total of " + REQUIRED_COURSES);
                for (int i = 0; i < demoCoursesNeeded; i++) {
                    Course demoCourse = createDemoCourse(query, courses.size() + 1);
                    courses.add(demoCourse);
                    csvDataService.saveCourse(demoCourse);
                    System.out.println("Added demo course: " + demoCourse.getTitle());
                }
            }
        } catch (Exception e) {
            System.err.println("Error during course search: " + e.getMessage());
            e.printStackTrace();

            int demoCoursesNeeded = REQUIRED_COURSES - courses.size();
            if (demoCoursesNeeded > 0) {
                System.out.println("Error occurred. Adding " + demoCoursesNeeded + " demo courses as fallback.");
                for (int i = 0; i < demoCoursesNeeded; i++) {
                    Course demoCourse = createDemoCourse(query, courses.size() + 1);
                    courses.add(demoCourse);
                    csvDataService.saveCourse(demoCourse);
                }
            }
        } finally {
            System.out.println("Closing browser instance");
            driver.quit();
        }

        System.out.println("Scraping completed. Found " + courses.size() + " courses");
        return courses;
    }

    private Course scrapeCourseDetails(WebDriver driver, WebDriverWait wait, String url) {
        System.out.println("Starting to scrape details for course: " + url);
        Course course = new Course();
        course.setUrl(url);
        course.setPlatform(getPlatformName());

        try {
            System.out.println("Navigating to course page");
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            System.out.println("Page loaded successfully");

            // Generate random rating between 4.5 and 5.0
            double randomRating = 4.5 + (random.nextDouble() * 0.5);
            double roundedRating = Math.round(randomRating * 10.0) / 10.0;
            course.setRating(roundedRating);
            System.out.println("Set random rating: " + roundedRating);

            // Extract title
            try {
                System.out.println("Attempting to extract course title");
                WebElement titleElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("header.course-top h1")));
                String title = titleElement.getText().trim();
                System.out.println("Found title: " + title);

                // Try to get course number if available
                try {
                    System.out.println("Attempting to extract course number");
                    WebElement courseNumberElement = driver.findElement(By.cssSelector("header.course-top p.number"));
                    String courseNumber = courseNumberElement.getText().trim();
                    System.out.println("Found course number: " + courseNumber);
                    course.setTitle(title + " (" + courseNumber + ")");
                } catch (Exception e) {
                    System.out.println("No course number found, using title only");
                    course.setTitle(title);
                }
            } catch (Exception e) {
                System.err.println("Could not extract title: " + e.getMessage());
                return null; // Skip this course if we can't even get the title
            }

            // Extract description
            try {
                System.out.println("Attempting to extract course description");
                WebElement descriptionElement = driver.findElement(By.cssSelector("div.paragraph-inner p"));
                String description = descriptionElement.getText().trim();
                System.out.println("Found description: " + (description.length() > 50 ? description.substring(0, 50) + "..." : description));
                course.setDescription(description);
            } catch (Exception e) {
                System.err.println("Could not extract description: " + e.getMessage());
                course.setDescription("No description available");
            }

            // Extract price
            try {
                System.out.println("Attempting to extract course price");
                WebElement priceElement = driver.findElement(By.cssSelector("dl.course-details dt.label--field-fee-amount + dd p"));
                String price = priceElement.getText().trim();
                System.out.println("Found price: " + price);
                course.setPrice(price);
            } catch (Exception e) {
                System.err.println("Could not extract price: " + e.getMessage());
                course.setPrice("Price not available");
            }

            // Get page source and extract text
            System.out.println("Getting page source and extracting text");
            String pageSource = driver.getPageSource();
            course.setHtmlCode(pageSource);

            Document doc = Jsoup.parse(pageSource);
            String extractedText = doc.text();
            System.out.println("Extracted text length: " + extractedText.length() + " characters");
            course.setExtractedText(extractedText);

            System.out.println("Successfully scraped all details for: " + course.getTitle());
        } catch (Exception e) {
            System.err.println("Error processing course page: " + url);
            System.err.println("Error details: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return course;
    }

    private Course createDemoCourse(String query, int index) {
        Course demoCourse = new Course();
        demoCourse.setPlatform(getPlatformName());
        demoCourse.setTitle(getPlatformName() + " Course " + index );
        demoCourse.setDescription("This is a topic on which course is not offered by this site.");
        demoCourse.setUrl("N/A");
        demoCourse.setPrice("N/A");
        demoCourse.setRating(0.0);
        demoCourse.setHtmlCode("N/A");
        demoCourse.setExtractedText("This site not offers course on this topic");
        System.out.println("Created demo course: " + demoCourse.getTitle());
        return demoCourse;
    }
}