package com.nju.map;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import com.nju.map.DigitalMap.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

public class FourthActivity extends AppCompatActivity implements PopupWindow.OnDismissListener {
    static final int CHANGE_INIT = 4;
    static final int CHANGE_DEST = 5;
    private ImageView mapViewer;
    private ImageView pointViewer;
    private TextView searchRoom;
    private SetPopupWindow setPopupWindow;
    private String prefix;//图片前缀
    private Map map;//地图标签，抽象类
    private int currentIndex;//图片标签
    private int nextIndex;//滑动后的地图标签
    private String data;//图片信息（前缀+标签)
    private int mPosX;//按下时的横坐标
    private int mPosY;//按下时的纵坐标
    private int mCurrentPosX;//抬起时的横坐标
    private int mCurrentPosY;//抬起时的纵坐标
    private String destName;//目标名字
    private boolean saveInitPoint=false;//是否保留当前位置点
    private boolean saveDestPoint=false;//是否保留目的位置点
    private int temp_x;//点击的位置横坐标
    private int temp_y;//点击的位置纵坐标
    private int final_x;//确认保留的位置横坐标
    private int final_y;//确认保留的位置纵坐标
    private int screenWidth;
    private int screenHeight;
    private float ratio;
    private float offset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        //if(actionBar != null){
            //actionBar.setHomeButtonEnabled(true);
            //actionBar.setDisplayHomeAsUpEnabled(true);
        //}
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fourth);
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mapViewer=(ImageView)findViewById(R.id.mapViewer);
        pointViewer=(ImageView)findViewById(R.id.pointView);
        searchRoom=(TextView)findViewById(R.id.searchRoom);
        Intent intent=getIntent();
        prefix=intent.getStringExtra("prefix");
        currentIndex=intent.getIntExtra("currentIndex",0);
        screenWidth=intent.getIntExtra("screenWidth",0);
        screenHeight=intent.getIntExtra("screenHeight",0);
        nextIndex=currentIndex;
        data=prefix+Integer.toString(currentIndex);
        mapRefresh();
        popWindowInit();
        touchInit();
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish(); // back button
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //界面操作初始化
    private void touchInit(){
        pointViewer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    // 按下
                    case MotionEvent.ACTION_DOWN:
                        mPosX = (int)event.getX();
                        mPosY = (int)event.getY();
                        break;
                    // 拿起
                    case MotionEvent.ACTION_UP:
                        mCurrentPosX = (int)event.getX();
                        mCurrentPosY = (int)event.getY();
                        if (Math.abs(mCurrentPosX - mPosX) < 10 && Math.abs(mCurrentPosY - mPosY) < 10){
                            //单纯点击事件
                            map.standardization(mCurrentPosX,mCurrentPosY);
                            temp_x = map.getX();
                            temp_y = map.getY();
                            if(map.getFlag()){
                                saveInitPoint=false;
                                saveDestPoint=false;
                                destName=map.getDestName();
                                cleanMap();
                                drawPoint();
                                showSetWindow();
                            }
                            return false;
                        }
                        //滑动事件
                        if (mCurrentPosX - mPosX > 200 && Math.abs(mCurrentPosY - mPosY) < 100)
                            //右滑
                            nextIndex=map.goLeft();
                        else if (mCurrentPosX - mPosX < -200 && Math.abs(mCurrentPosY - mPosY) < 100)
                            //左滑
                            nextIndex=map.goRight();
                        else if (mCurrentPosY - mPosY > 200 && Math.abs(mCurrentPosX - mPosX) < 100)
                            //下滑
                            nextIndex=map.goUp();
                        else if (mCurrentPosY - mPosY < -200 && Math.abs(mCurrentPosX - mPosX) < 100)
                            //上滑
                            nextIndex=map.goDown();
                        mapChange();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        searchRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater factory = LayoutInflater.from(FourthActivity.this);
                final View searchView = factory.inflate(R.layout.search_room_dialog, null);
                final EditText searchRoomEdit=(EditText)searchView.findViewById(R.id.searchRoomEdit);
                final AlertDialog.Builder builder = new AlertDialog.Builder(FourthActivity.this);
                builder.setTitle("查找");
                builder.setView(searchView);
                builder.setCancelable(false);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        destName = searchRoomEdit.getText().toString();
                        int[] temp=getRoomInfo(destName);
                        if(temp!=null){
                            nextIndex=temp[0];
                            temp_x=temp[1];
                            temp_y=temp[2];
                            mapChange();
                            cleanMap();
                            drawPoint();
                            showSetWindow();
                        }else{
                            Toast.makeText(getApplicationContext(),"未找到房间",Toast.LENGTH_SHORT).show();
                        }
                        try{
                            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                                    hideSoftInputFromWindow(searchRoomEdit.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
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
                        try{
                            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                                    hideSoftInputFromWindow(searchRoomEdit.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
                builder.show();
            }
        });
    }

    //弹窗初始化，onIntro完善中...
    private void popWindowInit(){
        setPopupWindow=new SetPopupWindow(this, new SetClickListener() {
            @Override
            public void onSetInit() {
                saveInitPoint=true;
                final_x=temp_x;
                final_y=temp_y;
                setPopupWindow.dismiss();
            }

            @Override
            public void onSetDest(){
                saveDestPoint=true;
                final_x=temp_x;
                final_y=temp_y;
                setPopupWindow.dismiss();
            }

            @Override
            public void onIntro() {
                Toast.makeText(getApplicationContext(),"还在完善中...",Toast.LENGTH_SHORT).show();
                setPopupWindow.dismiss();
            }
        });
        setPopupWindow.setOnDismissListener(this);
    }

    //画出点击位置
    private void drawPoint(){
        Bitmap bitmap=Bitmap.createBitmap(pointViewer.getWidth(),pointViewer.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Paint paint=new Paint();
        ratio=map.getRatio();
        offset=map.getOffset();
        paint.setStrokeWidth(5*ratio);
        if(saveDestPoint)
            paint.setColor(Color.BLUE);
        else
            paint.setColor(Color.RED);
        canvas.drawCircle((float)temp_x*ratio,(float)(temp_y+offset)*ratio,20*ratio,paint);
        Drawable drawable=new BitmapDrawable(bitmap) ;
        pointViewer.setBackgroundDrawable(drawable);
    }

    //清空位置点
    private void cleanMap(){
        Bitmap bitmap=Bitmap.createBitmap(pointViewer.getWidth(),pointViewer.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
        Drawable drawable = new BitmapDrawable(bitmap);
        pointViewer.setBackgroundDrawable(drawable);
    }

    //更新显示的地图,并修改地图标签
    private void mapRefresh(){
        map=MapFactory.getMap(data,screenWidth,screenHeight);
        String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+ "/E行/"+data+".png";
        File file=new File(filePath);
        if (file.exists()){
            Bitmap bitmap= BitmapFactory.decodeFile(filePath);
            mapViewer.setImageBitmap(bitmap);
        }
    }

    //在滑动后调用，如果标签变化，则更换地图
    private void mapChange(){
        if(nextIndex!=currentIndex){
            saveInitPoint=false;
            saveDestPoint=false;
            cleanMap();
            data=prefix+Integer.toString(nextIndex);
            currentIndex=nextIndex;
            mapRefresh();
        }
    }

    //显示设置位置窗口
    private void showSetWindow(){
        if(setPopupWindow==null)return;
        setPopupWindow.showAtLocation(findViewById(R.id.myLayout), Gravity.BOTTOM,0,0);
    }

    //获取房间信息
    public  int[] getRoomInfo(String room){
        String str=Environment.getExternalStorageDirectory().getAbsolutePath() + "/E行/" + prefix+"room_data" + ".txt";
        return parsemapXML(readTxtFile(str),room);
    }

    //解析下载到的包含房间信息的XML文件
    private  static int[] parsemapXML(String xmlData, String roomName) {
        int temp[] = new int[3];
        String name= "";
        String index="";
        String mx="";
        String my="";
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = factory.newPullParser();
            xmlPullParser.setInput(new StringReader(xmlData));
            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String nodeName = xmlPullParser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG: {
                        if ("roomname".equals(nodeName)) {
                            name= xmlPullParser.nextText();
                        } else if ("roomindex".equals(nodeName)) {
                            index= xmlPullParser.nextText();
                        } else if ("roomx".equals(nodeName)){
                            mx= xmlPullParser.nextText();
                        } else if ("roomy".equals(nodeName)) {
                            my = xmlPullParser.nextText();
                        }
                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        if("room".equals(nodeName)){
                            if (name.equals(roomName)) {
                                temp[0] = Integer.parseInt(index);
                                temp[1] = Integer.parseInt(mx);
                                temp[2] = Integer.parseInt(my);
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

    @Override
    public void onDismiss() {
        if(!saveInitPoint)
            cleanMap();
        if(saveDestPoint)
            drawPoint();
    }

    @Override
    public void onBackPressed() {
        if(saveInitPoint){
            int[] temp=new int[3];
            temp[0]=currentIndex;
            temp[1]=final_x;
            temp[2]=final_y;
            Intent intent=new Intent();
            intent.putExtra("result",temp);
            this.setResult(CHANGE_INIT,intent);//需要更换图片和初始点
        }else if(saveDestPoint){
            int[] temp=new int[3];
            temp[0]=currentIndex;
            temp[1]=final_x;
            temp[2]=final_y;
            Intent intent=new Intent();
            intent.putExtra("result",temp);
            intent.putExtra("destName",destName);
            this.setResult(CHANGE_DEST,intent);//需要更换图片和添加目标点
        } else{
            this.setResult(0);//不需要更换图片和位置点
        }
        super.onBackPressed();
    }

}
