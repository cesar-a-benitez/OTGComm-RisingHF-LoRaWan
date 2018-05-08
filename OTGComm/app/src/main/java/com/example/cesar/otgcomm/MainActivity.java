package com.example.cesar.otgcomm;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import android.os.Bundle;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.Map;


public class MainActivity extends Activity {

   // UsbDeviceConnection connection = null;
   // UsbSerialDevice serial = null;

    TextView tv;
    EditText command;
    Button SendBtn;

    String text = null;

    final int MAX_LENGTH = 255;
    final int timeout = 500;

    byte[] buffer = new byte[MAX_LENGTH];

    public static final String TAG = "OTGComm";
    UsbDeviceConnection connection = null;
    UsbSerialDevice serial;

    boolean syncMode = true;

    /**
     * Assync mode crashes and force application restart due to multiple mReadCallback interruptions
     */

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.sample_text);
        SendBtn = (Button) findViewById(R.id.SendBtn);
        command = (EditText) findViewById(R.id.CommandLine);

        tv.setMovementMethod(new ScrollingMovementMethod());


        getDevice();

        SendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               sendCommand();
        }});
    }

    private void sendCommand() {
        String toSend = command.getText().toString();

        if(toSend.toUpperCase() == "CLEAR" || toSend.toUpperCase() == "CLR") {
            tv.setText("");
        }else {
            try {
                if (syncMode == false) {
                    serial.write(toSend.getBytes());
                } else {
                    serial.syncWrite(toSend.getBytes(), timeout);
                    receiveSync();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error:\n" + e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    void getDevice() {
        UsbManager usbManager = getSystemService(UsbManager.class);
        Map<String, UsbDevice> connectedDevices = usbManager.getDeviceList();
        String print = null;
        for(UsbDevice device : connectedDevices.values()) {
            if(device.getVendorId() == 1155) {
                tv.setText("Device connected: " + device.getProductName() + "\nProductID: " + device.getProductId() + "\nVendorID:" + device.getVendorId() + "\n\n");
                SerialConnectionSetup(usbManager, device);
                break;
            }
        }

        if (tv.getText() == "") {
            tv.setText("No device found");
            return;
        }

        if (syncMode == false) {
            serial.write("AT\n".getBytes());
        }else {
            serial.syncWrite("AT\n".getBytes(), timeout);
            receiveSync();
        }
        tv.append("\n\n");
    }

    private void receiveSync() {
        try {
            while (true) {
                if (serial.syncRead(buffer, timeout) > 0) {
                    String received = null;
                    try {
                        received = new String(buffer, "UTF-8");
                        tv.append(received);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error:\n" + e.toString(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Error: " + e.toString());
                    }

                }else {
                    tv.append("\n\n");
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error:\n" + e.toString(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Error: " + e.toString());
        }
    }


    private void SerialConnectionSetup(UsbManager usbManager, UsbDevice device) {
        try {
            connection = usbManager.openDevice(device);
            serial = UsbSerialDevice.createUsbSerialDevice(device, connection);

            if(syncMode == false && serial != null && serial.open()){
                serial.setBaudRate(115200);
                serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serial.setParity(UsbSerialInterface.PARITY_NONE);
                serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                serial.read(mCallback);
            }else if (syncMode == true && serial != null && serial.syncOpen()) {
                serial.setBaudRate(115200);
                serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serial.setParity(UsbSerialInterface.PARITY_NONE);
                serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                //threadReceive();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
        }

    }

    UsbSerialInterface.UsbReadCallback mCallback = (data) -> {
        String dataStr = null;
        try {
            dataStr = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "Error: " + e.toString());
            Toast.makeText(getApplicationContext(), "Error:\n" + e.toString(), Toast.LENGTH_SHORT).show();
        }
        tv.append(dataStr);
    };

}
