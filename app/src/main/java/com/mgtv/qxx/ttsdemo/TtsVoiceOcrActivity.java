package com.mgtv.qxx.ttsdemo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class TtsVoiceOcrActivity extends Activity implements DirectoryChooserFragment.OnFragmentInteractionListener {
    private static final String LOG_TAG = "TtsVoiceOcrActivity";

    private static final String DEFAULT_ENCODING = "UTF-8";

    private GoogleSpeech googleSpeech;
    private Settings ttsSetting;
    private String pictureSavePath = "";

            //根据不同选项所要变更的文本控件
    TextView tvShowLang = null;

    private Button btnSpeak;
    private Button btnSave;
    private EditText etTextToSpeech;
    private CheckBox checkBoxRw;
    private static String sSelectedLanguage = "Chinese";
    private static String innerSdCardPath = "";
    private static String externSdCardPath = "";
    private static String sSelectedDir = "";
    private static String sSelectedFile = "";

    private TextView mDirectoryTextView;
    private TextView mFilePathTextView;
    private DirectoryChooserFragment mDialog;
    private boolean bReadFile = false;

    // 新窗口
    // 检查tts数据是否开启
    public static final int ACTIVITY_GET_CONTENT = 1;
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 2;
    public static final int REQ_CHECK_TTS_DATA = 3;
    public static final int REQ_OCR = 4;

    private String ttsSettingFile = "";

    public class StreamGobbler extends Thread {
        InputStream is;
        String type;
        OutputStream os;

        StreamGobbler(InputStream is, String type) {
            this(is, type, null);
        }

        StreamGobbler(InputStream is, String type, OutputStream redirect) {
            this.is = is;
            this.type = type;
            this.os = redirect;
        }

        public void run() {
            try {
                PrintWriter pw = null;
                if (os != null)
                    pw = new PrintWriter(os);

                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (pw != null)
                        pw.println(line);
                    System.out.println(type + ">" + line);
                }
                if (pw != null)
                    pw.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public class GoodWinRedirect {
        public void main(String args[]) {
            if (args.length < 1) {
                System.out.println("USAGE java GoodWinRedirect <outputfile>");
                System.exit(1);
            }

            try {
                FileOutputStream fos = new FileOutputStream(args[0]);
                Runtime rt = Runtime.getRuntime();
                Process proc = rt.exec("java jecho 'Hello World'");
                // any error message?
                StreamGobbler errorGobbler = new
                        StreamGobbler(proc.getErrorStream(), "ERROR");

                // any output?
                StreamGobbler outputGobbler = new
                        StreamGobbler(proc.getInputStream(), "OUTPUT", fos);

                // kick them off
                errorGobbler.start();
                outputGobbler.start();

                // any error???
                int exitVal = proc.waitFor();
                System.out.println("ExitValue: " + exitVal);
                fos.flush();
                fos.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        btnSpeak = (Button) findViewById(R.id.buttonSpeakOut);
        btnSave = (Button) findViewById(R.id.buttonSaveWave);
        etTextToSpeech = (EditText) findViewById(R.id.editTextToSpeak);
        checkBoxRw = (CheckBox) findViewById(R.id.checkBoxReadWrite);
        mDirectoryTextView = (EditText) findViewById(R.id.editTextShowDirectory);
        mFilePathTextView = (TextView) findViewById(R.id.editTextToSpeak);


        // 获取SD卡路径
        innerSdCardPath = QxxExec.getInnerSDCardPath();
        externSdCardPath = QxxExec.getExternalSDCardPath();
        Log.e(LOG_TAG, externSdCardPath);
        if (externSdCardPath.isEmpty()) {
            sSelectedDir = innerSdCardPath;
        } else {
            sSelectedDir = externSdCardPath;
        }

        // 设置默认保存目录
        ((EditText) findViewById(R.id.editTextShowDirectory)).setText(sSelectedDir);

        // 获取TTS的配置
        ttsSettingFile = sSelectedDir + "/ttsSetting.properties";
        ttsSetting = new Settings(ttsSettingFile);

        googleSpeech = new GoogleSpeech(this, btnSpeak, ttsSettingFile);

        // 设置默认文件名
        sSelectedFile = sSelectedDir + "/Download/license.txt";

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab); // 识别按钮
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "进行语音识别，请确保麦克风的权限", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startVoiceRecognitionActivity();
            }
        });

        // 检查语言文字转换数据是否存在
        checkTtsData();
        // 检查语音识别程序是否存在
        PackageManager pm = getPackageManager();
        List activities = pm.queryIntentActivities(new Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0); // 本地识别程序
        /*
         * 此处没有使用捕捉异常，而是检测是否有语音识别程序。
         * 也可以在startRecognizerActivity()方法中捕捉ActivityNotFoundException异常
         */
        if (activities.size() == 0) {
            // 若检测不到语音识别程序在本机安装，测将扭铵置灰
            activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_WEB_SEARCH), 0); // 网络识别程序
            if (activities.size() == 0) {
                // fab.setEnabled(false);
                Toast.makeText(TtsVoiceOcrActivity.this, "未检测到语音识别设备", Toast.LENGTH_SHORT).show();
            }
        }


        FloatingActionButton fab_ocr = (FloatingActionButton) findViewById(R.id.fab_ocr); // 识别按钮
        fab_ocr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "进行图像识别，请确保照相机的权限", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startCamera2Activity();
            }
        });

        FloatingActionButton fab_camera2 = (FloatingActionButton) findViewById(R.id.fab_camera2);
        fab_camera2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "进行相机专业模式，请确保照相机的权限", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                // startCamera2Activity();
                startCamera1Activity();
            }
        });

        // button on click event
        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String text = etTextToSpeech.getText().toString();
                File f = new File(text);
                if (f.exists()) {
                    bReadFile = true;
                } else {
                    bReadFile = false;
                }
                if (bReadFile) {
                    OpenTxtReader();
                } else {
                    Log.e("btnSpeak", text);
                    googleSpeech.speakOut(text, bReadFile);
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //将EditText里的内容保存为语音文件
                String strCurrentTime = getFormattedTime();
                String text = etTextToSpeech.getText().toString();
                String filePath = sSelectedDir + "/speak_" + strCurrentTime + ".wav";
                googleSpeech.saveToWav(filePath, text);
            }
        });

        //EditText内容变化监听
        etTextToSpeech.addTextChangedListener(mTextWatcher);
        tvShowLang = (TextView) this.findViewById(R.id.textViewShowLanguage);
        RadioGroup group = (RadioGroup) this.findViewById(R.id.radioGroupLanguage);

        //绑定一个匿名监听器
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup arg0, int arg1) {
                // TODO Auto-generated method stub
                //获取变更后的选中项的ID
                int radioButtonId = arg0.getCheckedRadioButtonId();
                //根据ID获取RadioButton的实例
                RadioButton rb = (RadioButton) TtsVoiceOcrActivity.this.findViewById(radioButtonId);
                //更新文本内容，以符合选中项
                TtsVoiceOcrActivity.this.tvShowLang.setText("您的语言是：" + rb.getText());
                TtsVoiceOcrActivity.this.sSelectedLanguage = rb.getText().toString();
                int result = -1;
                // You can change language to speak by using setLanguage() function. Lot of languages are supported like Canada, French, Chinese, Germany etc.,
                if (TtsVoiceOcrActivity.this.sSelectedLanguage.equals("English") || TtsVoiceOcrActivity.this.sSelectedLanguage.equals("英语")) {
                    result = googleSpeech.setTtsLanguage(Locale.US); // English language
                } else {
                    result = googleSpeech.setTtsLanguage(Locale.CHINESE); // Chinese language
                }

                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "This Language is not supported");
                    displayToast("不支持当前语言！");
                    Log.d("TTS", "set Language success");
                } else {
                    Properties prop = ttsSetting.loadConfig();
                    prop.put("Language", TtsVoiceOcrActivity.this.sSelectedLanguage);
                    ttsSetting.saveConfig(prop);
                }
            }
        });

        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName(sSelectedFile.substring(0, sSelectedFile.lastIndexOf("/")))
                .build();

        findViewById(R.id.buttonSelectTextFile)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 打开各种 activity
                        // Intent intent = new  Intent(Intent.ACTION_PICK_ACTIVITY);
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("text/*"); //intent.setType("file/*");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        startActivityForResult(intent, ACTIVITY_GET_CONTENT);
                    }
                });

        findViewById(R.id.btnChoose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialog.bInitInstance) {
                    mDialog.SetFileFlag(false);
                } else {
                    Log.e("btnChoose", "Start Init Instance");
                    mDialog = DirectoryChooserFragment.newInstance(config, false);
                    mDialog.show(getFragmentManager(), null);
                }
            }
        });
    }

    //显示Toast函数
    private void displayToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    //  校验TTS引擎安装及资源状态
    //  检查TTS数据是否已经安装并且可用

    private boolean checkTtsData() {
        try {
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            startActivityForResult(checkIntent, REQ_CHECK_TTS_DATA);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable editText) {
            //如果是边写边读
            if (checkBoxRw.isChecked() && (editText.length() != 0)) {
                //获得EditText的所有内容
                String text = editText.toString();
                googleSpeech.speakOut(text.substring(editText.length() - 1), bReadFile);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
//            displayToast(String.valueOf(s));
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            displayToast(String.valueOf(s));
        }
    };

    @Override
    public void onSelectDirectory(@NonNull final String path) {
        Log.e("onSelectDirectory", path);
        Log.e("onSelectDirectory", String.valueOf(mDialog.bGetFile));
        if (path != null) {
            if (mDialog.bGetFile) {
                Log.e("onSelectDirectory", "GetFile");
                mFilePathTextView.setText(path);
                sSelectedFile = path;
                bReadFile = true;
            } else {
                if (path != null) {
                    mDirectoryTextView.setText(path);
                    sSelectedDir = path;
                }
                bReadFile = false;
            }
        } else {
            Log.e("onSelectDirectory", "Path is NULL");
        }
        mDialog.dismiss();
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (ACTIVITY_GET_CONTENT == requestCode) {
            // Log.e("onActivityResult Code",String.valueOf(resultCode));
            // Log.e("onActivityResult data",data.getData().toString());
            if (resultCode == RESULT_OK) {
                //得到文件的Uri
                Uri uri = data.getData();
                ContentResolver resolver = getContentResolver();
                //ContentResolver对象的getType方法可返回形如content://的Uri的类型
                //如果是一张图片，返回结果为image/jpeg或image/png等
                String fileType = resolver.getType(uri);
                if (fileType == null || fileType.isEmpty()) {
                    String path = uri.getPath();
                    fileType = path.substring(path.lastIndexOf(".") + 1, path.length());
                }
                //  Log.e("onActivityResult","ACTIVITY_GET_CONTENT get file's type：" + fileType);
                if (fileType.startsWith("image"))//判断用户选择的是否为图片
                {
                    //根据返回的uri获取图片路径
                    Cursor cursor = resolver.query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                    cursor.moveToFirst();
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    //do  anything you want
                } else if (fileType.startsWith("text")) {
                    // sSelectedDir = uri.getLastPathSegment(); //7b3:00wenjian.txt
                    // String string = uri.getPath();
                    sSelectedFile = QxxExec.translateAbsolutePath(QxxExec.getAbsolutePath(this, uri));
                    mFilePathTextView.setText(sSelectedFile);
                }
            } else if (resultCode == RESULT_CANCELED) {
                Log.d("ACTIVITY_GET_CONTENT", "取消");
            }

        } else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) { // 回调获取从谷歌得到的数据
                // 取得语音的字符
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String resultString = "";
                for (int i = 0; i < results.size(); i++) {
                    resultString += results.get(i);
                }
                etTextToSpeech.setText(resultString);
                Toast.makeText(this, resultString, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getBaseContext(), "檢測失敗，請重新點擊識別!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_CHECK_TTS_DATA) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // TTS引擎可用
                // 针对于重新绑定引擎，需要先shutdown()
                // 重置mTts，以便通知创建TextToSpeech对象
                googleSpeech.initTts();
            } else if (TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL == resultCode) {

            } else {
                googleSpeech.notifyReinstallDialog(); // 提示用户是否重装TTS引擎数据的对话框
            }
        } else if (REQ_OCR == requestCode) {
            if (resultCode != RESULT_OK){
                pictureSavePath = "";
            }
            startOcrActivity(data);
        }
        // 语音识别后的回调，将识别的字串以Toast显示
        super.onActivityResult(requestCode, resultCode, data);
    }


    public String getFormattedTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HHmmss");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        return formatter.format(curDate);
    }

    private void startCamera2Activity() {
        startCameraActivity(2);
    }

    private void startCamera1Activity() {
        startCameraActivity(1);
    }

    private void startCameraActivity(int cameraApiVersion) {
        Intent intent;
        if (cameraApiVersion == 1){
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        }else{
            intent = new Intent(this, CameraActivity.class);
        }
        File path = new File(externSdCardPath + "/DCIM/OCR/");
        if (!path.exists())
            path.mkdirs();
        String mStrFileName = getFormattedTime() + ".jpg";
        pictureSavePath = externSdCardPath + "/DCIM/OCR/" + mStrFileName;
        // Log.e("REQ_OCR", pictureSavePath);
        File file = new File(pictureSavePath);
        Uri uri = Uri.fromFile(file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, REQ_OCR);
    }

    private void startOcrActivity(Intent data) {
        // Log.e("REQ_OCR", pictureSavePath);
        Intent intent = new Intent(this, OcrActivity.class);
         Bundle bundle=new Bundle();
         bundle.putString("picture_path",pictureSavePath);
        // bundle.putString("ocr_language",sSelectedLanguage);
        // bundle.putString(ttsSetting.PROPERTY_IMAGE_PROCESSING, String.valueOf(ttsSetting.getImageProcessing()));
         intent.putExtras(bundle);
//        if (data != null) {
//            intent.putExtras(data);
//        }
        startActivity(intent);
    }
    /**
     * 調用方法
     * 开始本地识别
     */
    private void startVoiceRecognitionActivity() {
        try {
            // 通过Intent传递语音识别的模式，开启语音
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            // 语言模式和自由模式的语音识别
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            // 提示语音开始
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "开始语音");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-HK");
            // 开始语音识别
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            showDialog();
        }
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_content);
        builder.setTitle(R.string.dialog_title);
        builder.setNegativeButton(R.string.download,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Uri uri = Uri.parse(getApplication().getString(R.string.voice_url));
                        Intent it = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(it);
                    }
                });
        builder.setPositiveButton(R.string.cancel,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    /**
     * 調用方法
     * 开始网络识别
     */
    private void startWebVoiceRecognizerActivity() {
        // 通过Intent传递语音识别的模式，开启语音
        Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        // 语言模式和自由模式的语音识别
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // 提示语音开始
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "开始语音");
        // 开始语音识别
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
        // 调出识别界面
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater menuInflater = new MenuInflater(getApplication());
        menuInflater.inflate(R.menu.menu_tts_settings, menu);
        return super.onCreateOptionsMenu(menu);

        // getMenuInflater().inflate(R.menu.menu_tts_settings, menu);
        // return true;
    }

    /**
     * 监听menu选项
     **/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        int itemId = item.getItemId();
        // displayToast(String.valueOf(itemId));
        switch (itemId) {
            case R.id.c11_setting: // do something here
                Log.i("MenuTest:", "ItemSelected: setting");
                Intent intent = new Intent(TtsVoiceOcrActivity.this, Settings.class);
                Bundle bd = new Bundle();
                bd.putString("PropertyFile", ttsSettingFile);
                intent.putExtras(bd);
                startActivity(intent);
                break;
            case R.id.c11_no_icon: // do something here
                Log.i("MenuTest:", "ItemSelected: Sans Icon");
                break;
            case R.id.c11_other_stuff: // do something here
                Log.i("MenuTest:", "ItemSelected:c11_other_stuff");
                break;
            case R.id.c11_later: // do something here
                Log.i("MenuTest:", "ItemSelected:c11_later");
                break;
            case R.id.last: // do something here
                Log.i("MenuTest:", "ItemSelected:last");
                break;
            case R.id.c11_submenu: // do something here
                Log.i("MenuTest:", "ItemSelected:c11_submenu");
                break;
            case R.id.c11_non_ghost: // do something here
                Log.i("MenuTest:", "ItemSelected:c11_non_ghost");
                break;
            case R.id.c11_ghost: // do something here
                Log.i("MenuTest:", "ItemSelected:c11_ghost");
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    public void OpenTxtReader() {
        //新建一个显式意图，第一个参数为当前Activity类对象，第二个参数为你要打开的Activity类
        Intent intent = new Intent(getBaseContext(), TxtReader.class);
        // Properties prop = loadConfig(ttsSettingFile);
        // speechPitch = Float.valueOf(prop.getProperty("SpeechPitch"));
        // speechRate =  Float.valueOf(prop.getProperty("SpeechRate"));
        ttsSetting.getTtsSettings(ttsSettingFile);
        //用Bundle携带数据
        Bundle bundle = new Bundle();
        //传递name参数为
        bundle.putString("Filename", sSelectedFile);
        bundle.putString("Language", ttsSetting.getLanguage());
        bundle.putFloat("SpeechPitch", ttsSetting.getSpeechPitch());
        bundle.putFloat("SpeechRate", ttsSetting.getSpeechRate());
        bundle.putInt("SpeechLength", ttsSetting.getSpeechLength());
        intent.putExtras(bundle);

        // 新打开的activity返回的数据
        startActivity(intent); // 不需要返回值
        // startActivityForResult(intent, ACTIVITY_FILE_SPEECH); // 需要返回, 以便重置bReadFile变量
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        googleSpeech.finish();
    }

    @Override
    public void finish() {
        super.finish();
        if (googleSpeech != null) googleSpeech.finish();
    }

}

/*
* Build.BOARD // 主板
* Build.BRAND // android系统定制商
* Build.CPU_ABI // cpu指令集
* Build.DEVICE // 设备参数
* Build.DISPLAY // 显示屏参数
* Build.FINGERPRINT // 硬件名称
* Build.HOST
* Build.ID // 修订版本列表
* Build.MANUFACTURER // 硬件制造商
* Build.MODEL // 版本
* Build.PRODUCT // 手机制造商
* Build.TAGS // 描述build的标签
* Build.TIME
* Build.TYPE // builder类型
* Build.USER
*  Build.VERSION
* Java代码
* // 当前开发代号
* Build.VERSION.CODENAME
* // 源码控制版本号
* Build.VERSION.INCREMENTAL
* // 版本字符串
* Build.VERSION.RELEASE
* // 版本号
* Build.VERSION.SDK
* // 版本号*
* Build.VERSION.SDK_INT
* */