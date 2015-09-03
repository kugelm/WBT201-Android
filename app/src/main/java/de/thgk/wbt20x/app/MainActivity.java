package de.thgk.wbt20x.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Set;


public class MainActivity extends ActionBarActivity {

    private static String TAG = "WBT201_APP";

    public static final String ACTION_FEEDBACK = "de.thgk.wbt20x.app.action.FEEDBACK";
    public static final String EXTRA_MSG = "de.thgk.wbt20x.app.extra.MSG";
    public static final String ACTION_RESULT = "de.thgk.wbt20x.app.action.RESULT";
    public static final String EXTRA_SUCCESS = "de.thgk.wbt20x.app.extra.SUCCUESS";
    public static final String ACTION_PROGRESS = "de.thgk.wbt20x.app.action.PROGRESS";
    public static final String EXTRA_PERCENTAGE = "de.thgk.wbt20x.app.extra.PERCENTAGE";


    private int cmdLogger = 0;
    private static final int IDLE = 0;
    private static final int CMD_TEST = 1;
    private static final int CMD_FETCH_TRACKS = 2;
    private static final int CMD_ERASE = 3;

    private BluetoothAdapter BA = BluetoothAdapter.getDefaultAdapter();
    private static final int SWITCHON_BLUETOOTH = 1;

    private static TextView feedback;

