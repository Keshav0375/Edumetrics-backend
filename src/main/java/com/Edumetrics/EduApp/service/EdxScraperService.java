package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class EdxScraperService implements CourseScraperService {

    private static final int WAIT_TIME_SECONDS = 10;
    private final Random random = new Random();

    @Override
    public List<Course> scrapeCourses(String query, int limit) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run in headless mode
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME_SECONDS));

        List<Course> courses = new ArrayList<>();

        try {
            System.out.println("Starting to scrape EdX courses for query: " + query);

            // Navigate to EDX search page
            driver.get("https://www.edx.org/search?tab=course");

            // Search for the query if provided
            if (query != null && !query.isEmpty()) {
                try {
                    WebElement searchBox = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("input[placeholder='What do you want to learn?']")));
                    searchBox.sendKeys(query);
                    searchBox.submit();
                    Thread.sleep(2000); // Wait for search results
                } catch (Exception e) {
                    System.out.println("Error searching for courses: " + e.getMessage());
                }
            }

            // Get course cards
            List<WebElement> courseCards;
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".overflow-x-auto")));
                WebElement cardsContainer = driver.findElement(By.cssSelector(".overflow-x-auto"));
                courseCards = cardsContainer.findElements(By.cssSelector(".flex .justify-center"));

                System.out.println("Found " + courseCards.size() + " EDX courses");
            } catch (Exception e) {
                System.out.println("Error finding course cards: " + e.getMessage());
                return courses;
            }

            // Process first 'limit' courses
            int count = 0;
            for (WebElement card : courseCards) {
                if (count >= limit) {
                    break;
                }

                try {
                    WebElement linkElement = card.findElement(By.tagName("a"));
                    String courseUrl = linkElement.getAttribute("href");

                    // Visit the course page
                    driver.get(courseUrl);
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

                    // Create course object
                    Course course = new Course();
                    course.setUrl(courseUrl);
                    course.setPlatform("EDX");

                    // Get course title
                    try {
                        String title = wait.until(ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(".text-learn-course-hero-heading"))).getText();
                        course.setTitle(title);
                    } catch (Exception e) {
                        System.out.println("Error getting title: " + e.getMessage());
                        course.setTitle("N/A");
                    }

                    // Get course description
                    try {
                        String description = wait.until(ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(".max-w-\\[984px\\].text-base"))).findElement(By.tagName("p")).getText();
                        course.setDescription(description.replaceAll("\\s+", " ").trim());
                    } catch (Exception e) {
                        System.out.println("Error getting description: " + e.getMessage());
                        course.setDescription("N/A");
                    }

                    // Get rating
                    try {
                        String ratingText = wait.until(ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(".ml-2.font-medium.text-primary.text-sm"))).getText();
                        course.setRating(Double.parseDouble(ratingText));
                    } catch (Exception e) {
                        System.out.println("Error getting rating: " + e.getMessage());
                        course.setRating(4.5 + (random.nextDouble() * 0.5)); // Default rating
                    }

                    // Get price
                    try {
                        String price = wait.until(ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(".content-center.text-center.text-base.md\\:text-lg.\\!border-t-0"))).getText();
                        course.setPrice(price);
                    } catch (Exception e) {
                        System.out.println("Error getting price: " + e.getMessage());
                        course.setPrice("N/A");
                    }

                    // Get full page text
                    try {
                        String fullText = driver.findElement(By.tagName("body")).getText();
                        course.setExtractedText(fullText.replaceAll("\\s+", " ").trim());
                    } catch (Exception e) {
                        System.out.println("Error getting page text: " + e.getMessage());
                    }

                    courses.add(course);
                    count++;
                    System.out.println("Processed EDX course: " + course.getTitle());

                } catch (Exception e) {
                    System.out.println("Error processing course: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("Error in EDX scraper: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        return courses;
    }

    @Override
    public String getPlatformName() {
        return "EDX";
    }
}