# BluetoothSerialForTasker
A plugin for Tasker (https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm) to communicate with a Bluetooth device using the Serial Port Profile (SPP). SPP is a protocole for serial communication over Bluetooth. For instance, it can be used to send/receive data to/from an arduino or ESP32 using Android. Make sure to pair the device first in Android.

With this pugin, you can:
- Connect, disconnect and send data to a Bluetooth device using a Tasker Action.
- Receive data from the Bluetooth device using a Tasker Profile.

This plugin can use Tasker variables for the address of the Bluetooth device and the data to send or receive. The format of the transmitted data can be either string or hex.

