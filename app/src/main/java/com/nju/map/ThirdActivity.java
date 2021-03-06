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
    public static final int REQUEST_BLUETOOTH = 103; //??????????????????
    public static final int SEE_MAP = 3;//???????????????????????????
    public static final int DOC_COUNT = 1001;//Message?????????
    private Toolbar toolbar;
    private TextView mapViewer;
    private ImageView imageView=null;//?????????
    private ImageView canvasView=null;//?????????
    private ImageView initPointView=null;//?????????
    private TextView nameText=null;
    private TextView directionText=null;
    private ProgressDialog mProgressDialog=null;
    private GuidePopupWindow guidePopupWindow=null;
    private String data;//?????????????????????
    private int screenWidth;//???????????? 1080
    private int screenHeight;//???????????? 1920-title??????
    private int init_X=0;//?????????????????????
    private int init_Y=0;
    private int dest_X=0;//??????????????????
    private int dest_Y=0;
    private Map map=null;//????????????????????????
    private ArrayList<Integer> Xarray = new ArrayList<>();//???????????????
    private ArrayList<Integer> Yarray = new ArrayList<>();
    private ArrayList<Integer> digitalX = new ArrayList<>();//???????????????
    private ArrayList<Integer> digitalY = new ArrayList<>();
    public static enum Orientation{North,South,West,East};//???????????????
    Orientation orientation;
    private boolean isDrawingPath=false;//???????????????
    private boolean isImageValid=false;//????????????????????????
    private boolean saveImage=false;//??????????????????
    private int docCount=0;//????????????
    private int mapsNum;//???????????????
    private int currentIndex;//??????????????????
    private int nextIndex;//?????????????????????????????????
    private String prefix;//??????????????????
    private int scale;//???????????????
    private Intent BLEintent;//????????????????????????
    private SensorManager manager;//???????????????
    private Sensor accelerometer; // ??????????????????
    private Sensor magnetic; // ??????????????????
    private Sensor stepDetector; //???????????????
    private MySensorEventListener mListener;//??????????????????
    private float[] accelerometerValues = new float[3];//????????????????????????
    private float[] magneticFieldValues = new float[3];//?????????????????????
    private float[] values = new float[3];//??????????????????
    private String xmlMapData;//????????????XML?????????????????????????????????
    private String xmlRoomData;//????????????XML?????????????????????????????????
    private SharedPreferences mSharedPreferences;//????????????????????????????????????
    private float ratio;//X?????????????????????
    private float offset;//Y???????????????
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
    }; //?????????????????????????????????
    private BroadcastReceiver locationChangeReceiver = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            iBeacon ibeacon =BLEService.ibeaconQuery();
            if(ibeacon!=null){
                locationQuery(ibeacon);
            }
            BLEintent = new Intent(getApplicationContext(), BLEService.class);
            startService(BLEintent);
        }
    }; //?????????????????????BLEService??????????????????????????????????????????????????????????????????

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
        mProgressDialog.setTitle("??????");
        mProgressDialog.setMessage("???????????????...");
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

    //?????????????????????
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
            Toast.makeText(getApplicationContext(),"????????????????????????",Toast.LENGTH_SHORT).show();
        } else {
            try {
                prefix=new String(data);
                prefix=prefix.replaceAll("\\d", "");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (fileExists(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E???/"+data+".png")) {
                saveImage=true;
                xmlMapData=readTxtFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/E???/" + prefix + ".txt");
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
                Toast.makeText(getApplicationContext(),"?????????????????????????????????",Toast.LENGTH_SHORT).show();
            }
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(imageView.getDrawable()==null){
                    mProgressDialog.dismiss();
                    isImageValid=false;
                    imageView.setImageResource(R.drawable.not_found);
                    Toast.makeText(getApplicationContext(),"???????????????...",Toast.LENGTH_SHORT).show();
                }
            }
        },10000);
    }

    //??????????????????onIntro?????????...
    private void popWindowInit(){
        guidePopupWindow=new GuidePopupWindow(this, new GuideClickListener() {
            @Override
            public void onEnd() {
                guidePopupWindow.dismiss();
            }
        });
        guidePopupWindow.setOnDismissListener(this);
    }

    //?????????????????????
    private void bluetoothInit(){
        BLEintent = new Intent(getApplicationContext(), BLEService.class);
        startService(BLEintent);
    }

    //????????????????????????????????????,????????????????????????????????????
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

    //?????????????????????????????????????????????????????????????????????????????????????????????
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

    //???????????????,????????????????????????????????????isImageValid??????
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
        canvas.save(); // ????????????
        canvas.translate((float)init_X*ratio,(float)(init_Y+offset)*ratio); // ????????????
        canvas.rotate((float)Math.toDegrees(values[0])); // ????????????
        canvas.drawPath(mArrowPath, paint);
        canvas.restore(); // ????????????
        Drawable drawable=new BitmapDrawable(bitmap) ;
        initPointView.setBackgroundDrawable(drawable);
    }

    //?????????????????????????????????????????????
    private void mapRefresh(){
        mProgressDialog.dismiss();
        try{
            String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E???/"+data+".png";
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
            Toast.makeText(getApplicationContext(),"????????????????????????",Toast.LENGTH_SHORT).show();
        }
    }

    //??????????????????,drawPath?????????
    private void directionGuide(){
        int x=Xarray.get(0);
        int y=Yarray.get(0);
        int temp=0;
        if(init_X==x&&init_Y>y){
            temp=(init_Y-y)/scale;
            directionText.setText("????????????"+Integer.toString(temp)+"???");
        }else if(init_X==x&&init_Y<y){
            temp=(y-init_Y)/scale;
            directionText.setText("????????????"+Integer.toString(temp)+"???");
        }else if(init_X>x&&init_Y==y){
            temp=(init_X-x)/scale;
            directionText.setText("????????????"+Integer.toString(temp)+"???");
        }else if(init_X<x&&init_Y==y){
            temp=(x-init_X)/scale;
            directionText.setText("????????????"+Integer.toString(temp)+"???");
        }
    }

    //??????????????????
    private void cleanMap(){
        Bitmap bitmap=Bitmap.createBitmap(screenWidth,screenHeight,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
        Drawable drawable = new BitmapDrawable(bitmap);
        canvasView.setBackgroundDrawable(drawable);
    }

    //????????????????????????
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

    //??????????????????
    private void reachDest(){
        if(((init_X==dest_X)&&(Math.abs(init_Y-dest_Y)<5*scale))|| ((init_Y==dest_Y)&&(Math.abs(init_X-dest_X)<5*scale))){
            cleanMap();
            isDrawingPath=false;
            guidePopupWindow.dismiss();
        }
    }

    //??????????????????????????????????????????
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

    //????????????????????????????????????????????????????????????????????????
    private boolean isOnPath(){
        for(int i=0;i<Xarray.size()-1;i++){
            if(((Xarray.get(i).equals(Xarray.get(i+1)))&&(Xarray.get(i)==init_X))||((Yarray.get(i).equals(Yarray.get(i+1)))&&(Yarray.get(i)==init_Y)))
                return true;
        }
        return false;
    }

    //??????????????????
    private void showGuideWindow() {
        isDrawingPath=true;
        if (guidePopupWindow == null) return;
        guidePopupWindow.showAtLocation(findViewById(R.id.myLayout), Gravity.BOTTOM,0,0);
    }

    //????????????????????????
    public static boolean isNetAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return (info != null && info.isAvailable());
    }

    //?????????????????????????????????
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

    //?????????????????????
    public void shareWeChat(String path){
        Uri uriToImage = Uri.fromFile(new File(path));
        Intent shareIntent = new Intent();
        //????????????????????????
        //ComponentName comp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI");
        //????????????????????????
        ComponentName comp = new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI");
        shareIntent.setComponent(comp);
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);
        shareIntent.setType("image/*");
        startActivity(Intent.createChooser(shareIntent, "????????????"));
    }

    //??????????????????????????????
    private void getAndSaveCurrentImage(Context mContext) {
        //1.??????Bitmap
        Bitmap bmp = Bitmap.createBitmap(screenWidth,screenHeight,Bitmap.Config.ARGB_8888);
        //2.????????????
        View decorview = this.getWindow().getDecorView();
        decorview.draw(new Canvas(bmp));
        String SavePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/E???/ScreenImage";
        //3.??????Bitmap
        try {
            File path = new File(SavePath);
            //??????
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

    //???????????????ibeacon??????????????????????????????????????????????????????????????????????????????????????????...
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
            String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E???/"+data+".png";
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

    //????????????????????????????????????????????????????????????Y?????????????????????????????????????????????????????????
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

    //???????????????????????????????????????????????????,??????????????????????????????????????????
    private void tryMove(){
        int deltaX=0;
        int deltaY=0;
        boolean movable;//??????????????????
        int[] exitable=new int[3];//??????????????????
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
            String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E???/"+data+".png";
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

    //???????????????
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

    //??????????????????????????????
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

    //??????????????????????????????
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

    //????????????
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
                        File appDir = new File(Environment.getExternalStorageDirectory(),"E???");
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

    //???????????????????????????????????????XML??????
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

    //??????txt??????
    private String readTxtFile(String strFilePath) {
        String path = strFilePath;
        String content = ""; //?????????????????????
        //????????????
        File file = new File(path);
        //??????path????????????????????????????????????????????????????????????
        try {
            InputStream instream = new FileInputStream(file);
            if (instream != null)
            {
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line;
                //????????????
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

    //?????????????????????????????????txt??????
    private void saveFile(String str, String name) {
        String filePath = null;
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/E???/" + name + ".txt";
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
                    shareWeChat(Environment.getExternalStorageDirectory().getAbsolutePath()+"/E???/ScreenImage"+"/Screen_"+data+".png");
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
                        String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E???/"+data+".png";
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
                        nameText.setText("?????????"+returnData.getStringExtra("destName"));
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
                            Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                } else {
                    Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
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
            String filePath=Environment.getExternalStorageDirectory().getAbsolutePath()+"/E???/ScreenImage";
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
            String filePath=Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E???";
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
