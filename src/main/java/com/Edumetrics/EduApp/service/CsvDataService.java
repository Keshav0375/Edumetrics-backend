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
public class CsvDataService {

    private static final String CSV_FILE_PATH = "courses.csv";
    private static final String[] HEADERS = {
            "Title", "Description", "URL", "Price", "Rating", "Platform",
            "CourseId", "Instructors", "Duration", "Level", "Language", "ExtractedText"
    };

    public void saveCourse(Course course) {
        try {
            Path path = Paths.get(CSV_FILE_PATH);
            boolean fileExists = Files.exists(path);

            FileWriter out = new FileWriter(CSV_FILE_PATH, true);

            try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
                    .withHeader(fileExists ? null : HEADERS))) {

                printer.printRecord(
                        formattingForCSV(course.getTitle()).toString(),
                        formattingForCSV(course.getDescription()).toString(),
                                formattingForCSV(course.getUrl()).toString(),
                                        formattingForCSV(course.getPrice()).toString(),
                                                formattingForCSV(String.valueOf(course.getRating())).toString(),
                                                        formattingForCSV(course.getPlatform()).toString(),
                                                                formattingForCSV(course.getExtractedText()).toString()
                );

                System.out.println("Course saved to CSV: " + course.getTitle());
            }
        } catch (IOException e) {
            System.err.println("Error saving course to CSV: " + e.getMessage());
        }
    }

    public void saveCourses(List<Course> courses) {
        for (Course course : courses) {
            saveCourse(course);
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

                try {
                    course.setRating(Double.parseDouble(record.get("Rating")));
                } catch (Exception e) {
                    course.setRating(5.0);
                }
                try {
                    course.setExtractedText(record.get("ExtractedText"));
                } catch (Exception e) {
                    course.setExtractedText("");
                }

                courses.add(course);
            }

        } catch (IOException e) {
            System.err.println("Error reading courses from CSV: " + e.getMessage());
        }

        return courses;
    }

    public List<Course> getCoursesByPlatform(String platform) {
        List<Course> allCourses = getAllCourses();
        List<Course> filteredCourses = new ArrayList<>();

        for (Course course : allCourses) {
            if (platform.equalsIgnoreCase(course.getPlatform())) {
                filteredCourses.add(course);
            }
        }

        return filteredCourses;
    }

    private CharSequence formattingForCSV(String value) {
        // TODO Auto-generated method stub
        try {
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                value = value.replace("\"", "\"\"");
                value=value.replaceAll(",", "");
                return "\"" + value + "\"";
            }else {
                return value;
            }
        }catch(Exception e) {
            return "\"" + " " + "\"";
}
    }

}