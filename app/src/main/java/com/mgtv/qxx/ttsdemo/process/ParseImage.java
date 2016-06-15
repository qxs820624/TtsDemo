/*      						
 * Copyright 2010 Beijing Xinwei, Inc. All rights reserved.
 * 
 * History:
 * ------------------------------------------------------------------------------
 * Date    	|  Who  		|  What  
 * 2016-2-24	| duanbokan 	| 	create the file                       
 */

package com.mgtv.qxx.ttsdemo.process;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.mgtv.qxx.ttsdemo.constant.TessConstantConfig;
import com.mgtv.qxx.ttsdemo.constant.TessErrorCode;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class ParseImage
{
	private static final String TAG = "ParseImage";
	
	private static ParseImage instance;
	
	public static ParseImage getInstance()
	{
		if (instance == null)
		{
			instance = new ParseImage();
		}
		return instance;
	}

	/**
	 * 识别图片中文字,需要放入异步线程中进行执行
	 *
	 * @param imagePath
	 * @return
	 * @throws IOException
	 */
	public static String parseImageToString(String imagePath, boolean bImageProcessing) throws IOException
	{
		// 检验图片地址是否正确
		if (imagePath == null || imagePath.equals(""))
		{
			return TessErrorCode.IMAGE_PATH_IS_NULL;
		}

		// 获取Bitmap
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 2;
		Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

		// 图片旋转角度
		int rotate = 0;
		
		ExifInterface exif = new ExifInterface(imagePath);

		// 先获取当前图像的方向，判断是否需要旋转
		int imageOrientation = exif
				.getAttributeInt(ExifInterface.TAG_ORIENTATION,
						ExifInterface.ORIENTATION_NORMAL);
		
		Log.i(TAG, "Current image orientation is " + imageOrientation);
		
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
		
		Log.i(TAG, "Current image need rotate: " + rotate);

		// 获取当前图片的宽和高
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		// 使用Matrix对图片进行处理
		Matrix mtx = new Matrix();
		mtx.preRotate(rotate);

		// 旋转图片
		bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

		// 修改背景色为黑色，字体为白色
		// PreProcess(bitmap);
		if (bImageProcessing){
			Bitmap linegray = lineGrey(bitmap);
			bitmap = gray2Binary(linegray);
		}

		// 开始调用Tess函数对图像进行识别
		TessBaseAPI baseApi = new TessBaseAPI();
		baseApi.setDebug(true);
		// 使用默认语言初始化BaseApi
		baseApi.init(TessConstantConfig.TESSBASE_PATH,
				TessConstantConfig.DEFAULT_LANGUAGE_CHI);
		baseApi.setImage(bitmap);

		// 获取返回值
		String recognizedText = baseApi.getUTF8Text();
		baseApi.end();
		return recognizedText;
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
			File file = new File("/sdcard2/tmp/tmp.txt");
			file.getParentFile().mkdirs();

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
			newBmpFile=null;
		}
		return newBmpFile;
		// long startTime = System.currentTimeMillis();
	}

	// 图像灰度化
	public static Bitmap bitmap2Gray(Bitmap bmSrc) {
		// 得到图片的长和宽
		int width = bmSrc.getWidth();
		int height = bmSrc.getHeight();
		// 创建目标灰度图像
		Bitmap bmpGray = null;
		bmpGray = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		// 创建画布
		Canvas c = new Canvas(bmpGray);
		Paint paint = new Paint();
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);
		c.drawBitmap(bmSrc, 0, 0, paint);
		return bmpGray;
	}

	// 对图像进行线性灰度变化
	public static Bitmap lineGrey(Bitmap image)
	{
		//得到图像的宽度和长度
		int width = image.getWidth();
		int height = image.getHeight();
		//创建线性拉升灰度图像
		Bitmap linegray = null;
		linegray = image.copy(Bitmap.Config.ARGB_8888, true);
		//依次循环对图像的像素进行处理
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				//得到每点的像素值
				int col = image.getPixel(i, j);
				int alpha = col & 0xFF000000;
				int red = (col & 0x00FF0000) >> 16;
				int green = (col & 0x0000FF00) >> 8;
				int blue = (col & 0x000000FF);
				// 增加了图像的亮度
				red = (int) (1.1 * red + 30);
				green = (int) (1.1 * green + 30);
				blue = (int) (1.1 * blue + 30);
				//对图像像素越界进行处理
				if (red >= 255)
				{
					red = 255;
				}

				if (green >= 255) {
					green = 255;
				}

				if (blue >= 255) {
					blue = 255;
				}
				// 新的ARGB
				int newColor = alpha | (red << 16) | (green << 8) | blue;
				//设置新图像的RGB值
				linegray.setPixel(i, j, newColor);
			}
		}
		return linegray;
	}

	// 该函数实现对图像进行二值化处理
	public static Bitmap gray2Binary(Bitmap graymap) {
		//得到图形的宽度和长度
		int width = graymap.getWidth();
		int height = graymap.getHeight();
		//创建二值化图像
		Bitmap binarymap = null;
		binarymap = graymap.copy(Bitmap.Config.ARGB_8888, true);
		//依次循环，对图像的像素进行处理
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				//得到当前像素的值
				int col = binarymap.getPixel(i, j);
				//得到alpha通道的值
				int alpha = col & 0xFF000000;
				//得到图像的像素RGB的值
				int red = (col & 0x00FF0000) >> 16;
				int green = (col & 0x0000FF00) >> 8;
				int blue = (col & 0x000000FF);
				// 用公式X = 0.3×R+0.59×G+0.11×B计算出X代替原来的RGB
				int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
				//对图像进行二值化处理
				if (gray <= 95) {
					gray = 0;
				} else {
					gray = 255;
				}
				// 新的ARGB
				int newColor = alpha | (gray << 16) | (gray << 8) | gray;
				//设置新图像的当前像素值
				binarymap.setPixel(i, j, newColor);
			}
		}
		return binarymap;
	}
}
