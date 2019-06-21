package me.lebob.taskerbluetoothserial.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import me.lebob.taskerbluetoothserial.utils.BundleScrubber;
import me.lebob.taskerbluetoothserial.utils.Constants;
import me.lebob.taskerbluetoothserial.utils.TaskerPlugin;

import android.util.Log;

import java.util.Locale;

/**
 * Created by dmillerw
 */
public class EventQueryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.v(Constants.LOG_TAG, "EventQueryReceiver::onReceive");

        if (!com.twofortyfouram.locale.api.Intent.ACTION_QUERY_CONDITION.equals(intent.getAction())) {
            Log.e(Constants.LOG_TAG,
                    String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction()));
            return;
        }

        final int messageID = TaskerPlugin.Event.retrievePassThroughMessageID(intent);
        if (messageID == -1) {
            String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction());
            setResultCode(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN);
        } else {
            // Get the user parameters (BT address and the hex flag) of the Event Receiver
            BundleScrubber.scrub(intent);
            final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
            BundleScrubber.scrub(bundle);
            Boolean hex = bundle.getBoolean(Constants.BUNDLE_BOOL_HEX);


            // Get the data from serial socket thread
            Bundle dataBundle = TaskerPlugin.Event.retrievePassThroughData(intent);
            // Get the MAC address of the serial socket
            String serialMac=dataBundle.getString(Constants.BUNDLE_STRING_MAC);
            // Get the serial data from the serial socket
            byte[] serialData=dataBundle.getByteArray(Constants.BUNDLE_STRING_MSG);
            String serialDataStr=new String();


            // If the mac address of the serial connection matches the one of the event receiver,
            // the condition is satisfied
            if (serialData.length>0) {
                // Send the data to Tasker through the variable %serial_data
                if (TaskerPlugin.Condition.hostSupportsVariableReturn(intent.getExtras())) {
                    Bundle variables = new Bundle();
                    if (hex)
                    {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < serialData.length; ++i) {
                            sb.append(String.format("%02X ", serialData[i]));
                        }
                        serialDataStr=sb.toString();
                    }
                    else
                        serialDataStr = new String(serialData);
                    variables.putString("%serial_data", serialDataStr);
                    variables.putString("%serial_addr", serialMac);
                    TaskerPlugin.addVariableBundle(getResultExtras(true), variables);
                }
                setResultCode(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED);
            }
            else
              setResultCode(com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNSATISFIED);
        }
    }

}