    private boolean isListening = false;
    public BroadcastReceiver feedbackReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_FEEDBACK.equals(intent.getAction())) {
                final String msg = intent.getStringExtra(EXTRA_MSG);
                feedback.append(msg);
            }
            if (ACTION_RESULT.equals(intent.getAction())) {
                final boolean success = intent.getBooleanExtra(EXTRA_SUCCESS, false);
                if (success) {
                    // last cmd was ok
                }
                cmdLogger = IDLE;
            }
            if (ACTION_PROGRESS.equals(intent.getAction())) {
                final int p = (int)intent.getFloatExtra(EXTRA_PERCENTAGE, (float) 0.0);
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                Log.d(TAG, "progress = "+p+"/"+progressBar.getMax());
                progressBar.setProgress(p);
            }
        }

    };

    private static String macAddrOfGpsLogger = "XX:XX:XX:XX:XX:XX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (BA == null) {
            Toast.makeText(this,
                    R.string.string_bt_unavailable,
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("WBT201", Context.MODE_PRIVATE);


        final Button connectButton = (Button) findViewById(R.id.buttonConnect);
        connectButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cmdLogger != IDLE) {
                    Toast.makeText(getApplicationContext(), getString(R.string.string_still_busy), Toast.LENGTH_LONG).show();
                    return;
                }
                if (!BA.isEnabled()) {
                    cmdLogger = CMD_FETCH_TRACKS;
                    Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnOn, SWITCHON_BLUETOOTH);
                    //Toast.makeText(getApplicationContext(), "Turned on"
                    //        , Toast.LENGTH_LONG).show();
                }
                else{
                    if (!connectToGpsLogger()) {
                        feedback.append(getString(R.string.string_connect_failed));
                        cmdLogger = IDLE;
                    } else {
                        cmdLogger = CMD_FETCH_TRACKS;
                        ConnectionService.startActionFetch(getApplicationContext(), macAddrOfGpsLogger);
                    }
                }


            }
        });

        feedback = (TextView) findViewById(R.id.feedbackTextView);
        feedback.setText("WBT201 Logger App\n");
        feedback.append("=====================\n");
        feedback.append(getString(R.string.string_GPL));
        feedback.append("https://github.com/kugelm/WBT201-Android");
        feedback.append(getString(R.string.string_J2ME));
        feedback.append("   (c)2008, Dirkjan Krijnders\n      <dirkjan@krijnders.net>\n");
        feedback.append(getString(R.string.string_dir_chooser_reference));
        //feedback.append("    ");
        feedback.append(getString(R.string.string_COPL));
        feedback.append("\n");

        if (! isListening ) {
            registerReceiver(feedbackReceiver, new IntentFilter(ACTION_FEEDBACK));
            registerReceiver(feedbackReceiver, new IntentFilter(ACTION_PROGRESS));
            registerReceiver(feedbackReceiver, new IntentFilter(ACTION_RESULT));
            isListening = true;
        }

        String prefDownloadPath = prefs.getString("trackDir", null);
        if (prefDownloadPath!=null) {
            File prefDir = new File(prefDownloadPath);
            if (prefDir.isDirectory()) {
                ConnectionService.downloadDir = prefDir;
            }
        } else {
            File osmandDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "osmand");
            File osmTrackDir = new File(osmandDir.getPath(), "tracks");
            if (osmandDir.exists()) {
                if (!osmTrackDir.exists()) {
                    osmTrackDir.mkdir();
                }
                if (osmTrackDir.isDirectory()) {
                    ConnectionService.downloadDir = osmTrackDir;
                }
            }
        }
        feedback.append(getString(R.string.string_track_download_to));
        feedback.append(ConnectionService.downloadDir.getAbsolutePath()+"\n");

    }

    @Override
    public void onResume() {
        super.onResume();

        if (! isListening ) {
            registerReceiver(feedbackReceiver, new IntentFilter(ACTION_FEEDBACK));
            registerReceiver(feedbackReceiver, new IntentFilter(ACTION_PROGRESS));
            registerReceiver(feedbackReceiver, new IntentFilter(ACTION_RESULT));
            isListening = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (isListening) {
            unregisterReceiver(feedbackReceiver);
            isListening = false;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_erase) {
            if (cmdLogger != IDLE) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.string_still_busy), Toast.LENGTH_LONG).show();
                return true;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.string_really_erase));
            builder.setCancelable(false);
            builder.setPositiveButton(getString(R.string.string_erase_ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (BA.isEnabled()) {
                                cmdLogger = CMD_ERASE;
                                connectToGpsLogger();
                                ConnectionService.startActionErase(getApplicationContext(), macAddrOfGpsLogger);
                            } else {
                                Toast.makeText(getApplicationContext(),
                                       getString(R.string.string_missed_swon_bt),
                                        Toast.LENGTH_LONG).show();
                            }

                        }
                    });
            builder.setNegativeButton(getString(R.string.string_dont_erase),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;
        }
        if (id == R.id.action_test) {
            if (cmdLogger != IDLE) {
                Toast.makeText(getApplicationContext(), getString(R.string.string_still_busy), Toast.LENGTH_LONG).show();
                return true;
            }
            if (!BA.isEnabled()) {
                cmdLogger = CMD_TEST;
                Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnOn, SWITCHON_BLUETOOTH);
            }
            else{
                if (!connectToGpsLogger()) {
                    feedback.append(getString(R.string.string_connect_failed));
                    cmdLogger = IDLE;
                } else {
                    cmdLogger = CMD_TEST;
                    ConnectionService.startActionList(this, macAddrOfGpsLogger);
                }
            }
            return true;
        }
        if (id == R.id.action_dl_path) {
            // Create DirectoryChooserDialog and register a callback
            DirectoryChooserDialog directoryChooserDialog =
                    new DirectoryChooserDialog(MainActivity.this,
                            new DirectoryChooserDialog.ChosenDirectoryListener()
                            {
                                @Override
                                public void onChosenDir(String chosenDir)
                                {
                                    SharedPreferences prefs = getSharedPreferences("WBT201", Context.MODE_PRIVATE);

                                    File prefDir = new File(chosenDir);
                                    if (prefDir.isDirectory()) {
                                        ConnectionService.downloadDir = prefDir;
                                        SharedPreferences.Editor ed = prefs.edit();
                                        ed.putString("trackDir", chosenDir);
                                        ed.commit();
                                    }
                                    feedback.append(getString(R.string.string_track_download_to));
                                    feedback.append(ConnectionService.downloadDir.getAbsolutePath()+"\n");
                                }
                            });
            // Toggle new folder button enabling
            directoryChooserDialog.setNewFolderEnabled(true);
            // Load directory chooser dialog for initial 'm_chosenDir' directory.
            // The registered callback will be called upon final directory selection.
            directoryChooserDialog.chooseDirectory(/*ConnectionService.downloadDir.getAbsolutePath()*/"/");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == SWITCHON_BLUETOOTH) {
            if (!connectToGpsLogger()) {
                feedback.append(getString(R.string.string_connect_failed));
                cmdLogger = IDLE;
            } else {
                if (cmdLogger == CMD_TEST) {
                    ConnectionService.startActionList(this, macAddrOfGpsLogger);
                } else if (cmdLogger == CMD_FETCH_TRACKS) {
                    ConnectionService.startActionFetch(this, macAddrOfGpsLogger);
                }
            }
        }
    }

    public boolean connectToGpsLogger() {

        Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();
        BluetoothDevice wbt201 = null;
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            feedback.append(getString(R.string.string_known_bt_devices));
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                Log.d(TAG, "paired bt dev: " + device.getName() + " - " + device.getAddress());

                if (device.getName().matches("G-Rays2")) {
                    if (wbt201!=null) {
                        feedback.append( getString(R.string.string_multiple_loggers));
                    }
                    wbt201 = device;
                    feedback.append( "  ** "+device.getName() + " **\n");
                } else {
                    feedback.append( "   ( "+device.getName() + " )\n");
                }
            }
        } else {
            feedback.append( getString(R.string.string_no_matching_bt_logger));
            return false;
        }

        if (wbt201 != null) {
            macAddrOfGpsLogger = wbt201.getAddress();
            feedback.append(getString(R.string.string_connecting_with) + macAddrOfGpsLogger + " ...\n");
        } else {
            Toast.makeText(this,
                    getString(R.string.string_no_matching_bt_logger),
                    Toast.LENGTH_LONG).show();
            return false;
        }


        return true;
    }


}
