package me.lebob.taskerbluetoothserial.utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;
import java.util.regex.Pattern;

import me.lebob.taskerbluetoothserial.R;

import static me.lebob.taskerbluetoothserial.utils.Constants.clrf_string_display;
import static me.lebob.taskerbluetoothserial.utils.Constants.crlf_bytes;

public class ActionBundleManager {

    // only accept valid MAC addresses of form 00:11:22:AA:BB:CC, where colons can be dashes
    public static boolean isMacValid(String mac) {
        if (mac == null) {
            return false;
        }

        if (Pattern.matches("([0-9a-fA-F]{2}[:-]){5}[0-9a-fA-F]{2}", mac))
            return true;

        // We allow variable MACs
        return TaskerPlugin.variableNameValid(mac);
    }

    // Whether the bundle is valid. Strings must be non-null, and either variables
    // or valid format (correctly-formatted MAC, non-empty, proper hex if binary, etc.)
    public static boolean isBundleValid(final Bundle bundle) {
        if (bundle == null) {
            Log.w(Constants.LOG_TAG, "Null bundle");
            return false;
        }

        String[] keys = {Constants.BUNDLE_CON_DISCON_SEND, Constants.BUNDLE_BOOL_CRLF,
                Constants.BUNDLE_BOOL_HEX, Constants.BUNDLE_STRING_MAC, Constants.BUNDLE_STRING_MSG};
        for (String key: keys) {
            if (!bundle.containsKey(key)) {
                Log.w(Constants.LOG_TAG, "Bundle missing key " + key);
            }
        }

        String mac = getMac(bundle);
        if (!isMacValid(mac)) {
            Log.w(Constants.LOG_TAG, "Invalid MAC");
            return false;
        }

        int conDisconSend = getConDisconSend(bundle);
        if (conDisconSend!=2)
            return true;

        // Check for the case of sending a message
        String msg = getMsg(bundle);
        if (msg == null) {
            Log.w(Constants.LOG_TAG, "Null message");
            return false;
        }

        // allow variable replacement, at the expense
        // of sanity checking hex
        // We allow variable MACs
        if (TaskerPlugin.variableNameValid(msg)) {
            return true;
        }

        boolean hex = getHex(bundle);
        boolean crlf = getCrlf(bundle);

        if (hex) {
            // If we interpret message as hex, we expect it to be non-null
            byte[] normalized = getByteArrayFromHexString(msg);
            boolean valid = normalized != null;
            if (!valid) {
                Log.w(Constants.LOG_TAG, "Message is not well-formed HEX");
            }
            return valid;
        } else {
            boolean valid = crlf || !msg.isEmpty();
            if (!valid) {
                Log.w(Constants.LOG_TAG, "Empty message and no CRLF");
            }
            return valid;
        }
    }

    // method to get error message for the given values, or null if no error exists
    public static String getErrorMessage(Context context,int connDisconSend, final String mac, final String msg, boolean crlf, boolean hex) {
        Resources res = context.getResources();
        if (connDisconSend==0 || connDisconSend==1) {
            if (!isMacValid(mac)) {
                return res.getString(R.string.invalid_mac);
            }
        }

        if (connDisconSend==2) {
            if (hex) {
                if (getByteArrayFromHexString(msg) == null) {
                    return res.getString(R.string.invalid_hex);
                }
            } else {
                if (msg == null || (msg.isEmpty() && !crlf)) {
                    return res.getString(R.string.invalid_msg);
                }
            }
        }

        return null;
    }

    // Method to create bundle from the individual values
    public static Bundle generateBundle(final int conDisconSend, final String mac, final String msg, boolean crlf, boolean hex) {
        if (mac == null || msg == null) {
            return null;
        }

        final Bundle bundle = new Bundle();
        bundle.putInt(Constants.BUNDLE_CON_DISCON_SEND, conDisconSend);
        bundle.putString(Constants.BUNDLE_STRING_MAC, mac);
        bundle.putString(Constants.BUNDLE_STRING_MSG, msg);
        bundle.putBoolean(Constants.BUNDLE_BOOL_CRLF, crlf);
        bundle.putBoolean(Constants.BUNDLE_BOOL_HEX, hex);

        if (!isBundleValid(bundle)) {
            return null;
        } else {
            return bundle;
        }
    }

