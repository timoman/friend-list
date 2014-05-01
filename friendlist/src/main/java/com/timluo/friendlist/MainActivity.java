package com.timluo.friendlist;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import org.joda.time.LocalDate;
import org.joda.time.Period;

import java.util.Calendar;
import java.util.List;

import static android.provider.ContactsContract.Contacts;
import static android.widget.AdapterView.AdapterContextMenuInfo;
import static com.timluo.friendlist.DatabaseHandler.doWithHandle;

public class MainActivity extends ListActivity {
    public static final String TAG = "MainActivity";
    private SMSReceiver smsReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<Contact> contactList = doWithHandle(this, new DatabaseHandler.DatabaseHandlerCallback<List<Contact>>() {
            @Override
            public List<Contact> doWithDatabase(DatabaseHandler handler) {
                return handler.getAllContacts();
            }
        });

        ListAdapter adapter =
                new ContactAdapter(this, R.layout.friend_list_item, contactList);
        setListAdapter(adapter);

        setupSearchBox();
        registerOnListViewClick();

        this.smsReceiver = new SMSReceiver(getContactAdapter());
        registerReceiver(this.smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // TODO: unregister on destroy for now, though maybe we want to keep it running in the future?
        unregisterReceiver(this.smsReceiver);
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

    private void deleteContact(int position) {
        ContactAdapter adapter = getContactAdapter();
        Contact contact = adapter.getItem(position);
        adapter.remove(contact);
    }

    private void editContact(final int position) {
        String[] editContactArray = getResources().getStringArray(R.array.edit_contact_array);
        // Menu for what field of the contact to edit
        AlertDialog editPicker = new AlertDialog.Builder(this)
                                .setTitle(getResources().getString(R.string.edit_contact))
                                .setItems(editContactArray, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int selectedIndex) {
                                        switch (selectedIndex) {
                                            case 0:
                                                editContactFrequency(position);
                                                break;
                                            default:
                                                editContactLastContacted(position);
                                                break;
                                        }
                                    };
                                }).create();
        addCancelButton(editPicker);
        editPicker.show();
    }

    private void editContactLastContacted(int position) {
        ContactAdapter adapter = getContactAdapter();
        final Contact contact = adapter.getItem(position);
        // Pick the last contacted date
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                final Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, day);

                LocalDate newLastContacted = LocalDate.fromCalendarFields(calendar);
                newLastContacted.minusMonths(1);
                contact.setLastContacted(newLastContacted);
                getContactAdapter().add(contact);
                refreshAdapter();
            }
        };
        // Set the default date on the DatePicker to be the last contacted date.
        LocalDate oldDate = contact.getLastContacted();
        DatePickerDialog datePicker = new DatePickerDialog(this, dateSetListener,
                oldDate.getYear(), oldDate.minusMonths(1).getMonthOfYear(), oldDate.getDayOfMonth());
        addCancelButton(datePicker);
        datePicker.getDatePicker().setCalendarViewShown(true);
        datePicker.show();
    }

    private void editContactFrequency(int position) {
        ContactAdapter adapter = getContactAdapter();
        final Contact contact = adapter.getItem(position);

        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.contact_frequency_layout, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
        .setView(view)
        .setPositiveButton(getResources().getText(R.string.save), new DialogInterface.OnClickListener() {
            // Set the contact's days to contact, calculating from value + time unit
            @Override
            public void onClick(DialogInterface dialog, int id) {
                final EditText timeValueField = (EditText) view.findViewById(R.id.time_value);
                final Spinner timeResolutionField = (Spinner) view.findViewById(R.id.time_resolution_spinner);

                Integer timeValue = Integer.parseInt(timeValueField.getText().toString());
                String timeResolution = timeResolutionField.getSelectedItem().toString().toLowerCase();

                Period period;
                if (timeResolution.contains("day")) {
                    period = Period.days(timeValue);
                }
                else if (timeResolution.contains("week")) {
                    period = Period.weeks(timeValue);
                }

                // Can't use years or months because .toStandardDays complains of varying length
                else if (timeResolution.contains("month")) {
                    period = Period.days(30 * timeValue);
                }
                else { // if (timeResolution.contains("year"))
                    period = Period.days(365 * timeValue);
                }
                contact.setDaysToContact(period.toStandardDays());
                getContactAdapter().add(contact);
                refreshAdapter();
            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        })
        .setTitle("Edit Frequency")
        .create();
        dialog.show();
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
                    // Add new contact to list
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
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_add:
                addContact();
        }
        return super.onOptionsItemSelected(item);
    }

    /* End action bar option menu */

    private static void addCancelButton(AlertDialog dialog) {
        dialog.setButton(
                DialogInterface.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            // Cancel the date picking action
                        }
                    }
                });
        dialog.setCancelable(true);
    }


    private void setupSearchBox() {
        EditText searchBox = (EditText) findViewById(R.id.inputSearch);
        searchBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence filterText, int start, int count, int after) {
                getContactAdapter().getFilter().filter(filterText);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
                // No-op
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // No-op
            }
        });

        Button clearSearchButton = (Button) findViewById(R.id.clearSearch);
        clearSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText searchBox = (EditText) findViewById(R.id.inputSearch);
                searchBox.setText("");
            }
        });
    }

    private void registerOnListViewClick() {

        ListView listView = getListView();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                openContextMenu(view);
            }
        });
        registerForContextMenu(listView);
    }

}
