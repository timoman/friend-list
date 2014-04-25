package com.timluo.friendlist;

import android.content.Context;
import android.net.Uri;
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

    public List<Contact> getContacts() {
        return new ArrayList<Contact>(allContacts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = this.inflater.inflate(this.resource, parent, false);
        } else {
            view = convertView;
        }

        Contact contact = getItem(position);
        if (contact == null) {
            return view;
        }

        ImageView profileThumbnail = (ImageView) view.findViewById(R.id.profileThumbnail);
        profileThumbnail.setImageBitmap(contact.getPhotoThumbnail(this.context.getContentResolver()));

        TextView nameText = (TextView) view.findViewById(R.id.contactName);
        nameText.setText(contact.getDisplayName());

        TextView lastContactedText = (TextView) view.findViewById(R.id.lastContacted);
        lastContactedText.setText("Last Contacted: " + contact.getLastContacted().toString());

        TextView scoreText = (TextView) view.findViewById(R.id.contactScore);
        scoreText.setText("Score: " + SCORE_FORMAT.format(contact.getScore()));

        TextView frequencyText = (TextView) view.findViewById(R.id.contactFrequency);
        frequencyText.setText("Contact every " + contact.getDaysToContact().getDays() + " days");

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
}
