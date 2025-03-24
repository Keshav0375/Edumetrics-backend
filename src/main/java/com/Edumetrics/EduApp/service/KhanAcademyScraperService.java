package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

//@Service
public class KhanAcademyScraperService implements CourseScraperService {
    @Autowired
    private CsvDataService csvDataService;

    // Constants optimized for performance and reliability
    private static final int WAIT_TIMEOUT = 5;
    private static final String BASE_URL = "https://www.khanacademy.org";
    private static final int MAX_WAIT_MS = 400;
    private static final int THREAD_POOL_SIZE = 4;
    private static final double MIN_RELEVANCE_SCORE = 3.0;
    private static final int TARGET_TOTAL_COURSES = 20;
    private static final int MIN_COURSES_REQUIRED = 5;

    // ExecutorService for parallel processing
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final AtomicInteger totalCoursesCollected = new AtomicInteger(0);

    @Override
    public String getPlatformName() {
        return "KhanAcademy";
    }

    @Override
    public List<Course> scrapeCourses(String query, int limit) {
        WebDriverManager.chromedriver().setup();
        List<Course> courses = new ArrayList<>();

        try {
            // Parse user topics from query
            List<String> userTopics = parseUserTopics(query);

            // Build search URLs based on query
            List<String> searchUrls = buildSearchUrls(query);
            List<Future<List<Course>>> futures = new ArrayList<>();

            // Reset counter for this search
            totalCoursesCollected.set(0);

            // Submit tasks to executor service
            for (String url : searchUrls) {
                futures.add(executorService.submit(() -> scrapeCoursesFromUrl(url, limit, userTopics)));
            }

            // Collect results with timeout to ensure we stay within time budget
            for (Future<List<Course>> future : futures) {
                try {
                    List<Course> urlCourses = future.get(5, TimeUnit.SECONDS);
                    courses.addAll(urlCourses);
                } catch (Exception e) {
                    System.out.println("Warning: Search took too long and was skipped: " + e.getMessage());
                }
            }

            // Handle case where not enough courses were found
            if (courses.size() < MIN_COURSES_REQUIRED) {
                System.out.println("Not enough courses found. Searching more deeply...");
                List<String> deepSearchUrls = generateDeepSearchUrls(userTopics);
                for (String url : deepSearchUrls) {
                    try {
                        List<Course> deepCourses = scrapeCoursesFromUrl(url, limit, userTopics);
                        courses.addAll(deepCourses);
                    } catch (Exception e) {
                        // Continue with next URL
                    }
                    if (courses.size() >= MIN_COURSES_REQUIRED) break;
                }
            }

            // Filter low-relevance courses
            Map<Course, Double> scoreMap = new HashMap<>();
            for (Course course : courses) {
                double score = calculateRelevanceScore(course, userTopics);
                scoreMap.put(course, score);
            }

            courses.removeIf(course -> scoreMap.get(course) < MIN_RELEVANCE_SCORE);

            // Remove duplicates, sort by relevance and limit results
            courses = removeDuplicates(courses);
            courses.sort((c1, c2) -> Double.compare(scoreMap.getOrDefault(c2, 0.0), scoreMap.getOrDefault(c1, 0.0)));

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
     * Parse user topics from query string
     */
    private List<String> parseUserTopics(String query) {
        List<String> userTopics = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return userTopics;
        }

        String[] topics = query.split(",");
        for (String topic : topics) {
            String cleanTopic = topic.trim().toLowerCase();
            if (!cleanTopic.isEmpty()) {
                userTopics.add(cleanTopic);
            }
        }

        return userTopics;
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
                // Add direct search URLs
                urls.add(BASE_URL + "/search?page_search_query=%22" + cleanTopic.replace(" ", "+") + "%22");
                urls.add(BASE_URL + "/search?page_search_query=" + cleanTopic.replace(" ", "+"));
                urls.add(BASE_URL + "/search?page_search_query=" + cleanTopic.replace(" ", "+") + "+course");

                // Add topic-specific URLs
                String lowerTopic = cleanTopic.toLowerCase();
                if (lowerTopic.contains("python")) {
                    urls.add(BASE_URL + "/computing/computer-programming/programming");
                    urls.add(BASE_URL + "/computing/computer-science/algorithms");
                } else if (lowerTopic.contains("javascript")) {
                    urls.add(BASE_URL + "/computing/computer-programming/html-css-js");
                } else if (lowerTopic.contains("html") || lowerTopic.contains("css")) {
                    urls.add(BASE_URL + "/computing/computer-programming/html-css");
                } else if (lowerTopic.contains("math")) {
                    if (lowerTopic.contains("algebra")) {
                        urls.add(BASE_URL + "/math/algebra");
                        urls.add(BASE_URL + "/math/linear-algebra");
                    } else if (lowerTopic.contains("calculus")) {
                        urls.add(BASE_URL + "/math/calculus-home");
                        urls.add(BASE_URL + "/math/ap-calculus-ab");
                    } else if (lowerTopic.contains("geometry")) {
                        urls.add(BASE_URL + "/math/geometry");
                    } else {
                        urls.add(BASE_URL + "/math");
                    }
                } else if (lowerTopic.contains("physics")) {
                    urls.add(BASE_URL + "/science/physics");
                    urls.add(BASE_URL + "/science/ap-physics-1");
                } else if (lowerTopic.contains("chemistry")) {
                    urls.add(BASE_URL + "/science/chemistry");
                    urls.add(BASE_URL + "/science/organic-chemistry");
                } else if (lowerTopic.contains("biology")) {
                    urls.add(BASE_URL + "/science/biology");
                    urls.add(BASE_URL + "/science/ap-biology");
                } else if (lowerTopic.contains("history")) {
                    urls.add(BASE_URL + "/humanities/world-history");
                    urls.add(BASE_URL + "/humanities/us-history");
                }

                // Add category URLs based on topic keywords
                if (lowerTopic.contains("programming") ||
                        lowerTopic.contains("code") ||
                        lowerTopic.contains("computer")) {
                    urls.add(BASE_URL + "/computing/computer-programming");
                    urls.add(BASE_URL + "/computing/computer-science");
                } else if (lowerTopic.contains("science") ||
                        lowerTopic.contains("physics") ||
                        lowerTopic.contains("chemistry") ||
                        lowerTopic.contains("biology")) {
                    urls.add(BASE_URL + "/science");
                }
            }
        }

