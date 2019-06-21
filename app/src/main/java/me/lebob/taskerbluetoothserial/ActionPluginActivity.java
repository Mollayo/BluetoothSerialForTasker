package me.lebob.taskerbluetoothserial;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;


import java.util.Set;

import me.lebob.taskerbluetoothserial.utils.BundleScrubber;
import me.lebob.taskerbluetoothserial.utils.Constants;
import me.lebob.taskerbluetoothserial.utils.ActionBundleManager;
import me.lebob.taskerbluetoothserial.utils.TaskerPlugin;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;


public class ActionPluginActivity extends AppCompatActivity {

    // Constant for activity result
    public static final int REQUEST_ENABLE_BT = 134;

    // Variables necessary for querying and setting MAC addresses
    private BluetoothAdapter mBluetoothAdapter;
    private ListPopupWindow popupWindow;
    private String[] addresses = new String[]{};
    private String[] names = new String[]{};
    private EditText macText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(Constants.LOG_TAG, "ActionPluginActivity::onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_action);

        macText = findViewById(R.id.mac);

        final String previousBlurb = getIntent().getStringExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB);

        final Bundle previousBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(previousBundle);
        if (isBundleValid(previousBundle) && previousBlurb!=null)
            onPostCreateWithPreviousResult(previousBundle,previousBlurb);

        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // On clicking the button, ask user to enable bluetooth, and then show paired devices
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null) {
                    Context context = getApplicationContext();
                    String msg = context.getResources().getString(R.string.bluetooth_error);
                    Log.w(Constants.LOG_TAG, msg);
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    showPairedDevices(getBaseContext());
                }
            }
        });

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.conDisconSend);
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                updateWidgetVisibility(checkedId);
            }
        });
    }

    // Method that popups a list of paired devices
    private void showPairedDevices(Context context) {
        Log.v(Constants.LOG_TAG, "ActionPluginActivity::showPairedDevices");
        if (mBluetoothAdapter == null) {
            return;
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        addresses = new String[pairedDevices.size()];
        names = new String[pairedDevices.size()];
        int i = 0;

        for (BluetoothDevice device : pairedDevices) {
            addresses[i] = device.getAddress();
            names[i] = device.getName() + " (" + addresses[i] + ")";
            ++i;
        }

        popupWindow = new ListPopupWindow(context);
        popupWindow.setAdapter(new ArrayAdapter(context, R.layout.list_item_action, names));
        popupWindow.setAnchorView(macText);

        popupWindow.setModal(true);
        popupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < addresses.length) {
                    macText.setText(addresses[position]);
                }
                popupWindow.dismiss();
            }
        });
        popupWindow.show();
    }

    private void updateWidgetVisibility(int checkedId)
    {
        EditText msg=findViewById(R.id.msg);
        CheckBox crlf_checkbox=findViewById(R.id.crlf_checkbox);
        CheckBox hex_checkbox=findViewById(R.id.hex_checkbox);
        // checkedId is the RadioButton selected
        if (checkedId==R.id.send) {
            // Show the widgets specific to sending a message
            msg.setVisibility(VISIBLE);
            crlf_checkbox.setVisibility(VISIBLE);
            hex_checkbox.setVisibility(VISIBLE);
        }
        else {
            // Hide the widgets
            msg.setVisibility(INVISIBLE);
            crlf_checkbox.setVisibility(INVISIBLE);
            hex_checkbox.setVisibility(INVISIBLE);
        }
    }

    // Method that gets called once request to enable bluetooth completes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(Constants.LOG_TAG, "ActionPluginActivity::onActivityResult");
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            showPairedDevices(getBaseContext());
        }
    }

    // Method that checks if bundle is valid
    public boolean isBundleValid(Bundle bundle) {
        Log.v(Constants.LOG_TAG, "ActionPluginActivity::isBundleValid");
        return ActionBundleManager.isBundleValid(bundle);
    }

    // Method that uses previously saved bundle
    public void onPostCreateWithPreviousResult(Bundle bundle,String blurb) {
        Log.v(Constants.LOG_TAG, "ActionPluginActivity::onPostCreateWithPreviousResult");

        final int conDisconSend = ActionBundleManager.getConDisconSend(bundle);
        if (conDisconSend==0) {
            updateWidgetVisibility(R.id.connect);
            ((RadioGroup) findViewById(R.id.conDisconSend)).check(R.id.connect);
        }
        else if (conDisconSend==1) {
            updateWidgetVisibility(R.id.disconnect);
            ((RadioGroup) findViewById(R.id.conDisconSend)).check(R.id.disconnect);
        }
        if (conDisconSend==2) {
            updateWidgetVisibility(R.id.send);
            ((RadioGroup) findViewById(R.id.conDisconSend)).check(R.id.send);
        }

        final String mac = ActionBundleManager.getMac(bundle);
        macText.setText(mac);

        final String msg = ActionBundleManager.getMsg(bundle);
        ((EditText) findViewById(R.id.msg)).setText(msg);

        final boolean crlf = ActionBundleManager.getCrlf(bundle);
        ((CheckBox) findViewById(R.id.crlf_checkbox)).setChecked(crlf);

        final boolean hex = ActionBundleManager.getHex(bundle);
        ((CheckBox) findViewById(R.id.hex_checkbox)).setChecked(hex);
    }

    // Method that returns the bundle to be saved
    public Bundle getResultBundle() {
        Log.v(Constants.LOG_TAG, "ActionPluginActivity::getResultBundle");
        String mac = macText.getText().toString();
        String msg = ((EditText) findViewById(R.id.msg)).getText().toString();
        boolean crlf = ((CheckBox) findViewById(R.id.crlf_checkbox)).isChecked();
        boolean hex = ((CheckBox) findViewById(R.id.hex_checkbox)).isChecked();
        int val = ((RadioGroup)findViewById(R.id.conDisconSend)).getCheckedRadioButtonId();
        int connDisconSend=0;
        if (val==R.id.connect)
            connDisconSend=0;
        else if (val==R.id.disconnect)
            connDisconSend=1;
        else if (val==R.id.send)
            connDisconSend=2;

        Bundle bundle = ActionBundleManager.generateBundle(connDisconSend, mac, msg, crlf, hex);

        if (bundle == null) {
            Context context = getApplicationContext();
            String error = ActionBundleManager.getErrorMessage(context, connDisconSend, mac, msg, crlf, hex);
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show();
            } else {
                Log.e(Constants.LOG_TAG, "Null bundle, but no error");
            }
            return null;
        }

        // This is to specify that the Mac address and the message can be Tasker variables
        if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this)) {
            TaskerPlugin.Setting.setVariableReplaceKeys(bundle, new String[]{
                    Constants.BUNDLE_STRING_MAC,
                    Constants.BUNDLE_STRING_MSG});
        }
        return bundle;
    }

    // Method that creates summary of bundle
    public String getResultBlurb(Bundle bundle) {
        Log.v(Constants.LOG_TAG, "ActionPluginActivity::getResultBlurb");
        return ActionBundleManager.getBundleBlurb(bundle);
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onBackPressed() {

        // The bundle to save the user parameters
        final Bundle resultBundle = getResultBundle();
        if (!isBundleValid(resultBundle))
            return;

        super.onStop();
        Intent intent = new Intent();
        intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE, resultBundle);

        // For the explanation text of the plugin
        String blurbStr=getResultBlurb(resultBundle);
        intent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, blurbStr);

        // For the synchronous execution
        if (TaskerPlugin.Setting.hostSupportsSynchronousExecution( getIntent().getExtras()))
            TaskerPlugin.Setting.requestTimeoutMS( intent, 7000 );

        setResult(RESULT_OK, intent);
        finish();
    }
}

