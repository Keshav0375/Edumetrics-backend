package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Service
public class KhanAcademyScraperService implements CourseScraperService {
    @Autowired
    private CsvDataService csvDataService;

    // Constants optimized for performance and reliability
    private static final int WAIT_TIMEOUT = 3;
    private static final String BASE_URL = "https://www.khanacademy.org";
    private static final int MAX_WAIT_MS = 200;
    private static final int THREAD_POOL_SIZE = 8;

    // ExecutorService for parallel processing
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    @Override
    public String getPlatformName() {
        return "KhanAcademy";
    }

    @Override
    public List<Course> scrapeCourses(String query, int limit) {
        WebDriverManager.chromedriver().setup();
        List<Course> courses = new ArrayList<>();

        try {
            // Build search URLs based on query
            List<String> searchUrls = buildSearchUrls(query);
            List<Future<List<Course>>> futures = new ArrayList<>();

            // Submit tasks to executor service
            for (String url : searchUrls) {
                futures.add(executorService.submit(() -> scrapeCoursesFromUrl(url, limit)));
            }

            // Collect results with timeout to ensure we stay within time budget
            for (Future<List<Course>> future : futures) {
                try {
                    List<Course> urlCourses = future.get(10, TimeUnit.SECONDS);
                    courses.addAll(urlCourses);
                } catch (Exception e) {
                    System.out.println("Warning: Search took too long and was skipped: " + e.getMessage());
                }
            }

            // Remove duplicates and limit results
            courses = removeDuplicates(courses);
            if (courses.size() > limit) {
                courses = courses.subList(0, limit);
            }

        } catch (Exception e) {
            System.out.println("Error in Khan Academy scraper: " + e.getMessage());
            e.printStackTrace();
        }

        return courses;
    }

    /**
     * Build search URLs from query
     */
    private List<String> buildSearchUrls(String query) {
        List<String> urls = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            // Default URLs for empty query
            urls.add(BASE_URL + "/math");
            urls.add(BASE_URL + "/computing/computer-programming");
            urls.add(BASE_URL + "/science");
            return urls;
        }

        String[] topics = query.split(",");

        for (String topic : topics) {
            String cleanTopic = topic.trim();
            if (!cleanTopic.isEmpty()) {
                // Add direct search URL
                urls.add(BASE_URL + "/search?page_search_query=" + cleanTopic.replace(" ", "+"));

                // Add targeted URLs for better results
                if (cleanTopic.toLowerCase().contains("python") ||
                        cleanTopic.toLowerCase().contains("programming") ||
                        cleanTopic.toLowerCase().contains("code") ||
                        cleanTopic.toLowerCase().contains("computer")) {
                    urls.add(BASE_URL + "/computing/computer-programming");
                    urls.add(BASE_URL + "/computing/computer-science");
                }

                if (cleanTopic.toLowerCase().contains("math") ||
                        cleanTopic.toLowerCase().contains("algebra") ||
                        cleanTopic.toLowerCase().contains("calculus")) {
                    urls.add(BASE_URL + "/math");
                }

                if (cleanTopic.toLowerCase().contains("science") ||
                        cleanTopic.toLowerCase().contains("physics") ||
                        cleanTopic.toLowerCase().contains("chemistry") ||
                        cleanTopic.toLowerCase().contains("biology")) {
                    urls.add(BASE_URL + "/science");
                }
            }
        }

