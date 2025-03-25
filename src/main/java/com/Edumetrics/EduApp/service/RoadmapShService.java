package com.Edumetrics.EduApp.service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class RoadmapShService {

    public static void main(String[] args) {

        final String chromeDriverPropertyKey = "webdriver.chrome.driver";
        final String chromeDriverFilePath = "C:\\Users\\HP\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe";
        System.setProperty(chromeDriverPropertyKey, chromeDriverFilePath);

        WebDriver webBrowserController = new ChromeDriver();
        final String outputDataFileName = "combined_website_data.csv"; // Changed to .csv

        try (BufferedWriter dataOutputWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputDataFileName), "UTF-8"))) {

            // Write CSV Header
            dataOutputWriter.write("Website,Section,Type,Data\n"); // Added CSV Header


            // --- EXTRACTOR 2 ---
            // Define the target URL for scraping
            final String targetWebURL2 = "https://roadmap.sh/teams";
            webBrowserController.get(targetWebURL2);

            // Capture all heading elements (h1 to h6) and record their text content
            List<WebElement> headingElementsList2 = webBrowserController.findElements(By.xpath("//h1 | //h2 | //h3 | //h4 | //h5 | //h6"));
            for (WebElement headingElement : headingElementsList2) {
                dataOutputWriter.write("roadmap.sh/teams,Headings,Text,\"" + escapeCsv(headingElement.getText()) + "\"\n");
            }

            // Retrieve all paragraph elements and extract their text
            List<WebElement> paragraphElementsList2 = webBrowserController.findElements(By.tagName("p"));
            for (WebElement paragraphElement : paragraphElementsList2) {
                dataOutputWriter.write("roadmap.sh/teams,Paragraphs,Text,\"" + escapeCsv(paragraphElement.getText()) + "\"\n");
            }

            // Obtain all image elements and save their source (src) attributes
            List<WebElement> imageElementsList2 = webBrowserController.findElements(By.tagName("img"));
            for (WebElement imageElement : imageElementsList2) {
                dataOutputWriter.write("roadmap.sh/teams,Images,URL,\"" + escapeCsv(imageElement.getAttribute("src")) + "\"\n");
            }

            // Extract hyperlink (anchor) elements and store their 'href' attribute values
            List<WebElement> hyperlinkElementsList2 = webBrowserController.findElements(By.tagName("a"));
            for (WebElement hyperlinkElement : hyperlinkElementsList2) {
                dataOutputWriter.write("roadmap.sh/teams,Hyperlinks,URL,\"" + escapeCsv(hyperlinkElement.getAttribute("href")) + "\"\n");
            }

            // Extract the text content from all 'div' elements
            List<WebElement> divElementsList2 = webBrowserController.findElements(By.tagName("div"));
            for (WebElement divElement : divElementsList2) {
                dataOutputWriter.write("roadmap.sh/teams,Divs,Text,\"" + escapeCsv(divElement.getText()) + "\"\n");
            }

            // --- EXTRACTOR 3 ---
            final String targetURLAddress3 = "https://roadmap.sh/get-started";
            webBrowserController.get(targetURLAddress3);

            // Retrieve all heading elements and extract their text
            List<WebElement> headingElements3 = webBrowserController.findElements(By.xpath("//h1 | //h2 | //h3"));
            for (WebElement heading : headingElements3) {
                dataOutputWriter.write("roadmap.sh/get-started,Headings,Text,\"" + escapeCsv(heading.getText()) + "\"\n");
            }

            // Retrieve all URL links from anchor tags, filtering out null/empty values
            List<WebElement> urlLinkElements3 = webBrowserController.findElements(By.tagName("a"));
            for (WebElement link : urlLinkElements3) {
                String urlHref = link.getAttribute("href");
                if (urlHref != null && !urlHref.isEmpty()) {
                    dataOutputWriter.write("roadmap.sh/get-started,URLs,URL,\"" + escapeCsv(urlHref) + "\"\n");
                }
            }

            // Retrieve all image sources from image tags, filtering out null/empty values
            List<WebElement> imageElements3 = webBrowserController.findElements(By.tagName("img"));
            for (WebElement image : imageElements3) {
                String imageUrlSource = image.getAttribute("src");
                if (imageUrlSource != null && !imageUrlSource.isEmpty()) {
                    dataOutputWriter.write("roadmap.sh/get-started,Images,URL,\"" + escapeCsv(imageUrlSource) + "\"\n");
                }
            }

            // Retrieve all paragraph text from paragraph tags
            List<WebElement> paragraphElements3 = webBrowserController.findElements(By.tagName("p"));
            for (WebElement paragraph : paragraphElements3) {
                dataOutputWriter.write("roadmap.sh/get-started,Paragraphs,Text,\"" + escapeCsv(paragraph.getText()) + "\"\n");
            }

            // --- EXTRACTOR 1 ---
            final String targetWebpageAddress1 = "https://roadmap.sh/frontend";
            webBrowserController.get(targetWebpageAddress1);

            // Capture all heading elements (h1 to h6)
            List<WebElement> pageHeadings1 = webBrowserController.findElements(By.xpath("//h1 | //h2 | //h3 | //h4 | //h5 | //h6"));
            for (WebElement headingElement : pageHeadings1) {
                dataOutputWriter.write("roadmap.sh/frontend,Headings,Text,\"" + escapeCsv(headingElement.getText()) + "\"\n");
            }

            // Retrieve all image elements and extract their source URLs
            List<WebElement> pageImages1 = webBrowserController.findElements(By.tagName("img"));
            for (WebElement imageElement : pageImages1) {
                String imageSource = imageElement.getAttribute("src");
                dataOutputWriter.write("roadmap.sh/frontend,Images,URL,\"" + escapeCsv(imageSource) + "\"\n");
            }

            // Gather all hyperlink elements and obtain their 'href' attributes
            List<WebElement> webpageLinks1 = webBrowserController.findElements(By.tagName("a"));
            for (WebElement linkElement : webpageLinks1) {
                String linkTarget = linkElement.getAttribute("href");
                dataOutputWriter.write("roadmap.sh/frontend,Hyperlinks,URL,\"" + escapeCsv(linkTarget) + "\"\n");
            }

            // Extract all text from the body element, representing the main page content
            WebElement documentBody1 = webBrowserController.findElement(By.tagName("body"));
            dataOutputWriter.write("roadmap.sh/frontend,Body,Text,\"" + escapeCsv(documentBody1.getText()) + "\"\n");


            System.out.println("Successfully extracted data and saved to: " + outputDataFileName);

        } catch (IOException ioException) {
            System.err.println("An error occurred while handling file input/output operations.");
            ioException.printStackTrace();
        } finally {
            if (webBrowserController != null) {
                webBrowserController.quit();
            }
        }
    }

    // Helper function to escape commas and quotes in CSV data
    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        value = value.replace("\"", "\"\""); // Escape double quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = "\"" + value + "\""; // Enclose in double quotes if it contains commas, quotes, or newlines
        }
        return value;
    }
}