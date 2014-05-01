package com.timluo.friendlist;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ArrayAdapter for {@link Contact} objects.
 */
public class ContactAdapter extends BaseAdapter implements Filterable {
    private int resource;
    private LayoutInflater inflater;
    private List<Contact> allContacts = new ArrayList<Contact>();
    private List<Contact> visibleContacts = new ArrayList<Contact>();
    private Context context;
    private DatabaseHandler databaseHandler;

    private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("0.00");

    public ContactAdapter(Context context, int resource, List<Contact> contacts, DatabaseHandler databaseHandler) {
        this.context = context;
        this.resource = resource;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.allContacts = contacts;
        this.visibleContacts = this.allContacts;
        this.databaseHandler = databaseHandler;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return this.visibleContacts.size();
    }

    @Override
    public Contact getItem(int position) {
        if (this.visibleContacts.size() > position) {
            return this.visibleContacts.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public boolean add(Contact contact) {
        if (this.allContacts.contains(contact)) {
            return false;
        }
        this.allContacts.add(contact);
        this.databaseHandler.addContact(contact);
        notifyDataSetChanged();
        return true;
    }

    /**
     * Persists a contact. Only works if the contact already exists.
     * @param contact   the contact to persist
     * @return          success of the persistence
     */
    public boolean update(Contact contact) {
        if (this.allContacts.contains(contact)) {
            this.databaseHandler.addContact(contact);
            return true;
        }
        return false;
    }

    public void insert(Contact contact, int position) {
        this.allContacts.add(position, contact);
        notifyDataSetChanged();
    }

    public void remove(Contact contact) {
        this.allContacts.remove(contact);
        this.databaseHandler.deleteContact(contact);
        notifyDataSetChanged();
    }

    public Contact contactForUri(Uri uri) {
        for (Contact contact : this.allContacts) {
            if (contact.uri == uri) {
                return contact;
            }
        }
        return null;
    }

    public Contact contactForId(String id) {
        for (Contact contact : this.allContacts) {
            if (contact.getId().equals(id)) {
                return contact;
            }
        }
        return null;
    }

    public List<Contact> getContacts() {
        return new ArrayList<Contact>(this.allContacts);
    }

    static class ViewHolder {
        ImageView profileThumbnail;
        TextView nameText;
        TextView lastContactedText;
        TextView scoreText;
        TextView frequencyText;
        int position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder holder;
        if (convertView == null) {
            view = this.inflater.inflate(this.resource, parent, false);
            holder = new ViewHolder();
            holder.profileThumbnail = (ImageView) view.findViewById(R.id.profileThumbnail);
            holder.nameText = (TextView) view.findViewById(R.id.contactName);
            holder.lastContactedText = (TextView) view.findViewById(R.id.lastContacted);
            holder.scoreText = (TextView) view.findViewById(R.id.contactScore);
            holder.frequencyText = (TextView) view.findViewById(R.id.contactFrequency);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
        holder.position = position;

        final Contact contact = getItem(position);
        if (contact == null) {
            return view;
        }

        new AsyncTask<ViewHolder, Void, Bitmap>() {
            private ViewHolder v;

            @Override
            protected Bitmap doInBackground(ViewHolder... params) {
                v = params[0];
                return contact.getPhotoThumbnail(context.getContentResolver());
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                super.onPostExecute(result);
                if (v.position == position) {
                    v.profileThumbnail.setImageBitmap(result);
                }
            }
        }.execute(holder);

        holder.nameText.setText(contact.getDisplayName());
        holder.lastContactedText.setText("Last Contacted: " + contact.getLastContacted().toString());
        holder.scoreText.setText("Score: " + SCORE_FORMAT.format(contact.getScore()));
        holder.frequencyText.setText("Contact every " + contact.getDaysToContact().getDays() + " days");

        return view;
    }


    private static final Comparator<Contact> BY_SCORE = new Comparator<Contact>() {
        @Override
        public int compare(Contact contact, Contact contact2) {
            // Return descending; larger values have more priority
            return -1 * contact.getScore().compareTo(contact2.getScore());
        }
    };

    @Override
    public void notifyDataSetChanged() {
        Collections.sort(this.allContacts, BY_SCORE);
        Collections.sort(this.visibleContacts, BY_SCORE);

        super.notifyDataSetChanged();
    }

    public List<Contact> getFilteredResults(CharSequence constraint) {
        if (constraint == null || (constraint != null && constraint.equals(""))) {
            return this.allContacts;
        }

        List<Contact> results = new ArrayList<Contact>();
        for (Contact contact : this.allContacts) {
            if (contact.displayName.toLowerCase().contains(constraint.toString().toLowerCase())) {
                results.add(contact);
            }
        }
        return results;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<Contact> filteredResults = getFilteredResults(constraint);

                FilterResults results = new FilterResults();
                results.values = filteredResults;

                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                @SuppressWarnings("unchecked")
                List<Contact> contacts = (List<Contact>) filterResults.values;
                visibleContacts = contacts;
                notifyDataSetChanged();
            }
        };
    }

    public String getContactIdByNumber(String number) {
        //TODO: does this sanitize the numbers (get rid of +'s, ()'s, etc.
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String contactId = "?";

        ContentResolver contentResolver = context.getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return contactId;
    }

    /** Probably for debug only */
    public String getDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = "?";

        ContentResolver contentResolver = context.getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }
}
