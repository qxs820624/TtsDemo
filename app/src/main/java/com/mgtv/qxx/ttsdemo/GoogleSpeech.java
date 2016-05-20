package com.mgtv.qxx.ttsdemo;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Administrator on 2016/5/20.
 */
public class GoogleSpeech implements TextToSpeech.OnInitListener{

    private static final String LOG_TAG = "GoogleSpeech";
    private AudioManager audioManager; // 音频管理对象

    // 设置对象
    private static SetTts ttsSetting;

    private TextToSpeech mTts;

    private String recognizeText;

    private static  boolean isInited = false;

    private boolean isSetting = false; // 进入设置标记
    private boolean isRateChanged = false; // 速率改变标记
    private boolean isStopped = false; // TTS引擎停止发声标记

    // 初始化文本解析
    private ParseStringEncoding pse = new ParseStringEncoding();

    // 配置文件路径
    private String ttsSettingFile = "";

    // TTS配置
    private String language = "Chinese";
    private String encoding = "UTF-8";
    private float speechRate = (float) 0.8;
    private float speechPitch =  (float)1.5;
    private int speechLength =  200;
    private Button btnSpeak;

    private String sSelectedFile = "";

    private Context context;

    public GoogleSpeech(Context context, Button btnSpeak, String ttsSettingFile){
        this.btnSpeak = btnSpeak;
        this.ttsSettingFile = ttsSettingFile;
        this.ttsSetting = new SetTts(ttsSettingFile);
        this.context = context;
        initTts();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Log.e("TTS", "Initialization Success!");
            displayToast("初始化成功！");
            isInited = true;
            btnSpeak.setEnabled(true);
            mTts.setLanguage(Locale.CHINESE);
            //        Changing Pitch Rate
            //        You can set speed pitch level by using setPitch() function. By default the value is 1.0 You can set lower values than 1.0 to decrease pitch level or greater values for increase pitch level.
            mTts.setPitch(speechPitch);

            //        Changing Speed Rate
            //        The speed rate can be set using setSpeechRate(). This also will take default of 1.0 value. You can double the speed rate by setting 2.0 or make half the speed level by setting 0.5
            mTts.setSpeechRate(speechRate);
        } else {
            Log.e("TTS", "Initilization Failed!");
            displayToast("初始化错误！");
            isInited = false;
            // installApkFromAssets();
        }
    }

    /** 提示用户是否重装TTS引擎数据的对话框 */
    public void notifyReinstallDialog() {
        new AlertDialog.Builder(context).setTitle("TTS引擎数据错误")
                .setMessage("是否尝试重装TTS引擎数据到设备上？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 触发引擎在TTS引擎在设备上安装资源文件
                        Intent dataIntent = new Intent();
                        dataIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        context.startActivity(dataIntent);
                    }
                }).setNegativeButton("否", null).show();
    }

    /** 跳转到“语音输入与输出”设置界面 */
    private boolean jumpTtsSettings() {
        try {
            context.startActivity(new Intent("com.android.settings.TTS_SETTINGS"));
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    // /** 判断TTS是否正在发声 */
    // private boolean isSpeaking() {
    // // 使用mTts.isSpeaking()判断时，第一次speak()返回true，多次就返回false了。
    // return audioManager.isMusicActive();
    // }

    /** 停止当前发声，同时放弃所有在等待队列的发声 */
    private int ttsStop() {
        isStopped = true; // 设置标记
        return (null == mTts) ? TextToSpeech.ERROR : mTts.stop();
    }

    /** 释放资源（解除语音服务绑定） */
    private void ttsShutDown() {
        if (null != mTts) {
            mTts.shutdown();
        }
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

    public void finish(){
        return;
    }
    private Context mContext;

    // 安装tts的Apk
    private void installApkFromAssets(){
        String apkFile = context.getString(R.string.tts_package);
        final String destFile = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+apkFile;
        mContext = this.context;
        //第一步：保存apk文件到sdcard或者其他地方
        if(copyApkFromAssets(context, apkFile,destFile)){
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
        final Context mContext = context;
        final AlertDialog.Builder alertInstall = new AlertDialog.Builder(mContext)
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
                        mContext.startActivity(ttsIntent);
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

    public void  saveToWav(String filePath, String text){
        initTts();
        //将EditText里的内容保存为语音文件
        int r = -1;
        File fileSave = new File(filePath);
        if (Build.VERSION.SDK_INT < 21){
            r = mTts.synthesizeToFile(text, null, filePath);
        }else{
            r = mTts.synthesizeToFile(text,null,fileSave,TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
        }
        if(r == TextToSpeech.SUCCESS){
            displayToast("保存成功！");
        }
    }

    public void initTts(){
        //if (mTts != null){
        //    mTts.stop();
        //    mTts.shutdown();
        //}
        if (mTts == null){
            mTts = new TextToSpeech(context,this);
        }
    }

    public int setTtsLanguage(Locale language){
        initTts();
        return mTts != null ? mTts.setLanguage(language) : TextToSpeech.ERROR;
    }

    public int setPitch(float pitch){
        initTts();
        return mTts != null ? mTts.setPitch(pitch) : TextToSpeech.ERROR;
    }

    public int setSpeechRate(float rate){
        initTts();
        return mTts != null ? mTts.setSpeechRate(rate) : TextToSpeech.ERROR;
    }


    public void speakOut(String text, boolean bReadFile) {
        initTts();
        if (isInited){
            int result = -1;
            // 每次启动的时候都获取一下配置
            ttsSetting.getTtsSettings(ttsSettingFile);
            setPitch(ttsSetting.getSpeechPitch());
            setSpeechRate(ttsSetting.getSpeechRate());

            // displayToast(this.sSelectedLanguage);
            if (!bReadFile){
                // 当TTS调用speak方法时，它会中断当前实例正在运行的任务(也可以理解为清除当前语音任务，转而执行新的语音任务)
                // tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                // 当TTS调用speak方法时，会把新的发音任务添加到当前发音任务列队之后

                ArrayList<ParseStringEncoding.EncodingString> EsLists = pse.Parse(text);

                Log.e(LOG_TAG,String.valueOf(EsLists.size()));

                // 为了效率牺牲代码可读性
                if (Build.VERSION.SDK_INT < 21){
                    for (ParseStringEncoding.EncodingString es: EsLists){
                        mTts.setLanguage(es.txtType);
                        mTts.speak(es.txt, TextToSpeech.QUEUE_ADD, null);
                    }
                }else{
                    for (ParseStringEncoding.EncodingString es: EsLists){
                        Log.e(LOG_TAG,String.valueOf(es.txtType));
                        Log.e(LOG_TAG,String.valueOf(es.txt));
                        mTts.setLanguage(es.txtType);
                        mTts.speak(text,TextToSpeech.QUEUE_ADD,null,TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                    }
                }
            }
        }else {
            displayToast("TTS引擎初始化失败");
        }
    }

    //显示Toast函数
    private void displayToast(String s)
    {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

}
