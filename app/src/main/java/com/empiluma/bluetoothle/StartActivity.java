package com.empiluma.bluetoothle;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by PierPaolo on 15/11/2016.
 * This is the start screen for the app.
 * Ultimately it should only show a splash screen and test for the presence of the Bluetooth functionality.
 * For now, it also forces the selection of the type of bluetooth connection required: as a server or as a client.
 * This class only uses the MainActivity method setTypeServer to update the variable that records the type of
 *  bluetooth connection, which will not change for the whole life of the app.
 *
 */

public class StartActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if ((BluetoothAdapter.getDefaultAdapter() == null) || (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)))
    { // App can't run without Bluetooth LE
      new AlertDialog.Builder(this)
        .setTitle(R.string.alert_title_no_bluetooth)
        .setMessage(R.string.alert_message_no_bluetooth)
        .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {finish();}
        })
        .show();
    } else {
      Intent intent = new Intent(this, MainActivity.class);
      startActivity(intent);
      finish();
    }
  }
}

