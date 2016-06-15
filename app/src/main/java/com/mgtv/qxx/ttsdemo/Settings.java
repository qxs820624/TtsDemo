package com.mgtv.qxx.ttsdemo;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Created by Administrator on 2016/5/16.
 */
public class Settings extends Activity {

    private  static final String LOG_TAG="Settings";

    private Button btSaveSetting;
    private String propertyFile = "";
    private Spinner spEncoding;
    private Properties properties;

    private EditText etSpeechPitch,etSpeechRate,etSpeechLength;
    private SeekBar pitchBar, rateBar,lengthBar; // 音量&语速

    private String language, encoding;
    private float speechPitch, speechRate;
    private int speechLength;
    private boolean bImageProcessing;

    private CheckBox cbImageProcessing;
    private  static final String DEFAULT_ENCODING = "UTF-8";
    public  static final String PROPERTY_IMAGE_PROCESSING = "ImageProcessing";
    public  static final String PROPERTY_SPEECH_LENGTH = "SpeechLength";

    public Settings(String ttsSettingFile){
        Log.d(LOG_TAG,"SetTts ttsSettingFile");
        if (ttsSettingFile.length() > 0 ){
            this.propertyFile = ttsSettingFile;
        }
    }

    // constructor
    public Settings(){
        Log.d(LOG_TAG,"Start SetTts Activity");
    }

    public float getSpeechPitch(){
        return this.speechPitch;
    }

    public float getSpeechRate(){
        return this.speechRate;
    }

    public String getEncoding(){
        return this.encoding;
    }

    public String getLanguage(){
        return this.language;
    }

    public int getSpeechLength(){
        Properties properties = loadConfig();
        return Integer.getInteger(properties.getProperty(PROPERTY_SPEECH_LENGTH));
    }

    public void setTtsLanguage(String language){
        this.language = language;
        Properties properties = loadConfig();
        properties.put("Language",language);
        saveConfig(properties);
    }

    public boolean getImageProcessing(){
        Properties properties = loadConfig();
        return Boolean.getBoolean(properties.getProperty(PROPERTY_IMAGE_PROCESSING));
    }

