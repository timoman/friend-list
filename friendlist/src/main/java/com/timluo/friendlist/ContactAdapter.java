package com.timluo.friendlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by timluo on 4/14/14.
 */
public class ContactAdapter extends ArrayAdapter<Contact> {
    private int resource;

    public ContactAdapter(Context context, int resource, List<Contact> objects) {
        super(context, resource, objects);
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            LayoutInflater inflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(this.resource, parent, false);
        } else {
            view = convertView;
        }

        Contact contact = getItem(position);
        TextView nameText = (TextView) view.findViewById(R.id.contactName);
        nameText.setText(contact.displayName);

        TextView numberText = (TextView) view.findViewById(R.id.contactNumber);
        numberText.setText(contact.phoneNumber);

        return view;
    }
}