    // Method for getting short String description of bundle
    public static String getBundleBlurb(final Bundle bundle) {
        if (!isBundleValid(bundle)) {
            return null;
        }

        final String mac = getMac(bundle);
        final String msg = getMsg(bundle);
        final boolean crlf = getCrlf(bundle);
        final boolean hex = getHex(bundle);
        final int conDisconSend = getConDisconSend(bundle);

        final int max_len = 60;
        final int crlf_len = crlf ? clrf_string_display.length() : 0;
        final String ellipses = "...";

        StringBuilder builder = new StringBuilder();
        if (conDisconSend==0)
        {
            builder.append("Connect to ");
            builder.append(mac);
        }
        else if (conDisconSend==1)
        {
            builder.append("Disconnect from ");
            builder.append(mac);
        }
        else if (conDisconSend==2) {
            builder.append("Send to ");
            builder.append(mac);
            builder.append(" <- ");
            if (hex) {
                builder.append("(hex) ");
            }
            builder.append(msg);
            int length = builder.length() + crlf_len;

            if (length > max_len) {
                builder.delete(max_len - crlf_len - ellipses.length(), length);
                builder.append(ellipses);
            }
            if (crlf) {
                builder.append(clrf_string_display);
            }
        }

        return builder.toString();
    }

    // Method to get MAC address of bundle
    public static int getConDisconSend(final Bundle bundle) {
        return bundle.getInt(Constants.BUNDLE_CON_DISCON_SEND, 0);
    }

    // Method to get MAC address of bundle
    public static String getMac(final Bundle bundle) {
        return bundle.getString(Constants.BUNDLE_STRING_MAC, null);
    }

    // Method to get message part of bundle
    public static String getMsg(final Bundle bundle) {
        return bundle.getString(Constants.BUNDLE_STRING_MSG, null);
    }

    // Method to get CRLF part of bundle
    public static boolean getCrlf(final Bundle bundle) {
        return bundle.getBoolean(Constants.BUNDLE_BOOL_CRLF, true);
    }

    // Method to get whether message should be interpreted as binary hex
    public static boolean getHex(final Bundle bundle) {
        return bundle.getBoolean(Constants.BUNDLE_BOOL_HEX, false);
    }

    // method to get the message bytes for the given bundle, or null if the bundle is invalid
    public static byte[] getMsgBytes(final Bundle bundle) {
        if (!isBundleValid(bundle)) {
            return null;
        }
        String msg = getMsg(bundle);

        byte[] msg_bytes;
        if (getHex(bundle)) {
            msg_bytes = getByteArrayFromHexString(msg);
        } else {
            msg_bytes = msg.getBytes();
        }

        if (msg_bytes == null) {
            return null;
        }

        if (getCrlf(bundle)) {
            int old_length = msg_bytes.length;

            // add CRLF bytes
            msg_bytes = Arrays.copyOf(msg_bytes, old_length + crlf_bytes.length);
            for (int i = 0; i < crlf_bytes.length; ++i) {
                msg_bytes[old_length + i] = crlf_bytes[i];
            }
        }

        return msg_bytes;
    }

    // Hex string to byte array. null if invalid

    private static byte[] getByteArrayFromHexString(String s) {
        if (s == null) {
            return null;
        }
        // remove spaces and convert to uppercase
        s = s.replaceAll("\\s+","").toUpperCase();

        final int length = s.length();

        // need even length
        if (length % 2 != 0) {
            return null;
        }

        // we want at least one character, and make sure it's hex value
        if (!s.matches("^[0-9A-F]+$")) {
            return null;
        }

        byte[] bytes = new byte[length/2];

        for (int i = 0; i < bytes.length; ++i) {
            int cur_index = 2*i;
            bytes[i] = (byte) Short.parseShort(s.substring(cur_index, cur_index + 2), 16);
        }
        return bytes;
    }

}
