package me.toxicmushroom.fourhbbuttons;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.*;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    //Bluetooth objects
    private BluetoothAdapter bluetoothAdapter;
    private OutputStream bluetoothOutputStream;

    //Components
    private Button leftTopButton;
    private Button leftBottomButton;
    private Button rightTopButton;
    private Button rightBottomButton;

    private Button selectorButton;
    private TextView deviceTextView;

    private SeekBar speedSeekBar;
    private TextView speedTextView;

    private int speed = 255;
    private boolean registeredReceiver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setActionBar(myToolbar);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeAsUpIndicator(R.drawable.ic_bluetooth_buttons);
        }

        leftTopButton = findViewById(R.id.leftTopBtn);
        leftBottomButton = findViewById(R.id.leftBottomBtn);
        rightBottomButton = findViewById(R.id.rightBottomBtn);
        rightTopButton = findViewById(R.id.rightTopBtn);

        selectorButton = findViewById(R.id.selectBtn);
        deviceTextView = findViewById(R.id.deviceTextView);

        speedSeekBar = findViewById(R.id.speedSeekBar);
        speedTextView = findViewById(R.id.progressTV);

        selectorButton.setOnClickListener(selectorListener);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        addButtonListener(leftTopButton);
        addButtonListener(leftBottomButton);
        addButtonListener(rightTopButton);
        addButtonListener(rightBottomButton);

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speed = progress;
                speedTextView.setText(String.valueOf(speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (Variables.selectedDevice != null) openBluetoothTunnel();
    }

    private void addButtonListener(final Button button) {
        button.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                sendCode(button, hasFocus);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registeredReceiver) {
            unregisterReceiver(broadcastReceiver);
        }

        try {
            if (bluetoothOutputStream != null && bluetoothAdapter.isEnabled()) {
                bluetoothOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final View.OnClickListener selectorListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (bluetoothAdapter == null) {
                Toast.makeText(view.getContext(), "This device doesn't have bluetooth", Toast.LENGTH_LONG).show();
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                Intent bluetoothEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(bluetoothEnableIntent);

                IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(broadcastReceiver, intentFilter);
                registeredReceiver = true;
            } else {
                openDeviceSelector();
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) &&
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_ON) {
                openDeviceSelector();
            }
        }
    };

    private void openDeviceSelector() {
        List<String> options = new ArrayList<>();
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            Variables.deviceList.add(device);
            options.add(device.getName() + " - " + device.getAddress());
        }

        if (options.isEmpty()) {
            Toast.makeText(this, "You don't have any devices", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Select a device")
                .setIcon(R.drawable.ic_bluetooth_white_24dp)
                .setSingleChoiceItems(options.toArray(new String[0]), 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int index) {
                        if (index >= 0 && index < Variables.deviceList.size())
                            Variables.selectedDevice = Variables.deviceList.get(index);
                        else Variables.selectedDevice = Variables.deviceList.get(0);
                    }
                })
                .setPositiveButton("confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {
                        if (Variables.selectedDevice == null)
                            Variables.selectedDevice = Variables.deviceList.get(0);
                        openBluetoothTunnel();
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (id >= 0 && id < Variables.deviceList.size()) {
                            System.out.println(Variables.deviceList.get(id));
                        }
                    }
                })
                .create();
        alertDialog.show();
    }

    private void openBluetoothTunnel() {
        try {
            BluetoothSocket socket = Variables.selectedDevice.createRfcommSocketToServiceRecord(Variables.selectedDevice.getUuids()[0].getUuid());
            if (!socket.isConnected()) {
                Toast.makeText(this, "Connecting ...", Toast.LENGTH_SHORT).show();
                deviceTextView.setText(R.string.connecting);
                socket.connect();
            }

            bluetoothOutputStream = socket.getOutputStream();
            deviceTextView.setText(Variables.selectedDevice.getName());
            Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            deviceTextView.setText(R.string.select_a_device);
            Toast.makeText(this, "Failed to connect!", Toast.LENGTH_LONG).show();
        }
    }

    private void sendCode(Button btn, boolean focus) {
        if (bluetoothOutputStream == null) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (btn.getId() == leftBottomButton.getId()) {
                if (focus)
                    bluetoothOutputStream.write(("<" + (255 - speed) + ">").getBytes());
                else bluetoothOutputStream.write(("<" + 255 + ">").getBytes());

            } else if (btn.getId() == leftTopButton.getId()) {
                if (focus)
                    bluetoothOutputStream.write(("<" + (255 + speed) + ">").getBytes());
                else bluetoothOutputStream.write(("<" + 255 + ">").getBytes());

            } else if (btn.getId() == rightBottomButton.getId()) {
                if (focus)
                    bluetoothOutputStream.write(("<" + (766 - speed) + ">").getBytes());
                else bluetoothOutputStream.write(("<" + 766 + ">").getBytes());

            } else if (btn.getId() == rightTopButton.getId()) {
                if (focus)
                    bluetoothOutputStream.write(("<" + (766 + speed) + ">").getBytes());
                else bluetoothOutputStream.write(("<" + 766 + ">").getBytes());

            }
        } catch (IOException e) {
            Toast.makeText(this, "Err: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
