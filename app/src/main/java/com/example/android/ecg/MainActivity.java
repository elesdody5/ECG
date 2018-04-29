package com.example.android.ecg;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String Tag = "Main";
    private static double incomingData =0.0;
    BluetoothAdapter mBluetoothAdapter;
    private MenuItem menuItem;
    private final Handler mHandler = new Handler();
    private Runnable mTimer2;
    private LineGraphSeries<DataPoint> mSeries2;
    private double graph2LastXValue = 5d;
    private ArrayList<BluetoothDevice> DevicesList;
    private ListView listDeviceView;
    listDevicesAdapter adapter;
    private BluetoothConnectionService mBluetoothConnectionService;
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private BluetoothDevice mBTdevice;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            testBluetoothStatue(context, intent);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GraphView graph2 = (GraphView) findViewById(R.id.graph2);
        listDeviceView =  findViewById(R.id.list_device_view);
        DevicesList = new ArrayList<>();
        Switch connect = findViewById(R.id.connect_switch);
        mSeries2 = new LineGraphSeries<>();
        graph2.addSeries(mSeries2);
        graph2.getViewport().setXAxisBoundsManual(true);
        graph2.getViewport().setMinX(0);
        graph2.getViewport().setMaxX(40);
       /* graph2.getViewport().setYAxisBoundsManual(true);
        graph2.getViewport().setMinY(0);
        graph2.getViewport().setMaxY(100);*/
        //Broadcast when bound state change (pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        listDeviceView.setOnItemClickListener(this);
        registerReceiver(mReceiver, filter);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        connect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    startBTConnection(mBTdevice,MY_UUID_INSECURE);
            }
        });
    }

    private void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(Tag, "startBTConnection: Initializing RFCOM Bluetooth Connection.");
  if (device==null)
    Toast.makeText(this,"Choose device ",Toast.LENGTH_SHORT).show();
  else {
      mBluetoothConnectionService.startClient(device, uuid);
  }

    }

    @Override
    public void onResume() {
        super.onResume();


        mTimer2 = new Runnable() {
            @Override
            public void run() {
                graph2LastXValue += 1d;
                mSeries2.appendData(new DataPoint(graph2LastXValue, getRandom()), true, 40);
                mHandler.postDelayed(this, 200);
            }
        };
        mHandler.postDelayed(mTimer2, 1000);
    }

    @Override
    public void onPause() {

        mHandler.removeCallbacks(mTimer2);
        super.onPause();
    }

    private DataPoint[] generateData() {
        int count = 30;
        DataPoint[] values = new DataPoint[count];
        for (int i = 0; i < count; i++) {
            double x = i;
            double f = mRand.nextDouble() * 0.15 + 0.3;
            double y = Math.sin(i * f + 2) + mRand.nextDouble() * 0.3;
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }
        return values;
    }

    double mLastRandom = 2;
    Random mRand = new Random();

    private double getRandom() {

        if(mBluetoothConnectionService!=null)

        incomingData=Double.parseDouble(mBluetoothConnectionService.getIncomingMessage());

        return incomingData;
    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bluetooth_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        menuItem = item;
        int id = item.getItemId();
        switch (id) {
            case R.id.on_off_button:
                changeBluetooth();
                break;
            case R.id.scan_button:
                scanDevices();
                break;


        }


        return super.onOptionsItemSelected(item);
    }

    private void changeBluetooth() {

        if (menuItem.isChecked()) {

            menuItem.setChecked(false);
            Log.d(Tag, "Disable");
            mBluetoothAdapter.disable();
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiver, filter);

        } else {
            menuItem.setChecked(true);
            // to open bluetooth and make it discoverable for 200 sec
            Intent dicoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            dicoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 200);
            startActivity(dicoverIntent);
            // create intent filter and pass it to Broadcast Reciver to test the bluetooth statue
            IntentFilter dicoverFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            registerReceiver(mReceiver, dicoverFilter);
            scanDevices();
        }

    }

    private void scanDevices() {

        // Cancel discovery because it otherwise slows down the connection.
        if(mBluetoothAdapter.isDiscovering())
        {
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter.startDiscovery();
            IntentFilter decoverDevicesIntent= new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver,decoverDevicesIntent);
        }
        else
        {
            // to check manifest permission
            /* checkBTPermission();*/
            // need permission if api greater than lollipop
            Log.d(Tag,"listDevice");
            mBluetoothAdapter.startDiscovery();
            IntentFilter decoverDevicesIntent= new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver,decoverDevicesIntent);
        }
        Snackbar.make(getWindow().getDecorView().findViewById(android.R.id.content), "Scanning", Snackbar.LENGTH_LONG).show();

    }

    private void testBluetoothStatue(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {

                case BluetoothAdapter.STATE_TURNING_OFF:
                    Snackbar.make(getWindow().getDecorView().findViewById(android.R.id.content), "Turning off", Snackbar.LENGTH_LONG).show();

                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Snackbar.make(getWindow().getDecorView().findViewById(android.R.id.content), "Turning on ", Snackbar.LENGTH_LONG).show();

                    break;


            }
        } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //if (!DevicesList.contains(device))
              DevicesList.add(device);
            adapter = new listDevicesAdapter(context,R.layout.list_devices,DevicesList);

          // Apply the adapter to the listview
            listDeviceView.setAdapter(adapter);


        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
        mBluetoothAdapter.cancelDiscovery();
        String name = DevicesList.get(i).getName();
        String  address = DevicesList.get(i).getAddress();
        Log.d(Tag,"name "+name + "address"+address);
        // to check version must greater than jelly bean (not important her)
        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            Log.d(Tag,"Trying to pairing with "+name);
            DevicesList.get(i).createBond();
            mBTdevice =DevicesList.get(i);
        }
        mBluetoothConnectionService= new BluetoothConnectionService(this);
    }

}


