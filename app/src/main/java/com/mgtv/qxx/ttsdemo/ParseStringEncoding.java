package com.mgtv.qxx.ttsdemo;

import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2016/5/18.
 */
public class ParseStringEncoding {
    private static final String LOG_TAG = "ParseStringEncoding";
    private static final String EnglishPattern = "[a-zA-Z!@#$%^&*()-+:;\"'|\\,./<>?\\[\\]\\x20]{1,}";
    private static final String ChineseNumPattern = "[0-9\\u4e00-\\u9fa5。，：；“”【】！@￥……（）——《》？、·~_=]{1,}";
    public class EncodingString{
        public String txt;
        public Locale txtType;
        public EncodingString(String txt,Locale txtType){
            this.txt = txt;
            this.txtType = txtType;
        }
        public EncodingString getValue(){
            return this;
        }
    }
    public ArrayList<EncodingString> EsLists = new ArrayList<EncodingString>();
    public ArrayList<EncodingString> Parse(String txt){
        boolean isChinese = false;
        Matcher m = null;
        Pattern p = null;
        Locale txtType;
        String newTxt = "";
        newTxt = txt;
        int tlen = txt.length();
        Log.e(LOG_TAG,"tlen : " + String.valueOf(tlen));
        for (int i = 0;i<txt.length();){
            char[] chars=newTxt.toCharArray();
            byte[] bytes=(""+chars[0]).getBytes();
            // Log.e(LOG_TAG,"长度" + String.valueOf(bytes.length) + "第一个字符" + String.valueOf(chars[0]));
            if(bytes.length > 1 || (chars[0] >= 0x30 && chars[0] <= 0x39)){
                isChinese = true;
            }else{
                isChinese = false;
            }
            if (isChinese){
                // 数字和中文
                p = Pattern.compile(ChineseNumPattern);
                txtType = Locale.CHINA;
                // Log.d(LOG_TAG,"输入的是数字或者中文,");
            }else{
                // 数字和中文
                p = Pattern.compile(EnglishPattern);
                txtType = Locale.US;
                // Log.d(LOG_TAG,"输入的是英文,");
            }
            m = p.matcher(newTxt);
            if(m.find()){
                //Toast.makeText(this,"输入的是数字", Toast.LENGTH_SHORT).show();
                String txt1 = m.group();
                if (txt1 == null || txt.isEmpty()){
                    Log.e(LOG_TAG,"解析结束");
                    return EsLists;
                }
                i += txt1.length();
                if (i < tlen){
                    // Log.e(LOG_TAG,txt1 + txtType);
                    newTxt = txt.substring(i,tlen);
                    // Log.e(LOG_TAG,newTxt);
                    EncodingString es = new EncodingString(txt1,txtType);
                    EsLists.add(es.getValue());
                }else {
                    Log.i(LOG_TAG,"解析结束");
                    break;
                }
            }else {
                Log.e(LOG_TAG,"匹配失败");
                i++;
            }
        }
        return EsLists;
    }
}
