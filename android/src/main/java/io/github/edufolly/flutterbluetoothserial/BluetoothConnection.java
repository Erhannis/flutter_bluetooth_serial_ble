package io.github.edufolly.flutterbluetoothserial;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public interface BluetoothConnection {
    public boolean isConnected();
    /// Connects to given device by hardware address
    public void connect(String address, UUID uuid) throws Exception;
    /// Connects to given device by hardware address (default UUID used)
    public void connect(String address) throws Exception;
    /// Disconnects current session (ignore if not connected)
    public void disconnect();
    /// Writes to connected remote device
    public void write(byte[] data) throws IOException;
    /// Callback for reading data.
    public void onRead(byte[] data);
    /// Callback for disconnection.
    public void onDisconnected(boolean byRemote);
}
