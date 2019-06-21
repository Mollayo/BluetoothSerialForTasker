package me.lebob.taskerbluetoothserial;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ListPopupWindow;


import me.lebob.taskerbluetoothserial.utils.Constants;
import me.lebob.taskerbluetoothserial.utils.TaskerPlugin;


public class EventEditActivity extends AppCompatActivity {

    // Variables necessary for querying and setting MAC addresses
    private BluetoothAdapter mBluetoothAdapter;
    private ListPopupWindow popupWindow;
    private String[] addresses = new String[]{};
    private String[] names = new String[]{};
    private CheckBox checkBox;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.v(Constants.LOG_TAG, "EventEditActivity::onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);
        checkBox = findViewById(R.id.hex_checkbox);

        // Load the previously defined user parameters
        if (getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE) != null) {
            Bundle bundleExtra = getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
            checkBox.setChecked(bundleExtra.getBoolean(Constants.BUNDLE_BOOL_HEX));
        }
    }

    @Override
    public void onBackPressed()
    {
        Log.v(Constants.LOG_TAG, "EventEditActivity::onBackPressed");
        finish();
    }

    @Override
    public void finish()
    {
        Log.v(Constants.LOG_TAG, "EventEditActivity::finish");
        Intent intent = new Intent();

        Bundle result = new Bundle();

        result.putBoolean(Constants.BUNDLE_BOOL_HEX, checkBox.isChecked());
        /*
         * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note
         * that anything placed in this Bundle must be available to Locale's class loader. So storing
         * String, int, and other standard objects will work just fine. Parcelable objects are not
         * acceptable, unless they also implement Serializable. Serializable objects must be standard
         * Android platform objects (A Serializable class private to this plug-in's APK cannot be
         * stored in the Bundle, as Locale's classloader will not recognize it).
         */
        intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, result);

        /*
         * The blurb is concise status text to be displayed in the host's UI.
         */
        intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, "Receiving serial data");

        if ( TaskerPlugin.hostSupportsRelevantVariables( getIntent().getExtras() ) ) {
            TaskerPlugin.addRelevantVariableList(intent, new String [] {
                    "%serial_data\nSerial data\nThe serial data received from the Bluetooth device",
                    "%serial_addr\nBluetooth address\nAddress of the Bluetooth device sending the data"
            });
        }

        setResult(RESULT_OK, intent);

        super.finish();
    }


}
