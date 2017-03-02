package com.empiluma.bluetoothle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static android.R.layout.simple_list_item_1;

/**
 * This activity selects a device out of a list of already paired devices.
 * It does not start a pairing process or any other bluetooth connection process.
 *
 */
public class DeviceSelection extends AppCompatActivity {
  private static final String TAG = "***DevSelection";  // Debugging
  private BluetoothAdapter mAdapter;
  private ArrayAdapter<String> mArrayAdapter;
  private ListView mListView;
  private List<BluetoothDevice> pairedDevicesList;   // Separate 'copy' list to make sure to retrieve the right selection.

  private AdapterView.OnItemClickListener mItemClickedHandler= new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView parent, View v, int position, long id) {
      // position = id = position of the selection in the array, starting from 0
      setResult(Activity.RESULT_OK, new Intent()
                                          .putExtra("address", pairedDevicesList.get(position).getAddress())
                                          .putExtra("name", pairedDevicesList.get(position).getName()));
      finish();
    }
  };

  // Error codes returned
  public static final int ERROR_BLUETOOTH_DISABLED = 1;
  public static final int ERROR_NO_PAIRED_DEVICES = 2;
  public static final int ERROR_NO_SELECTION = 3;

  /**
   * Populates the list with the already paired devices.
   * I had to create a duplicate list to be able to retrieve the right address once selection is made.
   * Maybe there is a better way to do it directly in the ArrayAdapter but I could not find it.
   *
   * @param savedInstanceState
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");
    setContentView(R.layout.activity_device_selection);
    mAdapter = BluetoothAdapter.getDefaultAdapter();
    if (!mAdapter.isEnabled()) {  // Bluetooth not enabled, can't proceed.
      setResult(Activity.RESULT_CANCELED, new Intent().putExtra("error_type",ERROR_BLUETOOTH_DISABLED ));
      finish();
    }
    Set<BluetoothDevice> pairedDevicesSet = mAdapter.getBondedDevices();  // Bluetooth needs to be enabled for this to work.
    if (pairedDevicesSet.size() == 0) {  // there are no devices paired
      Log.d(TAG, "No devices paired found");
      setResult(Activity.RESULT_CANCELED, new Intent().putExtra("error_type",ERROR_NO_PAIRED_DEVICES ));
      finish();
    } else {
      mArrayAdapter = new ArrayAdapter<String>(this, simple_list_item_1);
      mListView = (ListView) findViewById(R.id.listView1);
      mListView.setOnItemClickListener(mItemClickedHandler);
      mListView.setAdapter(mArrayAdapter);
      pairedDevicesList = new ArrayList<BluetoothDevice>();
      for (BluetoothDevice device : pairedDevicesSet) {
        // Add the name and address to an array adapter to show in a ListView
        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
        // Create the duplicate list at the same time in same order to retrieve the correct element afterwards
        pairedDevicesList.add(device);
      }
    }
  }

  @Override
  public void onBackPressed() {
    Log.d(TAG, "Back button pressed, no selection made.");
    setResult(Activity.RESULT_CANCELED,  new Intent().putExtra("error_type",ERROR_NO_SELECTION ));
    finish();
  }
}