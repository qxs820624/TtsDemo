package com.mgtv.qxx.ttsdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;

public class OcrActivity extends AppCompatActivity {

    private TextView text;
    TessBaseAPI baseApi;
    private String picFile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        // 采用budle的方式
        Bundle extras  = this.getIntent().getExtras();

        //接收name值
        picFile  = extras.getString("picture_path");
        Log.e("OcrActivity",picFile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        Button bt=new Button(getBaseContext());
        bt=(Button)findViewById(R.id.button1);

        text=new TextView(getBaseContext());
        text=(TextView)findViewById(R.id.textView1);

        baseApi=new TessBaseAPI();
        baseApi.init("/mnt/sdcard2/tesseract/", "eng");

        bt.setOnClickListener(new View.OnClickListener() {
                                  @Override
                                  public void onClick(View sourse) {
                // text.setText("sb");
                //设置要ocr的图片bitmap
                baseApi.setImage(getDiskBitmap(picFile));
                //根据Init的语言，获得ocr后的字符串
                String text1= baseApi.getUTF8Text();
                text.setText(text1);
                //释放bitmap
                baseApi.clear();
            }
            }
        );
    }

    private Bitmap getDiskBitmap(String pathString)
    {
        Bitmap bitmap = null;
        try
        {
            File file = new File(pathString);
            if(file.exists())
            {
                bitmap = BitmapFactory.decodeFile(pathString);

            }
        } catch (Exception e)
        {
            // TODO: handle exception
        }


        return bitmap;
    }
}