        // Remove duplicates
        return new ArrayList<>(new HashSet<>(urls));
    }

    /**
     * Scrape courses from a specific URL
     */
    private List<Course> scrapeCoursesFromUrl(String url, int limit) {
        List<Course> courses = new ArrayList<>();
        WebDriver driver = null;

        try {
            // Initialize driver with optimized settings
            driver = initializeDriver();

            // Navigate to URL
            driver.get(url);
            Thread.sleep(MAX_WAIT_MS);

            // Extract subject name
            String subjectName = extractSubjectName(driver, url);

            // Find course links
            List<WebElement> links = findCourseLinks(driver);
            Set<String> processedUrls = new HashSet<>();

            int courseCount = 0;
            for (WebElement link : links) {
                if (courseCount >= Math.min(10, limit)) break;

                try {
                    String href = link.getAttribute("href");

                    // Skip invalid or processed URLs
                    if (href == null || href.isEmpty() || processedUrls.contains(href)) continue;

                    // Keep only Khan Academy URLs with learning content
                    if (!href.startsWith(BASE_URL) ||
                            !(href.contains("/learn/") || href.contains("/course/") ||
                                    href.contains("/unit/") || href.contains("/v/") ||
                                    href.contains("/programming/") || href.contains("/computer") ||
                                    href.contains("/math/") || href.contains("/science/"))) {
                        continue;
                    }

                    processedUrls.add(href);

                    // Extract course details
                    String title = extractTitle(link, href);
                    String description = extractDescription(link, driver, subjectName);

                    // Create course object
                    Course course = new Course();
                    course.setTitle(title);
                    course.setDescription(description);
                    course.setUrl(href);
                    course.setPrice("Free");
                    course.setPlatform(getPlatformName());
                    course.setRating(0.0); // Not rated

                    courses.add(course);
                    courseCount++;

                } catch (Exception e) {
                    // Skip problematic links
                }
            }

        } catch (Exception e) {
            System.out.println("Error processing " + url + ": " + e.getMessage());
        } finally {
            // Ensure driver is closed
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        }

        return courses;
    }

    /**
     * Initialize Chrome WebDriver with optimized settings
     */
    private WebDriver initializeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-images");
        options.addArguments("--blink-settings=imagesEnabled=false");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(WAIT_TIMEOUT));
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(1000));

        return driver;
    }

    /**
     * Extract title from link or URL
     */
    private String extractTitle(WebElement link, String href) {
        // Try to get text from the link
        String title = link.getText().trim();

        // If empty, try to extract from URL
        if (title.isEmpty() || title.length() < 3) {
            String urlEnd = href.substring(href.lastIndexOf('/') + 1).replace('-', ' ');
            title = capitalizeWords(urlEnd);
        }

        return title;
    }

    /**
     * Extract description from link context or parent elements
     */
    private String extractDescription(WebElement link, WebDriver driver, String subject) {
        String description = "";

        try {
            // Try to find a description near the link
            WebElement parent = link.findElement(By.xpath("./.."));
            List<WebElement> paragraphs = parent.findElements(By.tagName("p"));

            if (!paragraphs.isEmpty()) {
                for (WebElement p : paragraphs) {
                    String text = p.getText().trim();
                    if (!text.isEmpty() && text.length() > 15) {
                        description = text;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors in description extraction
        }

        // If no description found, create a generic one
        if (description.isEmpty()) {
            description = "Learn " + subject + " with Khan Academy's interactive lessons";
        }

        return description;
    }

    /**
     * Extract subject name from URL or page title
     */
    private String extractSubjectName(WebDriver driver, String url) {
        String subject = "";

        // Try to get from page title
        try {
            String pageTitle = driver.getTitle();
            if (pageTitle != null && !pageTitle.isEmpty()) {
                if (pageTitle.contains("|")) {
                    subject = pageTitle.substring(0, pageTitle.indexOf("|")).trim();
                } else if (pageTitle.contains("-")) {
                    subject = pageTitle.substring(0, pageTitle.indexOf("-")).trim();
                } else {
                    subject = pageTitle.trim();
                }
            }
        } catch (Exception e) {
            // Ignore errors in subject extraction
        }

        // If still empty, extract from URL
        if (subject.isEmpty()) {
            // Extract from path segment
            String[] pathSegments = url.replace(BASE_URL, "").split("/");
            for (String segment : pathSegments) {
                if (!segment.isEmpty() && !segment.contains("?")) {
                    subject = segment.replace("-", " ");
                    break;
                }
            }

            // Fall back to "Khan Academy"
            if (subject.isEmpty()) {
                subject = "Khan Academy Course";
            }
        }

        return capitalizeWords(subject);
    }

    /**
     * Try different strategies to find course links
     */
    private List<WebElement> findCourseLinks(WebDriver driver) {
        List<WebElement> links = new ArrayList<>();

        // Try multiple selectors to find course links
        String[] selectors = {
                "a.card", // Card links
                "a.link_1uvuyao-o_O-link_cv47nc", // Course links
                "a[data-test-id*='tutorial-card']", // Tutorial cards
                "a[data-test-id*='course']", // Course links
                "a[data-test-id*='lesson']", // Lesson links
                "a.link_xt8zc8", // Another class used for course links
                "a", // Fallback to all links
        };

        for (String selector : selectors) {
            try {
                List<WebElement> found = driver.findElements(By.cssSelector(selector));
                if (!found.isEmpty()) {
                    links.addAll(found);
                    if (links.size() > 20) break; // Get a reasonable number to process
                }
            } catch (Exception e) {
                // Try next selector
            }
        }

        return links;
    }

    /**
     * Helper method to capitalize words
     */
    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : text.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Remove duplicate courses based on URL
     */
    private List<Course> removeDuplicates(List<Course> courses) {
        Map<String, Course> uniqueMap = new LinkedHashMap<>();

        for (Course course : courses) {
            // Only add if URL not already in map
            if (!uniqueMap.containsKey(course.getUrl())) {
                uniqueMap.put(course.getUrl(), course);
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }
}
