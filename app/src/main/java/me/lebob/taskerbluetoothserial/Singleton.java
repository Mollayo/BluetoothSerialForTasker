package me.lebob.taskerbluetoothserial;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import me.lebob.taskerbluetoothserial.utils.Constants;

public class Singleton
{
    static class BTConnection
    {
        BTConnection(String newAddr, SerialSocket newSocketThread)
        {
            socketThread=newSocketThread;
            addr=newAddr;
        }
        SerialSocket socketThread;
        String addr;
    }
    static public List<BTConnection> btConnectionList=init();

    /*
    static public class SerialData
    {
        SerialData(String newAddr, byte[] newData, Instant newTime)
        {
            data=newData;
            addr=newAddr;
            time=newTime;
        }

        public byte[] data;
        public Instant time;
        public String addr;
    }
    static public BlockingQueue<SerialData> serialData=new LinkedBlockingDeque();
    */
    static public Context context=null;


    static private synchronized List<BTConnection> init()
    {
        Log.v(Constants.LOG_TAG, "Singleton::init");
        return new ArrayList<BTConnection>();
    }


    public static synchronized SerialSocket getSerialSocketThread(String addr)
    {
        Log.v(Constants.LOG_TAG, "Singleton::getSerialSocketThread");
        int i;
        for (i=0;i<btConnectionList.size();i++) {
            if (btConnectionList.get(i).addr.equals(addr))
                return btConnectionList.get(i).socketThread;
        }
        return null;
    }

    private static synchronized void addSerialSocketThread(String addr, SerialSocket socketThread)
    {
        Log.v(Constants.LOG_TAG, "Singleton::addSerialSocketThread");
        if  (getSerialSocketThread(addr)==null)
            btConnectionList.add(new BTConnection(addr,socketThread));
    }

    public static synchronized void removeSerialSocketThread(String addr)
    {
        Log.v(Constants.LOG_TAG, "Singleton::removeSerialSocketThread");
        int i;
        for (i=0;i<btConnectionList.size();i++)
        {
            if (btConnectionList.get(i).addr.equals(addr))
            {
                btConnectionList.remove(i);
                return;
            }
        }
        assert(false);
    }

    public static synchronized Boolean createSerialSocketThread(String addr)
    {
        Log.v(Constants.LOG_TAG, "Singleton::createSerialSocketThread");
        SerialSocket socketThread = getSerialSocketThread(addr);
        if (socketThread!=null) {
            Log.v(Constants.LOG_TAG, "Singleton::createSerialSocketThread already created");
            return true;
        }
        socketThread = new SerialSocket();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(addr);
        if (socketThread.connect(context,addr,device))
            // If the connection has been done, add the thread to the list
            addSerialSocketThread(addr, socketThread);
        else
            return false;
        return true;
    }

    public static synchronized void destroySerialSocketThread(String addr)
    {
        SerialSocket socketThread = getSerialSocketThread(addr);
        if (socketThread!=null)
            socketThread.disconnect();
    }

    public static synchronized Boolean sendData(String addr, byte[] data)
    {
        SerialSocket socketThread = getSerialSocketThread(addr);
        if (socketThread==null)
            return false;
        try {
            socketThread.write(data);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
