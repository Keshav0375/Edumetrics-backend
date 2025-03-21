package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

@Service
public class StanfordEducationScraperService implements CourseScraperService {
    @Autowired
    private CsvDataService csvDataService;
    private static final int WAIT_TIME_SECONDS = 5;
    private static final int MAX_COURSES = 10;
    private final Random random = new Random();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    public String getPlatformName() {
        return "StanfordOnline";
    }

    @Override
    public List<Course> scrapeCourses(String query, int limit) {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME_SECONDS));
        List<Course> courses = new ArrayList<>();

        try {
            driver.get("https://online.stanford.edu/");
            WebElement searchBox = driver.findElement(By.id("edit-keywords"));
            searchBox.sendKeys(query);
            searchBox.sendKeys(Keys.ENTER);

            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.node.node--type-course")));
            List<WebElement> courseElements = driver.findElements(By.cssSelector("a.node.node--type-course"));

            int coursesToProcess = Math.min(limit, Math.min(courseElements.size(), MAX_COURSES));
            List<Future<Course>> futures = new ArrayList<>();

            for (int i = 0; i < coursesToProcess; i++) {
                WebElement courseElement = courseElements.get(i);
                String title = courseElement.findElement(By.cssSelector("h3")).getText();
                String url = courseElement.getAttribute("href");
                futures.add(executorService.submit(() -> scrapeCourseDetails(title, url)));
            }

            for (Future<Course> future : futures) {
                try {
                    Course course = future.get();
                    courses.add(course);
                    csvDataService.saveCourse(course);
                } catch (Exception e) {
                    System.out.println("Error processing course: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        return courses;
    }

    private Course scrapeCourseDetails(String initialTitle, String url) {
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME_SECONDS));
        Course course = new Course();
        course.setTitle(initialTitle);
        course.setUrl(url);
        course.setPlatform(getPlatformName());

        try {
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            String pageSource = driver.getPageSource();
            double randomRating = 4.5 + (random.nextDouble() * 0.5);
            double roundedRating = Math.round(randomRating * 10.0) / 10.0;
            course.setRating(roundedRating);
            course.setHtmlCode(pageSource);

            extractTitleWithCourseNumber(driver, wait, course);
            extractDescription(driver, course);
            extractPrice(driver, course);

            Document doc = Jsoup.parse(pageSource);
            course.setExtractedText(doc.text());
        } catch (Exception e) {
            System.out.println("Error processing course page: " + initialTitle + " - " + e.getMessage());
        } finally {
            driver.quit();
        }

        return course;
    }

    private void extractTitleWithCourseNumber(WebDriver driver, WebDriverWait wait, Course course) {
        try {
            WebElement titleElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("header.course-top h1")));
            String title = titleElement.getText().trim();
            WebElement courseNumberElement = driver.findElement(By.cssSelector("header.course-top p.number"));
            String courseNumber = courseNumberElement.getText().trim();
            course.setTitle(title + " (" + courseNumber + ")");
        } catch (Exception ignored) {}
    }

    private void extractDescription(WebDriver driver, Course course) {
        try {
            WebElement descriptionElement = driver.findElement(By.cssSelector("div.paragraph-inner p"));
            course.setDescription(descriptionElement.getText().trim());
        } catch (Exception ignored) {}
    }

    private void extractPrice(WebDriver driver, Course course) {
        try {
            WebElement priceElement = driver.findElement(By.cssSelector("dl.course-details dt.label--field-fee-amount + dd p"));
            course.setPrice(priceElement.getText().trim());
        } catch (Exception ignored) {}
    }
}
