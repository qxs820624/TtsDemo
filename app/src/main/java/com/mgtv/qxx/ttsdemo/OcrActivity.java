package com.mgtv.qxx.ttsdemo;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;

public class OcrActivity extends AppCompatActivity {

    private TextView textRecognitionResult;
    TessBaseAPI baseApi;
    private String picFile;
    private EditText etSelectPicture;
    private Button btnSelectPic;

    private static final int ACTIVITY_GET_CONTENT = 10;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        // 采用budle的方式
        Bundle extras  = this.getIntent().getExtras();

        //接收name值
        picFile  = extras.getString("picture_path");
        // Log.e("OcrActivity",picFile);
        etSelectPicture = (EditText)findViewById(R.id.et_select_picture);
        etSelectPicture.setText(picFile);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        btnSelectPic = (Button) findViewById(R.id.btn_select_picture);

        Button btRecognition=new Button(getBaseContext());
        btRecognition=(Button)findViewById(R.id.btn_recognition);

        textRecognitionResult=new TextView(getBaseContext());
        textRecognitionResult=(TextView)findViewById(R.id.tv_recognition_result);

        baseApi=new TessBaseAPI();
        boolean bInitBaseApi = baseApi.init("/sdcard2/tesseract/", "eng");
        if (!bInitBaseApi)  {
            Log.e("OCRActivity", "baseApi init Failed!");
            Toast.makeText(this, this.getResources().getString(R.string.ocr_init_faile_prompt), Toast.LENGTH_LONG).show();
            finish();
        }

        btRecognition.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View sourse) {
                    //设置要ocr的图片bitmap
                    Bitmap bm = getDiskBitmap(picFile);
                    if (bm != null){
                        baseApi.setImage(bm);
                        //根据Init的语言，获得ocr后的字符串
                        String text1= baseApi.getUTF8Text();
                        textRecognitionResult.setText(text1);
                    }else {
                        textRecognitionResult.setText("SBBBBBBBBBBBBBBBBBBBB");
                    }
                    //释放bitmap
                    baseApi.clear();
                }
            }
        );
        btnSelectPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new  Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/bmp");
                startActivityForResult(intent,ACTIVITY_GET_CONTENT);
            }
        });
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
            }else{
                Toast.makeText(this,"file not exists",Toast.LENGTH_LONG).show();
            }
        } catch (Exception e)
        {
            // TODO: handle exception
        }


        return bitmap;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (ACTIVITY_GET_CONTENT == requestCode){
            // Log.e("onActivityResult Code",String.valueOf(resultCode));
            // Log.e("onActivityResult data",data.getData().toString());
            if(resultCode  == RESULT_OK){
                //得到文件的Uri
                Uri uri = data.getData();
                ContentResolver resolver = getContentResolver();
                //ContentResolver对象的getType方法可返回形如content://的Uri的类型
                //如果是一张图片，返回结果为image/jpeg或image/png等
                String fileType = resolver.getType(uri);
                if (fileType == null || fileType.isEmpty()){
                    String path = uri.getPath();
                    fileType = path.substring(path.lastIndexOf(".")+1,path.length());
                }
                if(fileType.startsWith("image"))//判断用户选择的是否为图片
                {
                    //根据返回的uri获取图片路径
                    Cursor cursor = resolver.query(uri,  new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                    cursor.moveToFirst();
                    String  path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

                    //do  anything you want
                    etSelectPicture.setText(path);
                }
            }
        }
    }

    /**
     * Created on 2010-7-13
     * <p>Discription:[convert GIF->JPG GIF->PNG PNG->GIF(X) PNG->JPG ]</p>
     * @param source
     * @param formatName
     * @param result
     * @author:[shixing_11@sina.com]
     */
    /*
    public static void convert(String source, String formatName, String result)
    {
        try
        {
            File f = new File(source);
            f.canRead();
            BufferedImage src = ImageIO.read(f);
            ImageIO.write(src, formatName, new File(result));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    */
}
