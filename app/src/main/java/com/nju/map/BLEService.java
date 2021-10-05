package com.nju.map;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.nju.map.iBeaconClass.iBeacon;

import java.util.ArrayList;

/**
 * Created by YSL on 2017/6/17.
 */

public class BLEService extends Service {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mbluetoothLeScanner;
    public final static String uuid="fda50693-a4e2-4fb1-afcf-c6eb07647825";
    private static ArrayList<iBeacon> iBeaconArray;
    private BluetoothAdapter.LeScanCallback mLeScanCallback1;
    private ScanCallback mLeScanCallback2;

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        iBeaconArray=new ArrayList<>();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "无蓝牙服务", Toast.LENGTH_SHORT).show();
            return;
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        try{
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }catch (Exception e){
            e.printStackTrace();
        }
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }
        }
        //扫描回调结果，符合要求的蓝牙设备将加入ArrayList
        if(Build.VERSION.SDK_INT>=21){ //API>=21
            try{
                mbluetoothLeScanner=mBluetoothAdapter.getBluetoothLeScanner();
            }catch (Exception e){
                e.printStackTrace();
            }
            mLeScanCallback2 = new ScanCallback() {
                @Override
                @TargetApi(21)
                public void onScanResult(int callbackType, final ScanResult result) {
                    super.onScanResult(callbackType, result);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            iBeacon ibeacon=iBeaconClass.fromScanData(result.getDevice(),result.getRssi(),result.getScanRecord().getBytes());
                            if(ibeacon!=null){
                                if(ibeacon.proximityUuid.equals(uuid)){
                                    if (ibeaconExist(ibeacon)){
                                        int index=ibeaconSearch(ibeacon);
                                        if(index!=-1)
                                            iBeaconArray.remove(iBeaconArray.get(index));
                                    }
                                    iBeaconArray.add(ibeacon);
                                }
                            }
                        }
                    }).start();
                }
            };
        }else{
            mLeScanCallback1=new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            iBeacon ibeacon=iBeaconClass.fromScanData(device,rssi,scanRecord);
                            if(ibeacon!=null) {
                                if(ibeacon.proximityUuid.equals(uuid)){
                                    if (ibeaconExist(ibeacon)){
                                        int index=ibeaconSearch(ibeacon);
                                        if(index!=-1)
                                            iBeaconArray.remove(iBeaconArray.get(index));
                                    }
                                    iBeaconArray.add(ibeacon);
                                }
                            }
                        }
                    }).start();
                }
            };
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        iBeaconArray.clear();
        if(Build.VERSION.SDK_INT>=21){
            startLeScan2();
        }else{
            startLeScan1();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(Build.VERSION.SDK_INT>=21){
                    try{
                        mbluetoothLeScanner.stopScan(mLeScanCallback2);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else{
                    try{
                        mBluetoothAdapter.stopLeScan(mLeScanCallback1);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                Intent BLEintent=new Intent("BLE_call_back");
                sendBroadcast(BLEintent);
            }
        },2000);//2秒后停止设备扫描，并且广播给主线程采取对应操作
        return super.onStartCommand(intent, flags, startId);
    }

    //开始扫描蓝牙设备，API<21
    private void startLeScan1(){
        if(mBluetoothAdapter.isEnabled()){
            try{
                mBluetoothAdapter.startLeScan(mLeScanCallback1);//开始扫描蓝牙设备
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @TargetApi(21)
    //开始扫描蓝牙设备，API>=21
    private void startLeScan2(){
        if(mbluetoothLeScanner!=null&&mBluetoothAdapter.isEnabled()){
            try{
                mbluetoothLeScanner.startScan(mLeScanCallback2);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    //外界查询接口，返回可接受到的最近的蓝牙基站
    public static iBeacon ibeaconQuery(){
        return ibeaconClosest();
    }

    //判断ArrayList中是否包含该设备
    private boolean ibeaconExist(iBeacon ibeacon){
        for(iBeacon iBeacon: iBeaconArray){
            if(ibeacon.major==iBeacon.major&&ibeacon.minor==iBeacon.minor)
                return true;
        }
        return false;
    }

    //找到ArrayList中对应设备（同major,同minor）的位置
    private int ibeaconSearch(iBeacon ibeacon){
        int num=0;
        for(iBeacon iBeacon: iBeaconArray){
            if(ibeacon.major==iBeacon.major&&ibeacon.minor==iBeacon.minor)
                return num;
            ++num;
        }
        return -1;
    }

    //以rssi为标准判断最近的蓝牙设备
    private static iBeacon ibeaconClosest(){
        if(!iBeaconArray.isEmpty()) {
            iBeacon temp = iBeaconArray.get(0);
            int Rssi = temp.rssi;
            for (int i = 1; i < iBeaconArray.size(); i++) {
                if (iBeaconArray.get(i).rssi > Rssi) {
                    temp = iBeaconArray.get(i);
                    Rssi = iBeaconArray.get(i).rssi;
                }
            }
            if (temp.rssi>-90)
                return temp;
            else
                return null;
        }else
            return null;
    }
}