    public void setImageProcessing(boolean bImageProcessing){
        this.bImageProcessing = bImageProcessing;
        Properties properties = loadConfig();
        properties.put(PROPERTY_IMAGE_PROCESSING,String.valueOf(bImageProcessing));
        saveConfig(properties);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Log.e(LOG_TAG,"SetTts");

        setContentView(R.layout.settings);
        btSaveSetting = (Button) findViewById(R.id.bt_save_setting);
        spEncoding = (Spinner) findViewById(R.id.spinnerTextEncoding);

        etSpeechPitch = (EditText) findViewById(R.id.et_speech_pitch);
        etSpeechRate = (EditText) findViewById(R.id.et_speech_rate);
        etSpeechLength = (EditText) findViewById(R.id.et_speech_length);

        // 进度条
        pitchBar = (SeekBar) findViewById(R.id.seekBar_speech_pitch) ;
        rateBar = (SeekBar) findViewById(R.id.seekBar_speech_rate);
        lengthBar = (SeekBar) findViewById(R.id.seekBar_speech_length) ;
        cbImageProcessing = (CheckBox) findViewById(R.id.checkbox_image_processing);

        //新页面接收数据
        Bundle bundle = this.getIntent().getExtras();
        propertyFile = bundle.getString("PropertyFile");

        // 获取配置
        properties = loadConfig();
        // Log.e(LOG_TAG,properties.toString());
        if(properties==null || properties.isEmpty()){
            //配置文件不存在的时候创建配置文件 初始化配置信息
            properties=new Properties();

            properties.setProperty("SpeechPitch","0.8");
            properties.setProperty("SpeechRate","1.5");
            properties.setProperty("Language","Chinese");

            properties.setProperty("Encoding", "UTF-8");
            properties.setProperty(PROPERTY_SPEECH_LENGTH, "200");
            properties.setProperty(PROPERTY_IMAGE_PROCESSING,"true");
            saveConfig(properties);
        }
        etSpeechPitch.setText(properties.getProperty("SpeechPitch"));
        etSpeechRate.setText(properties.getProperty("SpeechRate"));
        etSpeechLength.setText(properties.getProperty(PROPERTY_SPEECH_LENGTH));

        String s = properties.getProperty(PROPERTY_IMAGE_PROCESSING);
        if (s.equalsIgnoreCase("true")){
            cbImageProcessing.setChecked(true);
        }else{
            cbImageProcessing.setChecked(false);
        }


        float fpitchBar = Float.parseFloat(properties.getProperty("SpeechPitch")) * 10.0f;
        pitchBar.setProgress(Integer.parseInt(String.valueOf(Math.round(fpitchBar))));

        float frateBar = Float.parseFloat(properties.getProperty("SpeechRate")) * 10.0f;
        rateBar.setProgress(Integer.parseInt(String.valueOf(Math.round(frateBar))));

        lengthBar.setProgress(Integer.parseInt(properties.getProperty(PROPERTY_SPEECH_LENGTH)));

        String propertiesEncoding = properties.getProperty("Encoding");
        if (propertiesEncoding == null || propertiesEncoding.isEmpty()){
            propertiesEncoding = DEFAULT_ENCODING;
        }
        setSpinnerItemSelectedByValue(spEncoding,propertiesEncoding);


        etSpeechLength.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //TODO
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (start > 1){
                    int length = Integer.parseInt(s.toString());
                    if (length > 10000){
                        length = 10000;
                    }else if (length < 100){
                        length = 100;
                    }
                    etSpeechLength.setText(String.valueOf(length));
                    lengthBar.setProgress(Math.round(length));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                //TODO
            }
        });

        etSpeechPitch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //TODO
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (start > 1){
                    Float val = Float.parseFloat(s.toString());
                    if (val > 10.0){
                        val = 10.0f;
                    }else if (val < 0.1){
                        val = 0.1f;
                    }
                    etSpeechPitch.setText(String.valueOf(val));
                    pitchBar.setProgress(Math.round(val*10));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                //TODO
            }
        });

        etSpeechRate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //TODO
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (start > 1){
                    Float val = Float.parseFloat(s.toString());
                    if (val > 10.0){
                        val = 10.0f;
                    }else if (val < 0.1){
                        val = 0.1f;
                    }
                    etSpeechRate.setText(String.valueOf(val));
                    rateBar.setProgress(Math.round(val*10));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                //TODO
            }
        });

        btSaveSetting.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                String pitch = etSpeechPitch.getText().toString();
                String rate = etSpeechRate.getText().toString();
                String length = etSpeechLength.getText().toString();
                boolean bImageProcessing = cbImageProcessing.isChecked();
                properties = loadConfig();

                if(properties==null || properties.isEmpty()){
                    //配置文件不存在的时候创建配置文件 初始化配置信息
                    properties=new Properties();

                    properties.setProperty("SpeechPitch","0.8");
                    properties.setProperty("SpeechRate","1.5");
                    properties.setProperty("Language","Chinese");

                    properties.put("Encoding", "UTF-8");
                    properties.put(PROPERTY_SPEECH_LENGTH, "200"); //也可以添加基本类型数据 get时就需要强制转换成封装类型
                    properties.put(PROPERTY_IMAGE_PROCESSING,"false");
                    saveConfig(properties);
                }

                properties.put(PROPERTY_IMAGE_PROCESSING,String.valueOf(bImageProcessing));

                if (!pitch.isEmpty()){
                    // Toast.makeText(getBaseContext(),pitch,Toast.LENGTH_SHORT).show();
                    properties.put("SpeechPitch",pitch);
                }else{
                    properties.put("SpeechPitch","0.9");
                }

                if (!rate.isEmpty()){
                    // Toast.makeText(getBaseContext(),rate,Toast.LENGTH_SHORT).show();
                    properties.put("SpeechRate",rate);
                }else{
                    properties.put("SpeechRate","0.9");
                }

                if (!length.isEmpty()){
                    // Toast.makeText(getBaseContext(),length,Toast.LENGTH_SHORT).show();
                    properties.put(PROPERTY_SPEECH_LENGTH,length);
                }else{
                    properties.put(PROPERTY_SPEECH_LENGTH,"200");
                }

                String EncodingStr = spEncoding.getSelectedItem().toString();
                if (EncodingStr != null && !EncodingStr.isEmpty()){
                    properties.put("Encoding",EncodingStr);
                }else{
                    properties.put("Encoding","UTF-8");
                }
                // Log.e(LOG_TAG,properties.toString());
                saveConfig(properties);
                Toast.makeText(getBaseContext(),"保存配置成功",Toast.LENGTH_SHORT).show();
                Settings.this.finish();
            }
        });

        pitchBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                etSpeechPitch.setText(String.valueOf(progress*0.1f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        rateBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                etSpeechRate.setText(String.valueOf(progress*0.1f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        lengthBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                etSpeechLength.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
    }

    //读取配置文件
    public Properties loadConfig() {
        Properties properties = new Properties();
        try {
            FileInputStream s = new FileInputStream(propertyFile);
            properties.load(s);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return properties;
    }

    //保存配置文件
    public boolean saveConfig(Properties properties) {
        try {
            // Log.e(LOG_TAG,propertyFile);
            File fil=new File(propertyFile);
            if(!fil.exists())
                fil.createNewFile();
            FileOutputStream s = new FileOutputStream(fil);
            properties.store(s, "Settings");
            // Log.e(LOG_TAG,String.valueOf(fil.length()));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 根据值, 设置spinner默认选中:
     * @param spinner
     * @param value
     */
    public static void setSpinnerItemSelectedByValue(Spinner spinner,String value){
        SpinnerAdapter apsAdapter= spinner.getAdapter(); //得到SpinnerAdapter对象
        int k= apsAdapter.getCount();
        for(int i=0;i<k;i++){
            if(value.equals(apsAdapter.getItem(i).toString())){
                spinner.setSelection(i,true);// 默认选中项
                break;
            }
        }
    }

    public void getTtsSettings(String propFile){
        boolean b=false;
        String s="";
        int i=0;
        Properties prop;
        prop=loadConfig(); //"/mnt/sdcard/config.properties"
        if(prop==null){
            //配置文件不存在的时候创建配置文件 初始化配置信息
            prop=new Properties();

            prop.setProperty("SpeechPitch","0.8");
            prop.setProperty("SpeechRate","1.5");
            prop.setProperty("Language","Chinese");

            prop.put("Encoding", "UTF-8");
            prop.put(PROPERTY_SPEECH_LENGTH, "200"); //也可以添加基本类型数据 get时就需要强制转换成封装类型
            saveConfig(prop);
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

        String sSpeechLength = prop.getProperty(PROPERTY_SPEECH_LENGTH);
        if (sSpeechLength == null || sSpeechLength.isEmpty()) {
            speechLength = 200;
        }else {
            speechLength = Integer.parseInt(prop.getProperty(PROPERTY_SPEECH_LENGTH));
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
        saveConfig(prop);
    }

}
