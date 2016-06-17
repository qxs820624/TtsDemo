package com.mgtv.qxx.ttsdemo;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.mgtv.qxx.ttsdemo.process.ImgPretreatment;

import java.io.File;
import java.io.FileNotFoundException;

public class OcrActivity extends Activity {

    private static final int PHOTO_CAPTURE = 0x11;// 拍照
    private static final int PHOTO_RESULT = 0x12;// 结果

    private static String LOG_TAG = "OCRActivity";

    private static String LANGUAGE = "eng";
    private static String IMG_PATH = QxxExec.getExternalSDCardPath() + java.io.File.separator + "DCIM" + java.io.File.separator + "Ocr";

    private static TextView tvResult;
    private static ImageView ivSelected;
    private static ImageView ivTreated;
    private static Button btnCamera;
    private static Button btnSelect;
    private static CheckBox chPreTreat;
    private static RadioGroup radioGroup;
    private static String textResult;
    private static Bitmap bitmapSelected;
    private static Bitmap bitmapTreated;
    private static final int SHOWRESULT = 0x101;
    private static final int SHOWTREATEDIMG = 0x102;

    // 该handler用于处理修改结果的任务
    public static Handler myHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOWRESULT:
                    if (textResult.equals(""))
                        tvResult.setText("识别失败");
                    else
                        tvResult.setText(textResult);
                    break;
                case SHOWTREATEDIMG:
                    tvResult.setText("识别中......");
                    showPicture(ivTreated, bitmapTreated);
                    break;
            }
            super.handleMessage(msg);
        }

    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (chPreTreat.isChecked()) {
                bitmapTreated = ImgPretreatment
                        .doPretreatment(bitmapSelected);
                if (bitmapTreated == null){
                    LogPrint(LOG_LEVEL.ERROR,"doPretreatment Error!");
                    return;
                }
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

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        // 若文件夹不存在 首先创建文件夹
        File path = new File(IMG_PATH);
        if (!path.exists()) {
            path.mkdirs();
        }

        tvResult = (TextView) findViewById(R.id.tv_result);
        ivSelected = (ImageView) findViewById(R.id.iv_selected);
        ivTreated = (ImageView) findViewById(R.id.iv_treated);
        btnCamera = (Button) findViewById(R.id.btn_camera);
        btnSelect = (Button) findViewById(R.id.btn_select);
        chPreTreat = (CheckBox) findViewById(R.id.ch_pretreat);
        radioGroup = (RadioGroup) findViewById(R.id.radiogroup);

        btnCamera.setOnClickListener(new cameraButtonListener());
        btnSelect.setOnClickListener(new selectButtonListener());

        // 用于设置解析语言
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

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

        //新页面接收数据
        Intent intentPicture = this.getIntent();
        String picture_path = "";
        if (intentPicture != null && intentPicture.getExtras() != null){
            Bundle bundle = this.getIntent().getExtras();
            if (bundle != null) {
                picture_path = bundle.getString("picture_path");
            }
            LogPrint(LOG_LEVEL.INFO,"picture_path = %s",picture_path);
            if (picture_path != null && !picture_path.isEmpty()){
                ImgPretreatment.setImgPath(picture_path);
                bitmapSelected = ImgPretreatment.getDiskBitmap(picture_path);
                if (chPreTreat.isChecked())
                    tvResult.setText("预处理中......");
                else
                    tvResult.setText("识别中......");
                // 显示选择的图片
                showPicture(ivSelected, bitmapSelected);

                if (bitmapSelected != null){
                    new Thread(runnable).start();
                }
            }
        }
    }

    // 枚举日志级别
    private enum LOG_LEVEL{
        DEBUG,      // 调试
        VERBOSE,    // 随便
        INFO,       // 信息
        WARNING,    // 警告
        CRITICAL,   // 严重
        ERROR       // 错误
    }

    private void LogPrint(LOG_LEVEL level, String fmt, Object ...args){
        switch (level){
            case DEBUG:
                Log.e(LOG_TAG, String.format(fmt, args));
                break;
            case VERBOSE:
            default:
                Log.v(LOG_TAG, String.format(fmt, args));
                break;
            case INFO:
                Log.i(LOG_TAG, String.format(fmt, args));
                break;
            case WARNING:
                Log.w(LOG_TAG, String.format(fmt, args));
                break;
            case CRITICAL:
            case ERROR:
                Log.e(LOG_TAG, String.format(fmt, args));
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tts_settings, menu);
        return true;
    }

    private String getImagePath(Intent data){

        String  path = "";
        Uri uri = data.getData(); //获得图片的uri
        ContentResolver resolver = getContentResolver();
        // ContentResolver对象的getType方法可返回形如content://的Uri的类型
        // 如果是一张图片，返回结果为image/jpeg或image/png等
        String fileType = resolver.getType(uri);

        //根据返回的uri获取图片路径
        Cursor cursor = resolver.query(uri,  new String[]{MediaStore.Images.Media.DATA}, null, null, null);
        try {
            if (cursor != null){
                cursor.moveToFirst();
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                cursor.close();
            }
        }catch (Exception e) {
            e.printStackTrace();
            return path;
        }
        //data=Intent { dat=content://com.android.externalstorage.documents/document/6005-19D5:Pictures/Screenshots/Screenshot_2016-05-29-10-45-34.png flg=0x1 }}
        if (path!=null && !path.isEmpty()){
            Log.d(LOG_TAG,path);
            //do  anything you want
        }else {
            Log.i(LOG_TAG,"data.toString()=" + data.toString());
            path = QxxExec.translateAbsolutePath(data.getData().getPath());
            Log.i(LOG_TAG,path);
        }

        return path;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_CANCELED)
            return;

        ImgPretreatment.setImgPath(getImagePath(data));
        if (requestCode == PHOTO_CAPTURE) {
            tvResult.setText("abc");
            Log.v(LOG_TAG, "requestCode == PHOTO_CAPTURE" );
            startPhotoCrop(Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
        }

        // 处理结果
        if (requestCode == PHOTO_RESULT) {
            bitmapSelected = decodeUriAsBitmap(data.getData() /*Uri.fromFile(new File(IMG_PATH,"temp_cropped.jpg"))*/);
            if (chPreTreat.isChecked())
                tvResult.setText("预处理中......");
            else
                tvResult.setText("识别中......");
            // 显示选择的图片
            showPicture(ivSelected, bitmapSelected);

            // 新线程来处理识别
            new Thread(runnable).start();

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // 拍照识别
    class cameraButtonListener implements OnClickListener {

        @Override
        public void onClick(View arg0) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
            startActivityForResult(intent, PHOTO_CAPTURE);
        }
    };

    // 从相册选取照片并裁剪
    class selectButtonListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("return-data", false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
            Log.v(LOG_TAG,"selectButtonListener " + IMG_PATH);
            intent.putExtra("outputFormat",
                    Bitmap.CompressFormat.JPEG.toString());
            intent.putExtra("noFaceDetection", true); // no face detection
            startActivityForResult(intent, PHOTO_RESULT);
        }

    }

    // 将图片显示在view中
    public static void showPicture(ImageView iv, Bitmap bmp){
        iv.setImageBitmap(bmp);
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

        baseApi.init(QxxExec.getExternalSDCardPath() + java.io.File.separator + "tesseract" , language);

        // 必须加此行，tess-two要求BMP必须为此配置
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        baseApi.setImage(bitmap);

        String text = baseApi.getUTF8Text();

        baseApi.clear();
        baseApi.end();

        return text;
    }


    /**
     * 调用系统图片编辑进行裁剪
     */
    public void startPhotoCrop(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
        Log.v(LOG_TAG,"startPhotoCrop " + IMG_PATH);
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

}
