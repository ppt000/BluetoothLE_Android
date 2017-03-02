package com.empiluma.bluetoothle;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.BluetoothAdapter.checkBluetoothAddress;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

/**
 * Created by PierPaolo on 01/12/2016.
 *
 * This service takes care of the Bluetooth connection.
 */

public class BLEService extends IntentService {
  /** Utility to stop the service
   * In order to have a reliable way to stop the service at any time, we implement boolean variable,
   * the relevant getters and setters (synchronized as they can be accessed from anywhere), and a
   * Runnable that will periodically check the boolean; if it finds the boolean set to true, it launches
   * the end of the service through a simple function call, if not it posts itself for some time later.
   */
  private static boolean mInterrupt = false;
  public static synchronized void setBLEServiceInterrupt(boolean b) {mInterrupt = b;}
  public static synchronized boolean getBLEServiceInterrupt() {return mInterrupt;}
  private static final int INTERRUPT_CHECK_PERIOD = 1000;
  Runnable checkInterrupt = new Runnable() {
    @Override
    public void run() {
      if (getBLEServiceInterrupt()) {endService();}
      else {mHandler.postDelayed(checkInterrupt, INTERRUPT_CHECK_PERIOD);}
    }
  };

  // Debugging
  private static final String TAG = "***BLEService";


  // Time (in ms) allowed to scan, stops scanning after that
  private static final long SCAN_PERIOD = 5000;

  // Member fields
  private Handler mHandler = new Handler();
  private BluetoothLeScanner mScanner = null;
  private boolean mScanning = false;
  private BluetoothGatt mGatt = null; // mGatt is used to check if a connection is active (!=null) or not (==null)
  private BLEData mBLEData = new BLEData();

  public BLEService() {super("BLEService");}  // don't know why that is needed but apparently it is

  //==============================================================================================================

  private void endService() {
    Log.d(TAG, " endService");
    if (mScanning) {mScanner.stopScan(mScanCallback);mScanning=false;} // we assume stopScan works always as it does not seem to call the callback
    if (mGatt != null) {
      mBLEData.endSubscriptions(mGatt);
      mGatt.disconnect();
      // mGatt.close();  // TODO: check if it is ok to delete this call; it's done in the callback now.
    }
    sendMessageToUI(Constants.MESSAGE_STATE_CHANGE_DISCONNECTED);
    // No need to call stopSelf() as it does not seem to stop anything anyway...
  }

