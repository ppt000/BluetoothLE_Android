package com.empiluma.bluetoothle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Class that defines the services and characteristics needed for this specific application.
 *
 * Created by PierPaolo on 25/01/2017.
 */

class BLEData {
  // Debugging
  private static final String TAG = "***BLEData";

  // Standard BaseUUID:
  //-private final String BaseUUID = "-0000-1000-8000-00805F9B34FB";
  // AirLib BaseUUID
  private final String BaseUUID = "-0000-1000-8000-F32AFD58F22F";
  // Services
  //-private final int HEART_RATE_SERVICE = 0x180D;
  //-private final int ENVIRONMENTAL_SENSING_SERVICE = 0x181A;
  private final int AIRLIB = 0x43478D00;
  // Characteristics
  //-private final int HEART_RATE_CHARACTERISTIC = 0x2A37;
  //-private final int ELEVATION_CHARACTERISTIC = 0x2A6C;
  //-private final int POLLEN_CONCENTRATION_CHARACTERISTIC = 0x2A75;
  //-private final int TEMPERATURE_CHARACTERISTIC = 0x2A6E;
  //-private final int TRUE_WIND_SPEED_CHARACTERISTIC = 0x2A70;
  private final int AIRLIB_CONTROL = 0x43478D01; // 5 octets
  private final int AIRLIB_STATUS =  0x43478D02; // 1 octet
  private final int AIRLIB_RED = 0x43478D03; // 4 octets
  private final int AIRLIB_OX = 0x43478D04; // 4 octets

  // Descriptor to enable configuration
  private final int CLIENT_CHAR_CONFIG = 0x2902;
  private final UUID CLIENT_CHAR_CONFIG_UUID = UUID.fromString(String.format("%08X",CLIENT_CHAR_CONFIG).concat(BaseUUID));

  private class DescriptorNode {
    BluetoothGattDescriptor gattDescriptor;
  }

  private class CharacteristicNode {
    int code;
    String description;
    UUID uuid;
    boolean supported = false;
    boolean subscribed = false;
    List<DescriptorNode> descriptors;

    CharacteristicNode(int cd, String desc) {
      code = cd;
      description = desc;
      uuid = UUID.fromString(String.format("%08X",cd).concat(BaseUUID));
      descriptors = new ArrayList<>();
    }
  }

  private class ServiceNode {
    int code;
    String description;
    UUID uuid;
    boolean supported = false;
    List<CharacteristicNode> characteristics;

    ServiceNode (int cd, String desc) {
      code = cd;
      description = desc;
      uuid = UUID.fromString(String.format("%08X",cd).concat(BaseUUID));
      characteristics = new ArrayList<>();
    }

    CharacteristicNode findCharacteristic(UUID Uuid){
      for (CharacteristicNode CN : characteristics) {if (Uuid.equals(CN.uuid)) return CN;}
      return null;
    }
  }

  private class RootNode {
    List<ServiceNode> services;

    RootNode(){services = new ArrayList<>();}

    ServiceNode findService(UUID Uuid){
      for (ServiceNode SN : services) {if (Uuid.equals(SN.uuid)) return SN;}
      return null;
    }
  }

  private RootNode Requirements = new RootNode();

  BLEData() { // TODO: Implement reading a tree representation like JSON or XML.
    ServiceNode s;

    // code for AirLib
    s = new ServiceNode(AIRLIB, "AirLib");
    s.characteristics.add(new CharacteristicNode(AIRLIB_CONTROL, "AirLib Control"));
    s.characteristics.add(new CharacteristicNode(AIRLIB_STATUS, "AirLib Status"));
    s.characteristics.add(new CharacteristicNode(AIRLIB_RED, "AirLib RED"));
    s.characteristics.add(new CharacteristicNode(AIRLIB_OX, "AirLib OX"));
    Requirements.services.add(s);
  }

  void checkGattAndSubscribe(BluetoothGatt gatt) {
    for (ServiceNode ser : Requirements.services) {
      // Check that the service we want is there
      BluetoothGattService mGattService = gatt.getService(ser.uuid);
      if (mGattService == null) {
        ser.supported = false;
        Log.d(TAG, "The Gatt service <"+ ser.description+"> wasn't found.");
        continue;
      }
      ser.supported = true;
      Log.d(TAG, "The Gatt service <"+ ser.description+"> was found.");
      for (CharacteristicNode ch : ser.characteristics) {
        BluetoothGattCharacteristic mGattCharacteristic = mGattService.getCharacteristic(ch.uuid);
        if (mGattCharacteristic == null) {
          ch.supported = false;
          Log.d(TAG, "The Gatt characteristic "+ch.description+" wasn't found.");
          continue;
        }
        ch.supported = true;
        Log.d(TAG, "The Gatt characteristic "+ch.description+" was found.");
        // Subscribe
        if (!gatt.setCharacteristicNotification(mGattCharacteristic, true)) {
          Log.d(TAG, "setCharacteristicNotification returned FALSE");
        } // TODO: deal with error
        BluetoothGattDescriptor descriptor = mGattCharacteristic.getDescriptor(CLIENT_CHAR_CONFIG_UUID);
        if (descriptor == null) {Log.d(TAG, "Can't get the Client Characteristic Configuration descriptor");}
        else {
          boolean s = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
          if (!s) {Log.d(TAG, "setValue was unsuccessful");}
          boolean w = gatt.writeDescriptor(descriptor);
          if (!w) {Log.d(TAG, "writeDescriptor was unsuccessful");}
          ch.subscribed = true;
        }
      }
    }
  }

