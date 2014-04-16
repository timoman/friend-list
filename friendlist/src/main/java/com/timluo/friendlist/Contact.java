package com.timluo.friendlist;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.timluo.friendlist.model.PhoneNumber;

import java.util.ArrayList;
import java.util.List;

import static android.provider.ContactsContract.CommonDataKinds.Phone;

/**
 * Represents a contact and his/her information
 */
public class Contact {
    Uri uri;
    int daysToContact;
    int score;

    long lastUpdated = 0L;

    ContentResolver contentResolver;

    public Contact(ContentResolver contentResolver, Uri uri) {
        this.uri = uri;
        this.contentResolver = contentResolver;
        this.lastUpdated = System.currentTimeMillis();
    }

    public Contact(Contact toCopy) {
        this.uri = toCopy.uri;
        this.contentResolver = toCopy.contentResolver;
    }

    public List<PhoneNumber> getPhoneNumbers() {
        String contactId = this.uri.getLastPathSegment();
        Cursor phones = this.contentResolver.query(Phone.CONTENT_URI, null,
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

    public Long getLastUpdated() {
        String contactId = this.uri.getLastPathSegment();
        Cursor cursor = this.contentResolver.query(ContactsContract.Data.CONTENT_URI, null,
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

    public String getDisplayName() {
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
