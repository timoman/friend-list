package com.timluo.friendlist.model;

import java.util.Map;

/**
 * Represents a phone number and its type (home, work, mobile, etc).
 */
public class PhoneNumber {
    String phoneNumber;
    int type;

    public PhoneNumber(String fromString) {
        String parts[] = fromString.split(",");
        this.phoneNumber = parts[0];
        this.type = Integer.valueOf(parts[1]);
    }

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

    @Override
    public String toString() {
        return this.phoneNumber + "," + this.getType();
    }
}
