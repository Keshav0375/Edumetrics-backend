package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for scraping Khan Academy courses.
 * This implements the CourseScraperService interface to fit into the application's scraper architecture.
 */
@Service
public class KhanAcademyScraperService implements CourseScraperService {

    // Constants for configuration
    private static final int WAIT_TIMEOUT = 30; // Timeout duration in seconds
    private static final String BASE_URL = "https://www.khanacademy.org";

    /**
     * Returns the name of the platform this scraper supports.
     * @return The platform name
     */
    @Override
    public String getPlatformName() {
        return "KhanAcademy";
    }

    /**
     * Scrapes courses from Khan Academy based on a search query.
     * @param query Search query for courses
     * @param limit Maximum number of courses to scrape
     * @return List of Course objects
     */
    @Override
    public List<Course> scrapeCourses(String query, int limit) {
        WebDriver driver = null;
        List<Course> courses = new ArrayList<>();

        try {
            // Initialize the WebDriver
            driver = initializeDriver();
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // If query is provided, try to search for courses
            if (query != null && !query.isEmpty()) {
                courses.addAll(searchCourses(driver, js, query, limit));
            }

            // If we didn't find any courses from search or no query was provided,
            // fall back to scraping subject pages
            if (courses.isEmpty()) {
                courses.addAll(scrapeSubjectPages(driver, js, limit));
            }

            // Limit the number of courses if necessary
            if (courses.size() > limit && limit > 0) {
                courses = courses.subList(0, limit);
            }

        } catch (Exception e) {
            System.err.println("Error in scraping Khan Academy: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            if (driver != null) {
                driver.quit();
            }
        }

        return courses;
    }

    /**
     * Initializes the Chrome WebDriver with appropriate settings.
     * @return Configured WebDriver
     */
    private WebDriver initializeDriver() {
        System.out.println("Setting up Chrome driver for Khan Academy scraper...");

        // Configure Chrome options for better scraping
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-infobars");
        options.addArguments("--headless"); // Run in headless mode for server deployment

        // Initialize WebDriver
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(WAIT_TIMEOUT));

