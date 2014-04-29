package com.timluo.friendlist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import org.joda.time.LocalDate;

/**
 * Receives incoming SMS messages.
 */
public class SMSReceiver extends BroadcastReceiver {
    private final String TAG = this.getClass().getSimpleName();

    private ContactAdapter adapter;

    public SMSReceiver(ContactAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Bundle extras = intent.getExtras();

        if ( extras != null )
        {
            Object[] smsextras = (Object[]) extras.get( "pdus" );
            //TODO: are these smsextras ever from different contacts?
            for ( int i = 0; i < smsextras.length; i++ ) {
                SmsMessage smsmsg = SmsMessage.createFromPdu((byte[]) smsextras[i]);
                String fromAddress = smsmsg.getOriginatingAddress();

                //TODO: likely pull this out into its own method later for use in other Receivers, e.g. Phone
                //TODO: clean up this null checking and exactly when to break and/or notifyDataSetChanged
                String fromId = this.adapter.getContactIdByNumber(fromAddress);
                if (fromId != null) {
                    Contact contact = this.adapter.contactForId(fromId);
                    if (contact != null) {
                        contact.setLastContacted(LocalDate.now());
                        this.adapter.notifyDataSetChanged();
                        Log.i(TAG, "Updated contact " + contact.getDisplayName());
                        //TODO: Probably pop up a Toast message here informing the user? Also perhaps add an undo button
                    }
                }
            }
        }

    }
}