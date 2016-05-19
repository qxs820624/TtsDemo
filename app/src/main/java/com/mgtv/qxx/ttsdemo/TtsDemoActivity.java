package com.mgtv.qxx.ttsdemo;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.annotation.NonNull;
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
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class TtsDemoActivity extends Activity implements OnInitListener,DirectoryChooserFragment.OnFragmentInteractionListener {
    private  static final String DEFAULT_ENCODING = "UTF-8";

    //根据不同选项所要变更的文本控件
    TextView  tvShowLang = null;

    private TextToSpeech tts;
    private Button btnSpeak;
    private Button btnSave;
    private EditText txtText;
    private CheckBox checkBoxRw;
    private static  boolean isInited = false;
    private static  String sSelectedLanguage = "Chinese";
    private static  String innerSdCardPath = "";
    private static  String externSdCardPath = "";
    private static  String sSelectedDir = "";
    private static  String sSelectedFile = "";

    private TextView mDirectoryTextView;
    private TextView mFilePathTextView;
    private DirectoryChooserFragment mDialog;
    private boolean bReadFile = false;

    // 新窗口
    // 检查tts数据是否开启
    public static final int ACTIVITY_CHECK_TTS_DATA = 1;
    public static final int ACTIVITY_FILE_SPEECH = 2;
    public static final int ACTIVITY_TTS_SETTING = 3;
    public static final int ACTIVITY_GET_CONTENT = 4;
    public static final int VOICE_RECOGNITION_REQUEST_CODE=5;

    private String language = "";
    private String encoding = "";
    private float speechRate = (float) 0.8;
    private float speechPitch =  (float)1.5;
    private int speechLength =  200;

    private String ttsSettingFile = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts_demo);

        // 检查tts数据
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, ACTIVITY_CHECK_TTS_DATA);

        btnSpeak = (Button) findViewById(R.id.buttonSpeakOut);
        btnSave = (Button) findViewById(R.id.buttonSaveWave);
        txtText = (EditText) findViewById(R.id.editTextToSpeak);
        checkBoxRw = (CheckBox) findViewById(R.id.checkBoxReadWrite);

        // 获取SD卡路径
        String inPath = getInnerSDCardPath();
        List<String> extPaths = getExtSDCardPath();
        for (String path : extPaths) {
            externSdCardPath = path;
        }
        if (externSdCardPath.isEmpty()){
            sSelectedDir = inPath;
        }else {
            sSelectedDir = externSdCardPath;
        }

        // 设置默认保存目录
        ((EditText)findViewById(R.id.editTextShowDirectory)).setText(sSelectedDir);

        // 获取TTS的配置
        ttsSettingFile = sSelectedDir + "/ttsSetting.properties";
        getTtsSettings(ttsSettingFile);

        // 设置默认文件名
        sSelectedFile = sSelectedDir + "/Download/license.txt";

        ImageButton btn = (ImageButton) findViewById(R.id.btn_recognize_speech); // 识别按钮
        PackageManager pm = getPackageManager();
        List activities = pm.queryIntentActivities(new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0); // 本地识别程序

        /*
         * 此处没有使用捕捉异常，而是检测是否有语音识别程序。
         * 也可以在startRecognizerActivity()方法中捕捉ActivityNotFoundException异常
         */
        if (activities.size() != 0) {
            btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus){
                        startRecognizerActivity();
                    }
                }
            });
        } else {
            // 若检测不到语音识别程序在本机安装，测将扭铵置灰

            activities = pm.queryIntentActivities( new Intent(RecognizerIntent.ACTION_WEB_SEARCH), 0); // 网络识别程序
            if (activities.size() == 0){
                btn.setEnabled(false);
                Toast.makeText(TtsDemoActivity.this,"未检测到语音识别设备",Toast.LENGTH_SHORT);
            }else {

                btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus){
                            startWebRecognizerActivity();
                        }
                    }
                });
            }
        }
        // button on click event
        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                speakOut();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                SimpleDateFormat formatter    =   new    SimpleDateFormat("yyyy_MM_dd_HHmmss");
                Date curDate    =   new    Date(System.currentTimeMillis());//获取当前时间
                String    strCurrentTime    =    formatter.format(curDate);
                //将EditText里的内容保存为语音文件
                int r = -1;
                String text = txtText.getText().toString();
                String fileString = sSelectedDir+"/speak_"+strCurrentTime+".wav";
                File fileSave = new File(fileString);
                if (Build.VERSION.SDK_INT < 21){
                    r = tts.synthesizeToFile(text, null, fileString);
                }else{
                    r = tts.synthesizeToFile(text,null,fileSave,TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                }
                if(r == TextToSpeech.SUCCESS){
                    displayToast("保存成功！");
                }
            }
        });

        //EditText内容变化监听
        txtText.addTextChangedListener(mTextWatcher);
        tvShowLang = (TextView) this.findViewById(R.id.textViewShowLanguage);
        RadioGroup group = (RadioGroup)this.findViewById(R.id.radioGroupLanguage);

        //绑定一个匿名监听器
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup arg0, int arg1) {
                        // TODO Auto-generated method stub
                        //获取变更后的选中项的ID
                        int radioButtonId = arg0.getCheckedRadioButtonId();
                        //根据ID获取RadioButton的实例
                        RadioButton rb = (RadioButton)TtsDemoActivity.this.findViewById(radioButtonId);
                        //更新文本内容，以符合选中项
                        TtsDemoActivity.this.tvShowLang.setText("您的语言是：" + rb.getText());
                        TtsDemoActivity.this.sSelectedLanguage = rb.getText().toString();
                        int result = -1;
                        // You can change language to speak by using setLanguage() function. Lot of languages are supported like Canada, French, Chinese, Germany etc.,
                        if (TtsDemoActivity.this.sSelectedLanguage.equals("English") || TtsDemoActivity.this.sSelectedLanguage.equals("英语")){
                            result = tts.setLanguage(Locale.US); // English language
                        }else{
                            result = tts.setLanguage(Locale.CHINESE); // Chinese language
                        }

                        if (result == TextToSpeech.LANG_MISSING_DATA
                                || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("TTS", "This Language is not supported");
                            displayToast("不支持当前语言！");
                            Log.d("TTS", "set Language success");
                        }
                        Properties prop = loadConfig(ttsSettingFile);
                        prop.put("Language",TtsDemoActivity.this.sSelectedLanguage);
                        saveConfig(ttsSettingFile,prop);
                }
            });

        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName(sSelectedFile.substring(0,sSelectedFile.lastIndexOf("/")))
                .build();

        findViewById(R.id.btnChoose)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mDialog.bInitInstance){
                            mDialog.SetFileFlag(false);
                        }else{
                            if (true){
                                Intent intent = new  Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("text/plain");
                                /*
                                // 其作用与 startActivityForResult(intent,ACTIVITY_GET_CONTENT); 一致
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                startActivityForResult(Intent.createChooser(intent,
                                        TtsDemoActivity.this.getResources().getString(R.string.text_view_save_prompt)), ACTIVITY_GET_CONTENT);
                                */
                                startActivityForResult(intent,ACTIVITY_GET_CONTENT);

                            }else {
                                Log.e("btnChoose", "not Init Instance");
                                mDialog = DirectoryChooserFragment.newInstance(config, false);
                                mDirectoryTextView = (TextView) findViewById(R.id.editTextShowDirectory);
                                mDialog.show(getFragmentManager(), null);
                            }
                        }
                    }
                });

        findViewById(R.id.buttonShowTextFile).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (mDialog.bInitInstance){
                    mDialog.SetFileFlag(true);
                }
                else{
                    Log.e("buttonShowTextFile","not Init Instance");
                    mDialog = DirectoryChooserFragment.newInstance(config,true);
                    mFilePathTextView = (TextView) findViewById(R.id.editTextToSpeak);
                    mDialog.show(getFragmentManager(), null);
                }
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Log.e("TTS", "Initilization Success!");
            displayToast("初始化成功！");

            isInited = true;
            btnSpeak.setEnabled(true);
            tts.setLanguage(Locale.CHINESE);
            //        Changing Pitch Rate
            //        You can set speed pitch level by using setPitch() function. By default the value is 1.0 You can set lower values than 1.0 to decrease pitch level or greater values for increase pitch level.
            tts.setPitch(speechPitch);

            //        Changing Speed Rate
            //        The speed rate can be set using setSpeechRate(). This also will take default of 1.0 value. You can double the speed rate by setting 2.0 or make half the speed level by setting 0.5
            tts.setSpeechRate(speechRate);
            speakOut();
        } else {
            Log.e("TTS", "Initilization Failed!");
            displayToast("初始化错误！");
            installApkFromAssets();
        }
    }


    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if(tts != null)
        {
            tts.stop();
        }
    }

    private void speakOut() {
        String text="";
        if (isInited){
            int result = -1;
            // 每次启动的时候都获取一下配置
            getTtsSettings(ttsSettingFile);
            tts.setPitch(speechPitch);
            tts.setSpeechRate(speechRate);

            // displayToast(this.sSelectedLanguage);
            if (bReadFile){
                // text = ReadFromFile.readFileByChars(sSelectedFile);
                OpenTxtReader();
            }else{
                text = txtText.getText().toString();
                // 当TTS调用speak方法时，它会中断当前实例正在运行的任务(也可以理解为清除当前语音任务，转而执行新的语音任务)
                // tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                // 当TTS调用speak方法时，会把新的发音任务添加到当前发音任务列队之后

                if (Build.VERSION.SDK_INT < 21){
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null);
                }else{
                    tts.speak(text,TextToSpeech.QUEUE_ADD,null,TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                }
            }
        }else {
            displayToast("TTS引擎初始化失败");
        }
    }

    //显示Toast函数
    private void displayToast(String s)
    {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }


    private TextWatcher mTextWatcher = new TextWatcher(){
        @Override
        public void afterTextChanged(Editable s){
            //如果是边写边读
            if(checkBoxRw.isChecked()&&(s.length()!=0)){
                //获得EditText的所有内容
                String t = s.toString();
                if (Build.VERSION.SDK_INT < 21){
                    tts.speak(t.substring(s.length()-1), TextToSpeech.QUEUE_FLUSH, null);
                }else {
                    tts.speak(t.substring(s.length()-1), TextToSpeech.QUEUE_FLUSH, null,TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,int count){
//            displayToast(String.valueOf(s));
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,int after){
//            displayToast(String.valueOf(s));
        }
    };

    @Override
    public void onSelectDirectory(@NonNull final String path) {
        Log.e("onSelectDirectory",path);
        Log.e("onSelectDirectory",String.valueOf(mDialog.bGetFile));
        if (path != null){
            if (mDialog.bGetFile){
                Log.e("onSelectDirectory","GetFile");
                mFilePathTextView.setText(path);
                sSelectedFile = path;
                bReadFile = true;
            }else {
                mDirectoryTextView.setText(path);
                sSelectedDir = path;
            }
        }else {
            Log.e("onSelectDirectory","Path is NULL");
        }
        mDialog.dismiss();
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ACTIVITY_CHECK_TTS_DATA) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                if (tts == null){
                    tts = new TextToSpeech(this, this);
                }
            }
            else {
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        }else if(ACTIVITY_FILE_SPEECH == requestCode){
            if (tts == null || !isInited) {
                tts = new TextToSpeech(this,this);
            }
            Log.d("ttsDemo","onActivityResult ACTIVITY_FILE_SPEECH");
            displayToast(data.getStringExtra("result"));
            bReadFile = false;
        }else if (ACTIVITY_GET_CONTENT == requestCode){
            // Log.e("onActivityResult Code",String.valueOf(resultCode));
            // Log.e("onActivityResult data",data.getData().toString());
            if(resultCode  == RESULT_OK)
            {
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
                Log.e("onActivityResult",fileType);
                if(fileType.startsWith("image"))//判断用户选择的是否为图片
                {
                    //根据返回的uri获取图片路径
                    Cursor cursor = resolver.query(uri,  new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                    cursor.moveToFirst();
                    String  path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

                    //do  anything you want
                }
            }else if (resultCode == RESULT_CANCELED){
                Log.d("ACTIVITY_GET_CONTENT","取消");
            }

        }else if (requestCode == VOICE_RECOGNITION_REQUEST_CODE  && resultCode == RESULT_OK) { // 回调获取从谷歌得到的数据
                // 取得语音的字符
                ArrayList<String> results = data .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String resultString = "";
                for (int i = 0; i < results.size(); i++) {
                    resultString += results.get(i);
                }
                Toast.makeText(this, resultString, Toast.LENGTH_SHORT).show();
            }
        // 语音识别后的回调，将识别的字串以Toast显示
        super.onActivityResult(requestCode, resultCode, data);
    }
    /**
     * 获取内置SD卡路径
     * @return
     */
    public String getInnerSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    /**
     * 获取外置SD卡路径
     * @return	应该就一条记录或空
     */
    public List getExtSDCardPath()
    {
        List lResult = new ArrayList();
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("mount");
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                // Log.e("getExtSDCardPath",line);
                if (line.contains("extSdCard") || line.contains("sdcard2"))
                {
                    String [] arr = line.split(" ");
                    String path = arr[1];
                    File file = new File(path);
                    if (file.isDirectory())
                    {
                        lResult.add(path);
                    }
                }
            }
            isr.close();
        } catch (Exception e) {
        }
        return lResult;
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

     /** 监听menu选项 **/
     @Override public boolean onOptionsItemSelected(MenuItem item){
         // TODO Auto-generated method stub
         int itemId = item.getItemId();
         // displayToast(String.valueOf(itemId));
         switch (itemId) {
             case R.id.c11_setting: // do something here
                 Log.i("MenuTest:", "ItemSelected: setting");
                 Intent intent = new Intent(TtsDemoActivity.this, SetTts.class);
                 Bundle bd = new Bundle();
                 bd.putString("PropertyFile",ttsSettingFile);
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

    public class SendTts implements Serializable {
        private static final long serialVersionUID = -7060210544600464481L;
        private TextToSpeech tts = null;
        private String Filename = "";
        private String Language = "";
        private float SpeechPitch = (float)0.9;
        private float SpeechRate = (float)0.9;

        public void setTts(TextToSpeech tts){
            this.tts = tts;
        }

        public void setFilename(String Filename){
            this.Filename = Filename;
        }


        public void setLanguage(String Language){
            this.Language = Language;
        }

        public void setSpeechPitch(float SpeechPitch){
            this.SpeechPitch = SpeechPitch;
        }

        public void setSpeechRate(float SpeechRate){
            this.SpeechRate = SpeechRate;
        }

        public TextToSpeech getTts(){
            return this.tts;
        }

        public String getFilename(){
            return this.Filename;
        }
        public String getLanguage(){
            return this.Language;
        }
        public float getSpeechPitch(){
            return this.SpeechPitch;
        }
        public float getSpeechRate(){
            return this.SpeechRate;
        }
    }

    public void OpenTxtReader(){
        //新建一个显式意图，第一个参数为当前Activity类对象，第二个参数为你要打开的Activity类
        Intent intent =new Intent(TtsDemoActivity.this, TxtReader.class);
        // Properties prop = loadConfig(ttsSettingFile);
        // speechPitch = Float.valueOf(prop.getProperty("SpeechPitch"));
        // speechRate =  Float.valueOf(prop.getProperty("SpeechRate"));
        getTtsSettings(ttsSettingFile);
        //用Bundle携带数据
        Bundle bundle=new Bundle();
        //传递name参数为
        /*
        SendTts st = new SendTts();
        st.setTts(tts);
        st.setFilename(sSelectedFile);
        st.setLanguage(sSelectedLanguage);
        st.setSpeechPitch(speechPitch);
        st.setSpeechRate(speechRate);

        bundle.putSerializable("TTS_OBJ",st);

        intent.putExtras(bundle);
        */
        bundle.putString("Filename",sSelectedFile);
        bundle.putString("Language",language);
        bundle.putFloat("SpeechPitch",speechPitch);
        bundle.putFloat("SpeechRate",speechRate);
        bundle.putInt("SpeechLength",speechLength);
        intent.putExtras(bundle);

        // startActivity(intent);

        // 关闭tts
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        tts = null;
        isInited = false;
        // 新打开的activity返回的数据
        startActivityForResult(intent, ACTIVITY_FILE_SPEECH);
    }

    public void getTtsSettings(String propFile){
        boolean b=false;
        String s="";
        int i=0;
        Properties prop;
        prop=loadConfig(propFile); //"/mnt/sdcard/config.properties"
        if(prop==null){
            //配置文件不存在的时候创建配置文件 初始化配置信息
            prop=new Properties();

            prop.setProperty("SpeechPitch","0.8");
            prop.setProperty("SpeechRate","1.5");
            prop.setProperty("Language","Chinese");

            prop.put("Encoding", "UTF-8");
            prop.put("SpeechLength", "200"); //也可以添加基本类型数据 get时就需要强制转换成封装类型
            saveConfig(propFile,prop);
        }


        String sSpeechPitch = prop.getProperty("SpeechPitch");
        if (sSpeechPitch == null || sSpeechPitch.isEmpty()){
            speechPitch = 0.9f;
        }else{
            speechPitch = Float.valueOf(prop.getProperty("SpeechPitch")).floatValue();
        }

        String sSpeechRate = prop.getProperty("SpeechRate");
        if (sSpeechRate == null || sSpeechRate.isEmpty()){
            speechRate = 0.9f;
        }else{
            speechRate = Float.valueOf(prop.getProperty("SpeechRate")).floatValue();
        }

        language = prop.getProperty("Language");
        encoding = prop.getProperty("Encoding");

        String sSpeechLength = prop.getProperty("SpeechLength");
        if (sSpeechLength == null || sSpeechLength.isEmpty()) {
            speechLength = 200;
        }else {
            speechLength = Integer.parseInt(prop.getProperty("SpeechLength"));
        }

        if (encoding == null || encoding.isEmpty()){
            encoding = DEFAULT_ENCODING;
        }
        if (language == null || language.isEmpty()){
            language = "Chinese";
        }

        if (speechLength < 200){
            speechLength = 200;
        }else if (speechLength > 1000) {
            speechLength = 1000;
        }

        if (speechPitch <= 0){
            speechPitch = 0.9f;
        }
        if (speechRate <= 0){
            speechRate = 0.9f;
        }

        saveConfig(propFile,prop);
    }

    //读取配置文件
    public Properties loadConfig( String file) {
        Properties properties = new Properties();
        try {
            FileInputStream s = new FileInputStream(file);
            properties.load(s);
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return properties;
    }

    //保存配置文件
    public boolean saveConfig( String file, Properties properties) {
        try {
            File fil=new File(file);
            if(!fil.exists())
                fil.createNewFile();
            FileOutputStream s = new FileOutputStream(fil);
            properties.store(s, "");
            s.flush();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public boolean copyApkFromAssets(Context context, String fileName, String path) {
        boolean copyIsFinish = false;
        try {
            InputStream is = context.getAssets().open(fileName);
            File file = new File(path);
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            byte[] temp = new byte[1024];
            int i = 0;
            while ((i = is.read(temp)) > 0) {
                fos.write(temp, 0, i);
            }
            fos.close();
            is.close();
            copyIsFinish = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return copyIsFinish;
    }

    private Context mContext;
    // 安装tts的Apk
    private void installApkFromAssets(){
        String apkFile = this.getString(R.string.tts_package);
        final String destFile = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+apkFile;
        mContext = this;
        //第一步：保存apk文件到sdcard或者其他地方
        if(copyApkFromAssets(this, apkFile,destFile)){
            AlertDialog.Builder alertInstall = new AlertDialog.Builder(mContext)
                    .setIcon(R.drawable.borderless_button).setMessage("是否安装？")
                    .setIcon(R.drawable.borderless_button)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //第二步：修改apk文件的权限为可执行 ，例如chmod ‘777’ file：
                            String command     = "chmod 777 " + destFile;
                            Runtime runtime = Runtime.getRuntime();
                            try {
                                runtime.exec(command);
                            }catch (IOException e){
                                Log.e("installApkFromAssets","exec "+command + " Failed! ErrorMsg: " + e.getMessage());
                            }
                            //第三步：使用Intent 调用安装：
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setDataAndType(Uri.parse("file://" + destFile),
                                    "application/vnd.android.package-archive");
                            mContext.startActivity(intent);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            finish();
                        }
                    });
            alertInstall.create().show();
        }
    }

    //弹出对话框提示安装所需的TTS数据
    private void alertInstallEyesFreeTTSData()
    {
        mContext = this;
        AlertDialog.Builder alertInstall = new AlertDialog.Builder(this)
                .setTitle("缺少需要的语音包")
                .setMessage("下载安装缺少的语音包")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        //下载eyes-free的语音数据包
                        String ttsDataUrl = "http://eyes-free.googlecode.com/files/tts_3.1_market.apk";
                        // String ttsDataUrl = mContext.getResources().getString(R.string.tts_package);
                        Uri ttsDataUri = Uri.parse(ttsDataUrl);
                        Intent ttsIntent = new Intent(Intent.ACTION_VIEW, ttsDataUri);
                        startActivity(ttsIntent);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        finish();
                    }
                });
        alertInstall.create().show();
    }

    // 开始本地识别
    private void startRecognizerActivity() {
        // 通过Intent传递语音识别的模式，开启语音
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // 语言模式和自由模式的语音识别
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // 提示语音开始
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "开始语音");
        // 开始语音识别
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
        // 调出识别界面
    }

    // 开始网络识别
    private void startWebRecognizerActivity() {
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