        return driver;
    }

    /**
     * Searches for courses using the provided query.
     * @param driver WebDriver instance
     * @param js JavascriptExecutor for page interactions
     * @param query Search query
     * @param limit Maximum number of courses to return
     * @return List of Course objects
     */
    private List<Course> searchCourses(WebDriver driver, JavascriptExecutor js, String query, int limit) {
        List<Course> courses = new ArrayList<>();

        try {
            // Navigate to Khan Academy search page
            driver.get(BASE_URL + "/search?page=1&page_size=48&format=all&keyword=" + query.replace(" ", "+"));
            System.out.println("Searching Khan Academy for: " + query);
            Thread.sleep(5000);

            // Handle any popups
            handlePopups(driver);

            // Scroll to load all content
            scrollThroughPage(js);

            // Find search results
            List<WebElement> resultElements = driver.findElements(By.cssSelector(".search-result"));
            if (resultElements.isEmpty()) {
                resultElements = driver.findElements(By.cssSelector(".search-result-item"));
            }
            if (resultElements.isEmpty()) {
                resultElements = driver.findElements(By.cssSelector("[data-test-id*='search-result']"));
            }

            System.out.println("Found " + resultElements.size() + " search results");

            // Process each result
            int count = 0;
            for (WebElement element : resultElements) {
                if (count >= limit && limit > 0) break;

                try {
                    Course course = extractCourseFromElement(element);
                    if (course != null) {
                        courses.add(course);
                        count++;
                    }
                } catch (Exception e) {
                    System.err.println("Error processing search result: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error during search: " + e.getMessage());
        }

        return courses;
    }

    /**
     * Scrapes courses from subject pages.
     * @param driver WebDriver instance
     * @param js JavascriptExecutor for page interactions
     * @param limit Maximum number of courses to return
     * @return List of Course objects
     */
    private List<Course> scrapeSubjectPages(WebDriver driver, JavascriptExecutor js, int limit) {
        List<Course> courses = new ArrayList<>();

        try {
            // Navigate to subjects page
            driver.get(BASE_URL + "/subjects");
            System.out.println("Navigating to Khan Academy subjects page...");
            Thread.sleep(5000);

            // Handle any popups
            handlePopups(driver);

            // Get URLs for all subjects
            List<String> subjectUrls = getSubjectUrls(driver);
            System.out.println("Found " + subjectUrls.size() + " subject areas");

            // Process each subject until we reach the limit
            int count = 0;
            for (String url : subjectUrls) {
                if (count >= limit && limit > 0) break;

                System.out.println("Processing subject: " + url);

                // Get courses from subject page
                List<Course> subjectCourses = scrapeSubjectPage(driver, js, url);

                // Add courses up to the limit
                for (Course course : subjectCourses) {
                    if (count >= limit && limit > 0) break;
                    courses.add(course);
                    count++;
                }

                // If we still need more courses, check course lists
                if ((count < limit || limit <= 0) && courses.size() < 50) {
                    List<String> courseListUrls = getCourseListUrls(driver, js, url);
                    for (String courseListUrl : courseListUrls) {
                        if (count >= limit && limit > 0) break;

                        List<Course> listCourses = scrapeCourseListPage(driver, js, courseListUrl);
                        for (Course course : listCourses) {
                            if (count >= limit && limit > 0) break;
                            courses.add(course);
                            count++;
                        }
                    }
                }
            }

            // If we still don't have enough courses, try direct course paths
            if ((courses.size() < limit || limit <= 0) && courses.size() < 20) {
                List<Course> directCourses = scrapeDirectCoursePaths(driver, js);
                for (Course course : directCourses) {
                    if (count >= limit && limit > 0) break;
                    courses.add(course);
                    count++;
                }
            }

        } catch (Exception e) {
            System.err.println("Error scraping subject pages: " + e.getMessage());
        }

        return courses;
    }

    /**
     * Handles any popups that might appear on the page.
     * @param driver WebDriver instance
     */
    private void handlePopups(WebDriver driver) {
        System.out.println("Checking for popups...");

        // List of common popup selectors
        List<String> popupSelectors = List.of(
                "button[data-test-id='cookie-banner-accept']",
                ".cookie-notice-button",
                ".smart-banner-close",
                ".close-button",
                ".modal-close",
                "[aria-label='Close']",
                "[data-test-id='close-button']"
        );

        // Try to close each type of popup
        for (String selector : popupSelectors) {
            try {
                List<WebElement> popups = driver.findElements(By.cssSelector(selector));
                for (WebElement popup : popups) {
                    if (popup.isDisplayed()) {
                        popup.click();
                        Thread.sleep(500);
                    }
                }
            } catch (Exception e) {
                // Continue if popup not found or can't be closed
                continue;
            }
        }
    }

    /**
     * Scrolls through the page to load dynamic content.
     * @param js JavascriptExecutor for scrolling
     */
    private void scrollThroughPage(JavascriptExecutor js) {
        try {
            // Track scroll height
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
            int scrollAttempts = 0;
            int maxScrollAttempts = 6; // Limit scrolling attempts

            // Scroll incrementally
            while (scrollAttempts < maxScrollAttempts) {
                js.executeScript("window.scrollBy(0, 500);");
                Thread.sleep(600);

                long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) {
                    // Try one more time with a larger scroll
                    js.executeScript("window.scrollBy(0, 1000);");
                    Thread.sleep(800);

                    newHeight = (long) js.executeScript("return document.body.scrollHeight");
                    if (newHeight == lastHeight) {
                        // No new content loaded, stop scrolling
                        break;
                    }
                }
                lastHeight = newHeight;
                scrollAttempts++;
            }

            // Scroll back to top
            js.executeScript("window.scrollTo(0, 0);");
        } catch (Exception e) {
            System.err.println("Error during scrolling: " + e.getMessage());
        }
    }

    /**
     * Extracts URLs for all subject areas on Khan Academy.
     * @param driver WebDriver instance
     * @return List of subject URLs
     */
    private List<String> getSubjectUrls(WebDriver driver) {
        List<String> urls = new ArrayList<>();

        try {
            // Selectors for subject links
            List<String> subjectSelectors = List.of(
                    "a[role='button'][href*='/subject/']",
                    ".subject-card a",
                    "a[data-test-id*='subject-link']",
                    "div[role='navigation'] a[href*='/subject/']",
                    ".subject-link",
                    ".subjectsList a",
                    ".homepage-subject a"
            );

            // Try each selector
            for (String selector : subjectSelectors) {
                List<WebElement> subjectLinks = driver.findElements(By.cssSelector(selector));
                if (!subjectLinks.isEmpty()) {
                    for (WebElement link : subjectLinks) {
                        String url = link.getAttribute("href");
                        if (url != null && !url.isEmpty() && !urls.contains(url)) {
                            urls.add(url);
                        }
                    }
                }
            }

            // If no links found, use default subjects
            if (urls.isEmpty()) {
                System.out.println("No subject links found, using default subjects...");
                urls.addAll(List.of(
                        BASE_URL + "/math",
                        BASE_URL + "/science",
                        BASE_URL + "/computing",
                        BASE_URL + "/humanities",
                        BASE_URL + "/economics-finance-domain",
                        BASE_URL + "/ela",
                        BASE_URL + "/test-prep"
                ));
            }
        } catch (Exception e) {
            System.err.println("Error getting subject URLs: " + e.getMessage());
        }

        return urls;
    }

    /**
     * Gets URLs for course list pages from a subject page.
     * @param driver WebDriver instance
     * @param js JavascriptExecutor for interactions
     * @param subjectUrl The URL of the subject page
     * @return List of URLs for course list pages
     */
    private List<String> getCourseListUrls(WebDriver driver, JavascriptExecutor js, String subjectUrl) {
        List<String> urls = new ArrayList<>();

        try {
            driver.get(subjectUrl);
            Thread.sleep(3000);

            // Scroll to load all content
            scrollThroughPage(js);

            // Find links to course lists
            List<WebElement> links = driver.findElements(By.cssSelector("a[href*='/learn']"));
            links.addAll(driver.findElements(By.cssSelector("a[href*='/course']")));
            links.addAll(driver.findElements(By.cssSelector("a[href*='/unit']")));
            links.addAll(driver.findElements(By.cssSelector("a[href*='/topic']")));

            for (WebElement link : links) {
                String url = link.getAttribute("href");
                if (url != null && !url.isEmpty() && !urls.contains(url)) {
                    urls.add(url);
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting course list URLs: " + e.getMessage());
        }

        return urls;
    }

    /**
     * Scrapes a single subject page for course information.
     * @param driver WebDriver instance
     * @param js JavascriptExecutor for interactions
     * @param url The URL of the subject page
     * @return List of Course objects for the subject
     */
    private List<Course> scrapeSubjectPage(WebDriver driver, JavascriptExecutor js, String url) {
        List<Course> courses = new ArrayList<>();

        try {
            // Navigate to the subject page
            driver.get(url);
            System.out.println("Waiting for page to load: " + url);
            Thread.sleep(3000);

            // Scroll to load all content
            scrollThroughPage(js);

            // Extract the subject name from the page
            String subjectName = getPageTitle(driver);

            // Get all course cards or links
            List<WebElement> courseElements = new ArrayList<>();

            // Lists of selectors to try
            List<String> courseContainerSelectors = List.of(
                    ".topic-card",
                    ".topic-container",
                    ".course-item",
                    ".unit-card",
                    "[data-test-id*='topic-card']",
                    "[data-test-id*='course-card']",
                    ".tutorial-card"
            );

            // Try each container selector
            for (String selector : courseContainerSelectors) {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                if (!elements.isEmpty()) {
                    courseElements.addAll(elements);
                }
            }

            // If no course elements found, try direct links
            if (courseElements.isEmpty()) {
                List<WebElement> links = driver.findElements(By.cssSelector("a[href*='/learn/']"));
                links.addAll(driver.findElements(By.cssSelector("a[href*='/course/']")));
                links.addAll(driver.findElements(By.cssSelector("a[href*='/unit/']")));
                courseElements.addAll(links);
            }

            // Process each course element
            for (WebElement element : courseElements) {
                try {
                    Course course = extractCourseFromElement(element, subjectName);
                    if (course != null) {
                        courses.add(course);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing course element: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error scraping page " + url + ": " + e.getMessage());
        }

        return courses;
    }

    /**
     * Scrapes a course list page for detailed course information.
     * @param driver WebDriver instance
     * @param js JavascriptExecutor for interactions
     * @param url The URL of the course list page
     * @return List of Course objects from the page
     */
    private List<Course> scrapeCourseListPage(WebDriver driver, JavascriptExecutor js, String url) {
        List<Course> courses = new ArrayList<>();

        try {
            // Navigate to the page
            driver.get(url);
            Thread.sleep(3000);

            // Scroll to load all content
            scrollThroughPage(js);

            // Get page title as course category
            String category = getPageTitle(driver);

            // Find lesson items or content modules
            List<WebElement> lessonElements = new ArrayList<>();

            // Try multiple selectors for lesson items
            List<String> lessonSelectors = List.of(
                    ".lesson-card",
                    ".video-card",
                    ".tutorial-card",
                    ".content-card",
                    ".article-card",
                    "[data-test-id*='unit-card']",
                    "[data-test-id*='content-card']",
                    "[data-test-id*='video-card']",
                    "a[href*='/v/']"
            );

            // Try each selector
            for (String selector : lessonSelectors) {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                if (!elements.isEmpty()) {
                    lessonElements.addAll(elements);
                }
            }

            // Process each lesson element
            for (WebElement element : lessonElements) {
                try {
                    Course course = extractCourseFromElement(element, category);
                    if (course != null) {
                        courses.add(course);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing lesson element: " + e.getMessage());
                }
            }

            // If no specific lessons found, add the page itself as a course
            if (courses.isEmpty()) {
                Course pageCourse = createCourseFromPage(driver, category, url);
                if (pageCourse != null) {
                    courses.add(pageCourse);
                }
            }

        } catch (Exception e) {
            System.err.println("Error scraping course list page " + url + ": " + e.getMessage());
        }

        return courses;
    }

    /**
     * Scrapes courses from known direct paths as a fallback method.
     * @param driver WebDriver instance
     * @param js JavascriptExecutor for interactions
     * @return List of Course objects
     */
    private List<Course> scrapeDirectCoursePaths(WebDriver driver, JavascriptExecutor js) {
        List<Course> courses = new ArrayList<>();

        // List of known course paths to try
        List<String> coursePaths = List.of(
                "/math/cc-sixth-grade-math",
                "/math/cc-seventh-grade-math",
                "/math/cc-eighth-grade-math",
                "/math/algebra",
                "/math/geometry",
                "/math/algebra2",
                "/math/precalculus",
                "/math/ap-calculus-ab",
                "/science/physics",
                "/science/chemistry",
                "/science/biology",
                "/science/organic-chemistry",
                "/computing/computer-programming",
                "/computing/computer-science",
                "/humanities/world-history",
                "/humanities/us-history",
                "/economics-finance-domain/core-finance"
        );

        for (String path : coursePaths) {
            try {
                String url = BASE_URL + path;
                driver.get(url);
                Thread.sleep(3000);

                // Extract course data
                String title = getPageTitle(driver);
                Course course = createCourseFromPage(driver, title, url);
                if (course != null) {
                    courses.add(course);
                }

                // Also try to find sub-units on this page
                scrollThroughPage(js);
                List<Course> subCourses = scrapeCourseListPage(driver, js, url);
                courses.addAll(subCourses);

            } catch (Exception e) {
                System.err.println("Error processing direct course path: " + e.getMessage());
            }
        }

        return courses;
    }

    /**
     * Gets the title of the current page.
     * @param driver WebDriver instance
     * @return The page title
     */
    private String getPageTitle(WebDriver driver) {
        try {
            // Try to get title from h1 element first
            List<WebElement> h1s = driver.findElements(By.tagName("h1"));
            if (!h1s.isEmpty()) {
                String title = h1s.get(0).getText().trim();
                if (!title.isEmpty()) {
                    return title;
                }
            }

            // Try other common title elements
            List<String> titleSelectors = List.of(
                    ".title-text",
                    ".course-title",
                    ".unit-title",
                    "[data-test-id*='title']"
            );

            for (String selector : titleSelectors) {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                if (!elements.isEmpty()) {
                    String title = elements.get(0).getText().trim();
                    if (!title.isEmpty()) {
                        return title;
                    }
                }
            }

            // Fall back to page title
            return driver.getTitle().replace(" | Khan Academy", "").trim();

        } catch (Exception e) {
            return "Unknown Course";
        }
    }

    /**
     * Gets the description of the current page.
     * @param driver WebDriver instance
     * @return The page description
     */
    private String getPageDescription(WebDriver driver) {
        try {
            // Try to get description from meta tag
            try {
                WebElement metaDesc = driver.findElement(By.cssSelector("meta[name='description']"));
                String content = metaDesc.getAttribute("content");
                if (content != null && !content.isEmpty()) {
                    return content;
                }
            } catch (Exception e) {
                // Continue to other methods if meta tag not found
            }

            // Try common description elements
            List<String> descSelectors = List.of(
                    ".description",
                    ".course-description",
                    ".unit-description",
                    "p.intro",
                    "[data-test-id*='description']"
            );

            for (String selector : descSelectors) {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                if (!elements.isEmpty()) {
                    String desc = elements.get(0).getText().trim();
                    if (!desc.isEmpty()) {
                        return desc;
                    }
                }
            }

            // If no description found
            return "Learn about this topic on Khan Academy";

        } catch (Exception e) {
            return "No description available";
        }
    }

    /**
     * Extracts a Course object from a WebElement.
     * @param element WebElement containing course data
     * @return Course object or null if extraction fails
     */
    private Course extractCourseFromElement(WebElement element) {
        return extractCourseFromElement(element, null);
    }

    /**
     * Extracts a Course object from a WebElement with category context.
     * @param element WebElement containing course data
     * @param category Category/subject name for context
     * @return Course object or null if extraction fails
     */
    private Course extractCourseFromElement(WebElement element, String category) {
        try {
            // Extract title
            String title = element.getText().trim();

            // If title is empty or too long, try to find a more specific title element
            if (title.isEmpty() || title.length() > 100) {
                WebElement titleElement = findElementBySelectors(element, List.of(
                        "h2", "h3", ".title", "[data-test-id*='title']", "span"));
                if (titleElement != null) {
                    title = titleElement.getText().trim();
                }
            }

            // Skip if still no valid title
            if (title.isEmpty()) {
                return null;
            }

            // Add category to title if available
            if (category != null && !category.isEmpty()) {
                title = category + ": " + title;
            }

            // Extract URL
            String courseUrl;
            if (element.getTagName().equalsIgnoreCase("a")) {
                courseUrl = element.getAttribute("href");
            } else {
                WebElement link = findElementBySelectors(element, List.of("a"));
                courseUrl = link != null ? link.getAttribute("href") : "";
            }

            // If no valid URL, skip
            if (courseUrl == null || courseUrl.isEmpty()) {
                return null;
            }

            // Extract description
            String description = extractDescription(element);
            if (description.equals("No description available") && category != null) {
                description = "Course in " + category;
            }

            // Create and return the course object
            return createCourse(title, description, courseUrl, element.getAttribute("outerHTML"));

        } catch (Exception e) {
            System.err.println("Error extracting course from element: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a Course object from page data.
     * @param driver WebDriver instance
     * @param title Course title
     * @param url Course URL
     * @return Course object
     */
    private Course createCourseFromPage(WebDriver driver, String title, String url) {
        try {
            String description = getPageDescription(driver);

            // Get the HTML of the main course content
            String htmlCode;
            try {
                List<String> contentSelectors = List.of(
                        "main", "#main-content", ".course-container", ".unit-container", ".tutorial-container");

                for (String selector : contentSelectors) {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    if (!elements.isEmpty()) {
                        htmlCode = elements.get(0).getAttribute("outerHTML");
                        return createCourse(title, description, url, htmlCode);
                    }
                }

                // Fall back to body if main content area not found
                htmlCode = driver.findElement(By.tagName("body")).getAttribute("outerHTML");

            } catch (Exception e) {
                htmlCode = "<div>HTML not available</div>";
            }

            return createCourse(title, description, url, htmlCode);

        } catch (Exception e) {
            System.err.println("Error creating course from page: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a Course object with the provided data.
     * @param title Course title
     * @param description Course description
     * @param url Course URL
     * @param htmlCode HTML content
     * @return Course object
     */
    private Course createCourse(String title, String description, String url, String htmlCode) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setUrl(url);
        course.setPrice("Free"); // Khan Academy courses are free
        course.setPlatform("KhanAcademy");
        course.setHtmlCode(htmlCode);

        // Set default values for fields not available from Khan Academy
        course.setRating(0.0);

        return course;
    }

    /**
     * Finds an element using multiple possible selectors.
     * @param parent The parent element to search in
     * @param selectors List of CSS selectors to try
     * @return The first matching WebElement, or null if none found
     */
    private WebElement findElementBySelectors(WebElement parent, List<String> selectors) {
        for (String selector : selectors) {
            try {
                List<WebElement> elements = parent.findElements(By.cssSelector(selector));
                if (!elements.isEmpty()) {
                    return elements.get(0);
                }
            } catch (Exception e) {
                // Continue to next selector
            }
        }
        return null;
    }

    /**
     * Extracts course description from an element.
     * @param parent The parent element containing the description
     * @return The extracted description or a default message
     */
    private String extractDescription(WebElement parent) {
        try {
            // Selectors for description elements
            List<String> descriptionSelectors = List.of(
                    "p",
                    ".description",
                    "[data-test-id*='description']",
                    ".card-description",
                    ".summary"
            );

            // Try each selector
            WebElement descElement = findElementBySelectors(parent, descriptionSelectors);
            if (descElement != null) {
                String text = descElement.getText().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return "No description available";
    }
}