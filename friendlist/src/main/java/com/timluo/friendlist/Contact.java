package com.timluo.friendlist;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;

import com.timluo.friendlist.model.PhoneNumber;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static android.provider.ContactsContract.CommonDataKinds.Phone;
import static android.provider.ContactsContract.Contacts;

/**
 * Represents a contact and his/her information
 */
public class Contact {
    Uri uri;
    Days daysToContact;
    LocalDate lastContacted;
    int score;

    long lastUpdated;

    String displayName;
    List<PhoneNumber> phoneNumbers;

    public Contact(Uri uri) {
        this.uri = uri;
    }

    public Contact(Uri uri, ContentResolver contentResolver) {
        this.uri = uri;
        refresh(contentResolver);
        //TODO:
        this.daysToContact = Days.THREE;
        this.lastContacted = LocalDate.now();new DateTime().withYear(1970).withMonthOfYear(1).withDayOfMonth(1).withTimeAtStartOfDay();
        this.score = 0;
    }

    public Contact(Contact toCopy) {
        this.uri = toCopy.uri;
    }

    public void refresh(ContentResolver contentResolver) {
        if (this.lastUpdated < getLastUpdated(contentResolver)) {
            this.displayName = refreshDisplayName(contentResolver);
            this.phoneNumbers = refreshPhoneNumbers(contentResolver);
            this.lastUpdated = System.currentTimeMillis();
        }
        this.lastUpdated = System.currentTimeMillis();
    }

    public void setDaysToContact(Days daysToContact) {
        this.daysToContact = daysToContact;
    }

    public void setLastContacted(LocalDate lastContacted) {
        this.lastContacted = lastContacted;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public Uri getUri() {
        return this.uri;
    }

    public Days getDaysToContact() {
        return this.daysToContact;
    }

    public LocalDate getLastContacted() {
        return lastContacted;
    }

    public int getScore() {
        return score;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return this.phoneNumbers;
    }

    private List<PhoneNumber> refreshPhoneNumbers(ContentResolver contentResolver) {
        String contactId = this.uri.getLastPathSegment();
        Cursor phones = contentResolver.query(Phone.CONTENT_URI, null,
                Phone.CONTACT_ID + " = " + contactId, null, null);

        List<PhoneNumber> foundNumbers = new ArrayList<PhoneNumber>();
        try {
            while (phones.moveToNext()) {
                String number = phones.getString(phones.getColumnIndex(Phone.NUMBER));
                int type = phones.getInt(phones.getColumnIndex(Phone.TYPE));
                PhoneNumber phoneNumber = new PhoneNumber(number, type);
                foundNumbers.add(phoneNumber);
            }
        }
        finally {
            phones.close();
        }
        return foundNumbers;
    }

    private Long getLastUpdated(ContentResolver contentResolver) {
        String contactId = this.uri.getLastPathSegment();
        Cursor cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, null,
                Phone.CONTACT_ID + " = " + contactId, null, null);
        String lastUpdated = null;
        try {
            while (cursor.moveToNext()) {
                lastUpdated =
                        cursor.getString(cursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.StructuredName.CONTACT_LAST_UPDATED_TIMESTAMP));
            }
        }
        finally {
            cursor.close();
        }
        return Long.valueOf(lastUpdated);
    }

    private String refreshDisplayName(ContentResolver contentResolver) {
        String contactId = this.uri.getLastPathSegment();

        /* Set names */
        String whereName = ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID + " = ?";
        String[] whereNameParams = new String[] { ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, contactId };
        Cursor nameCur = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, whereName, whereNameParams, null);
        String displayName = null;
        try {
            while (nameCur.moveToNext()) {
                displayName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
            }
        }
        finally {
            nameCur.close();
        }
        return displayName;
    }

    public Bitmap getPhotoThumbnail(ContentResolver contentResolver) {
        Uri photoUri = Uri.withAppendedPath(this.uri, Contacts.Photo.DISPLAY_PHOTO);
        try {
            AssetFileDescriptor fd =
                    contentResolver.openAssetFileDescriptor(photoUri, "r");
            return BitmapFactory.decodeStream(fd.createInputStream());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Contact contact = (Contact) o;

        if (!this.uri.equals(contact.uri)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return this.uri.hashCode();
    }
}
