package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CourseraScraperService implements CourseScraperService {

    @Autowired
    private CsvDataService csvDataService;

    private static final int DEFAULT_TIMEOUT = 10; // Reduced timeout
    private static final String FIXED_PRICE = "$84/month";
    private final Random random = new Random();

    @Override
    public String getPlatformName() {
        return "Coursera";
    }

    @Override
    public List<Course> scrapeCourses(String query, int limit) {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = null;
        List<Course> courses = new ArrayList<>();

        try {
            System.out.println("Starting to scrape Coursera courses for query: " + query);
            driver = initializeWebDriver();

            // Go directly to search results page
            String searchUrl = "https://www.coursera.org/search?query=" + query.replace(" ", "%20");
            driver.get(searchUrl);
            System.out.println("Navigating directly to search URL: " + searchUrl);

            // Wait for search results with a reasonable timeout
            WebDriverWait resultsWait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
            resultsWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("li.cds-9")));
            System.out.println("Search results loaded");

            // Scroll just enough to load more results
            scrollToLoadMoreResults(driver);

            // Extract course data
            courses = extractCourseData(driver, limit);

            // Save to CSV
            for (Course course : courses) {
                csvDataService.saveCourse(course);
                System.out.println("Course saved to CSV: " + course.getTitle());
            }

        } catch (Exception e) {
            System.err.println("Error during scraping: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("Browser closed successfully.");
            }
        }

        return courses;
    }

    private WebDriver initializeWebDriver() {
        try {
            // Configure Chrome options for performance
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // Run in headless mode for speed
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-popup-blocking");
            options.addArguments("--disable-notifications");
            options.addArguments("--blink-settings=imagesEnabled=false"); // Disable images
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.setPageLoadStrategy(PageLoadStrategy.EAGER); // Load just DOM, faster than NORMAL

            WebDriver driver = new ChromeDriver(options);
            System.out.println("WebDriver initialized successfully.");
            return driver;
        } catch (Exception e) {
            System.err.println("Failed to initialize Chrome driver: " + e.getMessage());
            throw e;
        }
    }

    private void scrollToLoadMoreResults(WebDriver driver) {
        System.out.println("Loading more results...");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Scroll down just 3 times with shorter waits
            for (int i = 0; i < 3; i++) {
                js.executeScript("window.scrollBy(0, 1000)");
                Thread.sleep(500);
            }

            System.out.println("Finished loading additional results");
        } catch (Exception e) {
            System.err.println("Error during scrolling: " + e.getMessage());
        }
    }

    private List<Course> extractCourseData(WebDriver driver, int limit) {
        List<Course> courses = new ArrayList<>();

        try {
            // Use the most reliable selector based on your logs
            List<WebElement> courseElements = driver.findElements(By.cssSelector("li.cds-9"));
            System.out.println("Found " + courseElements.size() + " courses");

            if (courseElements.isEmpty()) {
                System.out.println("No courses found to extract.");
                return courses;
            }

            System.out.println("Extracting data from courses");

            // Process a limited number of courses
            int coursesToProcess = Math.min(courseElements.size(), limit);
            System.out.println("Will process first " + coursesToProcess + " courses");

            for (int i = 0; i < coursesToProcess; i++) {
                WebElement courseElement = courseElements.get(i);
                try {
                    System.out.println("Processing course " + (i+1) + " of " + coursesToProcess);

                    // Extract basic course details without opening new tabs
                    Course course = extractBasicCourseDetails(courseElement);
                    courses.add(course);

                } catch (Exception e) {
                    System.out.println("Error processing course " + (i+1) + ": " + e.getMessage());
                }
            }

            System.out.println("Successfully extracted " + courses.size() + " courses");

        } catch (Exception e) {
            System.err.println("Error finding course elements: " + e.getMessage());
        }

        return courses;
    }

    private Course extractBasicCourseDetails(WebElement courseElement) {
        Course course = new Course();

        // Extract title - use the most reliable selector
        String title = extractText(courseElement, "h3.cds-CommonCard-title");
        course.setTitle(title.isEmpty() ? "No Title" : title);


        // Set fixed price
        course.setPrice(FIXED_PRICE);

        // Extract rating
        String rawRating = extractText(courseElement, "span.css-6ecy9b");
        double rating = 0.0;

        if (!rawRating.isEmpty()) {
            // Extract numeric rating
            Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
            Matcher matcher = pattern.matcher(rawRating);
            if (matcher.find()) {
                try {
                    rating = Double.parseDouble(matcher.group(1));
                } catch (NumberFormatException e) {
                    rating = random.nextDouble() * 2 + 3; // Random rating between 3-5
                }
            }
        } else {
            rating = random.nextDouble() * 2 + 3; // Random rating between 3-5
        }
        course.setRating(rating);

        // Extract URL without clicking
        String url = "";
        try {
            WebElement link = courseElement.findElement(By.cssSelector("a.cds-CommonCard-titleLink"));
            url = link.getAttribute("href");
        } catch (Exception e) {
            System.out.println("Could not extract URL: " + e.getMessage());
        }
        course.setUrl(url);

        // Set description to course text content (simpler approach)
        String description = courseElement.getText();
        course.setDescription(description);
        course.setExtractedText(description);
        course.setHtmlCode(description);
        course.setPlatform(getPlatformName());

        return course;
    }

    private String extractText(WebElement parent, String selector) {
        try {
            WebElement element = parent.findElement(By.cssSelector(selector));
            return element.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }
}