package com.Edumetrics.EduApp.service;

import com.Edumetrics.EduApp.model.Course;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvService {

    private static final String CSV_FILE_PATH = "courses.csv";
    private static final String[] HEADERS = {"Title", "Description", "URL", "Price", "ExtractedText"};

    public void saveCourse(Course course) {
        try {
            Path path = Paths.get(CSV_FILE_PATH);
            boolean fileExists = Files.exists(path);

            FileWriter out = new FileWriter(CSV_FILE_PATH, true);

            try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
                    .withHeader(fileExists ? null : HEADERS))) {

                printer.printRecord(
                        course.getTitle(),
                        course.getDescription(),
                        course.getUrl(),
                        course.getPrice(),
                        course.getRating(),
                        course.getExtractedText()
                );

                System.out.println("Course saved to CSV: " + course.getTitle());
            }
        } catch (IOException e) {
            System.err.println("Error saving course to CSV: " + e.getMessage());
        }
    }

    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        Path path = Paths.get(CSV_FILE_PATH);

        if (!Files.exists(path)) {
            return courses;
        }

        try (Reader reader = new FileReader(CSV_FILE_PATH);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                Course course = new Course();
                course.setTitle(record.get("Title"));
                course.setDescription(record.get("Description"));
                course.setUrl(record.get("URL"));
                course.setPrice(record.get("Price"));
                course.setExtractedText(record.get("ExtractedText"));
                try {
                    course.setRating(Double.parseDouble(record.get("Rating")));
                } catch (Exception e) {
                    course.setRating(5.0);
                }

                courses.add(course);
            }

        } catch (IOException e) {
            System.err.println("Error reading courses from CSV: " + e.getMessage());
        }

        return courses;
    }

    // We don't save the full HTML to CSV as it would be too large
    // Instead, we save the extracted text which contains the important content
}