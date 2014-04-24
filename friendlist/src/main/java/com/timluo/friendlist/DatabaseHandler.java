package com.timluo.friendlist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite database for {@link Contact}s
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    // Database Name
    private static final String DATABASE_NAME = "contactsManager";

    // Contacts table name
    private static final String TABLE_CONTACTS = "contacts";

    // Contacts Table Columns names
    private static final String ID = "id";
    private static final String URI = "uri";
    private static final String DAYS_TO_CONTACT = "contact_frequency_target_days";
    private static final String LAST_CONTACTED = "last_contacted";

    private Context context;


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "("
                + URI + " TEXT PRIMARY KEY, "
                + DAYS_TO_CONTACT + " INT, "
                + LAST_CONTACTED + " TEXT)";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);

        // Create tables again
        onCreate(db);
    }

    // Adding new contact
    public void addContact(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(URI, contact.getUri().toString());
            values.put(DAYS_TO_CONTACT, contact.getDaysToContact().getDays());
            values.put(LAST_CONTACTED, contact.getLastContacted().toString());

            // Replace/update existing record
            db.insertWithOnConflict(TABLE_CONTACTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
        finally {
            db.close(); // Closing database connection
        }
    }

    // Getting single contact
    public Contact getContact(Uri uri) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CONTACTS, null, URI + "=?",
                new String[] { uri.toString() }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Contact contact = createNewContact(cursor);
        // return contact
        return contact;
    }

    private Contact createNewContact(Cursor cursor) {
        Contact contact = new Contact(Uri.parse(cursor.getString(cursor.getColumnIndex(URI))));
        contact.setDaysToContact(Days.days(cursor.getInt(cursor.getColumnIndex(DAYS_TO_CONTACT))));
        contact.setLastContacted(LocalDate.parse(cursor.getString(cursor.getColumnIndex(LAST_CONTACTED))));
        contact.refresh(this.context.getContentResolver());
        return contact;
    }

    // Getting All Contacts
    public List<Contact> getAllContacts() {
        List<Contact> contactList = new ArrayList<Contact>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_CONTACTS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Contact contact = createNewContact(cursor);
                // Adding contact to list
                contactList.add(contact);
            } while (cursor.moveToNext());
        }

        // return contact list
        return contactList;
    }

    // Getting contacts Count
    public int getContactsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_CONTACTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        // return count
        return cursor.getCount();
    }

    // Updating single contact
    public int updateContact(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(URI, contact.getUri().toString());
        values.put(DAYS_TO_CONTACT, contact.getDaysToContact().getDays());
        values.put(LAST_CONTACTED, contact.getLastContacted().toString());

        // updating row
        return db.update(TABLE_CONTACTS, values, URI + " = ?",
                new String[] { contact.getUri().toString() });
    }

    // Deleting single contact
    public void deleteContact(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CONTACTS, URI + " = ?",
                new String[] { contact.getUri().toString() });
        db.close();
    }
}
