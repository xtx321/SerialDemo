package com.xtx.serialdemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jdcz.serial.SerialPort;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MainActivity";

    final   String  mTestString = "0123456789";
    private boolean isRead      = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        //        tv.setText(new SerialPort().stringFromJNI());
        findViewById(R.id.btn_send).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                send();
                break;
            case R.id.btn_stop:
                closeSerial();
            default:
                break;
        }
    }

    private void send() {
        serialTest("/dev/ttyS4");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeSerial();

    }

    private void closeSerial() {
        isRead = false;
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
        try {
            mOutputStream.close();
            mOutputStream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        mReadThread = null;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    String data = msg.getData().getString("RECV");
                    Log.d(TAG, "receiver result=" + data);
                    break;
                default:
                    break;
            }
        }
    };

    SerialPort       mSerialPort   = null;
    FileOutputStream mOutputStream = null;
    ReadThread       mReadThread   = null;

    public void serialTest(String device) {
        try {
            if (mSerialPort == null) {
                mSerialPort = new SerialPort(new File(device), 9600, 0);
                mOutputStream = (FileOutputStream) mSerialPort.getOutputStream();
                isRead  = true;
                mReadThread = new ReadThread(mSerialPort, device);
                mReadThread.start();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            //            mTextMsg.append(device + " SEND : " + mTestString + "\n");
            mOutputStream.write(mTestString.getBytes());
            Log.d(TAG, "send success data=" + mTestString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    class ReadThread extends Thread {
        SerialPort      serialPort  = null;
        String          device      = null;
        FileInputStream inputStream = null;


        ReadThread(SerialPort port, String device) {
            serialPort = port;
            device = device;
            inputStream = (FileInputStream) serialPort.getInputStream();
        }

        @Override
        public void run() {
            super.run();

            while (isRead) {
                readSerial();
                //                if (serialPort != null) {
                //                    serialPort.close();
                //                    serialPort = null;
                //                }

                //                if (!isInterrupted()) {
                //                    Log.e(TAG, "isInterrupted error");
                //                    isRead = false;
                //                    interrupt();
                //                }

            }
        }

        void readSerial() {
            int size;
            try {
                if (inputStream == null) {
                    Log.e(TAG, "inputStream is null");
                    return;
                }

//                size = inputStream.available();//该大小总是 0
//                if (size <= 0) {
////                    try {
////                        sleep(10);
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
//                    Log.e(TAG, "size is 0");
////                    return;
//                }

                byte[] buffer = new byte[256];
                size = inputStream.read(buffer);
                if (size > 0) {
                    String data = new String(buffer, 0, size);
                    Log.d(TAG, "receiver data=" + data);
                    if (mTestString.equals(data)) {
                        Message msg = new Message();
                        msg.what = 1;
                        Bundle bundle = new Bundle();
                        bundle.putString("RECV", new String(device
                                + " RECV : " + data));
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
