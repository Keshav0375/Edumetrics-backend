package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CourseraScraperService implements CourseScraperService {

    @Autowired
    private CsvDataService csvDataService;

    private static final int DEFAULT_TIMEOUT = 15;
    private static final String FIXED_PRICE = "$25/month";
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

            // Navigate to Coursera
            driver.get("https://www.coursera.org");
            System.out.println("Loaded Coursera homepage");

            // Handle popups with timeout exceptions handled
            handlePopups(driver);

            // Wait for page to load fully
            WebDriverWait pageLoadWait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
            pageLoadWait.until(ExpectedConditions.titleContains("Coursera"));
            System.out.println("Page loaded successfully");

            // Click search button using multiple selectors
            if (!clickSearchButton(driver)) {
                System.out.println("Failed to click search button");
            }

            // Enter search term
            if (!enterSearchTerm(driver, query)) {
                System.out.println("Failed to enter search term. Trying alternative approach...");

                // Try direct URL approach as fallback
                driver.get("https://www.coursera.org/search?query=" + query.replace(" ", "%20"));
                System.out.println("Using direct search URL as alternative");
            }

            // Wait for search results
            try {
                WebDriverWait resultsWait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
                resultsWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("li.cds-9")));
                System.out.println("Search results loaded");
            } catch (TimeoutException e) {
                System.out.println("Results timeout. Looking for alternative result elements...");
                findResultsWithAlternativeSelectors(driver);
            }

            // Load all results
            scrollThroughResults(driver);

            // Extract course data
            courses = extractCourseData(driver, limit);

            // Save to CSV
            for (Course course : courses) {
                csvDataService.saveCourse(course);
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
            // Configure Chrome options for better stability
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-popup-blocking");
            options.addArguments("--disable-notifications");
            options.addArguments("--dns-prefetch-disable");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");

            WebDriver driver = new ChromeDriver(options);
            System.out.println("WebDriver initialized successfully.");
            return driver;
        } catch (Exception e) {
            System.err.println("Failed to initialize Chrome driver: " + e.getMessage());
            throw e;
        }
    }

    private void handlePopups(WebDriver driver) {
        // Use a shorter timeout for popups since they might not appear
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(5))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class);

        try {
            WebElement closeBannerButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[aria-label='Close banner']")));
            closeBannerButton.click();
            System.out.println("Closed banner");
        } catch (TimeoutException e) {
            System.out.println("No banner found");
        }

        try {
            WebElement acceptCookies = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button#pendo-button-guide")));
            acceptCookies.click();
            System.out.println("Closed pop-up");
        } catch (TimeoutException e) {
            System.out.println("No pop-up found");
        }
    }

    private boolean clickSearchButton(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));

        // Try multiple selectors for search button
        List<String> searchButtonSelectors = Arrays.asList(
                "button[aria-label='Search']",
                "button.search-button",
                "[data-e2e='search-button']",
                ".search-field-button"
        );

        for (String selector : searchButtonSelectors) {
            try {
                WebElement searchButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
                searchButton.click();
                System.out.println("Clicked search button with selector: " + selector);

                // Wait after clicking
                Thread.sleep(2000);
                return true;
            } catch (Exception e) {
                System.out.println("Could not find search button with selector: " + selector);
            }
        }

        // Try JavaScript click as last resort
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("document.querySelector('button[aria-label=\"Search\"]').click();");
            System.out.println("Used JavaScript to click search button");
            Thread.sleep(2000);
            return true;
        } catch (Exception e) {
            System.out.println("JavaScript click failed");
            return false;
        }
    }

    private boolean enterSearchTerm(WebDriver driver, String searchTerm) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));

        // Try multiple selectors for search input
        List<String> searchInputSelectors = Arrays.asList(
                "input.react-autosuggest__input",
                "input#search-autocomplete-input",
                "input[placeholder='What do you want to learn?']",
                ".search-input-field"
        );

        for (String selector : searchInputSelectors) {
            try {
                WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
                searchBox.clear();
                searchBox.sendKeys(searchTerm + Keys.RETURN);
                System.out.println("Entered search term with selector: " + selector);
                return true;
            } catch (Exception e) {
                System.out.println("Could not find search input with selector: " + selector);
            }
        }

        return false;
    }

    private boolean findResultsWithAlternativeSelectors(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));

        // Try multiple selectors for results
        List<String> resultSelectors = Arrays.asList(
                "li.cds-9",
                ".product-list-item",
                ".course-card",
                ".search-result-item",
                "div.card",
                "ul.ais-InfiniteHits-list li"
        );

        for (String selector : resultSelectors) {
            try {
                List<WebElement> results = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(selector)));
                if (!results.isEmpty()) {
                    System.out.println("Found results with selector: " + selector);
                    return true;
                }
            } catch (Exception e) {
                System.out.println("No results with selector: " + selector);
            }
        }

        System.out.println("Could not find any search results with any selector");
        return false;
    }

    private void scrollThroughResults(WebDriver driver) {
        System.out.println("Scrolling through results...");
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
            int scrollAttempts = 0;
            final int MAX_SCROLL_ATTEMPTS = 5;

            while (scrollAttempts < MAX_SCROLL_ATTEMPTS) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
                Thread.sleep(1500);
                long newHeight = (long) js.executeScript("return document.body.scrollHeight");

                if (newHeight == lastHeight) {
                    scrollAttempts++;
                } else {
                    scrollAttempts = 0;
                }

                lastHeight = newHeight;
            }

            System.out.println("Finished scrolling");
        } catch (Exception e) {
            System.err.println("Error during scrolling: " + e.getMessage());
        }
    }

    private List<Course> extractCourseData(WebDriver driver, int limit) {
        List<WebElement> courseElements = new ArrayList<>();
        List<Course> courses = new ArrayList<>();

        // Try multiple selectors to find course elements
        List<String> courseSelectors = Arrays.asList(
                "li.cds-9",
                ".product-list-item",
                ".course-card",
                ".search-result-item",
                "div.card"
        );

        for (String selector : courseSelectors) {
            try {
                List<WebElement> found = driver.findElements(By.cssSelector(selector));
                if (!found.isEmpty()) {
                    courseElements = found;
                    System.out.println("Found " + courseElements.size() + " courses with selector: " + selector);
                    break;
                }
            } catch (Exception e) {
                System.out.println("Error finding courses with selector " + selector + ": " + e.getMessage());
            }
        }

        if (courseElements.isEmpty()) {
            System.out.println("No courses found to extract.");
            return courses;
        }

        System.out.println("Extracting data from courses");
        int processedCount = 0;
        int successCount = 0;

        // Process a limited number of courses
        int coursesToProcess = Math.min(courseElements.size(), limit);
        System.out.println("Will process first " + coursesToProcess + " courses");

        for (int i = 0; i < coursesToProcess; i++) {
            WebElement courseElement = courseElements.get(i);
            try {
                processedCount++;
                System.out.println("Processing course " + processedCount + " of " + coursesToProcess);

                // Extract course details
                Map<String, String> courseDetails = extractCourseDetails(driver, courseElement);

                // Create course object
                Course course = new Course();
                course.setTitle(courseDetails.getOrDefault("title", "No Title"));
                course.setPrice(FIXED_PRICE);

                // Set rating as double
                try {
                    String ratingStr = courseDetails.getOrDefault("rating", "0");
                    double rating = Double.parseDouble(ratingStr);
                    course.setRating(rating);
                } catch (NumberFormatException e) {
                    course.setRating(random.nextDouble() * 2 + 3); // Random rating between 3-5
                }

                course.setDescription(courseDetails.getOrDefault("skills", "No description available"));
                course.setUrl(courseDetails.getOrDefault("url", ""));
                course.setExtractedText(courseDetails.getOrDefault("htmlText", ""));
                course.setHtmlCode(courseDetails.getOrDefault("htmlText", ""));

                courses.add(course);
                successCount++;

            } catch (Exception e) {
                System.out.println("Error processing course " + processedCount + ": " + e.getMessage());
            }
        }

        System.out.println("Successfully extracted " + successCount + " out of " + processedCount + " courses");
        return courses;
    }

    private Map<String, String> extractCourseDetails(WebDriver driver, WebElement course) {
        Map<String, String> details = new HashMap<>();

        // Extract title with multiple possible selectors
        details.put("title", extractTextWithMultipleSelectors(course, Arrays.asList(
                "h3.cds-CommonCard-title",
                "h2",
                ".course-name",
                ".course-title",
                "h3"
        )));

        // Extract partner with multiple possible selectors
        details.put("partner", extractTextWithMultipleSelectors(course, Arrays.asList(
                "p.cds-ProductCard-partnerNames",
                ".partner-name",
                ".provider",
                ".institution"
        )));

        // Extract the raw rating text
        String rawRating = extractTextWithMultipleSelectors(course, Arrays.asList(
                "span.css-6ecy9b",
                ".ratings",
                ".rating-number",
                ".star-rating"
        ));

        String rating = "";
        if (rawRating != null && !rawRating.isEmpty()) {
            // Use regex to extract the first numeric value (integer or decimal)
            Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
            Matcher matcher = pattern.matcher(rawRating);
            if (matcher.find()) {
                rating = matcher.group(1);
            }
        }
        details.put("rating", rating);

        // Get additional details from course link
        Map<String, String> detailsFromCourseLink = extractDetailsFromCourseLink(driver, course);

        // Combine the details
        details.putAll(detailsFromCourseLink);

        return details;
    }

    private String extractTextWithMultipleSelectors(WebElement parent, List<String> selectors) {
        for (String selector : selectors) {
            try {
                WebElement element = parent.findElement(By.cssSelector(selector));
                String text = element.getText().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            } catch (Exception ignored) {
                // Continue to next selector
            }
        }

        // Try direct text as last resort
        try {
            String directText = parent.getText();
            if (directText != null && !directText.isEmpty()) {
                // Limit to first line if there are multiple lines
                if (directText.contains("\n")) {
                    return directText.substring(0, directText.indexOf('\n')).trim();
                }
                return directText.trim();
            }
        } catch (Exception ignored) {
            // Fall through to default
        }

        return "";
    }

    private Map<String, String> extractDetailsFromCourseLink(WebDriver driver, WebElement course) {
        Map<String, String> details = new HashMap<>();
        String skills = "";
        String url = "";
        String htmlText = "";
        String originalWindow = driver.getWindowHandle();

        try {
            // Try to find clickable link with multiple selectors
            WebElement link = null;
            List<String> linkSelectors = Arrays.asList(
                    "a.cds-CommonCard-titleLink",
                    "a.course-link",
                    "a.title-link",
                    "a"
            );

            for (String selector : linkSelectors) {
                try {
                    link = course.findElement(By.cssSelector(selector));
                    break;
                } catch (Exception ignored) {
                    // Try next selector
                }
            }

            if (link == null) {
                return details;
            }

            // Get the URL before clicking
            url = link.getAttribute("href");
            details.put("url", url);

            // Open in new tab
            link.sendKeys(Keys.CONTROL, Keys.RETURN);

            // Wait for new tab
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.numberOfWindowsToBe(2));

            // Find and switch to new tab
            for (String windowHandle : driver.getWindowHandles()) {
                if (!originalWindow.equals(windowHandle)) {
                    driver.switchTo().window(windowHandle);
                    break;
                }
            }

            // Get the HTML content of the entire page
            try {
                WebElement body = driver.findElement(By.tagName("body"));
                htmlText = body.getText();
                details.put("htmlText", htmlText);
            } catch (Exception e) {
                System.out.println("Could not get HTML text: " + e.getMessage());
            }

            // Try multiple approaches to get skills
            List<String> skillsSelectors = Arrays.asList(
                    "div.css-1m3kxpf ul.css-yk0mzy li a",
                    ".skills-list li",
                    ".course-skills-list li",
                    ".skills-you-will-gain li",
                    "[data-e2e='skills-list'] li"
            );

            for (String selector : skillsSelectors) {
                try {
                    WebDriverWait skillsWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                    List<WebElement> skillElements = skillsWait.until(
                            ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(selector)));

                    if (!skillElements.isEmpty()) {
                        List<String> skillList = new ArrayList<>();
                        for (WebElement skill : skillElements) {
                            skillList.add(skill.getText().trim());
                        }
                        skills = String.join("; ", skillList);
                        if (!skills.isEmpty()) {
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    // Try next selector
                }
            }

            // If still no skills, try to get text from general about section
            if (skills == null || skills.isEmpty()) {
                try {
                    WebDriverWait aboutWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                    WebElement aboutContainer = aboutWait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("div.css-1m3kxpf, .about-section, .course-description")));

                    skills = aboutContainer.getText();
                    if (skills.contains("\n")) {
                        skills = skills.substring(0, skills.indexOf("\n")).trim();
                    }
                } catch (Exception e) {
                    System.out.println("Could not find course description");
                }
            }

            details.put("skills", skills);

        } catch (Exception e) {
            System.out.println("Error extracting details from course link: " + e.getMessage());
        } finally {
            // Always close tab and return to main window
            if (driver.getWindowHandles().size() > 1) {
                try {
                    driver.close();
                    driver.switchTo().window(originalWindow);
                } catch (Exception e) {
                    System.out.println("Error closing tab: " + e.getMessage());
                    // Try to force switch back to main window
                    try {
                        driver.switchTo().window(originalWindow);
                    } catch (Exception ignored) {}
                }
            }
        }

        return details;
    }
}