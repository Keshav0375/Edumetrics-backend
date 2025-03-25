package com.Edumetrics.EduApp.controller;

import com.Edumetrics.EduApp.model.*;
import com.Edumetrics.EduApp.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Edumetrics.EduApp.service.WordCompletionService;
import com.Edumetrics.EduApp.service.SpellCheckerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final ScraperManagerService scraperManager;
    private final CsvDataService csvDataService;

    @Autowired
    public CourseController(
            ScraperManagerService scraperManager,
            CsvDataService csvDataService) {
        this.scraperManager = scraperManager;
        this.csvDataService = csvDataService;
    }

    @GetMapping("/platforms")
    public ResponseEntity<List<String>> getSupportedPlatforms() {
        List<String> platforms = scraperManager.getSupportedPlatforms();
        return ResponseEntity.ok(platforms);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Course>> getAllCourses() {
        List<Course> courses = csvDataService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/platform/{platformName}")
    public ResponseEntity<List<Course>> getCoursesByPlatform(@PathVariable String platformName) {
        List<Course> courses = csvDataService.getCoursesByPlatform(platformName);
        return ResponseEntity.ok(courses);
    }

    @PostMapping("/scrape/{platformName}")
    public ResponseEntity<ScrapeResponse> scrapePlatform(
            @PathVariable String platformName,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "5") int limit,
            @RequestParam(required = false, defaultValue = "true") boolean saveToCSV) {

        long startTime = System.currentTimeMillis();
        ScrapeResponse response = new ScrapeResponse();

        try {
            List<Course> courses = scraperManager.scrapePlatform(platformName, query, limit, saveToCSV);

            response.setSuccess(true);
            response.setMessage("Successfully scraped courses from " + platformName);
            response.setCourses(courses);
            response.setTotalCourses(courses.size());

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error scraping courses: " + e.getMessage());
        }

        response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/scrape-edumetrics")
    public ResponseEntity<ScrapeResponse> scrapeAllPlatforms(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "5") int limit,
            @RequestParam(required = false, defaultValue = "true") boolean saveToCSV) {

        long startTime = System.currentTimeMillis();
        ScrapeResponse response = new ScrapeResponse();

        try {
            Map<String, List<Course>> coursesByPlatform = scraperManager.scrapeAllPlatforms(query, limit, saveToCSV);

            // Count total courses
            long totalCourses = coursesByPlatform.values().stream()
                    .mapToLong(List::size)
                    .sum();

            response.setSuccess(true);
            response.setMessage("Successfully scraped courses from all platforms");
            response.setCoursesByPlatform(coursesByPlatform);
            response.setTotalCourses(totalCourses);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error scraping courses: " + e.getMessage());
        }

        response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getPageRank")
    @ResponseBody
    public static Response<URLFrequencyKeywordNode> getPageRanking(@RequestParam("searchWord") String searchWord) {
        Response<URLFrequencyKeywordNode> response=new Response<URLFrequencyKeywordNode>();
        ArrayList<URLFrequencyKeywordNode> finalList=new ArrayList<URLFrequencyKeywordNode>();
        try {
            if(!searchWord.matches("[a-zA-Z]+")) {
                response.setStatusCode(-1);
                response.setMessage("Word received is not valid ");
                response.setData(finalList);
                return response;
            }

            System.out.println("Word to be Searched::"+searchWord);
            PageRanking pageRankinginstance=new PageRanking();
            finalList=pageRankinginstance.rankPage(searchWord);
            if(finalList==null || finalList.size()==0) {
                finalList=new ArrayList<URLFrequencyKeywordNode>();
                response.setMessage("No Data Found");
            }else {
                response.setMessage("Successfully Found Data");
            }
        }catch(Exception e) {
            System.out.println("Exception in Controller getPageRanking() as "+ e);
            response.setStatusCode(-1);
            response.setMessage("Exception arised as "+e);
        }

        response.setData(finalList);

        return response;
    }

    @GetMapping("/getInvertedIndex")
    @ResponseBody
    public static Response<WordPositionFrequencyStorage> getInvertedIndex(@RequestParam("searchWord") String searchWord) {
        Response<WordPositionFrequencyStorage> response=new Response<WordPositionFrequencyStorage>();
        ArrayList<WordPositionFrequencyStorage> finalList=new ArrayList<WordPositionFrequencyStorage>();
        try {
            if(!searchWord.matches("[a-zA-Z]+")) {
                response.setStatusCode(-1);
                response.setMessage("Word received is not valid ");
                response.setData(finalList);
                return response;
            }

            System.out.println("Word to be Searched::"+searchWord);
            InvertedIndexing inv=new InvertedIndexing();
            finalList=inv.getInvertedIndexInformation(searchWord);
            if(finalList==null || finalList.size()==0) {
                finalList=new ArrayList<WordPositionFrequencyStorage>();
                response.setMessage("No Data Found");
            }else {
                response.setMessage("Successfully Found Data");
            }
        }catch(Exception e) {
            System.out.println("Exception in Controller getPageRanking() as "+ e);
            response.setStatusCode(-1);
            response.setMessage("Exception arised as "+e);
        }
        response.setData(finalList);
        return response;
    }

    @GetMapping("/getFrequencyCount")
    @ResponseBody
    public static Response getFrequency(@RequestParam("keyword") String searchWord) {
        Response<WordFrequencyData> response=new Response();
        WordFrequencyData frequencyCount;
        try {
            if(!searchWord.matches("[a-zA-Z]+")) {
                response.setStatusCode(-1);
                response.setMessage("Word received is not valid ");
                response.setData(new ArrayList<WordFrequencyData>());
                return response;
            }

            System.out.println("Word to be Searched::"+searchWord);
            WordFrequencyService frequency=new WordFrequencyService();
            frequencyCount=frequency.countWordOccurrences(searchWord);
            ArrayList<WordFrequencyData> answer=new ArrayList<WordFrequencyData>();
            answer.add(frequencyCount);
            response.setData(answer);
            response.setMessage("Successfully Found Data");


        }catch(Exception e) {
            System.out.println("Exception in Controller getFrequency() as "+ e);
            response.setStatusCode(-1);
            response.setMessage("Exception arised as "+e);
            response.setData(new ArrayList<WordFrequencyData>());
        }

        return response;
    }

    @GetMapping("/verifyDetails")
    @ResponseBody
    public static Response<FormDetails> verifyDetails(@RequestBody FormDetails  formDetails) {
        Response<FormDetails> response=new Response();
        FormDetails validator;
        try {
            FeedbackFormValidator valid=new FeedbackFormValidator();
            validator=valid.validateFeedbackForm(formDetails.getName(), formDetails.getEmailAddress(), formDetails.getText(), formDetails.getPhoneNumber());
            ArrayList<FormDetails> answer=new ArrayList<FormDetails>();
            answer.add(validator);
            response.setData(answer);
            response.setMessage("Successfully Found Data");


        }catch(Exception e) {
            System.out.println("Exception in Controller verifyDetails() as "+ e);
            response.setStatusCode(-1);
            response.setMessage("Exception arised as "+e);
            response.setData(new ArrayList<FormDetails>());
        }

        return response;
    }
}