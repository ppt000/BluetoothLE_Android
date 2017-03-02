package com.empiluma.bluetoothle;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * This is the main part of the app, where the UI resides.
 */
public class MainActivity extends AppCompatActivity {
  // The variables that define the state of the app
  private String mDeviceAddress = "";  // The device currently selected or "" if none.
  private String mDeviceName = "";  // Convenience to have the name of the device at hand
  // Debugging
  private static final String TAG = "***MainActivity";
  private SharedPreferences mSharedPref;
  // Receiver for Broadcasts from the Bluetooth Service
  private Intent mServiceIntent;
  private Receiver mReceiver;
  // UI
  private Menu appMenu;
  private TextView textView_Status = null;
  private void refreshStatus(String status) {if (textView_Status != null) textView_Status.setText("Status: <"+status+">.");}
  private TextView textView_DeviceAddress= null;
  private void refreshDeviceAddress() {if (textView_DeviceAddress != null) textView_DeviceAddress.setText("Device Address: <"+mDeviceAddress+">.");}
  private TextView textView_DeviceName= null;
  private void refreshDeviceName() { if (textView_DeviceName != null) textView_DeviceName.setText("Device Name: <"+mDeviceName+">."); }
  private TextView textView_Output = null;   // Text box for the ongoing messages from device.
  private TextView textView_Debug = null;

  // Menu 'inflation'
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    appMenu = menu;
    getMenuInflater().inflate(R.menu.action_bar, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    boolean connect = ((mState == STATE_CONNECTED)||(mState == STATE_CONNECTING));
    appMenu.findItem(R.id.action_connect).setEnabled(!connect).setVisible(!connect);
    appMenu.findItem(R.id.action_disconnect).setEnabled(connect).setVisible(connect);
    return super.onPrepareOptionsMenu(menu);
  }

  private void updateDeviceData(String address, String name) {
    if (address != null) {mDeviceAddress = address; refreshDeviceAddress();}
    if (name != null) {mDeviceName = name; refreshDeviceName();}
  }

  private static final int STATE_DISCONNECTED = 1;
  private static final int STATE_CONNECTED = 2;
  private static final int STATE_DISCONNECTING = 3;
  private static final int STATE_CONNECTING = 4;
  private int mState;
  // it would be better to wrap the state in a class so it cannot be accessed directly.
  private void updateState(int newState) {
    // if (mState == newState) return;  // Not sure that it matters if we refresh even when state doesn't change
    mState = newState;
    switch (mState) {
      case STATE_DISCONNECTED:
        refreshStatus(getString(R.string.status_disconnected));
        break;
      case STATE_CONNECTED:
        refreshStatus(getString(R.string.status_connected));
        break;
      case STATE_DISCONNECTING:
        refreshStatus(getString(R.string.status_disconnecting));
        break;
      case STATE_CONNECTING:
        refreshStatus(getString(R.string.status_connecting));
        break;
      default:
        break;
    }
    invalidateOptionsMenu(); // make sure the menu will be refreshed
  }

  private void startCommunication() {
    if (mState != STATE_DISCONNECTED) return;
    updateState(STATE_CONNECTING);
    Log.d(TAG, "Launching startService.");
    BLEService.setBLEServiceInterrupt(false);
    mServiceIntent.putExtra(Constants.ID_DEVICE_ADDRESS,mDeviceAddress);
    ComponentName cName = startService(mServiceIntent);
    Log.d(TAG, "startService launched and returned: <"+cName+">.");
    textView_Output.setText("");
  }

  private void stopCommunication() {
    Log.d( TAG, " stopComm()");
    if ((mState==STATE_DISCONNECTED)||(mState==STATE_DISCONNECTING)) return;
    updateState(STATE_DISCONNECTING);
    BLEService.setBLEServiceInterrupt(true);
    //boolean r = stopService(mServiceIntent);Log.d(TAG, "stopService returned: <"+r+">.");
  }

