package com.mgtv.qxx.ttsdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.Toast;

import com.mgtv.qxx.ttsdemo.SwanTextView.OnPreDrawListener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Administrator on 2016/5/16.
 * Android大TXT文本文档读取
 */
public class TxtReader extends Activity implements
        OnInitListener,OnPreDrawListener {

    private static final String LOG_TAG = "TxtReader";
    private static final int BUF_SIZE = 1024 * 2;
    private static int BUF_SHOW = 10;

    private static final int ARROW_UP = 1;
    private static final int ARROW_DOWN = 2;

    private static String ENCODING = "UTF-8";

    private InputStreamReader mIsReader = null;
    private Uri mUri = null;
    private SwanTextView mTextShow;
    private ScrollView mScrollView;
    private String mStringShow = null;
    private StringBuilder mStringBuilder = null;

    private boolean mReadNext    = true;
    private boolean mReadBack    = false;
    private boolean mStopThread  = false;

    private TextToSpeech fileTts = null;
    private String filename = "";
    private String language = "";
    private float speechRate = (float) 0.8;
    private float speechPitch =  (float)1.5;
    private int speechLength = 200;

    private int mPreBottom  = -1;
    private int mCurBottom  = -1;
    private int mReadBufNum = 0;
    private int mBuffHeight = -1;
    private int mPreScrollY = -1;

    private  static final String DEFAULT_ENCODING = "UTF-8";

    private static  boolean isInited = false;

    public static final boolean isChineseCharacter(String chineseStr) {
        char[] charArray = chineseStr.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            Log.e(LOG_TAG, String.valueOf(charArray[i]));
            if ((charArray[i] >= 0x4e00) && (charArray[i] <= 0x9fbb)) {
                return true;
            }
        }
        return false;
    }

    public String trimPrintable(String str){
        // isChineseCharacter(str);
        if (str.isEmpty())
            return  "";
        byte [] bts =str.getBytes();

        int btsLength= bts.length;
        byte [] newBytes = new byte[btsLength];
        int j = 0;
        for (int i =0; i<btsLength;i++) {
            byte b =bts[i];
            // Log.e(LOG_TAG,String.valueOf(b));
            if((b >=0 && b <=31) || b ==127){
                b =32;
            }else {
                // Log.e(LOG_TAG,String.valueOf(b));
                newBytes[j]=b;
                j++;
            }
        }
        byte[] copy = new byte[j];
        System.arraycopy(newBytes,0,copy,0,j);
        return  new String(copy);
    }
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Log.e(LOG_TAG,"speakOut： " + ((CharBuffer) msg.obj).toString());
            String msgstr = trimPrintable(((CharBuffer) msg.obj).toString().trim());
            // Log.e(LOG_TAG,msgstr);
            speakOut(msgstr);
            switch (msg.what) {
                case ARROW_DOWN:
                    mTextShow.setText((CharBuffer) msg.obj);
                    break;
                case ARROW_UP:
                    mTextShow.setText((CharBuffer) msg.obj);
                    mScrollView.scrollTo(0, mBuffHeight);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_scroll);
        //新页面接收数据

        // 采用budle的方式
        Bundle extras  = this.getIntent().getExtras();

        //接收name值
        filename    = extras.getString("Filename");
        Log.i(LOG_TAG,"获取到的 filename 值为:"+ filename);
        language    = extras.getString("Language");
        Log.i(LOG_TAG,"获取到的 language 值为:"+ language);
        speechPitch = extras.getFloat("SpeechPitch");
        Log.i(LOG_TAG,String.valueOf("获取到的 speechPitch 值为:"+ speechPitch));
        speechRate  = extras.getFloat("SpeechRate");
        Log.i(LOG_TAG,String.valueOf("获取到的 speechRate 值为:"+ speechRate));
        speechLength = extras.getInt("SpeechLength");
        Log.i(LOG_TAG,String.valueOf("获取到的 speechLength 值为:"+ speechLength));

        ENCODING = (String) extras.get("Encoding");
        Log.i(LOG_TAG,"获取到的 filename 值为:"+ String.valueOf(ENCODING));
        if (ENCODING == null || ENCODING.isEmpty()){
            ENCODING = DEFAULT_ENCODING;
        }
        Log.i(LOG_TAG,"设置的 filename 值为:"+ String.valueOf(ENCODING));

        if (!isInited || fileTts == null){
            Log.e(LOG_TAG,"TextToSpeech 尚未初始化");
            fileTts = new TextToSpeech(this,this);
        }

        mUri = getIntent().getData();
    }

    @Override
    public void onPause(){
        if (fileTts != null) {
            fileTts.stop();
        }
        // isInited = false;
        // fileTts = null;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (fileTts != null) {
            fileTts.stop();
            fileTts.shutdown();
        }
        super.onDestroy();
    }

    private void showText(Uri uri) throws IOException, InterruptedException {
        BufferedReader inbr = null;
        // 高效缓存
        BufferedInputStream bis = null;
        FileInputStream fis = new FileInputStream(new File(uri.getPath()));
        // 设置 BUF_SHOW 的值
        BUF_SHOW = fis.available()/BUF_SIZE;
        Log.e(LOG_TAG, String.valueOf(BUF_SHOW));
        bis = new BufferedInputStream(fis);
        mIsReader = new InputStreamReader(bis, ENCODING);
        inbr = new BufferedReader(mIsReader,BUF_SIZE);


        mStringBuilder = new StringBuilder();
        int initBufSize = BUF_SIZE * (BUF_SHOW - 1);
        char[] buf = new char[BUF_SIZE];

        while (!mStopThread && (inbr != null) && inbr.ready()) {
            int scrollY = mScrollView.getScrollY();
            if (mCurBottom == scrollY && mPreScrollY < scrollY) {
                mReadNext = true;
                mReadBack = false;
            } else if (mReadBufNum > BUF_SHOW && 0 == scrollY && mPreScrollY != scrollY) {
                mReadNext = false;
                mReadBack = true;
            }

            mPreScrollY = scrollY;

            int len = -1;
            // if (mReadNext && (len = mIsReader.read(buf)) > 0) {
            if (mReadNext && (len = inbr.read(buf)) > 0) {
                mReadNext = false;
                mReadBufNum++;
                if (mStringBuilder.length() > initBufSize) {
                    mStringBuilder.delete(0, BUF_SIZE);
                    mPreBottom = mCurBottom;

                    Message msg = mHandler.obtainMessage(ARROW_DOWN);
                    msg.obj = CharBuffer.wrap(mStringBuilder.toString());
                    mHandler.sendMessage(msg);

                    mStringShow = mStringBuilder.append(buf, 0, len).toString();
                    Log.e(LOG_TAG,mStringShow);
                } else {
                    while (mStringBuilder.length() < initBufSize) {
                        mStringBuilder.append(buf);
                        // mIsReader.read(buf);
                        inbr.read(buf);
                        mReadBufNum++;
                        // Log.e(LOG_TAG,String.valueOf(buf));
                    }

                    mStringBuilder.append(buf);
                    Message msg = mHandler.obtainMessage(ARROW_DOWN);
                    msg.obj = CharBuffer.wrap(mStringBuilder.toString());
                    mHandler.sendMessage(msg);
                }
            } else if (mReadBack && mReadBufNum > BUF_SHOW) {
                Log.e(LOG_TAG, "Prepare to read back");
                mReadBack = false;
                mIsReader.close();
                inbr.close();
                bis.close();
                new BackBufReadThread(mStringBuilder).start();
            }
        }
    }

    private class TextShowTask extends AsyncTask<Object, Object, Object> {
        @Override
        protected void onPostExecute(Object param) {
            Log.d(LOG_TAG, "Send broadcast");
        }

        @Override
        protected Object doInBackground(Object... params) {
            Uri uri = (Uri) params[0];
            uri = Uri.parse(filename);
            try {
                showText(uri);
            } catch (Exception e) {
                Log.d(LOG_TAG, "Exception", e);
            }

            return null;
        }
    }

    private class BackBufReadThread extends Thread {
        StringBuilder mSbPre = null;

        public BackBufReadThread(StringBuilder sb) {
            mSbPre = sb.delete(0, sb.length());
        }

        @Override
        public void run() {
            try {
                // Log.d(LOG_TAG,"BackBufReadThread");
                BufferedReader inbr = null;
                // 高效缓存
                BufferedInputStream bis = null;

                bis = new BufferedInputStream(new FileInputStream(new File(mUri.getPath())));
                mIsReader = new InputStreamReader(bis, ENCODING);
                inbr = new BufferedReader(mIsReader,BUF_SIZE);

                String inputFile = mUri.getPath();
                try {
                    bis = new BufferedInputStream(new FileInputStream(new File(inputFile)));
                    inbr = new BufferedReader(new InputStreamReader(bis, "utf-8"), BUF_SIZE);//2K缓存

                    while (inbr.ready()) {
                        String line = inbr.readLine();
                        Log.d("largeFileIO",line + " ");
                        mSbPre.append(line);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                /*
                char[] buf = new char[BUF_SIZE];
                int i = 0;
                while((mReadBufNum - BUF_SHOW) > ++i && mIsReader.read(buf) > 0) {
                    // Just to skip the inputstream. Any better methods?
                }
                mReadBufNum--;

                for (i = 0; i < BUF_SHOW; i++) {
                    if (false){
                        mIsReader.read(buf);
                    }else{
                        inbr.read(buf);
                    }

                    if (buf.length > 0){
                        mSbPre.append(buf);
                    }
                }
                */
//                mSbPre.delete(mSbPre.length() - BUF_SIZE, mSbPre.length()).insert(0, buf);
                Message msg = mHandler.obtainMessage(ARROW_UP);
                msg.obj = CharBuffer.wrap(mSbPre.toString());
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception", e);
            }
        }
    }

    public void onPreDraw(int bottom) {
        mCurBottom = bottom - mScrollView.getHeight();

        if (!TextUtils.isEmpty(mStringShow)) {
            // Use the last deleted buff to evaluate the height
            mBuffHeight = mPreBottom - mScrollView.getScrollY();

            // Set the text to add new content without flash the view
            Message msg = mHandler.obtainMessage(ARROW_DOWN);
            msg.obj = CharBuffer.wrap(mStringShow);
            mHandler.sendMessage(msg);

            mStringShow = null;
        }
    }
    public void backActivity() {
        Intent intent = new Intent();
        intent.putExtra("result", "朗读完毕");

        TxtReader.this.setResult(RESULT_OK, intent);
        // TxtReader.this.finish();// 关闭activity

    }
    private ParseStringEncoding pse = new ParseStringEncoding();
    private void speakOut(String text) {
        //Log.d(LOG_TAG,"speakOut " + text);
        if (isInited){
            // Log.d(LOG_TAG,"TTS引擎初始化成功");
            int result = -1;
            /*
            int textLength = text.length();
            int leftLength = textLength;
            // 当TTS调用speak方法时，它会中断当前实例正在运行的任务(也可以理解为清除当前语音任务，转而执行新的语音任务)
            // tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            // 当TTS调用speak方法时，会把新的发音任务添加到当前发音任务列队之后
            for (int i = 0; i < textLength && leftLength > 0; i += speechLength){
                int start = i;
                int end = i;
                if (leftLength < speechLength){
                    end = i + leftLength;
                }else {
                    end = i + speechLength;
                }
                // Log.e(LOG_TAG,"textLength: " + String.valueOf(textLength) + ", leftLength:" + String.valueOf(leftLength));
                char sSpeakEachStr[]=new char[end - start];
                text.getChars(start,end,sSpeakEachStr,0);
                String toBeSpeak = String.valueOf(sSpeakEachStr);
                Log.e(LOG_TAG,"toBeSpeak: " + toBeSpeak);
                if (Build.VERSION.SDK_INT < 21){
                    fileTts.speak(toBeSpeak, TextToSpeech.QUEUE_ADD, null);
                }else{
                    fileTts.speak(toBeSpeak,TextToSpeech.QUEUE_ADD,null,TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                }
                leftLength -= speechLength;
            }
            */
            ArrayList<ParseStringEncoding.EncodingString> EsLists = pse.Parse(text);
            for (ParseStringEncoding.EncodingString es: EsLists){
                fileTts.setLanguage(es.txtType);
                if (Build.VERSION.SDK_INT < 21){
                    fileTts.speak(es.txt, TextToSpeech.QUEUE_ADD, null);
                }else{
                    fileTts.speak(es.txt,TextToSpeech.QUEUE_ADD,null,TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
                }
            }
        }else {
            Log.e(LOG_TAG,"TTS引擎初始化失败");
            Toast.makeText(this, "TTS引擎初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void finish() {
        mStopThread = true;
        backActivity();
        super.finish();
    }

    @Override
    public void onInit(int status) {
        // Log.e(LOG_TAG,"init Tts, status = " + String.valueOf(status));
        if (status == TextToSpeech.SUCCESS){
            if (language.equalsIgnoreCase("English") || language.equalsIgnoreCase("英语")){
                Log.e(LOG_TAG,"set language English");
                fileTts.setLanguage(Locale.US);
            }else {
                Log.e(LOG_TAG,"language=" + language + ", then set language Chinese");
                fileTts.setLanguage(Locale.CHINESE);
            }
            isInited = true;
            fileTts.setPitch(speechPitch);
            fileTts.setSpeechRate(speechRate);

            mScrollView = (ScrollView) findViewById(R.id.text_show_scroll);
            mTextShow = (SwanTextView) findViewById(R.id.text_to_read);
            mTextShow.setOnPreDrawListener(this);
            new TextShowTask().execute(mUri);
        }else {
            Toast.makeText(this,"TextToSpeech 初始化失败",Toast.LENGTH_SHORT).show();
        }
    }
}
