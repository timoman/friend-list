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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
import org.joda.time.DurationField;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.util.Calendar;
import java.util.List;

import static android.provider.ContactsContract.Contacts;
import static android.view.View.inflate;
import static android.widget.AdapterView.AdapterContextMenuInfo;

public class MainActivity extends ListActivity {
    public static final String TAG = "MainActivity";
    private DatabaseHandler databaseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.databaseHandler = new DatabaseHandler(this);
        List<Contact> contactList = this.databaseHandler.getAllContacts();

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
        String[] editContactArray = getResources().getStringArray(R.array.edit_contact_array);
        AlertDialog editPicker = new AlertDialog.Builder(this)
                                .setTitle("Edit Contact")
                                .setItems(editContactArray, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // The 'which' argument contains the index position
                                        // of the selected item
                                        switch (which) {
                                            case 0:
                                                editContactFrequency(position);
                                                break;
                                            default:
                                                editContactLastContacted(position);
                                                break;
                                        }
                                    }

                                    ;
                                }).create();
        addCancelButton(editPicker);
        editPicker.show();
    }

    private void editContactLastContacted(int position) {
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
        addCancelButton(datePicker);
        datePicker.show();
    }

    private void editContactFrequency(int position) {
        ContactAdapter adapter = getContactAdapter();
        final Contact contact = adapter.getItem(position);

        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();

        final View view = inflater.inflate(R.layout.contact_frequency_layout, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        .setView(view)

        // Add action buttons
        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                final EditText timeValueField = (EditText) view.findViewById(R.id.time_value);
                final Spinner timeResolutionField = (Spinner) view.findViewById(R.id.time_resolution_spinner);

                Integer timeValue = Integer.parseInt(timeValueField.getText().toString());
                String timeResolution = timeResolutionField.getSelectedItem().toString().toLowerCase();

                Period period;
                // TODO: can't do partial days, so hour screws things up... either remove hour or make daysToContact a double instead of a Days
                if (timeResolution.contains("hour")) {
                    period = Period.hours(timeValue);
                }
                else if (timeResolution.contains("day")) {
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
                databaseHandler.addContact(contact);
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


}
