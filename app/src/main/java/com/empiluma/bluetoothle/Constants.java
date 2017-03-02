package com.empiluma.bluetoothle;

/**
 * Created by PierPaolo on 16/11/2016.
 */

final class Constants {

  // Package Name
  private static final String PREFIX = "com.empiluma.bluetoothle.";  // TODO: Update package name in code

  // Action for Intent. There is only one.
  static final String ACTION_BROADCAST = PREFIX + "BROADCAST";

  // IDs for extra data put in intent.
  static final String ID_MESSAGE = PREFIX + "MESSAGE";
  static final String ID_DEVICE_ADDRESS = PREFIX + "DEVICE_ADDRESS";
  static final String ID_DEVICE_NAME = PREFIX + "DEVICE_NAME";
  static final String ID_STATE = PREFIX + "STATE";
  static final String ID_DATA = PREFIX + "DATA";
  static final String ID_DATATYPE = PREFIX + "DATA_TYPE";
  static final String ID_OUTPUT = PREFIX + "OUTPUT";

  // Name and keys for the shared preferences file
  static final String PREF_FILENAME = PREFIX + R.string.app_name;
  static final String PREF_KEY_DEVICE_ADDRESS = "device_address";
  static final String PREF_KEY_DEVICE_NAME = "device_name";

  // Message sent to the Receiver
  static final int MESSAGE_DATA_READ = 5;
  static final int MESSAGE_STATE_CHANGE_DISCONNECTED = 20;
  static final int MESSAGE_STATE_CHANGE_DISCONNECTING = 21;
  static final int MESSAGE_STATE_CHANGE_CONNECTING = 22;
  static final int MESSAGE_STATE_CHANGE_CONNECTED = 23;
  static final int NO_ERROR = 100;
  static final int ERROR_CONNECTING = 101;
  static final int ERROR_DISCONNECTED_READING = 103;
  static final int ERROR_BLUETOOTH_DISABLED = 105;
  static final int ERROR_ADDRESS_INVALID = 31;
  static final int ERROR_DEVICE_UNKNOWN = 32;
  static final int MESSAGE_SCAN_NOT_FOUND = 33;
}