        // Remove duplicates
        return new ArrayList<>(new HashSet<>(urls));
    }

    /**
     * Generate deeper search URLs for specific user topics
     */
    private List<String> generateDeepSearchUrls(List<String> topics) {
        List<String> deepUrls = new ArrayList<>();
        Map<String, String[]> topicToPaths = new HashMap<>();

        // Define topic-specific paths
        topicToPaths.put("python", new String[]{
                "/computing/computer-programming/programming",
                "/computing/computer-science/algorithms",
                "/computing/ap-computer-science-principles"
        });
        topicToPaths.put("javascript", new String[]{
                "/computing/computer-programming/html-css-js",
                "/computing/computer-programming/programming/drawing-basics"
        });
        topicToPaths.put("html", new String[]{
                "/computing/computer-programming/html-css",
                "/computing/computer-programming/html-css-js"
        });
        topicToPaths.put("css", new String[]{
                "/computing/computer-programming/html-css",
                "/computing/computer-programming/html-css-js"
        });
        topicToPaths.put("physics", new String[]{
                "/science/physics",
                "/science/ap-college-physics-1"
        });
        topicToPaths.put("chemistry", new String[]{
                "/science/chemistry",
                "/science/organic-chemistry"
        });
        topicToPaths.put("biology", new String[]{
                "/science/biology",
                "/science/ap-biology"
        });
        topicToPaths.put("algebra", new String[]{
                "/math/algebra",
                "/math/linear-algebra"
        });
        topicToPaths.put("calculus", new String[]{
                "/math/calculus-1",
                "/math/ap-calculus-ab"
        });
        topicToPaths.put("geometry", new String[]{
                "/math/geometry",
                "/math/basic-geo"
        });

        // Generate deep search URLs
        for (String topic : topics) {
            String[] paths = topicToPaths.get(topic);
            if (paths != null) {
                for (String path : paths) {
                    deepUrls.add(BASE_URL + path);
                }
            }

            // Add advanced search parameters
            deepUrls.add(BASE_URL + "/search?referer=%2Fsearch&page_search_query=" +
                    topic.replace(" ", "+") + "+course");
            deepUrls.add(BASE_URL + "/search?referer=%2Fsearch&page_search_query=%22" +
                    topic.replace(" ", "+") + "%22");
        }

        return deepUrls;
    }

    /**
     * Scrape courses from a specific URL
     */
    private List<Course> scrapeCoursesFromUrl(String url, int limit, List<String> userTopics) {
        List<Course> courses = new ArrayList<>();
        WebDriver driver = null;

        try {
            // Skip if we already have enough courses
            if (totalCoursesCollected.get() >= TARGET_TOTAL_COURSES * 2) {
                return courses;
            }

            // Initialize driver with optimized settings
            driver = initializeDriver();

            // Navigate to URL
            driver.get(url);
            Thread.sleep(MAX_WAIT_MS);

            // Extract subject name and course links
            String subjectName = extractSubjectName(driver, url);
            List<WebElement> links = findCourseLinks(driver);
            Set<String> processedUrls = new HashSet<>();

            for (WebElement link : links) {
                try {
                    if (courses.size() >= Math.min(8, limit)) break;

                    String href = link.getAttribute("href");
                    if (href == null || href.isEmpty() || processedUrls.contains(href)) continue;
                    if (!href.startsWith(BASE_URL)) continue;

                    // Skip unwanted URLs
                    if (href.contains("/about/tos") || href.contains("/about/privacy") || href.contains("/about/cookies") ||
                            href.contains("/early-math") || href.contains("/cc-1st-grade-math") ||
                            href.contains("/cc-2nd-grade-math") || href.contains("/cc-3rd-grade-math") ||
                            href.contains("/cc-4th-grade-math") || href.contains("/cc-5th-grade-math") ||
                            href.contains("/cc-6th-grade-math") || href.contains("/cc-kindergarten-math") ||
                            href.contains("/k-8-grades")) {
                        continue;
                    }

                    processedUrls.add(href);

                    // Extract course details
                    String title = extractTitle(link, href, userTopics);
                    if (title.isEmpty() || title.toLowerCase().contains("skip to") ||
                            title.toLowerCase().contains("main content") || title.toLowerCase().contains("terms of service") ||
                            title.toLowerCase().contains("privacy policy") || title.toLowerCase().contains("cookies") ||
                            title.equals("Skip") || title.equals("Back") || title.equals("Next") || title.length() < 3) {
                        continue;
                    }

                    // Skip kids courses unless specifically requested
                    boolean isKidsCourse = title.toLowerCase().contains("for kids") ||
                            title.toLowerCase().contains("kindergarten") ||
                            title.toLowerCase().contains("1st grade") ||
                            title.toLowerCase().contains("2nd grade") ||
                            title.toLowerCase().contains("3rd grade") ||
                            title.toLowerCase().contains("4th grade") ||
                            title.toLowerCase().contains("5th grade") ||
                            title.toLowerCase().contains("6th grade") ||
                            title.toLowerCase().contains("early math") ||
                            title.toLowerCase().contains("elementary") ||
                            title.toLowerCase().contains("basic math");

                    boolean userWantsKidsContent = false;
                    for (String topic : userTopics) {
                        if (topic.contains("kid") || topic.contains("child") ||
                                topic.contains("elementary") || topic.contains("grade")) {
                            userWantsKidsContent = true;
                            break;
                        }
                    }

                    if (isKidsCourse && !userWantsKidsContent) continue;

                    String description = extractDescription(link, driver, subjectName);

                    // Create course object
                    Course course = new Course();
                    course.setTitle(title);
                    course.setDescription(description);
                    course.setUrl(href);
                    course.setPrice("Free");
                    course.setPlatform(getPlatformName());
                    course.setRating(0.0); // Khan Academy doesn't have course ratings

                    // Quick relevance check
                    double quickScore = calculateRelevanceScore(course, userTopics);
                    if (quickScore < MIN_RELEVANCE_SCORE) continue;

                    courses.add(course);
                    totalCoursesCollected.incrementAndGet();

                } catch (Exception e) {
                    // Skip problematic links
                }
            }

        } catch (Exception e) {
            System.out.println("Error processing " + url + ": " + e.getMessage());
        } finally {
            // Ensure driver is closed
            safeQuitDriver(driver);
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
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(800));

        return driver;
    }

    /**
     * Safely close a WebDriver
     */
    private void safeQuitDriver(WebDriver driver) {
        if (driver != null) {
            try {
                driver.manage().timeouts().implicitlyWait(Duration.ofMillis(100));
                driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(100));

                try { driver.manage().deleteAllCookies(); } catch (Exception e) {}
                try { driver.navigate().to("about:blank"); } catch (Exception e) {}
                try { driver.close(); } catch (Exception e) {}
                try {
                    Thread.sleep(100);
                    driver.quit();
                } catch (Exception e) {}
            } catch (Exception ex) {}
        }
    }

    /**
     * Extract title from link or URL
     */
    private String extractTitle(WebElement link, String href, List<String> userTopics) {
        String title = link.getText().trim();

        // If empty, extract from URL
        if (title.isEmpty() || title.length() < 3) {
            String urlEnd = href.substring(href.lastIndexOf('/') + 1).replace('-', ' ').replace('+', ' ');
            title = capitalizeWords(urlEnd);

            // If still problematic, use the parent segment
            if (title.length() < 3 || title.contains("?")) {
                String[] segments = href.split("/");
                if (segments.length >= 2) {
                    title = capitalizeWords(segments[segments.length - 2].replace('-', ' '));
                }
            }

            // Final fallback
            if (title.length() < 3 || title.contains("?") || title.equals("Khan Academy")) {
                for (String topic : userTopics) {
                    if (href.toLowerCase().contains(topic.toLowerCase())) {
                        title = "Khan Academy " + capitalizeWords(topic) + " Course";
                        break;
                    }
                }
                if (title.length() < 3) {
                    title = "Khan Academy Online Course";
                }
            }
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

            // Try spans if no paragraphs found
            if (description.isEmpty()) {
                List<WebElement> spans = parent.findElements(By.tagName("span"));
                for (WebElement span : spans) {
                    String text = span.getText().trim();
                    if (!text.isEmpty() && text.length() > 15 && !text.equals(link.getText().trim())) {
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
            String[] descriptions = {
                    "Learn " + subject + " with Khan Academy's interactive lessons and exercises.",
                    "Master the fundamentals of " + subject + " through practice problems and video lessons.",
                    "Comprehensive " + subject + " course with expert instruction from Khan Academy.",
                    subject + " concepts explained clearly with Khan Academy's world-class educational content."
            };

            description = descriptions[new Random().nextInt(descriptions.length)];
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
                ".subject a", // Subject links
                "a[href*='/learn/']", // Learn links
                "a[href*='/course/']", // Course links
                "a[href*='/unit/']", // Unit links
                "a[href*='/v/']", // Video links
                "a" // Fallback to all links
        };

        for (String selector : selectors) {
            try {
                List<WebElement> found = driver.findElements(By.cssSelector(selector));
                if (!found.isEmpty()) {
                    links.addAll(found);
                    if (links.size() > 40) break; // Get a reasonable number to process
                }
            } catch (Exception e) {
                // Try next selector
            }
        }

        return links;
    }

    /**
     * Calculate relevance score for a course
     */
    private double calculateRelevanceScore(Course course, List<String> userTopics) {
        if (userTopics.isEmpty()) return 0;

        String courseLower = (course.getTitle() + " " + course.getDescription() + " " + course.getUrl()).toLowerCase();
        double score = 0;
        boolean hasDirectMatch = false;

        // Penalize kids courses
        boolean isKidsCourse = courseLower.contains("for kids") ||
                courseLower.contains("kindergarten") ||
                courseLower.contains("grade math") ||
                courseLower.contains("early math") ||
                (courseLower.contains("children") && !userTopics.contains("children")) ||
                (courseLower.contains("elementary") && !userTopics.contains("elementary"));

        if (isKidsCourse) {
            boolean userWantsKidsContent = false;
            for (String topic : userTopics) {
                if (topic.contains("kid") || topic.contains("child") ||
                        topic.contains("elementary") || topic.contains("grade")) {
                    userWantsKidsContent = true;
                    break;
                }
            }

            if (!userWantsKidsContent) {
                score -= 20.0;
            }
        }

        // Score for each user topic
        for (String topic : userTopics) {
            // Exact topic match in title
            if (course.getTitle().toLowerCase().contains(" " + topic + " ") ||
                    course.getTitle().toLowerCase().startsWith(topic + " ") ||
                    course.getTitle().toLowerCase().endsWith(" " + topic)) {
                score += 15.0;
                hasDirectMatch = true;
            }
            // Partial match in title
            else if (course.getTitle().toLowerCase().contains(topic)) {
                score += 10.0;
                hasDirectMatch = true;
            }

            // Match in URL path
            if (course.getUrl().toLowerCase().contains("/" + topic) ||
                    course.getUrl().toLowerCase().contains("/" + topic.replace(" ", "-"))) {
                score += 8.0;
                hasDirectMatch = true;
            }

            // Match in description
            if (course.getDescription().toLowerCase().contains(topic)) {
                score += 3.0;
            }

            // Count occurrences
            int occurrences = countOccurrences(courseLower, topic);
            score += occurrences * 0.5;

            // Related terms matching
            if (topic.equals("python") && (courseLower.contains("programming") || courseLower.contains("code"))) {
                score += 2.0;
            } else if (topic.equals("javascript") && (courseLower.contains("web") || courseLower.contains("html"))) {
                score += 2.0;
            } else if (topic.equals("html") && (courseLower.contains("web") || courseLower.contains("css"))) {
                score += 2.0;
            }
        }

        // Additional scoring factors
        if (hasDirectMatch) score += 5.0;
        if (course.getTitle().toLowerCase().contains("kids") && !userTopics.contains("kids")) score -= 10.0;
        if (!hasDirectMatch && course.getUrl().contains("/search?")) score -= 3.0;
        if (course.getUrl().equals(BASE_URL) || course.getUrl().equals(BASE_URL + "/")) score -= 10.0;
        if (course.getTitle().toLowerCase().contains("terms") || course.getTitle().toLowerCase().contains("privacy") ||
                course.getTitle().toLowerCase().contains("policy") || course.getUrl().toLowerCase().contains("/about/tos") ||
                course.getUrl().toLowerCase().contains("/about/privacy")) {
            score -= 50.0;
        }

        return score;
    }

    /**
     * Count occurrences of topic in text
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
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
