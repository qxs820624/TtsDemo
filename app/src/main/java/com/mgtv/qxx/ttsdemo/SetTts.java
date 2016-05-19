package com.mgtv.qxx.ttsdemo;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
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
public class SetTts extends Activity {

    private  static final String LOG_TAG="SetTts";

    private Button btSaveSetting;
    private String propertyFile = "";
    private Spinner spEncoding;
    private Properties properties;

    private EditText etSpeechPitch,etSpeechRate,etSpeechLength;
    private SeekBar pitchBar, rateBar,lengthBar; // 音量&语速

    private  static final String DEFAULT_ENCODING = "UTF-8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        //新页面接收数据
        Bundle bundle = this.getIntent().getExtras();
        propertyFile = bundle.getString("PropertyFile");

        // 获取配置
        properties = loadConfig(propertyFile);
        if(properties==null){
            //配置文件不存在的时候创建配置文件 初始化配置信息
            properties=new Properties();

            properties.setProperty("SpeechPitch","0.8");
            properties.setProperty("SpeechRate","1.5");
            properties.setProperty("Language","Chinese");

            properties.setProperty("Encoding", "UTF-8");
            properties.setProperty("SpeechLength", "200");
            saveConfig(propertyFile,properties);
        }
        etSpeechPitch.setText(properties.getProperty("SpeechPitch"));
        etSpeechRate.setText(properties.getProperty("SpeechRate"));
        etSpeechLength.setText(properties.getProperty("SpeechLength"));

        float fpitchBar = Float.parseFloat(properties.getProperty("SpeechPitch")) * 10.0f;
        pitchBar.setProgress(Integer.parseInt(String.valueOf(Math.round(fpitchBar))));

        float frateBar = Float.parseFloat(properties.getProperty("SpeechRate")) * 10.0f;
        rateBar.setProgress(Integer.parseInt(String.valueOf(Math.round(frateBar))));

        lengthBar.setProgress(Integer.parseInt(properties.getProperty("SpeechLength")));

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
                // Log.e(LOG_TAG,pitch);
                // Log.e(LOG_TAG,rate);
                properties = loadConfig(propertyFile);

                if(properties==null){
                    //配置文件不存在的时候创建配置文件 初始化配置信息
                    properties=new Properties();

                    properties.setProperty("SpeechPitch","0.8");
                    properties.setProperty("SpeechRate","1.5");
                    properties.setProperty("Language","Chinese");

                    properties.put("Encoding", "UTF-8");
                    properties.put("SpeechLength", "200"); //也可以添加基本类型数据 get时就需要强制转换成封装类型

                    saveConfig(propertyFile,properties);
                }

                if (!pitch.isEmpty()){
                    // Toast.makeText(getBaseContext(),pitch,Toast.LENGTH_SHORT);
                    properties.put("SpeechPitch",pitch);
                }else{
                    properties.put("SpeechPitch","0.9");
                }

                if (!rate.isEmpty()){
                    // Toast.makeText(getBaseContext(),rate,Toast.LENGTH_SHORT);
                    properties.put("SpeechRate",rate);
                }else{
                    properties.put("SpeechRate","0.9");
                }

                if (!length.isEmpty()){
                    // Toast.makeText(getBaseContext(),length,Toast.LENGTH_SHORT);
                    properties.put("SpeechLength",length);
                }else{
                    properties.put("SpeechLength","200");
                }

                String EncodingStr = spEncoding.getSelectedItem().toString();
                if (EncodingStr != null && !EncodingStr.isEmpty()){
                    properties.put("Encoding",EncodingStr);
                }else{
                    properties.put("Encoding","UTF-8");
                }

                saveConfig(propertyFile,properties);
                Toast.makeText(getBaseContext(),"保存配置成功",Toast.LENGTH_SHORT);
                SetTts.this.finish();
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
    public Properties loadConfig(String file) {
        Properties properties = new Properties();
        try {
            FileInputStream s = new FileInputStream(file);
            properties.load(s);
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
}
