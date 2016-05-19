package com.mgtv.qxx.ttsdemo;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Administrator on 2016/5/18.
 */
public class LargFileRead {
    public void largeFileIO(String inputFile) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(inputFile)));
            BufferedReader in = new BufferedReader(new InputStreamReader(bis, "utf-8"), 1 * 1024);//10M缓存

            while (in.ready()) {
                String line = in.readLine();
                Log.e("largeFileIO",line + " ");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
