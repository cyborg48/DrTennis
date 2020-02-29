package com.caroline.drtennis;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    public final String ACTION_USB_PERMISSION = "com.caroline.DrTennis.USB_PERMISSION";
    TextView acceleration, pitch, roll, test, textView;
    EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    double[] vals = {0.0, 0.0, 0.0};

    /***
    ProtocolBuffer buffer = new ProtocolBuffer(ProtocolBuffer.TEXT); //Also Binary
    buffer.setDelimiter("\r\n");
     ***/

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("\n");
                //tvAppend(textView, data);
                //TextView tv = findViewById(R.id.test);
                //tv.setText(data);

                try{
                    if(data.charAt(0) == 'A'){

                        vals[0] = (double)Integer.parseInt(data.substring(1));

                    } else if(data.charAt(0) == 'P'){

                        vals[1] = Double.parseDouble(data.substring(1));

                    } else if(data.charAt(0) == 'R'){

                        vals[2] = Double.parseDouble(data.substring(1));

                    }

                    update();
                } catch(Exception f){
                    //tvAppend(textView, "ERROR" + f.getMessage());
                }


                try{

                    /***
                    if(data.indexOf('*')!=-1){

                        String[] tempvals = data.split("\\*");
                        tvAppend(textView, "VALUES: "+ Arrays.toString(tempvals));
                        vals[0] = (double)Integer.parseInt(tempvals[0]);
                        vals[1] = Double.parseDouble(tempvals[1]);
                        vals[2] = Double.parseDouble(tempvals[2]);

                    }
                     ***/

                    tvAppend(textView, "Acc:  " + vals[0]);
                    tvAppend(textView, "Pitch:  " + vals[1]);
                    tvAppend(textView, "Roll:  " + vals[2] + "\n");


                } catch(Exception f){
                    //tvAppend(textView, "ERROR "+f.getMessage());

                }


                /***

                acceleration.setText("Acceleration: " + vals[0]);
                pitch.setText("Pitch: " + vals[1]);
                roll.setText("Roll: " + vals[2]);

                ***/

            } catch (UnsupportedEncodingException e) {
                //e.printStackTrace();
                //test.setText(e.getMessage());
            }


        }
    };


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView,"Serial Connection Opened!\n");

                        } else {
                            tvAppend(textView, "PORT NOT OPEN");
                        }
                    } else {
                        tvAppend(textView, "PORT IS NULL");
                    }
                } else {
                    tvAppend(textView, "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                //onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                //onClickStop(stopButton);

            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        test = findViewById(R.id.test);
        textView = findViewById(R.id.textView);
        acceleration = findViewById(R.id.acceleration);
        pitch = findViewById(R.id.pitch);
        roll = findViewById(R.id.roll);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

    }


    public void onClickStart(View view) {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }


    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    private void tvWrite(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.setText(ftext);
            }
        });
    }

    private void update(){
        tvWrite(acceleration, Double.toString(vals[0]));
        tvWrite(pitch, Double.toString(vals[1]));
        tvWrite(roll, Double.toString(vals[2]));
    }

}