  public ScanCallback mScanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      if (mGatt != null) return;
      Log.d(TAG, "Callback Type <" + String.valueOf(callbackType) + "> - Result <" + result.toString() + ">.");
      BluetoothDevice btDevice = result.getDevice();
      Log.d(TAG, "Device name <" + btDevice.getName() + "> - Device address <" + btDevice.getAddress() + ">.");
      // Connect to device
      mGatt = btDevice.connectGatt(getApplication(), false, mGattCallback);
      // stop scanning anyway after a ScanResult, as we are looking for one device only and it has been found
      mScanner.stopScan(mScanCallback); mScanning = false;
    }

    // This one never seems to be called...
    @Override
    public void onBatchScanResults(List<ScanResult> results) {for (ScanResult sr : results) {Log.d(TAG, "ScanResult - Results" + sr.toString());} }

    @Override
    public void onScanFailed(int errorCode) {Log.e(TAG, "Scan Failed " + errorCode);}
  };

  private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      Log.d(TAG, "onConnectionStateChange Status: <" + status + ">.");
      if (status != BluetoothGatt.GATT_SUCCESS) {
        sendMessageToUI(Constants.ERROR_CONNECTING);
        mGatt.disconnect();mGatt.close(); mGatt = null;
        Log.d(TAG, "onConnectionStateChange returned UNSUCCESSFUL");
        return;
      }
      switch (newState) {
        case BluetoothProfile.STATE_CONNECTED:
          sendMessageToUI(Constants.MESSAGE_STATE_CHANGE_CONNECTED);
          Log.d(TAG, "mGattCallback STATE_CONNECTED");
          if (!gatt.discoverServices()) {Log.d(TAG, "discoverServices returned FALSE");} // TODO: deal with error
          break;
        case BluetoothProfile.STATE_DISCONNECTED:
          sendMessageToUI(Constants.MESSAGE_STATE_CHANGE_DISCONNECTED);
          Log.d(TAG, "mGattCallback STATE_DISCONNECTED");
          mGatt.close(); mGatt = null;  // TODO: should this be here?
          break;
        default: // TODO: deal with this case
          Log.d(TAG, "mGattCallback STATE_OTHER");
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        mBLEData.checkGattAndSubscribe(gatt);
      } else {Log.w( TAG, "onServicesDiscovered received: " + status);}
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      // Don't do anything for now here, should not be called.
      Log.d(TAG, "onCharacteristicRead <" + characteristic.toString() + ">.");
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
      Intent data = mBLEData.getCharacteristic(characteristic);
      if (data == null) return;
      LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(data);
    }
  };

  //==============================================================================================================

  private void sendMessageToUI(int msg) {
    LocalBroadcastManager.
      getInstance(this).
      sendBroadcast(new Intent(Constants.ACTION_BROADCAST).
        putExtra(Constants.ID_MESSAGE, msg));
  }

  Runnable delayedStopScan = new Runnable() {
    @Override
    public void run() {
      Log.d( TAG, "Delayed Stop Scan - mScanning is <"+mScanning+">.");
      // if scanning is already stopped then it means some device has been found, so do nothing
      if (!mScanning) return;
      // stop the scan and send message that nothing was found
      mScanner.stopScan(mScanCallback); mScanning = false;
      sendMessageToUI(Constants.MESSAGE_SCAN_NOT_FOUND);
      }
  };

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.d(TAG, "BEGIN onHandleIntent");
    // Various preliminary checks
    BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    mScanner = mAdapter.getBluetoothLeScanner();
    if (!mAdapter.isEnabled()) {sendMessageToUI(Constants.ERROR_BLUETOOTH_DISABLED);return;}
    String mDeviceAddress = intent.getStringExtra(Constants.ID_DEVICE_ADDRESS);
    if (!checkBluetoothAddress(mDeviceAddress)) {sendMessageToUI(Constants.ERROR_ADDRESS_INVALID);return;}
    BluetoothDevice mBluetoothDevice = mAdapter.getRemoteDevice(mDeviceAddress);
    if (mBluetoothDevice.getBondState() != BOND_BONDED) {sendMessageToUI(Constants.ERROR_DEVICE_UNKNOWN);return;}
    // At this stage the Bluetooth Device should be 'usable'
    mHandler.postDelayed(checkInterrupt, INTERRUPT_CHECK_PERIOD); // Launch the interrupt checker
    // Not sure if BLE supports direct connection with a Device Address without going through scan first, so do it
    ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
    List<ScanFilter> filters = new ArrayList<> ();
    filters.add(new ScanFilter.Builder().setDeviceAddress(mDeviceAddress).build());
    // add here other filters if necessary. On profiles UUIDs for example.
    if (!mScanning) {  // the scan should be never running at this stage but as I don't fully understand the service...
      Log.d(TAG, "Start Scan");
      // Stops scanning after a pre-defined scan period.
      mHandler.postDelayed(delayedStopScan, SCAN_PERIOD);
      mScanning = true;
      mScanner.startScan(filters, settings, mScanCallback); // the callback from the scan will launch the connection
    }
    Log.d(TAG, "END onHandleIntent");
  }

  // onDestroy seems to be called after onHandleIntent even if other processes are running; it seems it could be called anytime...
  @Override
  public void onDestroy() {
    Log.d(TAG, " onDestroy BLEService");
    if (getBLEServiceInterrupt()) {endService();} // Don't do anything if onDestroy has been called 'by mistake'
  }
}