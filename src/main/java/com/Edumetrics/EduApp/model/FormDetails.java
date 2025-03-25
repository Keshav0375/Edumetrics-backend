package com.Edumetrics.EduApp.model;

public class FormDetails {
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public String getEmailAddress() {
        return emailAddress;
    }
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    String name;
    String phoneNumber;
    String emailAddress;
    String text;

    boolean isNameCorrect;
    public boolean isNameCorrect() {
        return isNameCorrect;
    }
    public void setNameCorrect(boolean isNameCorrect) {
        this.isNameCorrect = isNameCorrect;
    }
    public boolean isPhoneNumberCorrect() {
        return isPhoneNumberCorrect;
    }
    public void setPhoneNumberCorrect(boolean isPhoneNumberCorrect) {
        this.isPhoneNumberCorrect = isPhoneNumberCorrect;
    }
    public boolean isEmailAddressCorrect() {
        return isEmailAddressCorrect;
    }
    public void setEmailAddressCorrect(boolean isEmailAddressCorrect) {
        this.isEmailAddressCorrect = isEmailAddressCorrect;
    }
    boolean isPhoneNumberCorrect;
    boolean isEmailAddressCorrect;
    boolean isTextCorrect;
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public boolean isTextCorrect() {
        return isTextCorrect;
    }
    public void setTextCorrect(boolean isTextCorrect) {
        this.isTextCorrect = isTextCorrect;
    }
}
