package io.github.edufolly.flutterbluetoothserial.le;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import io.github.edufolly.flutterbluetoothserial.BluetoothConnection;
import io.github.edufolly.flutterbluetoothserial.BluetoothConnectionBase;
import io.github.edufolly.flutterbluetoothserial.BluetoothConnectionBase.OnReadCallback;
import io.github.edufolly.flutterbluetoothserial.BluetoothConnectionBase.OnDisconnectedCallback;

public class BluetoothConnectionLE extends BluetoothConnectionBase {
    public enum Connected { False, Pending, True } //DUMMY IDE claiming non-accessible

    private Connected connected = Connected.False;
    private SerialSocket socket;
    private Context ctx;

    public BluetoothConnectionLE(OnReadCallback onReadCallback, OnDisconnectedCallback onDisconnectedCallback, Context appCtx) {
        super(onReadCallback, onDisconnectedCallback);
        this.ctx = appCtx;
    }

    @Override
    public boolean isConnected() {
        return connected == Connected.True;
    }

    @Override
    public void connect(String address, UUID uuid) throws IOException {
        connect(address); // Ignore the uuid, not used
    }

    @Override
    public void connect(String address) throws IOException {
        if (isConnected()) {
            throw new IOException("already connected");
        }
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            System.out.println("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(ctx, device);

            Object[] connectionResult = {null};
            CountDownLatch done = new CountDownLatch(1);
            //RAINY //THINK Make sure the semantics here are the same as with the classic bluetooth
            socket.connect(new io.github.edufolly.flutterbluetoothserial.le.SerialListener() {
                @Override
                public void onSerialConnect() {
                    //System.out.println("onSerialConnect");
                    connectionResult[0] = true;
                    done.countDown();
                }

                @Override
                public void onSerialConnectError(Exception e) {
                    //System.out.println("onSerialConnectError:");
                    e.printStackTrace();
                    connectionResult[0] = e;
                    done.countDown();
                    BluetoothConnectionLE.this.onDisconnected(true); //CHECK byRemote?  Also, should this happen on initial connection attempt?
                }

                @Override
                public void onSerialRead(byte[] data) {
                    BluetoothConnectionLE.this.onRead(data);
                }

                @Override
                public void onSerialRead(ArrayDeque<byte[]> datas) {
                    for (byte[] data : datas) {
                        BluetoothConnectionLE.this.onRead(data);
                    }
                }

                @Override
                public void onSerialIoError(Exception e) {
                    throw new RuntimeException("//DUMMY", e); //THINK send connection error or what?
                }
            });
            //System.out.println("awaiting connect done...");
            done.await(); //DUMMY Timeout?
            //System.out.println("...connect done");
            if (connectionResult[0] instanceof Exception) {
                //System.out.println("with connect error");
                throw (Exception)connectionResult[0];
            }
            //System.out.println("with connect success");
            this.socket = socket;
            connected = Connected.True;
        } catch (Exception e) {
            //System.err.println("connection failed: " + e.getMessage());
            try {
                disconnect(); //THINK Is this correct still?
            } finally {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void disconnect() {
        if (isConnected()) {
            connected = Connected.False; // ignore data,errors while disconnecting
            //cancelNotification(); //DUMMY
            if (socket != null) {
                socket.disconnect();
                socket = null;
            }
        }
    }

    @Override
    public void write(byte[] data) throws IOException {
        if (!isConnected()) {
            throw new IOException("not connected");
        }
        socket.write(data);
    }
}
