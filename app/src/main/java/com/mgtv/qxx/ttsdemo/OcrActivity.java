package com.mgtv.qxx.ttsdemo;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.mgtv.qxx.ttsdemo.process.ParseImage;

import java.io.File;
import java.io.IOException;

public class OcrActivity extends AppCompatActivity {

    private TextView textRecognitionResult;
    TessBaseAPI baseApi;
    private String picFile;
    private EditText etSelectPicture;
    private Button btnSelectPic;
    private ImageView imgShow=null;
    private String lang = "Chinese";
    private String transLang = "";
    private boolean bImageProcessing = false;

    private static final String TESSERACT_ROOT = "/sdcard2/tesseract/";
    private static final String ENGLISH_LANGUAGE = "eng";
    private static final String CHINESE_LANGUAGE = "chi_sim";
    private static final int ACTIVITY_GET_IMAGE = 10;
    private static final String LOG_TAG = "OcrActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);
//        Log.e("OcrActivity", "OcrActivity");

        // 采用budle的方式
        Bundle extras  = this.getIntent().getExtras();

        //接收name值
        picFile  = extras.getString("picture_path");
        lang = extras.getString("ocr_language");
        if (lang.equalsIgnoreCase("Chinese") || lang.equalsIgnoreCase("中文")){
            transLang = CHINESE_LANGUAGE;
        } else {
            transLang = ENGLISH_LANGUAGE;
        }
        bImageProcessing = Boolean.getBoolean(extras.getString("ImageProcessing"));

        // Log.e("OcrActivity",picFile);
        etSelectPicture = (EditText)findViewById(R.id.et_select_picture);
        etSelectPicture.setText(picFile);

        imgShow=(ImageView) findViewById(R.id.imgShow);
        if (picFile != null && !picFile.isEmpty()){
            imgShow.setImageBitmap(getDiskBitmap(picFile));
        }

        btnSelectPic = (Button) findViewById(R.id.btn_select_picture);

        textRecognitionResult=new TextView(getBaseContext());
        textRecognitionResult=(TextView)findViewById(R.id.et_recognition_result);

        findViewById(R.id.btn_recognition).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View sourse) {
                //设置要ocr的图片bitmap
                // Log.e("OCRActivity", "parse" + picFile);
                try {
                    picFile = etSelectPicture.getText().toString();
                    testOCR();
                    // String textRecognized = ParseImage.parseImageToString(picFile,bImageProcessing);
                    // textRecognitionResult.setText(textRecognized);
                }catch (Exception e){
                    Log.e(LOG_TAG,e.toString());
                }
                }
            }
        );
        btnSelectPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new  Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,ACTIVITY_GET_IMAGE);
            }
        });
    }

    /**
     * 异步任务，识别图片
     *
     * @author duanbokan
     *
     */
    public class parseImageAsync extends AsyncTask<String, Integer, String>
    {
        @Override
        protected void onPreExecute()
        {
            textRecognitionResult.setText("正在识别，请稍等");
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params)
        {
            String result = "";

            try
            {
                result = ParseImage.getInstance().parseImageToString(params[0],bImageProcessing);
            }
            catch (IOException e)
            {
                result = "";
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (result != null && !result.equals(""))
            {
                textRecognitionResult.setText("识别完毕，结果为： \n" + result);
            }
            else
            {
                textRecognitionResult.setText("识别失败");
            }
            super.onPostExecute(result);
        }
    }

    /**
     * 测试OCR识别
     */
    public void testOCR()
    {
        // 先判断识别语言文件是否存在

        // 启动异步任务进行识别
        new parseImageAsync().execute(picFile);

        // 显示当前图片
        if (picFile != null && !picFile.equals(""))
        {
            Bitmap bitmap = BitmapFactory.decodeFile(picFile);
            imgShow.setImageBitmap(bitmap);
        }
    }

    private Bitmap getDiskBitmap(String pathString)
    {
        Bitmap bitmap = null;
        try
        {
            File file = new File(pathString);
            if(file.exists())
            {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                // 设置为ture只获取图片一半大小
                opts.inSampleSize=1;
                bitmap = BitmapFactory.decodeFile(pathString,opts);
                //Bitmap newbitmap = BitmapFactory.decodeResource(bitmap.getRowBytes(),R.drawable.ic_vd_mic_on); //createBitmap(bitmap.getWidth(), bitmap.getHeight(),null);
            }else{
                Toast.makeText(this,"file not exists",Toast.LENGTH_LONG).show();
            }
        } catch (Exception e)
        {
            // TODO: handle exception
            Log.e("Ocr-getDiskBitmap",e.toString());
            Toast.makeText(this,"OcrActivity getDiskBitmap" + e.toString(),Toast.LENGTH_LONG).show();
        }
        return bitmap;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (ACTIVITY_GET_IMAGE == requestCode){
            // Log.e("onActivityResult Code",String.valueOf(resultCode));
            // Log.e("onActivityResult data",data.getData().toString());
            if(resultCode  == RESULT_OK){
                //得到文件的Uri
                Uri uri = data.getData(); //获得图片的uri
                //外界的程序访问ContentProvider所提供数据 可以通过ContentResolver接口
                ContentResolver resolver = getContentResolver();
                try {
                    Bitmap bm = MediaStore.Images.Media.getBitmap(resolver, uri);
                    //显得到bitmap图片
                    if (bm != null) {
                        imgShow.setImageBitmap(bm);
                    }
                }catch (IOException e) {
                    Log.e("ACTIVITY_GET_IMAGE",e.toString());
                }
                // 这里开始的第二部分，获取图片的路径：
                // ContentResolver对象的getType方法可返回形如content://的Uri的类型
                // 如果是一张图片，返回结果为image/jpeg或image/png等
                String fileType = resolver.getType(uri);
                if (fileType == null || fileType.isEmpty()){
                    String path = uri.getPath();
                    Log.e("path",path);
                    fileType = path.substring(path.lastIndexOf(".")+1,path.length());
                }
                Log.i("fileType",fileType);
                if(fileType.startsWith("image"))//判断用户选择的是否为图片
                {
                    //根据返回的uri获取图片路径
                    Cursor cursor = resolver.query(uri,  new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                    cursor.moveToFirst();
                    String  path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    //data=Intent { dat=content://com.android.externalstorage.documents/document/6005-19D5:Pictures/Screenshots/Screenshot_2016-05-29-10-45-34.png flg=0x1 }}

                    if (path!=null && !path.isEmpty()){
                        Log.d("image path",path);
                        //do  anything you want
                    }else {
                        path = QxxExec.translateAbsolutePath(data.getData().getPath());
                        Log.i("get image path",path);
                    }
                    picFile = path;
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
