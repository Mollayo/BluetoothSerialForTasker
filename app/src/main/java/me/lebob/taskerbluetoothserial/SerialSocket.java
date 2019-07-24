package me.lebob.taskerbluetoothserial;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;

import me.lebob.taskerbluetoothserial.utils.Constants;
import me.lebob.taskerbluetoothserial.utils.TaskerPlugin;

import static me.lebob.taskerbluetoothserial.utils.Constants.crlf_bytes;


class SerialSocket implements Runnable {

    public static final Intent INTENT_REQUEST_REQUERY =
            new Intent(com.twofortyfouram.locale.api.Intent.ACTION_REQUEST_QUERY)
                    .putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_ACTIVITY_CLASS_NAME,
                            EventEditActivity.class.getName());

    private static final UUID BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Context context;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    String addr;



    SerialSocket() {
        Log.v(Constants.LOG_TAG, "SerialSocket::SerialSocket");
    }

    /**
     * connect-success and most connect-errors are returned asynchronously to listener
     */
    Boolean connect(Context context, String addr, BluetoothDevice device) {
        Log.v(Constants.LOG_TAG, "SerialSocket::connect");
        this.context = context;
        this.device = device;
        this.addr=addr;
        try {
            socket = device.createRfcommSocketToServiceRecord(BLUETOOTH_SPP);
            socket.connect();
        } catch (Exception e) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
            // Failed to connect
            return false;
        }
        // Start the thread
        Executors.newSingleThreadExecutor().submit(this);
        // Wait for the thread to run one loop
        if (socket.isConnected())
            Log.v(Constants.LOG_TAG, "Seems to be connected");
        return true;
    }

    void disconnect() {
        Log.v(Constants.LOG_TAG, "SerialSocket::disconnect");
        if(socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
    }

    void write(byte[] data) throws IOException {
        Log.v(Constants.LOG_TAG, "SerialSocket::write");
        socket.getOutputStream().write(data);
    }

    byte[] read(byte[] data) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        len = socket.getInputStream().read(buffer);
        byte[] newData = Arrays.copyOf(buffer, len);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write( data );
        outputStream.write( newData );
        data=outputStream.toByteArray();
        return data;
    }

    @Override
    public void run() { // connect & read
        Log.v(Constants.LOG_TAG, "SerialSocket::run");
        try {
            byte[] data=new byte[0];        // The buffer
            Instant lastSentDataTime =null;
            Instant lastReceivedDataTime =null;
            //noinspection InfiniteLoopStatement
            while (true)
            {
                // If the buffer is empty, we read the data
                if (data.length==0) {
                    data = read(data);
                    lastReceivedDataTime =Instant.now();
                }
                // If the buffer contains some data, we check if there are some more data to read
                else if (socket.getInputStream().available()>0) {
                    data=read(data);
                    lastReceivedDataTime =Instant.now();
                }


                // If the time between two consecutive broadcasts is too small, messages are lost
                // 200 ms minimum between each broadcast
                // 100 ms is too small
                if (lastSentDataTime !=null) {
                    Duration diff = Duration.between(lastSentDataTime, Instant.now());
                    diff = diff.minusMillis(200);
                    if (diff.isNegative()) {
                        Thread.sleep(10);
                        continue;
                    }
                }

                // Check if the data contains line return
                int crlf_idx=indexOf(data,crlf_bytes);
                // If not, we check the duration from the last received data
                if (crlf_idx==-1)
                {
                    Duration diff = Duration.between(lastReceivedDataTime, Instant.now());
                    diff = diff.minusMillis(50);
                    if (diff.isNegative()) {
                        Thread.sleep(10);
                        continue;
                    }
                }
                byte[] dataToSend;
                if (crlf_idx > -1) {
                    dataToSend = Arrays.copyOfRange(data, 0, crlf_idx + crlf_bytes.length);
                    data = Arrays.copyOfRange(data, crlf_idx + crlf_bytes.length, data.length);
                } else {
                    dataToSend = Arrays.copyOfRange(data, 0, data.length);
                    data = new byte[0];
                }
                String str = new String(dataToSend);
                Log.v(Constants.LOG_TAG, "Broadcasting " + str);

                // Send a message to Tasker for the new data received
                Bundle bundle = new Bundle();
                // Add the address of the bluetooth device
                bundle.putString(Constants.BUNDLE_STRING_MAC, addr);
                // Add the data received from serial connection
                bundle.putByteArray(Constants.BUNDLE_STRING_MSG, dataToSend);

                // Set the size to 0
                TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY, bundle);
                TaskerPlugin.Event.addPassThroughMessageID(INTENT_REQUEST_REQUERY);
                context.sendBroadcast(INTENT_REQUEST_REQUERY);
                lastSentDataTime =Instant.now();
            }
        } catch (Exception e) {
            Singleton.removeSerialSocketThread(addr);
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
    }

    // Find the last position of the smallerArray inside the outerArray
    private int indexOf(byte[] outerArray, byte[] smallerArray) {
        /*
        for(int i = 0; i < outerArray.length - smallerArray.length+1; ++i) {
            boolean found = true;
            for(int j = 0; j < smallerArray.length; ++j) {
                if (outerArray[i+j] != smallerArray[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }
    */
        for(int i = outerArray.length - smallerArray.length; i >0 ; --i) {
            boolean found = true;
            for(int j = 0; j < smallerArray.length; ++j) {
                if (outerArray[i+j] != smallerArray[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

}
