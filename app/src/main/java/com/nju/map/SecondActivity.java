package com.nju.map;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.uuzuche.lib_zxing.activity.CaptureFragment;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import android.app.Instrumentation;

import static android.app.Activity.RESULT_OK;

/**
 * 定制化显示扫描界面
 */
public class SecondActivity extends AppCompatActivity{
    public static final int CHOOSE_IMAGE = 2; //打开相册选取图片，扫描二维码
    private TextView album = null;
    private TextView loadLatest = null;
    private ImageView buttonflash = null;
    private CaptureFragment captureFragment;
    private boolean islight=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        captureFragment = new CaptureFragment();
        CodeUtils.setFragmentArgs(captureFragment, R.layout.my_camera); // 为二维码扫描界面设置定制化界面
        captureFragment.setAnalyzeCallback(analyzeCallback);
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_my_container, captureFragment).commit(); //动态加载容器内容
        album = (TextView) findViewById(R.id.album);
        loadLatest=(TextView)findViewById(R.id.loadLatest);
        buttonflash=(ImageView) findViewById(R.id.flash);
        buttonflash.setImageResource(R.drawable.torch_off);
        init();

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish(); // back button
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init() {
        album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, CHOOSE_IMAGE);
            }
        });
        buttonflash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!islight) {
                    CodeUtils.isLightEnable(true);
                    buttonflash.setImageResource(R.drawable.torch_on);
                    islight = true;
                } else {
                    CodeUtils.isLightEnable(false);
                    buttonflash.setImageResource(R.drawable.torch_off);
                    islight = false;
                }
            }
        });
        loadLatest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SecondActivity.this, ThirdActivity.class);
                intent.putExtra("flag",true);
                startActivity(intent);
            }
        });
    }

    //相册调用结果返回参数
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_IMAGE) {
            if (data != null) {
                Uri uri = data.getData();
                try {
                    CodeUtils.analyzeBitmap(ImageUtil.getImageAbsolutePath(this, uri), new CodeUtils.AnalyzeCallback() {
                        @Override
                        public void onAnalyzeSuccess(Bitmap mBitmap, String result) {
                            changeMap(result);
                        }

                        @Override
                        public void onAnalyzeFailed() {
                            Toast.makeText(SecondActivity.this, "获取失败", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //更改地图，转到ThirdActivity
    private void changeMap(String result) {
        if(result.contains("ground_floor"))
            Toast.makeText(getApplicationContext(), "目前位于一楼", Toast.LENGTH_LONG).show();
        else if(result.contains("first_floor"))
            Toast.makeText(getApplicationContext(), "目前位于二楼", Toast.LENGTH_LONG).show();
        else if(result.contains("second_floor"))
            Toast.makeText(getApplicationContext(), "目前位于三楼", Toast.LENGTH_LONG).show();
        else if(result.contains("third_floor"))
            Toast.makeText(getApplicationContext(), "目前位于四楼", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(SecondActivity.this,ThirdActivity.class);
        intent.putExtra("flag",false);
        intent.putExtra("photo",result);
        startActivity(intent);
        finish();
    }

    //二维码解析回调函数
    CodeUtils.AnalyzeCallback analyzeCallback = new CodeUtils.AnalyzeCallback() {
        @Override
        public void onAnalyzeSuccess(Bitmap mBitmap, String result) {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt(CodeUtils.RESULT_TYPE, CodeUtils.RESULT_SUCCESS);
            bundle.putString(CodeUtils.RESULT_STRING, result);
            resultIntent.putExtras(bundle);
            SecondActivity.this.setResult(RESULT_OK, resultIntent);
            SecondActivity.this.finish();
        }




        @Override
        public void onAnalyzeFailed() {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt(CodeUtils.RESULT_TYPE, CodeUtils.RESULT_FAILED);
            bundle.putString(CodeUtils.RESULT_STRING, "");
            resultIntent.putExtras(bundle);
            SecondActivity.this.setResult(RESULT_OK, resultIntent);
            SecondActivity.this.finish();
        }
    };


}
