package com.nju.map;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import com.nju.map.DigitalMap.*;
import com.nju.map.iBeaconClass.iBeacon;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ThirdActivity extends AppCompatActivity implements PopupWindow.OnDismissListener{
    public static final int REQUEST_BLUETOOTH = 103; //申请蓝牙权限
    public static final int SEE_MAP = 3;//查看图片请求的标识
    public static final int DOC_COUNT = 1001;//Message标识，
    private Toolbar toolbar;
    private TextView mapViewer;
    private ImageView imageView=null;//地图层
    private ImageView canvasView=null;//路径层
    private ImageView initPointView=null;//位置层
    private TextView nameText=null;
    private TextView directionText=null;
    private ProgressDialog mProgressDialog=null;
    private GuidePopupWindow guidePopupWindow=null;
    private String data;//二维码扫描信息
    private int screenWidth;//画布宽度 1080
    private int screenHeight;//画布高度 1920-title高度
    private int init_X=0;//初始位置点坐标
    private int init_Y=0;
    private int dest_X=0;//目的地点坐标
    private int dest_Y=0;
    private Map map=null;//地图标签，抽象类
    private ArrayList<Integer> Xarray = new ArrayList<>();//路径点集合
    private ArrayList<Integer> Yarray = new ArrayList<>();
    private ArrayList<Integer> digitalX = new ArrayList<>();//数字点集合
    private ArrayList<Integer> digitalY = new ArrayList<>();
    public static enum Orientation{North,South,West,East};//方向标签集
    Orientation orientation;
    private boolean isDrawingPath=false;//是否有路径
    private boolean isImageValid=false;//图片是否正常载入
    private boolean saveImage=false;//是否保存图片
    private int docCount=0;//文件数量
    private int mapsNum;//图片总张数
    private int currentIndex;//当前图片标签
    private int nextIndex;//目的地所位于地图的标签
    private String prefix;//地图信息前缀
    private int scale;//地图比例尺
    private Intent BLEintent;//开启蓝牙服务请求
    private SensorManager manager;//传感器管理
    private Sensor accelerometer; // 加速度传感器
    private Sensor magnetic; // 地磁场传感器
    private Sensor stepDetector; //震动传感器
    private MySensorEventListener mListener;//传感器监听器
    private float[] accelerometerValues = new float[3];//加速度传感器数值
    private float[] magneticFieldValues = new float[3];//磁场传感器数值
    private float[] values = new float[3];//保存方向信息
    private String xmlMapData;//保存解析XML文件得到的蓝牙信息数据
    private String xmlRoomData;//保存解析XML文件得到的房间信息数据
    private SharedPreferences mSharedPreferences;//保存最近一次退出时的位置
    private float ratio;//X方向上伸缩比例
    private float offset;//Y方向上偏置
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case DOC_COUNT:
                    ++docCount;
                    if(docCount==(mapsNum+2)) mapRefresh();
                    break;
            }
        }
    }; //接收文件下载完成的信号
    private BroadcastReceiver locationChangeReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            iBeacon ibeacon =BLEService.ibeaconQuery();
            if(ibeacon!=null){
                locationQuery(ibeacon);
            }
            BLEintent = new Intent(getApplicationContext(), BLEService.class);
            startService(BLEintent);
        }
    }; //广播接收器，当BLEService发送广播时，修改当前位置，同时开启新一轮定位

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);
        toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mapViewer=(TextView)findViewById(R.id.mapViewer);
        mapViewer.setEnabled(false);
        imageView=(ImageView)findViewById(R.id.mapView);
        canvasView=(ImageView)findViewById(R.id.canvasView);
        initPointView=(ImageView)findViewById(R.id.initPointView);
        mProgressDialog=new ProgressDialog(this);
        mProgressDialog.setTitle("提示");
        mProgressDialog.setMessage("图片加载中...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        manager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mListener = new MySensorEventListener();
        accelerometer=manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetic=manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        stepDetector=manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        List<String> permissionList=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(ThirdActivity.this, Manifest.permission.BLUETOOTH)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.BLUETOOTH);
        }
        if(ContextCompat.checkSelfPermission(ThirdActivity.this, Manifest.permission.BLUETOOTH_ADMIN)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if(!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(ThirdActivity.this,permissions,REQUEST_BLUETOOTH);
        }
        imageInit();
        popWindowInit();
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    //初始化地图界面
    private void imageInit(){
        Intent intent = getIntent();
        if(intent.getBooleanExtra("flag",false)){
            try {
                mSharedPreferences=getSharedPreferences("latestData",MODE_PRIVATE);
                data=mSharedPreferences.getString("data","data");
                mapInfoRefresh();
                init_X=mSharedPreferences.getInt("init_X",0);
                init_Y=mSharedPreferences.getInt("init_Y",0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            try {
                data = intent.getStringExtra("photo");
                String[] temp=data.split(" ");
                data=temp[0];
                temp=temp[1].split("_");
                init_X=Integer.parseInt(temp[0]);
                init_Y=Integer.parseInt(temp[1]);
                mapInfoRefresh();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(map == null) {
            isImageValid=false;
            imageView.setImageResource(R.drawable.not_found);
            Toast.makeText(getApplicationContext(),"无法识别的二维码",Toast.LENGTH_SHORT).show();
        } else {
            try {
                prefix=new String(data);
                prefix=prefix.replaceAll("\\d", "");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (fileExists(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E行/"+data+".png")) {
                saveImage=true;
                xmlMapData=readTxtFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/E行/" + prefix + ".txt");
                mProgressDialog.show();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mapRefresh();
                    }
                },1000);
            }
            else if (isNetAvailable(this)) {
                mProgressDialog.show();
                try {
                    for(int i=1;i<=mapsNum;++i)
                        loadMap("http://mapimages.oss-cn-shanghai.aliyuncs.com/"+prefix+Integer.toString(i)+".png",prefix+Integer.toString(i));
                    loadMapInfo("http://mapimages.oss-cn-shanghai.aliyuncs.com/"+prefix+".xml");
                    loadRoomInfo("http://mapimages.oss-cn-shanghai.aliyuncs.com/"+prefix+"room_data"+".xml");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                isImageValid=false;
                imageView.setImageResource(R.drawable.not_found);
                Toast.makeText(getApplicationContext(),"请打开无线网或数据流量",Toast.LENGTH_SHORT).show();
            }
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(imageView.getDrawable()==null){
                    mProgressDialog.dismiss();
                    isImageValid=false;
                    imageView.setImageResource(R.drawable.not_found);
                    Toast.makeText(getApplicationContext(),"网速太慢了...",Toast.LENGTH_SHORT).show();
                }
            }
        },10000);
    }

    //初始化弹窗，onIntro完善中...
    private void popWindowInit(){
        guidePopupWindow=new GuidePopupWindow(this, new GuideClickListener() {
            @Override
            public void onEnd() {
                guidePopupWindow.dismiss();
            }
        });
        guidePopupWindow.setOnDismissListener(this);
    }

    //初始化蓝牙服务
    private void bluetoothInit(){
        BLEintent = new Intent(getApplicationContext(), BLEService.class);
        startService(BLEintent);
    }

    //根据图片信息设置初始参数,添加新地图时要修改该函数
    private void mapInfoRefresh(){
        digitalX.clear();
        digitalY.clear();
        try {
            currentIndex=Integer.parseInt(data.replaceAll("\\D","").replaceAll("_",""));
        } catch (Exception e) {
            e.printStackTrace();
        }
        map=MapFactory.getMap(data,screenWidth,screenHeight);
        map.getDigitalPoint(digitalX,digitalY);
        scale=map.getScale();
        mapsNum=map.getMapsNum();
    }

    //根据传入的点击位置得到教室对应的数字点，并得到最短路径上的点集
    private void pathPointInfo(int x, int y){
        Xarray.clear();
        Yarray.clear();
        int nearest = aroundPoint();
        int[] path=map.floydPath(x, y, nearest);
        for (int i=0; i < path.length; i++) {
            map.pathPointInfo(path[i],Xarray,Yarray,x,y);
        }
        if(isOnPath()){
            Xarray.remove(0);
            Yarray.remove(0);
        }
    }

    //画出起始点,及当前手机的朝向，注意与isImageValid同用
    private void drawInitPoint(){
        Bitmap bitmap=Bitmap.createBitmap(screenWidth,screenHeight,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
        Paint paint=new Paint();
        paint.setStrokeWidth(5*ratio);
        paint.setColor(Color.RED);
        canvas.drawCircle((float)init_X*ratio,(float)(init_Y+offset)*ratio,20*ratio,paint);
        Path mArrowPath = new Path();
        float arrowR=20*ratio;
        mArrowPath.moveTo(0,-arrowR);
        mArrowPath.lineTo(-arrowR,-arrowR);
        mArrowPath.lineTo(0,-3*arrowR);
        mArrowPath.lineTo(arrowR,-arrowR);
        mArrowPath.lineTo(0,-arrowR);
        mArrowPath.close();
        canvas.save(); // 保存画布
        canvas.translate((float)init_X*ratio,(float)(init_Y+offset)*ratio); // 平移画布
        canvas.rotate((float)Math.toDegrees(values[0])); // 转动画布
        canvas.drawPath(mArrowPath, paint);
        canvas.restore(); // 恢复画布
        Drawable drawable=new BitmapDrawable(bitmap) ;
        initPointView.setBackgroundDrawable(drawable);
    }

    //绘制图片和当前位置，开启时调用
    private void mapRefresh(){
        mProgressDialog.dismiss();
        try{
            String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E行/"+data+".png";
            File file=new File(filePath);
            if (file.exists()){
                Bitmap bitmap= BitmapFactory.decodeFile(filePath);
                imageView.setImageBitmap(bitmap);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if(imageView.getDrawable()!=null) {
            isImageValid=true;
            directionText=(TextView)GuidePopupWindow.popupView.findViewById(R.id.directionText);
            nameText=(TextView)GuidePopupWindow.popupView.findViewById(R.id.nameText);
            screenWidth=canvasView.getWidth();
            screenHeight=canvasView.getHeight();
            map=MapFactory.getMap(data,screenWidth,screenHeight);
            ratio=map.getRatio();
            offset=map.getOffset();
            mapViewer.setEnabled(true);
            mapViewer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent=new Intent(ThirdActivity.this,FourthActivity.class);
                    intent.putExtra("prefix",prefix);
                    intent.putExtra("currentIndex",currentIndex);
                    intent.putExtra("screenWidth",screenWidth);
                    intent.putExtra("screenHeight",screenHeight);
                    startActivityForResult(intent,SEE_MAP);
                }
            });
            bluetoothInit();
            drawInitPoint();
        } else {
            isImageValid=false;
            imageView.setImageResource(R.drawable.not_found);
            Toast.makeText(getApplicationContext(),"未能正确获取图片",Toast.LENGTH_SHORT).show();
        }
    }

    //路线导航提示,drawPath时调用
    private void directionGuide(){
        int x=Xarray.get(0);
        int y=Yarray.get(0);
        int temp=0;
        if(init_X==x&&init_Y>y){
            temp=(init_Y-y)/scale;
            directionText.setText("向北行进"+Integer.toString(temp)+"米");
        }else if(init_X==x&&init_Y<y){
            temp=(y-init_Y)/scale;
            directionText.setText("向南行进"+Integer.toString(temp)+"米");
        }else if(init_X>x&&init_Y==y){
            temp=(init_X-x)/scale;
            directionText.setText("向西行进"+Integer.toString(temp)+"米");
        }else if(init_X<x&&init_Y==y){
            temp=(x-init_X)/scale;
            directionText.setText("向东行进"+Integer.toString(temp)+"米");
        }
    }

    //路径地图清屏
    private void cleanMap(){
        Bitmap bitmap=Bitmap.createBitmap(screenWidth,screenHeight,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
        Drawable drawable = new BitmapDrawable(bitmap);
        canvasView.setBackgroundDrawable(drawable);
    }

    //重新绘制路径地图
    private void drawPath(){
        if(!isDrawingPath)return;
        Bitmap bitmap=Bitmap.createBitmap(screenWidth,screenHeight,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
        Paint paint=new Paint();
        paint.setStrokeWidth(5*ratio);
        paint.setColor(Color.RED);
        canvas.drawLine((float)init_X*ratio,(float)(init_Y+offset)*ratio,(float)Xarray.get(0)*ratio,(float)(Yarray.get(0)+offset)*ratio,paint);
        for(int i=0;i<Xarray.size()-1;i++) {
            canvas.drawLine((float)Xarray.get(i)*ratio,(float)(Yarray.get(i)+offset)*ratio,
                    (float)Xarray.get(i+1)*ratio,(float)(Yarray.get(i+1)+offset)*ratio, paint);
        }
        if(nextIndex==currentIndex){
            paint.setColor(Color.BLUE);
            canvas.drawCircle((float)dest_X*ratio,(float)(dest_Y+offset)*ratio,20*ratio,paint);
        }
        drawInitPoint();
        Drawable drawable=new BitmapDrawable(bitmap);
        canvasView.setBackgroundDrawable(drawable);
        directionGuide();
    }

    //到达目的地点
    private void reachDest(){
        if(((init_X==dest_X)&&(Math.abs(init_Y-dest_Y)<5*scale))|| ((init_Y==dest_Y)&&(Math.abs(init_X-dest_X)<5*scale))){
            cleanMap();
            isDrawingPath=false;
            guidePopupWindow.dismiss();
        }
    }

    //取与当前位置相邻最近的数字点
    private int aroundPoint(){
        int distance = 10000; //MAX
        int nearest = -1;
        int temp;
        for(int i=0;i<digitalX.size();++i){
            if((temp=Math.abs(init_X-digitalX.get(i))+Math.abs(init_Y-digitalY.get(i)))<distance){
                distance=temp;
                nearest=i;
            }
        }
        return nearest;
    }

    //如果起始点已经在路径上，则删除第一个点（最近点）
    private boolean isOnPath(){
        for(int i=0;i<Xarray.size()-1;i++){
            if(((Xarray.get(i).equals(Xarray.get(i+1)))&&(Xarray.get(i)==init_X))||((Yarray.get(i).equals(Yarray.get(i+1)))&&(Yarray.get(i)==init_Y)))
                return true;
        }
        return false;
    }

    //显示导航窗口
    private void showGuideWindow() {
        isDrawingPath=true;
        if (guidePopupWindow == null) return;
        guidePopupWindow.showAtLocation(findViewById(R.id.myLayout), Gravity.BOTTOM,0,0);
    }

    //判断网络是否可用
    public static boolean isNetAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return (info != null && info.isAvailable());
    }

    //判断本机上是否存在图片
    public boolean fileExists(String fileDir){
        try{
            File f=new File(fileDir);
            if(!f.exists()){
                return false;
            }
        }catch (Exception e) {
            // TODO: handle exception
            return false;
        }
        return true;
    }

    //图片分享至微信
    public void shareWeChat(String path){
        Uri uriToImage = Uri.fromFile(new File(path));
        Intent shareIntent = new Intent();
        //发送图片到朋友圈
        //ComponentName comp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI");
        //发送图片给好友。
        ComponentName comp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI");
        shareIntent.setComponent(comp);
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);
        shareIntent.setType("image/*");
        startActivity(Intent.createChooser(shareIntent, "分享图片"));
    }

    //保存当前手机屏幕截图
    private void getAndSaveCurrentImage(Context mContext) {
        //1.构建Bitmap
        Bitmap bmp = Bitmap.createBitmap(screenWidth,screenHeight,Bitmap.Config.ARGB_8888);
        //2.获取屏幕
        View decorview = this.getWindow().getDecorView();
        decorview.draw(new Canvas(bmp));
        String SavePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/E行/ScreenImage";
        //3.保存Bitmap
        try {
            File path = new File(SavePath);
            //文件
            String filepath = SavePath+"/Screen_"+data+".png";
            File file = new File(filepath);
            if(!path.exists()){
                path.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos=new FileOutputStream(file);
            if (fos!=null) {
                bmp.compress(Bitmap.CompressFormat.PNG,100,fos);
                fos.flush();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //根据设备的ibeacon查询并修改当前位置，如果正在导航，则修改路径，网络服务完善中...
    private void locationQuery(iBeacon ibeacon){
        int[] temp=parseXMLWithPull(xmlMapData,ibeacon.major,ibeacon.minor);
        if (temp==null){
            return;
        }
        if (currentIndex!=temp[0]){
            currentIndex=temp[0];
            data=prefix+Integer.toString(currentIndex);
            mapInfoRefresh();
            cleanMap();
            String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E行/"+data+".png";
            File file=new File(filePath);
            if (file.exists()){
                Bitmap bitmap= BitmapFactory.decodeFile(filePath);
                imageView.setImageBitmap(bitmap);
            }
        }
        init_X=temp[1];
        init_Y=temp[2];
        drawInitPoint();
        if(isDrawingPath){
            cleanMap();
            if(nextIndex!=currentIndex){
                int[] result=map.getExitable(init_X,init_Y,nextIndex);
                pathPointInfo(result[0],result[1]);
            }else {
                pathPointInfo(dest_X,dest_Y);
                reachDest();
            }
            drawPath();
        }
    }

    //计算手机在水平于地面，屏幕朝上的情况下，Y轴正方向所指向的位置，并时时更新初始点
    private void calculateOrientation() {
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(R, values);
        float angle =(float)Math.toDegrees(values[0]);
        if (angle >= -45 && angle < 45) {
            orientation=Orientation.North;
        } else if (angle >= 45 && angle <= 135) {
            orientation=Orientation.East;
        } else if ((angle >= 135 && angle <= 180)
                || (angle) >= -180 && angle < -135) {
            orientation=Orientation.South;
        } else if (angle < -45 && angle >= -135) {
            orientation=Orientation.West;
        }
        if(isImageValid) drawInitPoint();
    }

    //尝试通过传感器信号来移动当前位置点,进入下一张地图后自动更换图片
    private void tryMove(){
        int deltaX=0;
        int deltaY=0;
        boolean movable;//是否可以移动
        int[] exitable=new int[3];//是否需要换图
        exitable[0]=0;
        switch (orientation){
            case North:
                deltaX =0;
                deltaY = -scale;
                break;
            case South:
                deltaX = 0;
                deltaY = scale;
                break;
            case West:
                deltaX = -scale;
                deltaY = 0;
                break;
            case East:
                deltaX = scale;
                deltaY = 0;
        }
        movable=map.canMove(deltaX,deltaY,init_X,init_Y);
        exitable=map.canExit(init_X,init_Y);
        if(movable) {
            init_X+=deltaX;
            init_Y+=deltaY;
            drawInitPoint();
        } else {
            int num = aroundPoint();
            if((Math.abs(digitalX.get(num)-init_X)+Math.abs(digitalY.get(num)-init_Y))<8*scale) {
                init_X=digitalX.get(num);
                init_Y=digitalY.get(num);
                drawInitPoint();
            }
        }
        if(exitable[0]==1) {
            data=prefix+Integer.toString(exitable[3]);
            String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E行/"+data+".png";
            File file=new File(filePath);
            if (file.exists()){
                Bitmap bitmap= BitmapFactory.decodeFile(filePath);
                imageView.setImageBitmap(bitmap);
            }
            mapInfoRefresh();
            init_X=exitable[1];
            init_Y=exitable[2];
            drawInitPoint();
        }
        if(isDrawingPath){
            cleanMap();
            if(nextIndex!=currentIndex){
                int[] result=map.getExitable(init_X,init_Y,nextIndex);
                pathPointInfo(result[0],result[1]);
            }else {
                pathPointInfo(dest_X,dest_Y);
                reachDest();
            }
            drawPath();
        }
    }

    //传感器监听
    private class MySensorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if(event.sensor.getType()==Sensor.TYPE_STEP_COUNTER){
                if(isImageValid)
                    tryMove();
            }
            if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
                accelerometerValues = event.values;
            }
            if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD) {
                magneticFieldValues = event.values;
            }
            calculateOrientation();
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }
    }

    //下载地图蓝牙信息文件
    private void loadMapInfo(final String url){
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    // *OkhttpClient
                    OkHttpClient client=new OkHttpClient();
                    Request request=new Request.Builder().url(url).build();
                    Response response=client.newCall(request).execute();
                    xmlMapData=response.body().string();
                    saveFile(xmlMapData,prefix);
                    Message msg=handler.obtainMessage();
                    msg.what=DOC_COUNT;
                    handler.sendMessage(msg);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //下载地图房间信息文件
    public void loadRoomInfo(final String url){
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    // *OkhttpClient
                    OkHttpClient client=new OkHttpClient();
                    Request request=new Request.Builder().url(url).build();
                    Response response=client.newCall(request).execute();
                    xmlRoomData=response.body().string();
                    saveFile(xmlRoomData,prefix+"room_data");
                    Message msg=handler.obtainMessage();
                    msg.what=DOC_COUNT;
                    handler.sendMessage(msg);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }

    //下载地图
    private void loadMap(final String url, final String data){
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    OkHttpClient client=new OkHttpClient();
                    Request request=new Request.Builder().url(url).build();
                    Response response=client.newCall(request).execute();
                    byte[] picByte=response.body().bytes();
                    if (picByte != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(picByte, 0, picByte.length);
                        File appDir = new File(Environment.getExternalStorageDirectory(),"E行");
                        if (!appDir.exists()) {
                            appDir.mkdir();
                        }
                        String fileName = data+ ".png";
                        File file = new File(appDir, fileName);
                        try {
                            FileOutputStream fos = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            fos.flush();
                            fos.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Message msg=handler.obtainMessage();
                        msg.what=DOC_COUNT;
                        handler.sendMessage(msg);
                    }
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //解析下载到的包含蓝牙信息的XML文件
    private int[] parseXMLWithPull(String xmlData,int major,int minor) {
        int temp[] = new int[3];
        String x = "";
        String y = "";
        String mi="";
        String ix = "";
        String iy = "";
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = factory.newPullParser();
            xmlPullParser.setInput(new StringReader(xmlData));
            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String nodeName = xmlPullParser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG: {
                        if ("x".equals(nodeName)) {
                            x = xmlPullParser.nextText();
                        } else if ("y".equals(nodeName)) {
                            y = xmlPullParser.nextText();
                        } else if ("mi".equals(nodeName)){
                            mi = xmlPullParser.nextText();
                        } else if ("ix".equals(nodeName)) {
                            ix = xmlPullParser.nextText();
                        } else if ("iy".equals(nodeName)) {
                            iy = xmlPullParser.nextText();
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        if("location".equals(nodeName)){
                            if (Integer.parseInt(x) == major && Integer.parseInt(y) == minor) {
                                temp[0] = Integer.parseInt(mi);
                                temp[1] = Integer.parseInt(ix);
                                temp[2] = Integer.parseInt(iy);
                                return temp;
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception exception) {exception.printStackTrace();}
        return null;
    }

    //读取txt文件
    private String readTxtFile(String strFilePath) {
        String path = strFilePath;
        String content = ""; //文件内容字符串
        //打开文件
        File file = new File(path);
        //如果path是传递过来的参数，可以做一个非目录的判断
        try {
            InputStream instream = new FileInputStream(file);
            if (instream != null)
            {
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line;
                //分行读取
                while (( line = buffreader.readLine()) != null) {
                    content += line + "\n";
                }
                instream.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return content;
    }

    //保存包含地图信息至一个txt文件
    private void saveFile(String str, String name) {
        String filePath = null;
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/E行/" + name + ".txt";
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                File dir = new File(file.getParent());
                dir.mkdirs();
                file.createNewFile();
            }
            FileOutputStream outStream = new FileOutputStream(file);
            outStream.write(str.getBytes());
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.third_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish(); // back button
                return true;
        }

        if(isImageValid){
            switch (item.getItemId()){
                case R.id.storePic:
                    saveImage=true;
                    break;
                case R.id.shareLocation:
                    getAndSaveCurrentImage(this);
                    shareWeChat(Environment.getExternalStorageDirectory().getAbsolutePath()+"/E行/ScreenImage"+"/Screen_"+data+".png");
                    break;
            }
            return true;
        }else{
            //do nothing
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnData) {
        switch (requestCode) {
            case SEE_MAP:
                if(resultCode==FourthActivity.CHANGE_INIT){
                    if(null!=returnData){
                        int[] temp=returnData.getIntArrayExtra("result");
                        data=prefix+Integer.toString(temp[0]);
                        String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E行/"+data+".png";
                        File file=new File(filePath);
                        if (file.exists()){
                            Bitmap bitmap= BitmapFactory.decodeFile(filePath);
                            imageView.setImageBitmap(bitmap);
                        }
                        mapInfoRefresh();
                        if(isDrawingPath)cleanMap();
                        isDrawingPath=false;
                        init_X=temp[1];
                        init_Y=temp[2];
                        drawInitPoint();
                    }
                }else if(resultCode==FourthActivity.CHANGE_DEST){
                    if(null!=returnData){
                        int[] temp=returnData.getIntArrayExtra("result");
                        nextIndex=temp[0];
                        dest_X=temp[1];
                        dest_Y=temp[2];
                        cleanMap();
                        nameText.setText("目标："+returnData.getStringExtra("destName"));
                        showGuideWindow();
                        if(nextIndex!=currentIndex){
                            int[] result=map.getExitable(init_X,init_Y,nextIndex);
                            pathPointInfo(result[0],result[1]);
                        }else {
                            pathPointInfo(dest_X,dest_Y);
                            reachDest();
                        }
                        drawPath();
                    }
                }else{
                    //do nothing
                }
                bluetoothInit();
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_BLUETOOTH:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "需要蓝牙权限", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplication(),MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDismiss() {
        cleanMap();
        isDrawingPath=false;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        IntentFilter BLEfilter = new IntentFilter("BLE_call_back");
        registerReceiver(locationChangeReceiver,BLEfilter);
        manager.registerListener(mListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        manager.registerListener(mListener, magnetic, SensorManager.SENSOR_DELAY_NORMAL);
        manager.registerListener(mListener, stepDetector,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        BLEintent=new Intent(getApplicationContext(),BLEService.class);
        stopService(BLEintent);
        if(isImageValid){
            mSharedPreferences=getSharedPreferences("latestData",MODE_PRIVATE);
            SharedPreferences.Editor mEditor=mSharedPreferences.edit();
            mEditor.putString("data",data);
            mEditor.putInt("init_X",init_X);
            mEditor.putInt("init_Y",init_Y);
            mEditor.apply();
        }
        unregisterReceiver(locationChangeReceiver);
        manager.unregisterListener(mListener);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(isImageValid){
            String filePath=Environment.getExternalStorageDirectory().getAbsolutePath()+"/E行/ScreenImage";
            try{
                File file=new File(filePath);
                File[] maps=file.listFiles();
                for(int i=0;i<maps.length;++i){
                    maps[i].delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(!saveImage){
            String filePath=Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E行";
            File file=new File(filePath);
            File[] maps=file.listFiles();
            for(int i=0;i<maps.length;++i){
                if(maps[i].toString().contains(prefix)){
                    maps[i].delete();
                }
            }
        }
    }
}
