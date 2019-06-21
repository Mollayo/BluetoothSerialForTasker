package me.lebob.taskerbluetoothserial.receivers;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;


import me.lebob.taskerbluetoothserial.Singleton;
import me.lebob.taskerbluetoothserial.utils.Constants;
import me.lebob.taskerbluetoothserial.utils.ActionBundleManager;
import me.lebob.taskerbluetoothserial.utils.TaskerPlugin;

public class ActionSettingReceiver  extends AbstractPluginSettingReceiver {

    // Method that checks whether bundle is valid
    @Override
    protected boolean isBundleValid(Bundle bundle) {
        Log.v(Constants.LOG_TAG, "ActionSettingReceiver::isBundleValid");
        return ActionBundleManager.isBundleValid(bundle);
    }

    // Method that suggests whether this task should be handled in a background thread
    @Override
    protected boolean isAsync() {
        Log.v(Constants.LOG_TAG, "ActionSettingReceiver::isAsync");
        return false;
    }

    // Method responsible for the connection and data transmission. Assumes bluetooth is enabled
    // and the device has been paired with.
    @Override
    protected void firePluginSetting(Context context, Bundle bundle)
    {
        Log.v(Constants.LOG_TAG, "ActionSettingReceiver::firePluginSetting");

        // Get the user parameters
        final String mac = ActionBundleManager.getMac(bundle);
        final int conDisconSend = ActionBundleManager.getConDisconSend(bundle);

        Singleton.context= context.getApplicationContext();
        Boolean success=true;
        if (conDisconSend==0)
            // Connect to the Bluetooth device
            success=Singleton.createSerialSocketThread(mac);
        else if (conDisconSend==1)
            // Disconnect from the Bluetooth device
            Singleton.destroySerialSocketThread(mac);
        else if (conDisconSend==2) {
            // Send data to the Bluetooth device
            byte[] bytes = ActionBundleManager.getMsgBytes(bundle);
            if (bytes.length>0)
                success=Singleton.sendData(mac,bytes);
        }
        if ( isOrderedBroadcast() ) {
            if (!success)
                setResultCode(TaskerPlugin.Setting.RESULT_CODE_FAILED);
            else {
                setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);
            }
        }
    }
}