  private void appExit() {
    new AlertDialog.Builder(this)
      .setIcon(android.R.drawable.ic_dialog_alert)
      .setTitle(R.string.alert_title_exit)
      .setMessage(R.string.alert_message_exit)
      .setPositiveButton(R.string.alert_yes, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {stopCommunication();finish();}
      })
      .setNegativeButton(R.string.alert_no, null)
      .show();
  }

  // Coming from app launch or after app killed following onStop() and then user returns to activity
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");
    setContentView(R.layout.activity_main);
    Toolbar appToolbar = (Toolbar) findViewById(R.id.app_toolbar);
    setSupportActionBar(appToolbar);
    Toast.makeText(getApplicationContext(), R.string.welcome, Toast.LENGTH_SHORT).show();  // Welcome message
    textView_Status = (TextView) findViewById(R.id.textView_Status);
    textView_DeviceAddress = (TextView) findViewById(R.id.textView_DeviceAddress); refreshDeviceAddress();
    textView_DeviceName = (TextView) findViewById(R.id.textView_DeviceName); refreshDeviceName();
    textView_Output = (TextView) findViewById(R.id.textView_Output); textView_Output.setText("");
    textView_Debug = (TextView) findViewById(R.id.textView_Debug);  textView_Debug.setText("");
    mServiceIntent = new Intent(this, BLEService.class);
    mReceiver = new Receiver();
    // Check in the 'Preferences' file if there is already a default device to connect to or set it to "".
    mSharedPref = getSharedPreferences(Constants.PREF_FILENAME, MODE_PRIVATE);
    Log.d(TAG, "sharedPref Address: <"+mSharedPref.getString(Constants.PREF_KEY_DEVICE_ADDRESS, "")+">.");
    updateDeviceData(mSharedPref.getString(Constants.PREF_KEY_DEVICE_ADDRESS, ""),mSharedPref.getString(Constants.PREF_KEY_DEVICE_NAME, ""));
    updateState(STATE_DISCONNECTED);
    LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(Constants.ACTION_BROADCAST));
    if (savedInstanceState != null) {} // Check whether we're recreating a previously destroyed instance = TODO: REMOVE
  }

  // Coming from onCreate() or onRestart()
  @Override
  protected void onStart() {super.onStart();Log.d(TAG, "onStart()");}

  // Coming from onStart() or onPause() if user returns to activity
  @Override
  protected void onResume() {super.onResume(); Log.d(TAG, "onResume()");}

  // After onStop() and user returns to activity
  @Override
  protected void onRestart() {super.onRestart();Log.d(TAG, "onRestart()");}

  // Called if another activity comes into the foreground
  @Override
  protected void onPause() {super.onPause(); Log.d(TAG, "onPause()");}

  // Called if the activity is not visible anymore
  @Override
  protected void onStop() {super.onStop();Log.d(TAG, "onStop()");}

  // Called if the activity is definitely terminated
  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy()");
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    // TODO: Do we need to clean up anything here?
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);
    savedInstanceState.putString(Constants.ID_DEVICE_ADDRESS, mDeviceAddress);
    savedInstanceState.putString(Constants.ID_DEVICE_NAME, mDeviceName);
    savedInstanceState.putInt(Constants.ID_STATE, mState);
    Log.d(TAG, "textView_Output: <" +textView_Output.getText().toString()+">.");
    savedInstanceState.putString(Constants.ID_OUTPUT, textView_Output.getText().toString());
  }

  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    updateDeviceData(savedInstanceState.getString(Constants.ID_DEVICE_ADDRESS),savedInstanceState.getString(Constants.ID_DEVICE_NAME));
    updateState(savedInstanceState.getInt(Constants.ID_STATE));
    Log.d(TAG, "savedInstance Output: <"+savedInstanceState.getString(Constants.ID_OUTPUT)+">.");
    textView_Output.setText(savedInstanceState.getString(Constants.ID_OUTPUT));
  }

  // Local constants for StartActivityForResult:
  private static final int REQUEST_ENABLE_BT = 1;  // Activity to request to enable bluetooth
  private static final int SELECT_DEVICE = 2;  // Activity to select the device among paired devices
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch(requestCode) {
      case REQUEST_ENABLE_BT:
        // if (resultCode == RESULT_OK) {} else {}  // Nothing to do
        break;
      case SELECT_DEVICE:
        if (resultCode == RESULT_OK) {  // Device Selected
          updateDeviceData(data.getStringExtra("address"),data.getStringExtra("name"));
          // Write the address (and the name-nice to have)in the 'Preferences' file straight away
          SharedPreferences.Editor sharedPrefEditor = mSharedPref.edit();
          sharedPrefEditor
            .putString(Constants.PREF_KEY_DEVICE_ADDRESS, mDeviceAddress)
            .putString(Constants.PREF_KEY_DEVICE_NAME, mDeviceName);
          sharedPrefEditor.apply();
        } else {  // No selection was made or something else went wrong
          updateDeviceData("","");  // Probably the only thing to do in that case
          switch (data.getIntExtra("error_type", Constants.NO_ERROR)) {
            case DeviceSelection.ERROR_BLUETOOTH_DISABLED:
              new AlertDialog.Builder(this)
                .setTitle(R.string.alert_title_bluetooth_disabled)
                .setMessage(R.string.alert_message_bluetooth_disabled)
                .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {}
                })
                .show();
              break;
            case DeviceSelection.ERROR_NO_PAIRED_DEVICES:
              new AlertDialog.Builder(this)
                .setTitle(R.string.alert_title_no_device)
                .setMessage(R.string.alert_message_no_device)
                .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {}
                })
                .show();
              break;
            case DeviceSelection.ERROR_NO_SELECTION:
              // Do nothing
              break;
            default:
              new AlertDialog.Builder(this)
                .setTitle(R.string.alert_title_unknown_error)
                .setMessage(R.string.alert_message_unknown_error)
                .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {}
                })
                .show();
          }
        }
        break;
      default:
        break;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
     switch (item.getItemId()) {
       case R.id.action_enable_bt:
         Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
         startActivityForResult(enableIntent, REQUEST_ENABLE_BT);  // The 'Result' launches onResume() on exit.
         return true;
       case R.id.action_select_device:
         Intent selectIntent = new Intent( this, DeviceSelection.class);
         startActivityForResult(selectIntent, SELECT_DEVICE);
         return true;
       case R.id.action_connect:
         startCommunication();
         return true;
       case R.id.action_disconnect:
         stopCommunication();
         return true;
       case R.id.action_exit:
         appExit();
         return true;
       default:  // the user's action was not recognized. Invoke the superclass to handle it.
         Toast.makeText(getApplicationContext(), "Unrecognised action...", Toast.LENGTH_SHORT).show();
         return super.onOptionsItemSelected(item);
     }
  }

  @Override
  public void onBackPressed() {appExit();}

  private class Receiver extends BroadcastReceiver {
    private long t0 = System.currentTimeMillis(), t1;
    private Receiver() {}  // Prevents instantiation by other packages (?)
    public void onReceive(Context context, Intent intent) {
      switch(intent.getIntExtra(Constants.ID_MESSAGE, Constants.NO_ERROR)) {
        case Constants.ERROR_BLUETOOTH_DISABLED:
          new AlertDialog.Builder(MainActivity.this)
            .setTitle(R.string.alert_title_bluetooth_disabled)
            .setMessage(R.string.alert_message_bluetooth_disabled)
            .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {}
            })
            .show();
          stopCommunication();
          break;
        case Constants.ERROR_ADDRESS_INVALID:
          new AlertDialog.Builder(MainActivity.this)
            .setTitle(R.string.alert_title_invalid_address)
            .setMessage(R.string.alert_message_invalid_address)
            .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {}
            })
            .show();
          stopCommunication();
          break;
        case Constants.ERROR_DEVICE_UNKNOWN:
          new AlertDialog.Builder(MainActivity.this)
            .setTitle(R.string.alert_title_device_unknown)
            .setMessage(R.string.alert_message_device_unknown)
            .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {}
            })
            .show();
          stopCommunication();
          break;
        case Constants.MESSAGE_STATE_CHANGE_DISCONNECTED:
          updateState(STATE_DISCONNECTED);
          break;
        case Constants.MESSAGE_STATE_CHANGE_CONNECTING:
          updateState(STATE_CONNECTING);
          break;
        case Constants.MESSAGE_STATE_CHANGE_CONNECTED:
          updateState(STATE_CONNECTED);
          break;
        case Constants.MESSAGE_DATA_READ:
          t1 = System.currentTimeMillis();
          String newLine = String.format(Locale.getDefault(),"%7dms-%10.10s:%5.5s\n",t1-t0,
            intent.getStringExtra(Constants.ID_DATATYPE),
            intent.getStringExtra(Constants.ID_DATA));
          textView_Output.setText(appendOutput(textView_Output.getText().toString(), newLine));
          // TODO: process the read data - into a chart for example
          // TODO: start a timer to check within x minutes if another data has been received; if not it's a time-out
          t0 = t1;
          break;
        case Constants.MESSAGE_SCAN_NOT_FOUND:
          new AlertDialog.Builder(MainActivity.this)
            .setTitle("")
            .setMessage(R.string.alert_message_scan_not_found)
            .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {}
            })
            .show();
          stopCommunication();
          break;
        case Constants.ERROR_CONNECTING:
          new AlertDialog.Builder(MainActivity.this)
            .setTitle(R.string.alert_title_problem_connecting)
            .setMessage(R.string.alert_message_problem_connecting)
            .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {}
            })
            .show();
          stopCommunication();
          break;
        case Constants.ERROR_DISCONNECTED_READING:
          new AlertDialog.Builder(MainActivity.this)
            .setTitle(R.string.alert_title_problem_reading)
            .setMessage(R.string.alert_message_problem_reading)
            .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {}
            })
            .show();
          stopCommunication();
          break;
        case Constants.NO_ERROR:
        default: // All ok
          break;
      }
    }
  }

  // Helper to manage the Output view
  private String appendOutput(String body, String newline) {
    String[] array = body.split("\\n");
    String temp = newline;
    for (int i=0;(i<array.length) && (i<7); i++) {temp = temp + array[i] + "\n";}
    return temp;
  }
}