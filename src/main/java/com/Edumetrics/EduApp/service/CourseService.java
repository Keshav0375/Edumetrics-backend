package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
public class CourseService {

    @Autowired
    private CsvService csvService;

    private static final int WAIT_TIME_SECONDS = 8;
    private static final int MAX_COURSES = 10;
    private final Random random = new Random();

    public List<Course> scrapeAndSaveCourses(String query) {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME_SECONDS));

        List<Course> courses = new ArrayList<>();
        try {
            System.out.println("Starting to scrape courses for query: " + query);
            driver.get("https://online.stanford.edu/");
            WebElement searchBox = driver.findElement(By.id("edit-keywords"));
            searchBox.sendKeys(query);
            searchBox.sendKeys(Keys.ENTER);

            // Wait for search results to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.node.node--type-course")));

            // Apply filter for paid courses
            try {
                // Click on the access dropdown
                WebElement accessDropdown = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("button.facet-toggle[aria-controls='aria-tablist-1-panel-1']")));
                accessDropdown.click();

                // Wait for the dropdown to expand
                wait.until(ExpectedConditions.attributeToBe(accessDropdown, "aria-expanded", "true"));

                // Click on the paid checkbox
                WebElement paidCheckbox = wait.until(ExpectedConditions.elementToBeClickable(
                        By.id("free-or-paid-paid")));
                paidCheckbox.click();

                // Wait for the page to refresh with filtered results
                Thread.sleep(1500); // Short sleep to allow filter to apply
            } catch (Exception e) {
                System.out.println("Error applying paid filter: " + e.getMessage());
                // Continue with unfiltered results if filter fails
            }

            // Get the course elements after filtering
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.node.node--type-course")));
            List<WebElement> courseElements = driver.findElements(By.cssSelector("a.node.node--type-course"));

            System.out.println("Found " + courseElements.size() + " courses. Processing first " + MAX_COURSES + "...");

            // Extract basic information from the search results
            for (int i = 0; i < Math.min(MAX_COURSES, courseElements.size()); i++) {
                WebElement courseElement = courseElements.get(i);
                Course course = new Course();
                try {
                    course.setTitle(courseElement.findElement(By.cssSelector("h3")).getText());
                    System.out.println("Processing course: " + course.getTitle());
                    course.setUrl(courseElement.getAttribute("href"));
                    courses.add(course);
                } catch (Exception e) {
                    System.out.println("Error extracting initial data: " + e.getMessage());
                }
            }

            // Visit each course page to extract detailed information
            for (Course course : courses) {
                try {
                    System.out.println("Visiting URL: " + course.getUrl());
                    driver.get(course.getUrl());

                    // Wait for the page to load
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

                    // Extract the full HTML content
                    String pageSource = driver.getPageSource();

                    try {
                        double rating = 4.5 + (random.nextDouble() * 0.5);
                        course.setRating(Math.round(rating * 10.0) / 10.0);
                    } catch (Exception e) {
                        System.out.println("Error setting rating, defaulting to 5.0");
                        course.setRating(5.0);
                    }

                    course.setHtmlCode(pageSource);

                    // Extract title with course number
                    try {
                        WebElement titleElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector("header.course-top h1")));
                        String title = titleElement.getText().trim();

                        WebElement courseNumberElement = driver.findElement(By.cssSelector("header.course-top p.number"));
                        String courseNumber = courseNumberElement.getText().trim();

                        course.setTitle(title + " (" + courseNumber + ")");
                    } catch (Exception e) {
                        System.out.println("Error extracting title: " + e.getMessage());
                        // Keep the original title if extraction fails
                    }

                    // Extract description
                    try {
                        WebElement descriptionElement = driver.findElement(By.cssSelector("div.paragraph-inner p"));
                        course.setDescription(descriptionElement.getText().trim());
                    } catch (Exception e) {
                        System.out.println("Error extracting description: " + e.getMessage());
                        course.setDescription("");
                    }

                    // Extract price
                    try {
                        WebElement priceElement = driver.findElement(
                                By.cssSelector("dl.course-details dt.label--field-fee-amount + dd p"));
                        course.setPrice(priceElement.getText().trim());
                    } catch (Exception e) {
                        System.out.println("Error extracting price: " + e.getMessage());
                        course.setPrice("");
                    }

                    // Extract all text using Jsoup for better parsing
                    Document doc = Jsoup.parse(pageSource);
                    String allText = doc.text();
                    course.setExtractedText(allText);

                    // Save to CSV
                    csvService.saveCourse(course);

                } catch (Exception e) {
                    System.out.println("Error processing course page: " + course.getTitle() + ": " + e.getMessage());
                    // Save whatever data we have so far
                    csvService.saveCourse(course);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        return courses;
    }

    public List<Course> getAllCourses() {
        return csvService.getAllCourses();
    }
}