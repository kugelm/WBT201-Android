package de.thgk.wbt20x.app;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.UUID;
import java.util.Vector;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 *
 */
public class ConnectionService extends IntentService {

    private static String TAG = "WBT201_APP";

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_LIST = "de.thgk.wbt20x.app.action.LIST";
    private static final String ACTION_FETCH = "de.thgk.wbt20x.app.action.FETCH";
    private static final String ACTION_ERASE = "de.thgk.wbt20x.app.action.ERASE";

    private static final String EXTRA_MAC = "de.thgk.wbt20x.app.extra.MAC";

    public static File downloadDir
            = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "WBT201");
    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionList(Context context, String mac) {

        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(ACTION_LIST);
        intent.putExtra(EXTRA_MAC, mac);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFetch(Context context, String mac) {
        if (!downloadDir.exists()) {
            downloadDir.mkdir();
        }
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(ACTION_FETCH);
        intent.putExtra(EXTRA_MAC, mac);
        //intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionErase(Context context, String mac) {
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(ACTION_ERASE);
        intent.putExtra(EXTRA_MAC, mac);
        //intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public ConnectionService() {
        super("ConnectionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LIST.equals(action)) {
                final String mac = intent.getStringExtra(EXTRA_MAC);
                handleActionList(mac);
            } else if (ACTION_FETCH.equals(action)) {
                final String mac = intent.getStringExtra(EXTRA_MAC);
                handleActionFetch(mac);
            } else if (ACTION_ERASE.equals(action)) {
                final String mac = intent.getStringExtra(EXTRA_MAC);
                handleActionErase(mac);
            }
        }
    }

    private void feedback(String txt) {
        //Log.d(TAG, "feedback = "+txt);
        Intent bc = new Intent(MainActivity.ACTION_FEEDBACK);
        bc.putExtra(MainActivity.EXTRA_MSG, txt);
        //lbm.sendBroadcast(bc);
        sendBroadcast(bc);
    }

    private void finalize(boolean success) {
        //Log.d(TAG, "feedback = "+txt);
        Intent bc = new Intent(MainActivity.ACTION_RESULT);
        bc.putExtra(MainActivity.EXTRA_SUCCESS, success);
        //lbm.sendBroadcast(bc);
        sendBroadcast(bc);
    }

    private WBT201 gpsDevice = null;

    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;

    private BluetoothAdapter BA = BluetoothAdapter.getDefaultAdapter();
    // Well known SPP UUID (will *probably* map to
    // RFCOMM channel 1 (default) if not in use);
    // see comments in onResume().
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private boolean connectGpsDevice(String macAddress) {

        BluetoothDevice wbt201 = BA.getRemoteDevice(macAddress);

        // We need two things before we can successfully connect
        // (authentication issues aside): a MAC address, which we
        // already have, and an RFCOMM channel.
        // Because RFCOMM channels (aka ports) are limited in
        // number, Android doesn't allow you to use them directly;
        // instead you request a RFCOMM mapping based on a service
        // ID. In our case, we will use the well-known SPP Service
        // ID. This ID is in UUID (GUID to you Microsofties)
        // format. Given the UUID, Android will handle the
        // mapping for you. Generally, this will return RFCOMM 1,
        // but not always; it depends what other BlueTooth services
        // are in use on your Android device.
        try {
            btSocket = wbt201.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "CONNECT LOGGER: Socket creation failed.", e);
            feedback(getString(R.string.string_cant_establish_connection));
            return false;
        }

        // Discovery may be going on, e.g., if you're running a
        // 'scan for devices' search from your handset's Bluetooth
        // settings, so we call cancelDiscovery(). It doesn't hurt
        // to call it, but it might hurt not to... discovery is a
        // heavyweight process; you don't want it in progress when
        // a connection attempt is made.
        BA.cancelDiscovery();

        // Blocking connect, for a simple client nothing else can
        // happen until a successful connection is made, so we
        // don't care if it blocks.
        try {
            btSocket.connect();
            Log.e(TAG, "CONNECT LOGGER: BT connection established, data transfer link open.");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.e(TAG,
                        "CONNECT LOGGER: Unable to close socket during connection failure", e2);
            }
            feedback(getString(R.string.string_connection_error));
            return false;
        }

        // Create a data stream so we can talk to server.

        try {
            outStream = btSocket.getOutputStream();
            inStream = btSocket.getInputStream();

            gpsDevice = new WBT201(inStream, outStream, this);

        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.e(TAG,
                        "CONNECT LOGGER: Unable to close socket during connection failure", e2);
            }
            feedback(getString(R.string.string_connection_error));
            Log.e(TAG, "CONNECT LOGGER: In/Output stream creation failed.", e);
            return false;
        }

        try {
            if (gpsDevice.login() || gpsDevice.login()) {
                feedback(getString(R.string.string_login_ok));
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            feedback(getString(R.string.string_login_failed));
            Log.e(TAG, "CONNECT LOGGER: Login failed.", e);
            return false;
        }


        return true;
    }

    private boolean getDeviceInfo() {
        try {
            feedback("Device : " + gpsDevice.getName() + "\n");
            feedback("SN : " + gpsDevice.getSerialNumber() + "\n");
            feedback("SW : " + gpsDevice.getSwVersion() + "\n");
            feedback("HW : " + gpsDevice.getHwVersion() + "\n");
            feedback(getString(R.string.string_points_approx) + gpsDevice.getPointCount() + "\n");
            feedback(getString(R.string.string_fillheight) +
                    (1000*gpsDevice.bytesToBeRead())/(gpsDevice.getCapacity())/10f + " %\n");

        } catch (IOException e) {
            e.printStackTrace();
            feedback(getString(R.string.string_connection_error));
            return false;
        }
        return true;
    }

    private boolean getTrackList() {

        Vector tracks = gpsDevice.getTracks();
        for (Enumeration e = tracks.elements(); e.hasMoreElements();) {
            Track tr = (Track) e.nextElement();
            if (tr == null) return true;
            int d = (int) tr.getDistance() * 10;
            Date date = tr.getStartDate();
            Log.d(TAG, "Track " + d/10f);
            if (tr.numberOfPoints()>0 && d > 0) {
                feedback(date + ": " + d / 10f + "km\n");
            }
        }

        return true;
    }

    void closeConnection() {
        if (gpsDevice==null) return;
        try {
            gpsDevice.logout();
        } catch (IOException e) {
            Log.e(TAG, "CONNECT LOGGER: Logout failed.", e);
            feedback(getString(R.string.string_connection_error));
        }

        try {
            btSocket.close();
        } catch (IOException e2) {
            Log.e(TAG,
                    "CONNECT LOGGER: Unable to close socket", e2);
            //feedback("Verbindungsfehler!\n");
        }
        gpsDevice = null;

        feedback(getString(R.string.string_connection_terminated));

    }

    /**
     * Handle action LIST in the provided background thread with the provided
     * parameters.
     */
    private void handleActionList(String mac) {

        boolean success = false;

        if (gpsDevice!=null) return; // wir sind schon beschäftigt..

        if (connectGpsDevice(mac)) {
            // erfolgreich!
            success = getDeviceInfo();

        } else {
            // fehlgeschlagen...
        }
        
        closeConnection();
        finalize(success);
    }

    /**
     * Handle action FETCH in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFetch(String mac) {

        boolean success = true;
        if (gpsDevice!=null) return; // wir sind schon beschäftigt..

        if (connectGpsDevice(mac)) {
            // erfolgreich!

            try {
                success = getDeviceInfo();
                gpsDevice.readAndWrite();
                getTrackList();
            } catch (IOException e) {
                e.printStackTrace();
                success = false;
            }


        } else {
            // fehlgeschlagen...
            success = false;
        }

        closeConnection();
        finalize(success);
    }

    /**
     * Handle action ERASE in the provided background thread with the provided
     * parameters.
     */
    private void handleActionErase(String mac) {

        boolean success = true;
        if (gpsDevice!=null) return; // wir sind schon beschäftigt..

        if (connectGpsDevice(mac)) {
            // erfolgreich!
            try {
                gpsDevice.sendCommand("@AL,05,06");
                success = getDeviceInfo();
            } catch (IOException e) {
                e.printStackTrace();
                feedback(getString(R.string.string_clear_failed));
                success = false;
            }

        } else {
            // fehlgeschlagen...
        }

        closeConnection();
        finalize(success);
    }


}
