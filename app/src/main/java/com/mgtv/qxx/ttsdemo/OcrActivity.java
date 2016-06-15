package com.mgtv.qxx.ttsdemo;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.mgtv.qxx.ttsdemo.process.ImgPretreatment;
import com.mgtv.qxx.ttsdemo.process.ParseImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class OcrActivity extends AppCompatActivity {

    private static TextView textRecognitionResult;
    TessBaseAPI baseApi;
    private String picFile;
    private EditText etSelectPicture;
    private Button btnSelectPic;
    private ImageView imgShow=null;

    private static CheckBox chPreTreat;
    private static RadioGroup radioGroup;
    private static ImageView ivTreated;
    private static Bitmap bitmapTreated;
    private static Bitmap bitmapSelected;

    private String lang = "Chinese";
    private String transLang = "";
    private boolean bImageProcessing = false;
    private static String textResult;

    private static final String TESSERACT_ROOT = "/sdcard2/tesseract/";
    private static final String ENGLISH_LANGUAGE = "eng";
    private static final String CHINESE_LANGUAGE = "chi_sim";
    private static final int ACTIVITY_GET_IMAGE = 10;
    private static final String LOG_TAG = "OcrActivity";

    private static final int SHOWRESULT = 0x101;
    private static final int SHOWTREATEDIMG = 0x102;
    private static final int PHOTO_RESULT = 0x12;// 结果
    private static final int PHOTO_CAPTURE = 0x11;// 拍照

    //    設置文字識別的語言
    private static String LANGUAGE = "eng";

    // 该handler用于处理修改结果的任务
    public static Handler myHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOWRESULT:
                    if (textResult.equals(""))
                        textRecognitionResult.setText("识别失败");
                    else
                        textRecognitionResult.setText(textResult);
                    break;
                case SHOWTREATEDIMG:
                    textRecognitionResult.setText("识别中......");
                    showPicture(ivTreated, bitmapTreated);
                    break;
            }
            super.handleMessage(msg);
        }

    };

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
            bitmapSelected = getDiskBitmap(picFile);
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
        btnSelectPic.setOnClickListener(new selectButtonListener());

        chPreTreat = (CheckBox) findViewById(R.id.ch_threshold);
        radioGroup = (RadioGroup) findViewById(R.id.radiogroup);
        ivTreated = (ImageView) findViewById(R.id.iv_treated);

        // 用于设置解析语言
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_en:
                        LANGUAGE = "eng";
                        break;
                    case R.id.rb_ch:
                        LANGUAGE = "chi_sim";
                        break;
                }
            }

        });
    }

    // 将图片显示在view中
    public static void showPicture(ImageView iv, Bitmap bmp){
        iv.setImageBitmap(bmp);
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
            showPicture(ivTreated, bitmapTreated);
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
        //new parseImageAsync().execute(picFile);

        // 新线程来处理识别
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (chPreTreat.isChecked()) {
                    bitmapTreated = ImgPretreatment
                            .doPretreatment(bitmapSelected);
                    Message msg = new Message();
                    msg.what = SHOWTREATEDIMG;
                    myHandler.sendMessage(msg);
                    textResult = doOcr(bitmapTreated, LANGUAGE);
                } else {
                    bitmapTreated = ImgPretreatment
                            .converyToGrayImg(bitmapSelected);
                    Message msg = new Message();
                    msg.what = SHOWTREATEDIMG;
                    myHandler.sendMessage(msg);
                    textResult = doOcr(bitmapTreated, LANGUAGE);
                }
                Message msg2 = new Message();
                msg2.what = SHOWRESULT;
                myHandler.sendMessage(msg2);
            }

        }).start();

        // 显示当前图片
        if (picFile != null && !picFile.equals(""))
        {
            Bitmap bitmap = BitmapFactory.decodeFile(picFile);
            imgShow.setImageBitmap(bitmap);
        }
    }

    /**
     * 进行图片识别
     *
     * @param bitmap
     *            待识别图片
     * @param language
     *            识别语言
     * @return 识别结果字符串
     */
    public String doOcr(Bitmap bitmap, String language) {
        TessBaseAPI baseApi = new TessBaseAPI();

        baseApi.init(TESSERACT_ROOT, language);

        // 必须加此行，tess-two要求BMP必须为此配置
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        baseApi.setImage(bitmap);

        String text = baseApi.getUTF8Text();

        baseApi.clear();
        baseApi.end();

        return text;
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
                bitmapSelected = decodeUriAsBitmap(uri);
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
        }else if (requestCode == PHOTO_RESULT) {
            //得到文件的Uri
            Uri uri = data.getData(); //获得图片的uri

            bitmapSelected = decodeUriAsBitmap(uri/*Uri.fromFile(new File(IMG_PATH,"temp_cropped.jpg"))*/);
            if (chPreTreat.isChecked())
                textRecognitionResult.setText("预处理中......");
            else
                textRecognitionResult.setText("识别中......");
            // 显示选择的图片
            showPicture(imgShow, bitmapSelected);

            // 新线程来处理识别
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (chPreTreat.isChecked()) {
                        bitmapTreated = ImgPretreatment
                                .doPretreatment(bitmapSelected);
                        Message msg = new Message();
                        msg.what = SHOWTREATEDIMG;
                        myHandler.sendMessage(msg);
                        textResult = doOcr(bitmapTreated, LANGUAGE);
                    } else {
                        bitmapTreated = ImgPretreatment
                                .converyToGrayImg(bitmapSelected);
                        Message msg = new Message();
                        msg.what = SHOWTREATEDIMG;
                        myHandler.sendMessage(msg);
                        textResult = doOcr(bitmapTreated, LANGUAGE);
                    }
                    Message msg2 = new Message();
                    msg2.what = SHOWRESULT;
                    myHandler.sendMessage(msg2);
                }

            }).start();

        }else if (requestCode == PHOTO_CAPTURE) {
            textRecognitionResult.setText("abc");
            startPhotoCrop(Uri.fromFile(new File(TESSERACT_ROOT, "temp.jpg")));
        }
    }

    // 从相册选取照片并裁剪
    class selectButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("return-data", false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(TESSERACT_ROOT, "temp_cropped.jpg")));
            intent.putExtra("outputFormat",
                    Bitmap.CompressFormat.JPEG.toString());
            intent.putExtra("noFaceDetection", true); // no face detection
            startActivityForResult(intent, ACTIVITY_GET_IMAGE);
        }
    }

    // 響應菜單
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tts_settings, menu);
        return true;
    }


    /**
     * 调用系统图片编辑进行裁剪
     */
    public void startPhotoCrop(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(new File(TESSERACT_ROOT, "temp_cropped.jpg")));
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        startActivityForResult(intent, PHOTO_RESULT);
    }

    /**
     * 根据URI获取位图
     *
     * @param uri
     * @return 对应的位图
     */
    private Bitmap decodeUriAsBitmap(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver()
                    .openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
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
