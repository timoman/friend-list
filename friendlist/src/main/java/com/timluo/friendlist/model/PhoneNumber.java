package com.timluo.friendlist.model;

/**
 * Represents a phone number and its type (home, work, mobile, etc).
 */
public class PhoneNumber {
    String phoneNumber;
    int type;

    public PhoneNumber(String phoneNumber, int type) {
        this.phoneNumber = phoneNumber;
        this.type = type;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getType() {
        return type;
    }
}
