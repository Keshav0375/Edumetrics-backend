package com.Edumetrics.EduApp.service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.Edumetrics.EduApp.model.FormDetails;

@Service
public class FeedbackFormValidator {
    // Regex for name: 2-50 characters, allows letters, spaces, and hyphens
    private static final String NAME_REGEX = "^[A-Za-z\\s-]{2,50}$";

    // Regex for email: standard email format validation
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$";

    // Regex for text: 10-500 characters, allows letters, numbers, punctuation, and spaces
    private static final String TEXT_REGEX = "^[\\p{L}\\p{N}\\p{P}\\s]{10,500}$";

    // Regex for Canadian phone number:
    // Formats: (###) ###-####, ###-###-####, ###.###.####, ### ### ####
    private static final String PHONE_REGEX = "^(\\(\\d{3}\\)\\s?|\\d{3}[-.]?)\\s?\\d{3}[-.]?\\d{4}$";

    // Method to validate entire form
    public static FormDetails validateFeedbackForm(String name, String email, String text, String phoneNumber) {
        FormDetails answer=new FormDetails();
        answer.setEmailAddressCorrect(validateEmail(email));
        answer.setNameCorrect(validateName(name));
        answer.setPhoneNumberCorrect(validateCanadianPhoneNumber(phoneNumber));
        answer.setTextCorrect(validateText(text));
        answer.setEmailAddress(email);
        answer.setName(name);
        answer.setPhoneNumber(phoneNumber);
        answer.setText(text);
        return  answer;
    }

    // Individual validation methods
    public static boolean validateName(String name) {
        return name != null && Pattern.matches(NAME_REGEX, name);
    }

    public static boolean validateEmail(String email) {
        return email != null && Pattern.matches(EMAIL_REGEX, email);
    }

    public static boolean validateText(String text) {
        return text != null && Pattern.matches(TEXT_REGEX, text);
    }

    public static boolean validateCanadianPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;

        // Remove all non-digit characters for clean validation
        String cleanedNumber = phoneNumber.replaceAll("[^0-9]", "");

        // Check if number has exactly 10 digits
        return cleanedNumber.length() == 10 &&
                Pattern.matches(PHONE_REGEX, phoneNumber);
    }
}
