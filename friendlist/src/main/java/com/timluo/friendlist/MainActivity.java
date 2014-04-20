package com.timluo.friendlist;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.joda.time.LocalDate;

import java.util.Calendar;
import java.util.List;

import static android.provider.ContactsContract.Contacts;
import static android.widget.AdapterView.AdapterContextMenuInfo;

public class MainActivity extends ListActivity {
    public static final String TAG = "MainActivity";
    private DatabaseHandler databaseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.databaseHandler = new DatabaseHandler(this);
        List<Contact> contactList = this.databaseHandler.getAllContacts();
        //TODO: contactList should be sorted by score

        ListAdapter adapter =
                new ContactAdapter(this, R.layout.friend_list_item, contactList, this.databaseHandler);
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

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.databaseHandler.close();
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
        ContactAdapter adapter = getContactAdapter();
        Contact contact = adapter.getItem(position);
        adapter.remove(contact);
    }

    void editContact(final int position) {
        ContactAdapter adapter = getContactAdapter();
        final Contact contact = adapter.getItem(position);
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                final Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, day);

                LocalDate newLastContacted = LocalDate.fromCalendarFields(calendar);
                newLastContacted.minusMonths(1);
                contact.setLastContacted(newLastContacted);
                databaseHandler.addContact(contact);
                refreshAdapter();
            }
        };
        LocalDate oldDate = contact.getLastContacted();
        DatePickerDialog datePicker = new DatePickerDialog(this, dateSetListener,
                oldDate.getYear(), oldDate.minusMonths(1).getMonthOfYear(), oldDate.getDayOfMonth());
        datePicker.show();
    }

    void bumpContact(int position) {
        ContactAdapter adapter = getContactAdapter();
        Contact contact = adapter.getItem(position);
        contact.setLastContacted(LocalDate.now());
        // Remove from list, then add to end
        adapter.remove(contact);
        adapter.add(contact);
        refreshAdapter();
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
                    Contact contact = new Contact(contactData, getContentResolver());
                    if (!getContactAdapter().add(contact)) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(this);
                        alert.setTitle("Duplicate contact");
                        alert.setMessage("This contact is already in your deck.");
                        alert.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Canceled.
                            }
                        });
                        alert.show();
                    }
                    break;
            }

        } else {
            Log.w(TAG, String.format("Warning: result not OK for requestCode: %d", requestCode));
        }
    }

    private ContactAdapter getContactAdapter() {
        ContactAdapter adapter = (ContactAdapter) getListAdapter();
        return adapter;
    }

    private void refreshAdapter() {
        runOnUiThread(new Runnable() {
            public void run() {
                ((ContactAdapter) getListAdapter()).notifyDataSetChanged();
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
