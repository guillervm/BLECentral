package com.test.guillervm.blecentral.core;

import android.bluetooth.BluetoothDevice;

/**
 * Created by guillervm on 14/4/15.
 */
public class Peripheral implements Comparable<Peripheral> {
    private BluetoothDevice device;
    /*
        < 0: not available.
        0: unknown availability.
        > 0: available.
     */
    private int available;
    private int txPowerLevel;
    private int rssi;

    /**
     * Peripheral constructor.
     * @param device The device of this <code>Peripheral</code>
     * @param available Value representing the availability of the device:<code><ul><li>< 0: device not available.</li><li>0: unknown availability.</li><li>> 0: device available.</li></ul></code>
     */
    public Peripheral(BluetoothDevice device, int available) {
        super();

        this.device = device;
        this.available = available;
    }

    /**
     * Peripheral constructor.
     * @param device The device of this <code>Peripheral</code>
     * @param available Value representing the availability of the device:<code><ul><li>< 0: device not available.</li><li>0: unknown availability.</li><li>> 0: device available.</li></ul></code>
     * @param scanRecord The received scan record.
     */
    public Peripheral(BluetoothDevice device, int available, byte[] scanRecord) {
        super();

        this.device = device;
        this.available = available;
        this.setTxPowerLevel(scanRecord);
    }

    /**
     * Getter for device.
     * @return The device of the <code>Peripheral</code>
     */
    public BluetoothDevice getDevice() {
        return device;
    }

    /**
     * Setter for device.
     * @param device The new device of this <code>Peripheral</code>
     */
    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    /**
     * Getter for availability.
     * @return Integer value representing the availability of the device:<code><ul><li>< 0: device not available.</li><li>0: unknown availability.</li><li>> 0: device available.</li></ul></code>
     */
    public int isAvailable() {
        return available;
    }

    /**
     * Setter for availability.
     * @param available The new value for availability:<code><ul><li>< 0: device not available.</li><li>0: unknown availability.</li><li>> 0: device available.</li></ul></code>
     */
    public void setAvailable(int available) {
        this.available = available;
    }

    /**
     * Getter for Tx power level.
     * @return Integer value representing the Tx power level of the device.
     */
    public int getTxPowerLevel() {
        return txPowerLevel;
    }

    /**
     * Setter for Tx power level.
     * @param txPowerLevel The new value for Tx power level.
     */
    public void setTxPowerLevel(int txPowerLevel) {
        this.txPowerLevel = txPowerLevel;
    }

    /**
     * Setter for Tx power level.
     * @param scanRecord The received scan record.
     */
    public void setTxPowerLevel(byte[] scanRecord) {
        int i = 0;
        while (i < scanRecord.length) {
            int len = scanRecord[i++] & 0xFF;
            if (len == 0) break;
            switch (scanRecord[i] & 0xFF) {
                // https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
                case 0x0A: // Tx Power
                    this.txPowerLevel = scanRecord[i+1];
            }
            i += len;
        }
        this.txPowerLevel = 0;
    }

    /**
     * Getter for RSSI.
     * @return Integer value representing the RSSI of the device.
     */
    public int getRssi() {
        return rssi;
    }

    /**
     * Setter for RSSI.
     * @param rssi The RSSI of the device.
     */
    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    /**
     * Estimated distance calc.
     * @return Estimated distance between devices.
     */
    public double getDistance() {
        /*
         * RSSI = TxPower - 10 * n * lg(d)
         * d = 10 ^ ((TxPower - RSSI) / (10 * n))
         * n = 2 (in free space)
         * d = 10 ^ ((TxPower - RSSI) / (10 * 2))
         */

        return Math.pow(10d, ((double) txPowerLevel - rssi) / (20d));
    }

    /**
     * Estimated distance calc.
     * @param rssi The RSSI of the device.
     * @return Estimated distance between devices.
     */
    public double getDistance(int rssi) {
        return Math.pow(10d, ((double) txPowerLevel - rssi) / (20d));
    }

    /**
     * String representing distance.
     * @return Formatted string for the distance.
     */
    public String getDistanceString() {
        return getDistanceString(getDistance());
    }

    /**
     * String representing distance.
     * @param distance The distance.
     * @return Formatted string for the distance.
     */
    public String getDistanceString(double distance) {
        if (distance < 100) {
            return "< 1 meter";
        }

        int intDistance = ((Double)Math.floor(distance / 100)).intValue();

        return "~" + intDistance + " meter" + (intDistance>1?"s":"");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Peripheral) {
            return ((Peripheral) o).getDevice().getAddress().equals(device.getAddress());
        }

        return false;
    }

    @Override
    public int compareTo(Peripheral peripheral) {
        // Returns a negative integer, zero, or a positive integer as this object is less than,
        // equal to, or greater than the specified object.
        if (peripheral.getDistance() < this.getDistance()) {
            return -1;
        } else if (peripheral.getDistance() > this.getDistance()) {
            return 1;
        }

        return 0;
    }
}
