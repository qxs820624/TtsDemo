package com.mgtv.qxx.ttsdemo.process;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.mgtv.qxx.ttsdemo.QxxExec;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by Administrator on 2016/6/15.
 */

public class ImgPretreatment {
    private static final String LOG_TAG = "ImgPretreatment";
    private static Bitmap img;
    private static String imgPath;
    private static int imgWidth;
    private static int imgHeight;
    private static int[] imgPixels;

    private static void setImgInfo(Bitmap image) {
        img = image;
        imgWidth = img.getWidth();
        imgHeight = img.getHeight();
        imgPixels = new int[imgWidth * imgHeight];
        img.getPixels(imgPixels, 0, imgWidth, 0, 0, imgWidth, imgHeight);
    }

    /**
     * 将图片化成灰度图
     *
     * @param img
     * @return
     */
    public static Bitmap converyToGrayImg(Bitmap img) {
        if (img ==null)
            return null;
        Log.e("converyToGrayImg",img.toString());
        setImgInfo(img);

        return getGrayImg();
    }

    public static void setImgPath(String path){
        imgPath = path;
    }

    private static String getImagePath(){
        return imgPath;
    }


    public static Bitmap getDiskBitmap(String pathString)
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
                Log.e(LOG_TAG,"getDiskBitmap failed!");
            }
        } catch (Exception e)
        {
            // TODO: handle exception
            Log.e("Ocr-getDiskBitmap",e.toString());
        }
        return bitmap;
    }

    /**
     * 对图像进行预处理
     *
     * @param img
     * @return
     */
    public static Bitmap doPretreatment(Bitmap img) {

        Bitmap rotatedBitmap = RotateProcess(img);
        if (rotatedBitmap == null) {
            Log.e(LOG_TAG, "RotateProcess FAILED! rotatedBitmap = null");
            return null;
        }
        setImgInfo(PreProcess(rotatedBitmap));

        Bitmap grayImg = getGrayImg();

        int[] p = new int[2];
        int maxGrayValue = 0, minGrayValue = 255;
        // 计算最大及最小灰度值
        getMinMaxGrayValue(p);
        minGrayValue = p[0];
        maxGrayValue = p[1];
        // 计算迭代法阈值
        int T1 = getIterationHresholdValue(minGrayValue, maxGrayValue);
        // // 计算大津法阈值
        // int T2 = getOtsuHresholdValue(minGrayValue, maxGrayValue);
        // // 计算最大熵法阈值
        // int T3 = getMaxEntropytHresholdValue(minGrayValue, maxGrayValue);
        // int[] T = { T1, T2, T3 };
        //
        // Bitmap result = selectBinarization(T);
        Bitmap result = binarization(T1);

        return result;
    }

    /**
     * 获取当前图片的灰度图
     *
     * param img
     *            原图片
     * @return 灰度图
     */
    private static Bitmap getGrayImg() {

        int alpha = 0xFF << 24;
        for (int i = 0; i < imgHeight; i++) {
            for (int j = 0; j < imgWidth; j++) {
                int grey = imgPixels[imgWidth * i + j];

                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                grey = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                imgPixels[imgWidth * i + j] = grey;
            }
        }
        Bitmap result = Bitmap
                .createBitmap(imgWidth, imgHeight, Config.RGB_565);
        result.setPixels(imgPixels, 0, imgWidth, 0, 0, imgWidth, imgHeight);
        return result;
    }

    private static int getGray(int argb) {
        int alpha = 0xFF << 24;
        int red = ((argb & 0x00FF0000) >> 16);
        int green = ((argb & 0x0000FF00) >> 8);
        int blue = (argb & 0x000000FF);
        int grey;
        grey = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
        grey = alpha | (grey << 16) | (grey << 8) | grey;
        return grey;
    }

    // 利用迭代法计算阈值
    private static int getIterationHresholdValue(int minGrayValue,
                                                 int maxGrayValue) {
        int T1;
        int T2 = (maxGrayValue + minGrayValue) / 2;
        do {
            T1 = T2;
            double s = 0, l = 0, cs = 0, cl = 0;
            for (int i = 0; i < imgHeight; i++) {
                for (int j = 0; j < imgWidth; j++) {
                    int gray = imgPixels[imgWidth * i + j];
                    if (gray < T1) {
                        s += gray;
                        cs++;
                    }
                    if (gray > T1) {
                        l += gray;
                        cl++;
                    }
                }
            }
            T2 = (int) (s / cs + l / cl) / 2;
        } while (T1 != T2);
        return T1;
    }

    /*
     * 用大津法计算阈值T 大津法又称为最大类间方差法，由大津在1979年提出，选取使类间方差最
     * 大的灰度级作为分割阈值，方差值越大，说明图像两部分差别越大。
     */
    private static int getOtsuHresholdValue(int minGrayValue, int maxGrayValue) {
        int T = 0;
        double U = 0, U0 = 0, U1 = 0;
        double G = 0;
        for (int i = minGrayValue; i <= maxGrayValue; i++) {
            double s = 0, l = 0, cs = 0, cl = 0;
            for (int j = 0; j < imgHeight - 1; j++) {
                for (int k = 0; k < imgWidth - 1; k++) {
                    int gray = imgPixels[imgWidth * j + k];
                    if (gray < i) {
                        s += gray;
                        cs++;
                    }
                    if (gray > i) {
                        l += gray;
                        cl++;
                    }
                }
            }
            U0 = s / cs;
            U1 = l / cl;
            U = (s + l) / (cs + cl);
            double g = (cs / (cs + cl)) * (U0 - U) * (U0 - U)
                    + (cl / (cl + cs)) * (U1 - U) * (U1 - U);
            if (g > G) {
                T = i;
                G = g;
            }
        }
        return T;
    }

    // 采用一维最大熵法计算阈值
    private static int getMaxEntropytHresholdValue(int minGrayValue,
                                                   int maxGrayValue) {
        int T3 = minGrayValue, sum = 0;
        double E = 0, Ht = 0, Hl = 0;
        int[] p = new int[maxGrayValue - minGrayValue + 1];
        for (int i = minGrayValue; i <= maxGrayValue; i++) {
            for (int j = 0; j < p.length; j++) {
                p[j] = 0;
            }
            sum = 0;
            for (int j = 0; j < imgHeight - 1; j++) {
                for (int k = 0; k < imgWidth - 1; k++) {
                    int gray = imgPixels[imgWidth * j + k];
                    p[gray - minGrayValue] += 1;
                    sum++;
                }
            }

            double pt = 0;
            int offset = maxGrayValue - i;
            for (int j = 0; j < p.length - offset; j++) {
                if (p[j] != 0) {
                    Ht += (p[j] * (Math.log(p[j]) - Math.log(sum))) / sum;
                    pt += p[j];
                }
            }
            for (int j = p.length - offset; j < maxGrayValue - minGrayValue + 1; j++) {
                if (p[j] != 0) {
                    Ht += (p[j] * (Math.log(p[j]) - Math.log(sum))) / sum;
                }
            }
            pt /= sum;
            double e = Math.log(pt * (1 - pt)) - (Ht / pt) - Hl / (1 - pt);

            if (E < e) {
                E = e;
                T3 = i;
            }
        }
        return T3;
    }

    // 针对单个阈值二值化图片
    private static Bitmap binarization(int T) {
        // 用阈值T1对图像进行二值化
        for (int i = 0; i < imgHeight; i++) {
            for (int j = 0; j < imgWidth; j++) {
                int gray = imgPixels[i * imgWidth + j];
                if (gray < T) {
                    // 小于阈值设为白色
                    imgPixels[i * imgWidth + j] = Color.rgb(0, 0, 0);
                } else {
                    // 大于阈值设为黑色
                    imgPixels[i * imgWidth + j] = Color.rgb(255, 255, 255);
                }
            }
        }

        Bitmap result = Bitmap
                .createBitmap(imgWidth, imgHeight, Config.RGB_565);
        result.setPixels(imgPixels, 0, imgWidth, 0, 0, imgWidth, imgHeight);

        return result;
    }

    // 计算最大最小灰度,保存在数组中
    private static void getMinMaxGrayValue(int[] p) {
        int minGrayValue = 255;
        int maxGrayValue = 0;
        for (int i = 0; i < imgHeight - 1; i++) {
            for (int j = 0; j < imgWidth - 1; j++) {
                int gray = imgPixels[i * imgWidth + imgHeight];
                if (gray < minGrayValue)
                    minGrayValue = gray;
                if (gray > maxGrayValue)
                    maxGrayValue = gray;
            }
        }
        p[0] = minGrayValue;
        p[1] = maxGrayValue;
    }

    /**
     * 由3个阈值投票二值化图片
     *
     * @param T
     *            原图片
     * @param T
     *            三种方法获得的阈值
     * @return 二值化的图片
     */
    private static Bitmap selectBinarization(int[] T) {
        for (int i = 0; i < imgHeight; i++) {
            for (int j = 0; j < imgWidth; j++) {
                int gray = imgPixels[i * imgWidth + j];
                if (gray < T[0] && gray < T[1] || gray < T[0] && gray < T[2]
                        || gray < T[1] && gray < T[2]) {
                    imgPixels[i * imgWidth + j] = Color.rgb(0, 0, 0);
                } else {
                    imgPixels[i * imgWidth + j] = Color.rgb(255, 255, 255);
                }
            }
        }

        Bitmap result = Bitmap
                .createBitmap(imgWidth, imgHeight, Config.RGB_565);
        result.setPixels(imgPixels, 0, imgWidth, 0, 0, imgWidth, imgHeight);

        return result;
    }

    // 计算像素点（x,y)周围像素点的中值
    private static int getCenterValue(Bitmap img, int x, int y) {
        int[] pix = new int[9];

        int w = img.getHeight() - 1;
        int h = img.getWidth() - 1;
        //
        if (x > 0 && y > 0)
            pix[0] = getGray(img.getPixel(x - 1, y - 1));
        if (y > 0)
            pix[1] = getGray(img.getPixel(x, y - 1));
        if (x < h && y > 0)
            pix[2] = getGray(img.getPixel(x + 1, y - 1));
        if (x > 0)
            pix[3] = getGray(img.getPixel(x - 1, y));
        pix[4] = getGray(img.getPixel(x, y));
        if (x < h)
            pix[5] = getGray(img.getPixel(x + 1, y));
        if (x > 0 && y < w)
            pix[6] = getGray(img.getPixel(x - 1, y + 1));
        if (y < w)
            pix[7] = getGray(img.getPixel(x, y + 1));
        if (x < h && y < w)
            pix[8] = getGray(img.getPixel(x + 1, y + 1));

        int max = 0, min = 255;
        for (int i = 0; i < pix.length; i++) {
            if (pix[i] > max)
                max = pix[i];
            if (pix[i] < min)
                min = pix[i];
        }
        int count = 0;
        int i = 0;
        for (i = 0; i < 9; i++) {
            if (pix[i] >= min)
                count++;
            if (count == 5)
                break;
        }
        return pix[i];
    }

    public static Bitmap RotateProcess(Bitmap img){
        String path = getImagePath();
        if (path == null || path.isEmpty()){
            Log.e(LOG_TAG, "RotateProcess FAILED! getImagePath is  Empty");
            return null;
        }
        // 图片旋转角度
        int rotate = 0;
        ExifInterface exif = null;
        int imageOrientation = 0;
        try {
            if (path != null && !path.isEmpty()){
                exif =  new ExifInterface(path);
            }


            // 先获取当前图像的方向，判断是否需要旋转
            if (exif != null) {
                imageOrientation = exif
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);
            }
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
        Log.i("ImgPretreatment", "Current image orientation is " + imageOrientation);

        switch (imageOrientation)
        {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
            default:
                break;
        }
        Log.i(LOG_TAG, "Current image need rotate: " + rotate);
        if (rotate != 0){
            // 获取当前图片的宽和高
            int w = img.getWidth();
            int h = img.getHeight();

            // 使用Matrix对图片进行处理
            Matrix mtx = new Matrix();
            mtx.preRotate(rotate);

            // 旋转图片
            img = Bitmap.createBitmap(img, 0, 0, w, h, mtx, false);
            img = img.copy(Bitmap.Config.ARGB_8888, true);
        }

        return img;
    }

    public static Bitmap PreProcess(Bitmap bmpFile) {
        // Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //在这里创建了一张bitmap
        // mBitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.ic_vd_mic_on);
        //将这张bitmap设置为背景图片
        //setBackgroundDrawable(new BitmapDrawable(mBitmap));

        Bitmap newBmpFile;
        int iBitmapWidth = bmpFile.getWidth();
        int iBitmapHeight = bmpFile.getHeight();

        int iArrayColorLengh = iBitmapWidth * iBitmapHeight;
        try{

            //创建一个临时文件
            File file = new File(QxxExec.getExternalSDCardPath() + "/tmp.txt");
            // file.getParentFile().mkdirs();

            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, iArrayColorLengh *4);

            //将位图信息写进buffer
            bmpFile.copyPixelsToBuffer(map);

            //释放原位图占用的空间
            bmpFile.recycle();
            //创建一个新的位图
            newBmpFile = Bitmap.createBitmap(iBitmapWidth, iBitmapHeight, Bitmap.Config.ARGB_8888);
            map.position(0);
            //从临时缓冲中拷贝位图信息
            newBmpFile.copyPixelsFromBuffer(map);
            channel.close();
            randomAccessFile.close();
            file.delete();
            // -----------------=============================
            int iArrayColor[]  = new int[iArrayColorLengh];
            int count = 0;
            for (int y = 0; y < iBitmapHeight; y++) {
                for (int x = 0; x < iBitmapWidth; x++) {
                    //获得Bitmap 图片中每一个点的color颜色值
                    int color = newBmpFile.getPixel(x, y);
                    //将颜色值存在一个数组中 方便后面修改
                    iArrayColor[count] = color;
                    //如果你想做的更细致的话 可以把颜色值的R G B 拿到做响应的处理 笔者在这里就不做更多解释
                    int r = Color.red(color);
                    int g = Color.green(color);
                    int b = Color.blue(color);

                    count++;

                    //去掉边框
                    if (x == 0 || y == 0 || x == iBitmapWidth - 1 || y == iBitmapHeight - 1)
                    {
                        newBmpFile.setPixel(x, y, Color.WHITE);
                    }else {
                        //如果点的颜色是背景干扰色，则变为白色
                        if (color == Color.rgb(204, 204, 51) ||
                                color == Color.rgb(153, 204, 51) ||
                                color == Color.rgb(204, 255, 102) ||
                                color == Color.rgb(204, 204, 204) ||
                                color == Color.rgb(204, 255, 51))
                        {
                            newBmpFile.setPixel(x, y, Color.WHITE);
                        }
                    }
                }
            }
        }
        catch(Exception ex){
            Log.e("ImgPretreatment",ex.getMessage());
            ex.printStackTrace();
            newBmpFile=null;
        }
        return newBmpFile;
        // long startTime = System.currentTimeMillis();
    }

}
