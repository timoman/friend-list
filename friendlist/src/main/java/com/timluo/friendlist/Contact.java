package com.timluo.friendlist;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import static android.provider.ContactsContract.CommonDataKinds.Phone;

/**
 * Represents a contact and his/her information
 */
public class Contact {
    Uri uri;
    String givenName;
    String familyName;
    String displayName;
    String phoneNumber;

    public Contact(Contact toCopy) {
        this.uri = toCopy.uri;
        this.givenName = toCopy.givenName;
        this.familyName = toCopy.familyName;
        this.displayName = toCopy.displayName;
        this.phoneNumber = toCopy.phoneNumber;
    }

    public Contact(ContentResolver contentResolver, Uri uri) {
        this.uri = uri;
        String contactId = this.uri.getLastPathSegment();

        /* Set names */
        String whereName = ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID + " = ?";
        String[] whereNameParams = new String[] { ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, contactId };
        Cursor nameCur = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, whereName, whereNameParams, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
        while (nameCur.moveToNext()) {
            this.givenName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
            this.familyName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
            this.displayName = nameCur.getString(nameCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
        }
        nameCur.close();


        /* Set phone numbers */
        Cursor phones = contentResolver.query(Phone.CONTENT_URI, null,
                Phone.CONTACT_ID + " = " + contactId, null, null);
        try {
            while (phones.moveToNext()) {
                String number = phones.getString(phones.getColumnIndex(Phone.NUMBER));
                int type = phones.getInt(phones.getColumnIndex(Phone.TYPE));
                switch (type) {
                    case Phone.TYPE_HOME:
                        // do something with the Home number here...
                        break;
                    case Phone.TYPE_MOBILE:
                        // do something with the Mobile number here...
                        this.phoneNumber = number;
                        break;
                    case Phone.TYPE_WORK:
                        // do something with the Work number here...
                        break;
                }
            }
        }
        finally {
            phones.close();
        }
    }
}
