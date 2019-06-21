package me.lebob.taskerbluetoothserial;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import me.lebob.taskerbluetoothserial.utils.Constants;


public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.v(Constants.LOG_TAG, "MainActivity::onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
}
