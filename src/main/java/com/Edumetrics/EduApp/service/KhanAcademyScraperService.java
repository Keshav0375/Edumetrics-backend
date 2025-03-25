package com.Edumetrics.EduApp.service;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import com.Edumetrics.EduApp.model.Course;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Khan Academy course scraper focusing on programming languages (Java, Python, JavaScript, HTML, CSS),
 * Computer Science, and Entrepreneurship with limited search depth
 */
@Service
public class KhanAcademyScraperService implements CourseScraperService{
    // Constants
    private static final int WAIT_TIMEOUT = 5;
    private static final String BASE_URL = "https://www.khanacademy.org";
    private static final String OUTPUT_FILE = "KhanAcademyProgrammingAndCS.csv";
    private static final int THREAD_POOL_SIZE = 1; // Reduced for more sequential behavior
    private static final int MAX_WAIT_MS = 500;
    private static final int PROGRAMMING_COURSE_TARGET = 10; // Target for programming language courses
    private static final int CS_COURSE_TARGET = 15; // Target for CS courses
    private static final int TOTAL_COURSE_TARGET = 25; // Total target including entrepreneurship
    private static final ConcurrentHashMap<String, Boolean> processedUrls = new ConcurrentHashMap<>();
    private static final AtomicInteger programmingCourseCounter = new AtomicInteger(0);
    private static final AtomicInteger csCourseCounter = new AtomicInteger(0);
    private static final AtomicInteger entrepreneurshipCourseCounter = new AtomicInteger(0);
    private static final int MAX_DEPTH = 2; // Reduced depth to avoid going deep into courses

    // Simplified non-subject patterns
    private static final List<String> NON_SUBJECT_PATTERNS = Arrays.asList(
            "skip", "login", "donate", "sign up", "help", "about", "careers",
            "account", "profile", "contact", "support", "privacy", "terms"
    );

    // Target subject patterns for Programming Languages
    private static final List<String> PROGRAMMING_LANGUAGE_PATTERNS = Arrays.asList(
            "java ", "python", "javascript", "html", "css", "programming language",
            "coding syntax", "language fundamentals", "web languages"
    );

    // Target subject patterns for Computer Science
    private static final List<String> CS_SUBJECT_PATTERNS = Arrays.asList(
            "computer"
//            "computer", "program", "algorithm", "coding", "software", "app development",
//            "web development", "data structure", "computing", "code"
    );

    // Target subject patterns for Entrepreneurship
    private static final List<String> ENTREPRENEURSHIP_SUBJECT_PATTERNS = Arrays.asList(
            "entrepreneur", "business", "startup", "finance", "economics", "market",
            "leadership", "management", "venture", "capital", "innovation"
    );

    // Current search phase
    private static volatile int searchPhase = 0; // 0 = Programming, 1 = CS, 2 = Entrepreneurship


    // Main method
    @Override
    public  ArrayList<Course> scrapeCourses(String query, int limit) {
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("Starting Khan Academy Scraper - Prioritizing Programming Languages, CS, then Entrepreneurship");

            // First phase: Programming Languages
            searchPhase = 0;
            System.out.println("Phase 1: Programming Languages - Target: " + PROGRAMMING_COURSE_TARGET + " courses");
            List<Course> programmingResults = searchForCourses(generateProgrammingLanguageUrls());
            System.out.println("Completed Programming Languages search. Found " + programmingResults.size() + " courses.");

            // Second phase: Computer Science
//            searchPhase = 1;
//            System.out.println("Phase 2: Computer Science - Target: " + CS_COURSE_TARGET + " courses");
//            List<Course> csResults = searchForCourses(generateCSSubjectUrls());
//            System.out.println("Completed CS search. Found " + csResults.size() + " Computer Science courses.");

//            // Third phase: Entrepreneurship
//            searchPhase = 2;
//            int remainingTarget = TOTAL_COURSE_TARGET - programmingResults.size() - csResults.size();
//            System.out.println("Phase 3: Entrepreneurship - Target: " + remainingTarget + " more courses");
//            List<Course> entrepreneurshipResults = searchForCourses(generateEntrepreneurshipSubjectUrls());
//            System.out.println("Completed Entrepreneurship search. Found " + entrepreneurshipResults.size() + " Entrepreneurship courses.");

            // Combine results
            List<Course> allCourses = new ArrayList<>(programmingResults);
//            allCourses.addAll(csResults);
//            allCourses.addAll(entrepreneurshipResults);

            // Remove any duplicates
            List<Course> uniqueCourses = removeDuplicates(allCourses);


            // Save results
//            saveToCsv(uniqueCourses, OUTPUT_FILE);

            // Print completion message
            long endTime = System.currentTimeMillis();
            System.out.println("Scraped " + uniqueCourses.size() + " total courses in " +
                    ((endTime - startTime) / 1000.0) + " seconds");
            System.out.println("Data saved to " + OUTPUT_FILE);
            ArrayList<Course> arrayListOfString = new ArrayList(uniqueCourses);
            return arrayListOfString;

        } catch (Exception e) {
            System.out.println("Error in main process: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<Course>();
        }
    }

