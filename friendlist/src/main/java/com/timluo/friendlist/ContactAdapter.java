package com.timluo.friendlist;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.timluo.friendlist.model.PhoneNumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ArrayAdapter for {@link Contact} objects.
 */
public class ContactAdapter extends BaseAdapter {
    private int resource;
    private LayoutInflater inflater;
    private List<Contact> contacts = new ArrayList<Contact>();
    private Context context;

    public ContactAdapter(Context context, int resource) {
        this.resource = resource;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public ContactAdapter(Context context, int resource, List<Contact> contacts) {
        this.context = context;
        this.resource = resource;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return this.contacts.size();
    }

    @Override
    public Contact getItem(int position) {
        return this.contacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public boolean add(Contact contact) {
        if (this.contacts.contains(contact)) {
            return false;
        }
        this.contacts.add(contact);
        notifyDataSetChanged();
        return true;
    }

    public void insert(Contact contact, int position) {
        this.contacts.add(position, contact);
        notifyDataSetChanged();
    }

    public void remove(Contact contact) {
        this.contacts.remove(contact);
        notifyDataSetChanged();
    }

    public Contact contactForUri(Uri uri) {
        for (Contact contact : this.contacts) {
            if (contact.uri == uri) {
                return contact;
            }
        }
        return null;
    }

    public List<Contact> getContacts() {
        return new ArrayList<Contact>(contacts);
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

        ImageView profileThumbnail = (ImageView) view.findViewById(R.id.profileThumbnail);
        profileThumbnail.setImageBitmap(contact.getPhotoThumbnail(this.context.getContentResolver()));

        TextView nameText = (TextView) view.findViewById(R.id.contactName);
        nameText.setText(contact.getDisplayName());

        TextView numberText = (TextView) view.findViewById(R.id.contactNumber);
        List<PhoneNumber> phoneNumbers = contact.getPhoneNumbers();
        String phoneNumberText = null;
        if (phoneNumbers != null && !phoneNumbers.isEmpty()) {
            phoneNumberText = phoneNumbers.get(0).getPhoneNumber();
        }
        numberText.setText(phoneNumberText);

        TextView lastContactedText = (TextView) view.findViewById(R.id.lastContacted);
        lastContactedText.setText(contact.getLastContacted().toString());

        TextView scoreText = (TextView) view.findViewById(R.id.contactScore);
        scoreText.setText("Score: " + contact.getScore());
        Log.i(MainActivity.TAG, "score: " + contact.getScore());

        return view;
    }

    @Override
    public void notifyDataSetChanged() {
        Collections.sort(this.contacts, new Comparator<Contact>() {
            @Override
            public int compare(Contact contact, Contact contact2) {
                // Return descending; larger values have more priority
                return -1 * contact.getScore().compareTo(contact2.getScore());
            }
        });
        super.notifyDataSetChanged();
    }
}
