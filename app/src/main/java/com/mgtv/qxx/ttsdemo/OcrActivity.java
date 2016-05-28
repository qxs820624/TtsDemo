package com.mgtv.qxx.ttsdemo;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class OcrActivity extends AppCompatActivity {

    private TextView textRecognitionResult;
    TessBaseAPI baseApi;
    private String picFile;
    private EditText etSelectPicture;
    private Button btnSelectPic;
    private ImageView imgShow=null;
    private String lang = "Chinese";
    private String transLang = "";

    private static final String TESSERACT_ROOT = "/sdcard2/tesseract/";
    private static final String ENGLISH_LANGUAGE = "eng";
    private static final String CHINESE_LANGUAGE = "chi_sim";
    private static final int ACTIVITY_GET_IMAGE = 10;
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
        // Log.e("OcrActivity",picFile);
        etSelectPicture = (EditText)findViewById(R.id.et_select_picture);
        etSelectPicture.setText(picFile);

        imgShow=(ImageView) findViewById(R.id.imgShow);
        imgShow.setImageBitmap(getDiskBitmap(picFile));

        btnSelectPic = (Button) findViewById(R.id.btn_select_picture);

        textRecognitionResult=new TextView(getBaseContext());
        textRecognitionResult=(TextView)findViewById(R.id.et_recognition_result);

        baseApi=new TessBaseAPI();
        // baseApi.init(TESSBASE_PATH, CHINESE_LANGUAGE+CHINESE_LANGUAGE); //多字库使用
        boolean bInitBaseApi = baseApi.init(TESSERACT_ROOT, transLang);
        baseApi.setPageSegMode(TessBaseAPI.PSM_AUTO);
        if (!bInitBaseApi)  {
            Log.e("OCRActivity", "baseApi init Failed!");
            Toast.makeText(this, this.getResources().getString(R.string.ocr_init_faile_prompt), Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.btn_recognition).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View sourse) {
                    //设置要ocr的图片bitmap
                    // Log.e("OCRActivity", "parse" + picFile);

                    Bitmap bm = PreProcess(getDiskBitmap(picFile));
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
                intent.setType("image/*");
                startActivityForResult(intent,ACTIVITY_GET_IMAGE);
            }
        });
    }

    public Bitmap PreProcess(Bitmap bmpFile) {
        // Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //在这里创建了一张bitmap
        // mBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.ic_vd_mic_on);
        //将这张bitmap设置为背景图片
        //setBackgroundDrawable(new BitmapDrawable(mBitmap));
        Bitmap newBmpFile;
        int iBitmapWidth = bmpFile.getWidth();
        int iBitmapHeight = bmpFile.getHeight();

        int iArrayColorLengh = iBitmapWidth * iBitmapHeight;
        try{

            //创建一个临时文件
            File file = new File("/sdcard2/tmp/tmp.txt");
            file.getParentFile().mkdirs();

            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, iArrayColorLengh *4);

            //将位图信息写进buffer
            bmpFile.copyPixelsToBuffer(map);

            //释放原位图占用的空间
            bmpFile.recycle();
            //创建一个新的位图
            newBmpFile = Bitmap.createBitmap(iBitmapWidth, iBitmapHeight, Bitmap.Config.ARGB_8888);
            map.position(0);
            //从临时缓冲中拷贝位图信息
            newBmpFile.copyPixelsFromBuffer(map);
            channel.close();
            randomAccessFile.close();
            // -----------------=============================
            int iArrayColor[]  = new int[iArrayColorLengh];
            int count = 0;
            for (int y = 0; y < iBitmapHeight; y++) {
                for (int x = 0; x < iBitmapWidth; x++) {
                    //获得Bitmap 图片中每一个点的color颜色值
                    int color = newBmpFile.getPixel(x, y);
                    //将颜色值存在一个数组中 方便后面修改
                    iArrayColor[count] = color;
                    //如果你想做的更细致的话 可以把颜色值的R G B 拿到做响应的处理 笔者在这里就不做更多解释
                    int r = Color.red(color);
                    int g = Color.green(color);
                    int b = Color.blue(color);

                    count++;

                    //去掉边框
                    if (x == 0 || y == 0 || x == iBitmapWidth - 1 || y == iBitmapHeight - 1)
                    {
                        newBmpFile.setPixel(x, y, Color.WHITE);
                    }else {
                        //如果点的颜色是背景干扰色，则变为白色
                        if (color == Color.rgb(204, 204, 51) ||
                                color == Color.rgb(153, 204, 51) ||
                                color == Color.rgb(204, 255, 102) ||
                                color == Color.rgb(204, 204, 204) ||
                                color == Color.rgb(204, 255, 51))
                        {
                            newBmpFile.setPixel(x, y, Color.WHITE);
                        }
                    }
                }
            }
        }
        catch(Exception ex){
            newBmpFile=null;
        }
        return newBmpFile;
        // long startTime = System.currentTimeMillis();
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
                    imgShow.setImageBitmap(bm);
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
                Log.e("fileType",fileType);
                if(fileType.startsWith("image"))//判断用户选择的是否为图片
                {
                    //根据返回的uri获取图片路径
                    Cursor cursor = resolver.query(uri,  new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                    cursor.moveToFirst();
                    String  path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

                    Log.e("image path",path);
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