    @Override
    public String getPlatformName() {
        return "KhanAcademy";
    }

    /**
     * Search for courses with a specific target category
     */
    private static List<Course> searchForCourses(List<String> startUrls) {
        List<Course> foundCourses = Collections.synchronizedList(new ArrayList<>());

        try {
            // Process URLs in parallel with improved concurrency
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            // Use a concurrent queue for breadth-first crawling
            BlockingQueue<ScrapingTask> scrapingQueue = new LinkedBlockingQueue<>();
            startUrls.forEach(url -> scrapingQueue.add(new ScrapingTask(url, 0)));

            // Counter for active threads
            AtomicInteger activeThreads = new AtomicInteger(0);
            CountDownLatch completionLatch = new CountDownLatch(THREAD_POOL_SIZE);

            // Target counter based on current phase
            AtomicInteger currentCounter;
            int currentTarget;

            if (searchPhase == 0) {
                currentCounter = programmingCourseCounter;
                currentTarget = PROGRAMMING_COURSE_TARGET;
            } else if (searchPhase == 1) {
                currentCounter = csCourseCounter;
                currentTarget = CS_COURSE_TARGET;
            } else {
                currentCounter = entrepreneurshipCourseCounter;
                currentTarget = TOTAL_COURSE_TARGET - programmingCourseCounter.get() - csCourseCounter.get();
            }

            // Start worker threads
            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                executor.submit(() -> {
                    try {
                        while (currentCounter.get() < currentTarget) {
                            ScrapingTask task = scrapingQueue.poll(100, TimeUnit.MILLISECONDS);
                            if (task == null) {
                                // If no tasks for a while and enough courses or no active threads, exit
                                if (currentCounter.get() >= currentTarget ||
                                        (activeThreads.get() <= 1 && scrapingQueue.isEmpty())) {
                                    break;
                                }
                                continue;
                            }

                            activeThreads.incrementAndGet();
                            try {
                                // Process URL if not already processed
                                if (processedUrls.putIfAbsent(task.url, Boolean.TRUE) == null) {
                                    ScrapeResult result = scrapeCourses1(task.url, task.depth);

                                    // Add found courses
                                    if (!result.courses.isEmpty()) {
                                        foundCourses.addAll(result.courses);
                                        int currentCount = currentCounter.addAndGet(result.courses.size());
                                        System.out.println("Found " + result.courses.size() +
                                                " courses from " + task.url + ". Total: " +
                                                currentCount + "/" + currentTarget);
                                    }

                                    // Add discovered URLs if needed and within depth limit
                                    if (currentCounter.get() < currentTarget && task.depth < MAX_DEPTH) {
                                        result.discoveredUrls.stream()
                                                .filter(url -> !processedUrls.containsKey(url))
                                                .filter(url -> {
                                                    if (searchPhase == 0) {
                                                        return isProgrammingLanguageUrl(url);
                                                    } else if (searchPhase == 1) {
                                                        return isCSSubjectUrl(url);
                                                    } else {
                                                        return isEntrepreneurshipSubjectUrl(url);
                                                    }
                                                })
                                                .forEach(url -> scrapingQueue.add(new ScrapingTask(url, task.depth + 1)));
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println("Error processing " + task.url + ": " + e.getMessage());
                            } finally {
                                activeThreads.decrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            // Wait for completion
            completionLatch.await();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            return foundCourses;

        } catch (Exception e) {
            System.out.println("Error in search process: " + e.getMessage());
            return foundCourses; // Return what we found so far
        }
    }

    /**
     * Check if a URL is related to Programming Languages
     */
    private static boolean isProgrammingLanguageUrl(String url) {
        if (url == null) return false;

        String lowerUrl = url.toLowerCase();

        // Direct check for specific programming language paths
        if (lowerUrl.contains("/java/") ||
                lowerUrl.contains("/python/") ||
                lowerUrl.contains("/javascript/") ||
                lowerUrl.contains("/html/") ||
                lowerUrl.contains("/css/") ||
                lowerUrl.contains("/html-css/")) {
            return true;
        }

        // Check for any programming language pattern in the URL
        return PROGRAMMING_LANGUAGE_PATTERNS.stream()
                .anyMatch(pattern -> lowerUrl.contains(pattern));
    }

    /**
     * Check if a URL is related to Computer Science
     */
    private static boolean isCSSubjectUrl(String url) {
        if (url == null) return false;

        String lowerUrl = url.toLowerCase();

        // Direct check for specific CS paths
        if (lowerUrl.contains("/computing/") ||
                lowerUrl.contains("/computer-science/") ||
                lowerUrl.contains("/computer-programming/")) {
            return true;
        }

        // Check for any CS subject pattern in the URL
        return CS_SUBJECT_PATTERNS.stream()
                .anyMatch(pattern -> lowerUrl.contains(pattern));
    }

    /**
     * Check if a URL is related to Entrepreneurship
     */
    private static boolean isEntrepreneurshipSubjectUrl(String url) {
        if (url == null) return false;

        String lowerUrl = url.toLowerCase();

        // Direct check for specific Entrepreneurship paths
        if (lowerUrl.contains("/entrepreneurship/") ||
                lowerUrl.contains("/economics-finance-domain/") ||
                lowerUrl.contains("/business/")) {
            return true;
        }

        // Check for any Entrepreneurship subject pattern in the URL
        return ENTREPRENEURSHIP_SUBJECT_PATTERNS.stream()
                .anyMatch(pattern -> lowerUrl.contains(pattern));
    }

    /**
     * Check if a course is a Programming Language course
     */
    private static boolean isProgrammingLanguageCourse(Course course) {
        if (course == null || course.getTitle() == null || course.getDescription() == null) {
            return false;
        }

        String lowerTitle = course.getTitle().toLowerCase();
        String lowerDesc = course.getDescription().toLowerCase();
        String lowerUrl = course.getUrl().toLowerCase();

        // Check specific programming languages first
        if (lowerTitle.contains("java ") || lowerUrl.contains("/java/") ||
                lowerTitle.contains("python") || lowerUrl.contains("/python/") ||
                lowerTitle.contains("javascript") || lowerUrl.contains("/javascript/") ||
                lowerTitle.contains("html") || lowerTitle.contains("css") ||
                lowerUrl.contains("/html/") || lowerUrl.contains("/css/") ||
                lowerUrl.contains("/html-css/")) {
            return true;
        }

        // Check if any programming language pattern exists
        return PROGRAMMING_LANGUAGE_PATTERNS.stream()
                .anyMatch(pattern ->
                        lowerTitle.contains(pattern) ||
                                lowerDesc.contains(pattern) ||
                                lowerUrl.contains(pattern)
                );
    }

    /**
     * Check if a course is a Computer Science course
     */
    private static boolean isCSCourse(Course course) {
        if (course == null || course.getTitle() == null || course.getDescription() == null) {
            return false;
        }

        String lowerTitle = course.getTitle().toLowerCase();
        String lowerDesc = course.getDescription().toLowerCase();
        String lowerUrl = course.getUrl().toLowerCase();

        // Check if any CS pattern exists in title, description or URL
        return CS_SUBJECT_PATTERNS.stream()
                .anyMatch(pattern ->
                        lowerTitle.contains(pattern) ||
                                lowerDesc.contains(pattern) ||
                                lowerUrl.contains(pattern)
                );
    }

    /**
     * Check if a course is an Entrepreneurship course
     */
    private static boolean isEntrepreneurshipCourse(Course course) {
        if (course == null || course.getTitle() == null || course.getDescription() == null) {
            return false;
        }

        String lowerTitle = course.getTitle().toLowerCase();
        String lowerDesc = course.getDescription().toLowerCase();
        String lowerUrl = course.getUrl().toLowerCase();

        // Check if any Entrepreneurship pattern exists in title, description or URL
        return ENTREPRENEURSHIP_SUBJECT_PATTERNS.stream()
                .anyMatch(pattern ->
                        lowerTitle.contains(pattern) ||
                                lowerDesc.contains(pattern) ||
                                lowerUrl.contains(pattern)
                );
    }

    /**
     * Generate URLs focusing on Programming Language subjects
     */
    private static List<String> generateProgrammingLanguageUrls() {
        Set<String> urls = new LinkedHashSet<>();

        // Programming language specific paths
        List<String> langSubpaths = Arrays.asList(
//        		"python"
//                "java", "python", "javascript", "html-css", "html", "css",
//                "programming", "intro-to-programming", "learn-to-code"
        );

        // Add main computing subject (which contains programming language courses)
//        urls.add(BASE_URL + "/computing");
//
//        // Add direct course listing pages first (prioritize)
//        urls.add(BASE_URL + "/computing/programming/courses");

        // Add language-specific subpaths
        langSubpaths.forEach(sub -> {
            String path = BASE_URL + "/computing/" + sub;
            // Prioritize direct course/lesson pages
//            urls.add(path + "/courses");
//            urls.add(path + "/lessons");
            urls.add(path);
        });

        // Add AP Computer Science paths which often contain Java programming
//        urls.add(BASE_URL + "/computing/ap-computer-science-java");
//        urls.add(BASE_URL + "/computing/ap-computer-science-principles");
//
//        // Add specific programming language intro pages
//        urls.add(BASE_URL + "/computing/programming/intro-to-java");
//        urls.add(BASE_URL + "/computing/programming/intro-to-python");
//        urls.add(BASE_URL + "/computing/programming/intro-to-javascript");
//        urls.add(BASE_URL + "/computing/programming/intro-to-html-css");

        return new ArrayList<>(urls);
    }

    /**
     * Generate URLs focusing on Computer Science subjects
     */
    private static List<String> generateCSSubjectUrls() {
        Set<String> urls = new LinkedHashSet<>();

        // Computing/Programming paths (CS focus)
        List<String> csSubpaths = Arrays.asList(
                "computer-programming"
//                "data-structures", "web-development", "intro-to-programming",
//                "information-theory", "cryptography", "ap-computer-science-principles", "sql"
        );

        // Add main subject
//        urls.add(BASE_URL + "/computing");
//
//        // Add direct course listing pages first (prioritize)
//        urls.add(BASE_URL + "/computing/courses");
//
//        // Add subpaths
        csSubpaths.forEach(sub -> {
            String path = BASE_URL + "/computing/" + sub;
            // Prioritize direct course/lesson pages
//            urls.add(path + "/courses");
//            urls.add(path + "/lessons");
            urls.add(path);
        });

        return new ArrayList<>(urls);
    }

    /**
     * Generate URLs focusing on Entrepreneurship subjects
     */
    private static List<String> generateEntrepreneurshipSubjectUrls() {
        Set<String> urls = new LinkedHashSet<>();

        // Economics/Business paths (Entrepreneurship focus)
        List<String> entrepreneurshipSubpaths = Arrays.asList(
                "entrepreneurship", "finance", "microeconomics", "macroeconomics",
                "business", "marketing", "accounting", "personal-finance",
                "venture-capital", "business-startups"
        );

        // Add main subject paths
        urls.add(BASE_URL + "/economics-finance-domain");

        // Add direct course listing pages first (prioritize)
        urls.add(BASE_URL + "/economics-finance-domain/courses");

        // Add subpaths
        entrepreneurshipSubpaths.forEach(sub -> {
            String path = BASE_URL + "/economics-finance-domain/" + sub;
            // Prioritize direct course/lesson pages
            urls.add(path + "/courses");
            urls.add(path + "/lessons");
            urls.add(path);
        });

        // Add additional potential subjects with entrepreneurship content
        urls.add(BASE_URL + "/college-careers-more/career-content/entrepreneurship");
        urls.add(BASE_URL + "/college-careers-more/personal-finance");

        return new ArrayList<>(urls);
    }

    /**
     * Initialize a Chrome WebDriver with optimized settings
     */
    private static WebDriver initializeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--disable-dev-shm-usage",
                "--no-sandbox", "--disable-extensions", "--disable-images",
                "--disable-javascript", "--blink-settings=imagesEnabled=false");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(WAIT_TIMEOUT));
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(300));

        return driver;
    }

    /**
     * Scrape courses with optimized approach
     */
    private static ScrapeResult scrapeCourses1(String url, int depth) {
        List<Course> courses = new ArrayList<>();
        Set<String> discoveredUrls = new HashSet<>();
        WebDriver driver = null;

        try {
            driver = initializeDriver();
            driver.get(url);

            // Quick sleep to allow minimal page load
            Thread.sleep(MAX_WAIT_MS);

            // Extract subject name
            String subjectName = extractSubjectFromUrl(url);

            // Get course elements with optimized selectors
            List<WebElement> elements = new ArrayList<>();

            // Try selectors based on current search phase
            if (searchPhase == 0) {
                // Programming language-specific selectors
                for (String selector : new String[] {
                        "a[data-test-id*='card']", ".subject-card", ".course-card",
                        "a[href*='/course']", "a[href*='/unit']",
                        "a[href*='/java/']", "a[href*='/python/']", "a[href*='/javascript/']",
                        "a[href*='/html/']", "a[href*='/css/']", "a[href*='/html-css/']"}) {
                    try {
                        elements.addAll(driver.findElements(By.cssSelector(selector)));
                        if (!elements.isEmpty()) break;  // Stop if found elements
                    } catch (Exception e) {}
                }
            } else if (searchPhase == 1) {
                // CS-specific selectors
                for (String selector : new String[] {
                        "a[data-test-id*='card']", ".subject-card", ".course-card",
                        "a[href*='/course']", "a[href*='/unit']",
                        "a[href*='/programming/']", "a[href*='/computer-science/']",
                        "a[href*='/algorithms/']", "a[href*='/data-structures/']"}) {
                    try {
                        elements.addAll(driver.findElements(By.cssSelector(selector)));
                        if (!elements.isEmpty()) break;  // Stop if found elements
                    } catch (Exception e) {}
                }
            } else {
                // Entrepreneurship-specific selectors
                for (String selector : new String[] {
                        "a[data-test-id*='card']", ".subject-card", ".course-card",
                        "a[href*='/course']", "a[href*='/unit']",
                        "a[href*='/entrepreneurship/']", "a[href*='/business/']",
                        "a[href*='/economics/']", "a[href*='/finance/']"}) {
                    try {
                        elements.addAll(driver.findElements(By.cssSelector(selector)));
                        if (!elements.isEmpty()) break;  // Stop if found elements
                    } catch (Exception e) {}
                }
            }

            // If no elements found with specific selectors, try all links but limit to likely lesson/course links
            if (elements.isEmpty()) {
                List<WebElement> allLinks = driver.findElements(By.tagName("a"));
                for (WebElement link : allLinks) {
                    try {
                        String href = link.getAttribute("href");
                        if (href != null && href.contains(BASE_URL) &&
                                (href.contains("/course") || href.contains("/lesson") ||
                                        href.contains("/unit") || href.contains("/topic"))) {
                            elements.add(link);
                        }
                    } catch (Exception e) {}
                }
            }

            // Process elements
            for (WebElement element : elements) {
                try {
                    // Get link URL
                    String href = element.getAttribute("href");
                    if (href == null || !href.startsWith(BASE_URL) ||
                            href.contains("#") || href.contains("login") ||
                            href.contains("about") || href.contains("help")) {
                        continue;
                    }

                    // Add to discovered URLs if at lower depth (to avoid going deep)
                    if (depth < MAX_DEPTH - 1) {
                        discoveredUrls.add(href);
                    }

                    // Extract course info
                    String title = extractTitle(element, href, subjectName);
                    if (title.length() < 5 || NON_SUBJECT_PATTERNS.stream()
                            .anyMatch(p -> title.toLowerCase().contains(p))) {
                        continue;
                    }

                    // Create course with minimal description
                    String description = generateDescription(href, subjectName);
                    String htmlCode = driver.findElement(By.tagName("body")).getText();
                    if(htmlCode!=null){
                        htmlCode=htmlCode.replaceAll("\\s+"," ").trim().replaceAll(",","");
                    }
                    Random random=new Random();
                    Double tempRatings=(4.5+random.nextDouble()*0.5);
                    String ratings=String.format("%.1f", tempRatings);
                    Course course = new Course();
//                    		course.(title, description, href, "Free",
//                            ratings, htmlCode);

                    // Add if it matches the current phase
                    if (searchPhase == 0 && isProgrammingLanguageCourse(course)) {
                        courses.add(course);
                    } else if (searchPhase == 1 && isCSCourse(course)) {
                        courses.add(course);
                    } else if (searchPhase == 2 && isEntrepreneurshipCourse(course)) {
                        courses.add(course);
                    }
                } catch (Exception e) {
                    // Skip problematic elements
                }
            }

        } catch (Exception e) {
            System.out.println("Error scraping " + url + ": " + e.getMessage());
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ex) {}
            }
        }

        return new ScrapeResult(courses, discoveredUrls);
    }

    /**
     * Extract title with simpler approach
     */
    private static String extractTitle(WebElement element, String href, String subjectName) {
        // Try direct text
        String title = element.getText().trim();

        // If empty, check for any visible text in children
        if (title.isEmpty()) {
            try {
                for (WebElement child : element.findElements(By.xpath(".//*"))) {
                    String text = child.getText().trim();
                    if (!text.isEmpty() && text.length() > 3) {
                        title = text;
                        break;
                    }
                }
            } catch (Exception e) {}
        }

        // If still empty, extract from URL
        if (title.isEmpty()) {
            String[] segments = href.replace(BASE_URL, "").split("/");
            if (segments.length > 0) {
                String lastSegment = segments[segments.length - 1].replace('-', ' ');
                if (!lastSegment.isEmpty()) {
                    title = capitalizeWords(lastSegment);
                }
            }

            // Add subject context based on URL patterns
            if (title.length() < 5) {
                if (href.contains("/java/")) {
                    title = "Java Programming: " + title;
                } else if (href.contains("/python/")) {
                    title = "Python Programming: " + title;
                } else if (href.contains("/javascript/")) {
                    title = "JavaScript Programming: " + title;
                } else if (href.contains("/html/") || href.contains("/html-css/")) {
                    title = "HTML/CSS: " + title;
                } else if (href.contains("/programming/")) {
                    title = "Programming: " + title;
                } else if (href.contains("/entrepreneurship/")) {
                    title = "Entrepreneurship: " + title;
                } else if (href.contains("/business/")) {
                    title = "Business: " + title;
                } else {
                    title = subjectName + ": " + title;
                }
            }
        }

        return title;
    }

    /**
     * Extract subject from URL path (simplified)
     */
    private static String extractSubjectFromUrl(String url) {
        // Check for specific programming languages first
        if (url.contains("/java/")) return "Java Programming";
        if (url.contains("/python/")) return "Python Programming";
        if (url.contains("/javascript/")) return "JavaScript Programming";
        if (url.contains("/html-css/")) return "HTML and CSS";
        if (url.contains("/html/")) return "HTML";
        if (url.contains("/css/")) return "CSS";

        // Check for other subjects
        if (url.contains("/programming/")) return "Programming";
        if (url.contains("/computer-science/")) return "Computer Science";
        if (url.contains("/entrepreneurship/")) return "Entrepreneurship";
        if (url.contains("/business/")) return "Business";
        if (url.contains("/economics/")) return "Economics";
        if (url.contains("/finance/")) return "Finance";

        // Extract from URL path
        String[] segments = url.replace(BASE_URL, "").split("/");
        if (segments.length > 1 && !segments[1].isEmpty()) {
            return capitalizeWords(segments[1].replace('-', ' '));
        }

        return "Khan Academy Course";
    }

    /**
     * Generate description without fetching from page
     */
    private static String generateDescription(String url, String subject) {
        // Create appropriate description based on subject
        subject = subject.toLowerCase();

        // Programming language specific descriptions
        if (url.contains("/java/")) {
            return "Learn Java programming with Khan Academy's interactive lessons and practical coding examples.";
        } else if (url.contains("/python/")) {
            return "Master Python programming fundamentals with Khan Academy's step-by-step tutorials and exercises.";
        } else if (url.contains("/javascript/")) {
            return "Develop JavaScript skills with Khan Academy's interactive coding environment and projects.";
        } else if (url.contains("/html/") || url.contains("/css/") || url.contains("/html-css/")) {
            return "Build web pages with HTML and CSS through Khan Academy's hands-on web development curriculum.";
        } else if (url.contains("/programming/") || url.contains("/computer-science/")) {
            return "Explore programming concepts with Khan Academy's computer science curriculum.";
        } else if (url.contains("/entrepreneurship/")) {
            return "Develop entrepreneurial skills with Khan Academy's business and startup curriculum.";
        } else if (url.contains("/business/") || url.contains("/economics/")) {
            return "Learn business and economic principles with Khan Academy's comprehensive course.";
        } else if (url.contains("/finance/")) {
            return "Master financial concepts essential for entrepreneurs with Khan Academy's structured curriculum.";
        } else {
            return "Learn " + subject + " with Khan Academy's curriculum featuring interactive lessons.";
        }
    }

    /**
     * Helper method to capitalize words
     */
    private static String capitalizeWords(String text) {
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
     * Remove duplicate courses efficiently
     */
    private static List<Course> removeDuplicates(List<Course> courses) {
        Map<String, Course> uniqueMap = new LinkedHashMap<>();
        for (Course course : courses) {
            String key = normalizeUrl(course.getUrl());
            if (!uniqueMap.containsKey(key)) {
                uniqueMap.put(key, course);
            }
        }
        return new ArrayList<>(uniqueMap.values());
    }

    /**
     * Normalize URL (simplified)
     */
    private static String normalizeUrl(String url) {
        if (url == null) return "";
        url = url.trim().toLowerCase();

        // Remove trailing slash, query params, and fragments
        int endPos = url.length();
        if (url.endsWith("/")) endPos--;
        int queryPos = url.indexOf('?');
        if (queryPos > 0) endPos = Math.min(endPos, queryPos);
        int fragmentPos = url.indexOf('#');
        if (fragmentPos > 0) endPos = Math.min(endPos, fragmentPos);

        return url.substring(0, endPos);
    }

    /**
     * Save courses to CSV file with minimal processing
     */
    private static void saveToCsv(List<Course> courses, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write CSV header
            writer.write("title,description,url,price,ratings,platform,html_text\n");

            // Write each course's data
            for (Course course : courses) {
                // Determine subject category
                String category = determineSubjectCategory(course);

                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        escapeCsvField(course.getTitle()),
                        escapeCsvField(course.getDescription()),
                        escapeCsvField(course.getUrl()),
                        escapeCsvField(course.getPrice()),
                        escapeCsvField(String.valueOf(course.getRating())),
                        escapeCsvField("Khan Academy"),
                        escapeCsvField(course.getHtmlCode())));
//                        /escapeCsvField(category)));
            }
        } catch (IOException e) {
            System.out.println("Error saving to CSV: " + e.getMessage());
        }
    }

    /**
     * Determine whether a course belongs to CS or Entrepreneurship category
     */
    private static String determineSubjectCategory(Course course) {
        if (course == null || course.getTitle() == null || course.getUrl() == null) {
            return "Unknown";
        }

        boolean isCS = isCSCourse(course);
        boolean isEntrepreneurship = isEntrepreneurshipCourse(course);

        if (isCS && !isEntrepreneurship) {
            return "Computer Science";
        } else if (isEntrepreneurship && !isCS) {
            return "Entrepreneurship";
        } else if (isCS && isEntrepreneurship) {
            return "Computer Science & Entrepreneurship";
        } else {
            // If unable to determine clearly, use URL analysis
            if (isCSSubjectUrl(course.getUrl())) {
                return "Computer Science";
            } else if (isEntrepreneurshipSubjectUrl(course.getUrl())) {
                return "Entrepreneurship";
            } else {
                return "Other";
            }
        }
    }

    /**
     * Simple CSV field escaping
     */
    private static String escapeCsvField(String field) {
        if (field == null) return "";
        return field.replace("\"", "\"\"").trim();
    }

    // Simplified data classes
    private static class CourseData {
        String title, description, url, price, ratings, html_code;

        CourseData(String title, String description, String url, String price, String ratings, String html_code) {
            this.title = title;
            this.description = description;
            this.url = url;
            this.price = price;
            this.ratings = ratings;
            this.html_code = html_code;
        }
    }

    private static class ScrapingTask {
        String url;
        int depth;

        ScrapingTask(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }

    private static class ScrapeResult {
        List<Course> courses;
        Set<String> discoveredUrls;

        ScrapeResult(List<Course> courses, Set<String> discoveredUrls) {
            this.courses = courses;
            this.discoveredUrls = discoveredUrls;
        }
    }
}