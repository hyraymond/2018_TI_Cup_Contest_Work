package com.nju.map;

import android.Manifest;
import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static android.view.LayoutInflater.*;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    public static final int REQUEST_CAMERA_PERM = 101; //申请相机权限
    public static final int REQUEST_MAP = 102; //申请地图权限
    public static final int SCAN_CODE = 1; //打开扫码界面，扫描二维码
    private FloatingActionButton scan = null;
    private NavigationView navView = null;
    private DrawerLayout mDrawerLayout;
    public LocationClient mLocationClient;
    private Toolbar toolbar;
    private MapView mapView;
    private BaiduMap baiduMap;
    private boolean isFirstLocate;
    private View myAbout;
    private View mainview;
    private long mExitTime = System.currentTimeMillis();  //为当前系统时间，单位：毫秒
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFirstLocate=true;
        mLocationClient=new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        mapView=(MapView)findViewById(R.id.bmapView);
        scan = (FloatingActionButton) findViewById(R.id.scan);
        LayoutInflater inflater = this.getLayoutInflater();
        myAbout = inflater.inflate(R.layout.activity_m_about, null);


        scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cameraTask();
            }
        });
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        navView = (NavigationView)findViewById(R.id.nav_view);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.nav_friends);
        }
        init();
    }

    private void init() {
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){
            public boolean onNavigationItemSelected(MenuItem item){
                int index = item.getItemId();
                switch (index){
                    case R.id.nav_delete:
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("清除缓存");
                        builder.setMessage("确定清除吗？");
                        builder.setCancelable(false);
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                try{
                                    String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E行";
                                    File file=new File(filePath);
                                    File[] files=file.listFiles();
                                    for(int i=0;i<files.length;++i){
                                        files[i].delete();
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        });
                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                //do nothing
                            }
                        });
                        builder.show();
                        break;
                    case R.id.nav_about:
                        Intent aboutintent = new Intent(MainActivity.this, mAbout.class);
                        aboutintent.putExtra("mAbout",true);
                        startActivity(aboutintent);
                        break;
                    case R.id.loadLatest:
                        Intent intent = new Intent(MainActivity.this, ThirdActivity.class);
                        intent.putExtra("flag",true);
                        startActivity(intent);
                }
                mDrawerLayout.closeDrawers();
                return true;
            }
        });
        baiduMap=mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        List<String> permissionList=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(android.Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,REQUEST_MAP);
        }else{
            requestLocation();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isFirstLocate=false;
            }
        },5000);
    }

    //开启定位
    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }

    //定位自定义设置
    private void initLocation(){
        LocationClientOption option=new LocationClientOption();
        option.setScanSpan(1000); //扫描间隔
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }

    //位置监听器
    public class MyLocationListener implements BDLocationListener {
        public void onReceiveLocation(BDLocation location){
            if(location.getLocType()==BDLocation.TypeGpsLocation||location.getLocType()==BDLocation.TypeNetWorkLocation)
                navigateTo(location);
        }
        public void onConnectHotSpotMessage(String text1,int text2){}
    }

    //将当前位置设置为location坐标，如果是第一次打开，则将镜头转到该坐标处，并设置镜头大小
    private void navigateTo(BDLocation location){
        if(isFirstLocate){
            LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
            MapStatus mMapStatus = new MapStatus.Builder()
                    .target(latLng)
                    .zoom(16)
                    .build();
            //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
            MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
            //改变地图状态
            baiduMap.animateMapStatus(mMapStatusUpdate);
        }
        MyLocationData.Builder locationBuilder=new MyLocationData.Builder();
        locationBuilder.latitude(location.getLatitude());
        locationBuilder.longitude(location.getLongitude());
        MyLocationData locationData=locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }

    //打开相机请求
    public void cameraTask() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            // Have permission, do the thing!
            Intent intent = new Intent(MainActivity.this, SecondActivity.class); //跳转到相机界面
            startActivityForResult(intent, SCAN_CODE);
        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(this, "需要请求camera权限",
                    REQUEST_CAMERA_PERM, Manifest.permission.CAMERA);  //发送相机权限申请
        }
    }

    //将二维码信息传递至ThirdActivity
    private void changeMap(String result) {
        if(result.contains("ground_floor"))
            Toast.makeText(getApplicationContext(), "目前位于一楼", Toast.LENGTH_LONG).show();
        else if(result.contains("first_floor"))
            Toast.makeText(getApplicationContext(), "目前位于二楼", Toast.LENGTH_LONG).show();
        else if(result.contains("second_floor"))
            Toast.makeText(getApplicationContext(), "目前位于三楼", Toast.LENGTH_LONG).show();
        else if(result.contains("third_floor"))
            Toast.makeText(getApplicationContext(), "目前位于四楼", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(MainActivity.this, ThirdActivity.class);
        intent.putExtra("flag",false);
        intent.putExtra("photo",result);
        startActivity(intent);
    }

    //扫描二维码返回结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCAN_CODE) {
            //处理扫描结果（在界面上显示）
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = bundle.getString(CodeUtils.RESULT_STRING);
                    changeMap(result);
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(MainActivity.this, "解析二维码失败", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //requestPermissions返回结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode){
            case REQUEST_MAP:
                if(grantResults.length>0){
                    for (int result:grantResults){
                        if(result!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"需要定位权限",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else {
                    Toast.makeText(this,"发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_CAMERA_PERM:
                EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
                break;
            default:
        }
    }

    //权限同意返回结果
    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {}

    //权限拒绝返回结果
    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this, "当前App需要申请camera权限,需要打开设置页面么?")
                    .setTitle("权限申请")
                    .setPositiveButton("确认")
                    .setNegativeButton("取消", null /* click listener */)
                    .setRequestCode(REQUEST_CAMERA_PERM)
                    .build()
                    .show();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            default:
        }
        return true;
    }


}




