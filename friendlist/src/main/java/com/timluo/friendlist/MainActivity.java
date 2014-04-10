package com.timluo.friendlist;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;

import static android.widget.AdapterView.AdapterContextMenuInfo;

public class MainActivity extends ListActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListAdapter adapter =
                new ArrayAdapter<String>(this, R.layout.friend_list_item,
                        Lists.newArrayList("Hello", "world"));
        setListAdapter(adapter);

        ListView listView = getListView();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                Toast.makeText(getApplicationContext(),
                        ((TextView) view).getText(), Toast.LENGTH_SHORT).show();
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
        String contact = (String) getListAdapter().getItem(info.position);
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

    void editContact(int position) {
        Toast.makeText(getApplicationContext(), "Edit Contact", Toast.LENGTH_SHORT).show();
    }

    void bumpContact(int position) {
        @SuppressWarnings("unchecked")
        ArrayAdapter<String> adapter = ((ArrayAdapter<String>) getListAdapter());
        String contact = adapter.getItem(position);
        // Remove from list, then add to end
        adapter.remove(contact);
        adapter.add(contact);
        this.runOnUiThread(new Runnable() {
            public void run() {
                ((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
            }
        });
    }
    /* End Contacts context menu */



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
                // TODO: trigger add contact flow
        }
        return super.onOptionsItemSelected(item);
    }

    /* End action bar option menu */

}
