package com.timluo.friendlist;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import static android.provider.ContactsContract.Contacts;
import static android.widget.AdapterView.AdapterContextMenuInfo;

public class MainActivity extends ListActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<String> list = new ArrayList<String>();
        ListAdapter adapter =
                new ArrayAdapter<String>(this, R.layout.friend_list_item, list);
        setListAdapter(adapter);

        ListView listView = getListView();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                openContextMenu(view);
            }
        });
        registerForContextMenu(listView);
    }

    /* Contacts context menu */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.delete_contact:
                deleteContact(info.position);
                return true;
            case R.id.edit_contact:
                editContact(info.position);
                return true;
            case R.id.bump_contact:
                bumpContact(info.position);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    void deleteContact(int position) {
        @SuppressWarnings("unchecked")
        ArrayAdapter<String> adapter = ((ArrayAdapter<String>) getListAdapter());
        adapter.remove(adapter.getItem(position));
    }

    void editContact(final int position) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Edit Contact");
        alert.setMessage("Enter a name");

        final EditText input = new EditText(this);
        String currentContact = getArrayAdapter().getItem(position);
        input.setText(currentContact);
        input.setSelection(currentContact.length());
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // This works for strings, but for contacts we'll want to copy over all unedited fields
                deleteContact(position);

                String value = input.getText().toString();
                // Do something with value!
                Log.i(TAG, value);
                if (value != null && !value.isEmpty()) {
                    getArrayAdapter().insert(value, position);
                    refreshListAdapter();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    void bumpContact(int position) {
        ArrayAdapter<String> adapter = getArrayAdapter();
        String contact = adapter.getItem(position);
        // Remove from list, then add to end
        adapter.remove(contact);
        adapter.add(contact);
        refreshListAdapter();
    }
    /* End Contacts context menu */

    private static final int CONTACT_PICKER_REQUEST = 1001;
    private void addContact() {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                Contacts.CONTENT_URI);
        startActivityForResult(contactPickerIntent, CONTACT_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICKER_REQUEST:
                    Uri contactData = data.getData();
                    Cursor cursor = getContentResolver().query(contactData, null, null, null, null);
                    if (cursor.moveToFirst()) {
                        String name =
                                cursor.getString(
                                        cursor.getColumnIndex(
                                                ContactsContract.Contacts.DISPLAY_NAME));
                        getArrayAdapter().add(name);
                    }
                    break;
            }

        } else {
            Log.w(TAG, String.format("Warning: result not OK for requestCode: %d", requestCode));
        }
    }

    private ArrayAdapter<String> getArrayAdapter() {
        // "all" and not "unchecked" because adapter will complain as being "redundant"
        @SuppressWarnings("all")
        ArrayAdapter<String> adapter = ((ArrayAdapter<String>) getListAdapter());
        return adapter;
    }

    private void refreshListAdapter() {
        runOnUiThread(new Runnable() {
            public void run() {
                ((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
            }
        });
    }


    /* Action bar option menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_add:
                addContact();
        }
        return super.onOptionsItemSelected(item);
    }

    /* End action bar option menu */

}