  Intent getCharacteristic(BluetoothGattCharacteristic characteristic) {
    Intent intent = new Intent(Constants.ACTION_BROADCAST)
      .putExtra(Constants.ID_MESSAGE, Constants.MESSAGE_DATA_READ);
    // Find the service it belongs to
    UUID serviceUUID = characteristic.getService().getUuid();
    Log.d(TAG, "The parent Gatt service is <"+ serviceUUID.toString()+">.");
    ServiceNode serviceNode = Requirements.findService(serviceUUID);
    if (serviceNode == null)  // this characteristic is an 'orphan', it should never happen.
      {Log.d(TAG, "Orphan characteristic with service <"+serviceUUID.toString()+"> found.");return null;} // TODO: Deal with error
    // Find the characteristic within the service
    UUID characteristicUUID = characteristic.getUuid();
    Log.d(TAG, "The Gatt characteristic is <"+characteristicUUID.toString()+">.");
    CharacteristicNode characteristicNode = serviceNode.findCharacteristic(characteristicUUID);
    if (characteristicNode == null)  // this characteristic is not required, it should never happen.
      {Log.d(TAG, "Unexpected characteristic <"+characteristicUUID.toString()+">found.");return null;} // TODO: Deal with error
    intent.putExtra(Constants.ID_DATATYPE, characteristicNode.description);
    switch (serviceNode.code) {
/*      case HEART_RATE_SERVICE:
        switch (characteristicNode.code) {
          case HEART_RATE_CHARACTERISTIC: {  // scope created to keep variable declarations local.
            int format;
            byte flags = characteristic.getValue()[0];
            boolean isHeartRateInUINT16 = ((flags & 1) != 0);
            if (isHeartRateInUINT16) {format = BluetoothGattCharacteristic.FORMAT_UINT16;}
            else {format = BluetoothGattCharacteristic.FORMAT_UINT8;}
            intent.putExtra(Constants.ID_DATA, String.valueOf(characteristic.getIntValue(format, 1)));
            return intent;
          }
          default:
            break;
        }*/
/*      case ENVIRONMENTAL_SENSING_SERVICE:
        switch (characteristicNode.code) {
          case ELEVATION_CHARACTERISTIC:  // Specification format is sint24
            intent.putExtra(Constants.ID_DATA,
              String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, 0)));
            return intent;
          case POLLEN_CONCENTRATION_CHARACTERISTIC:  // Specification format is uint24
            intent.putExtra(Constants.ID_DATA,
              String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0)));
            return intent;
          case TEMPERATURE_CHARACTERISTIC:  // Specification format is sint16
            intent.putExtra(Constants.ID_DATA,
              String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0)));
            return intent;
          case TRUE_WIND_SPEED_CHARACTERISTIC:  // Specification format is uint16
            intent.putExtra(Constants.ID_DATA,
              String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)));
            return intent;
          default:
            break;
        }*/
        case AIRLIB:
          switch (characteristicNode.code) {
            case AIRLIB_CONTROL:  // Specification format is ???
              intent.putExtra(Constants.ID_DATA,
                String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, 0)));
              return intent;
            case AIRLIB_STATUS:  // Specification format is ???
              intent.putExtra(Constants.ID_DATA,
                String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0)));
              return intent;
            case AIRLIB_RED:  // Specification format is ???
              intent.putExtra(Constants.ID_DATA,
                String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0)));
              return intent;
            case AIRLIB_OX:  // Specification format is ???
              intent.putExtra(Constants.ID_DATA,
                String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)));
              return intent;
            default:
              break;
      }
    }
    return null;
  }

  void endSubscriptions(BluetoothGatt gatt) {
    for (ServiceNode ser : Requirements.services) {
      for (CharacteristicNode ch : ser.characteristics) {
        if (ch.subscribed) {
          // cancel notification
          BluetoothGattDescriptor descriptor = gatt.getService(ser.uuid)
            .getCharacteristic(ch.uuid)
            .getDescriptors().get(0);
          descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
          gatt.writeDescriptor(descriptor);
          gatt.disconnect();
          ch.subscribed = false;
        }
      }
    }
  }
}
