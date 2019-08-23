package com.example.imageloader.ImageLoaderUtil;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileDescriptor;

/**
 * 图片压缩工具类
 */
public class ImageResizer {
    private static final String TAG = "ImageResizer";

    public ImageResizer(){}

    public Bitmap decodeSampleBitmapFromResource(Resources res,int reqId,int reqWidth,int reqHeight){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(res,reqId,options);

        options.inSampleSize = caclculateInSampleSize(options,reqWidth,reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res,reqId,options);
    }

    public Bitmap decodeSampledBitmapFromFileDescriptor(FileDescriptor fd,  int reqWidth, int reqHeight){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFileDescriptor(fd,null,options);

        options.inSampleSize = caclculateInSampleSize(options,reqWidth,reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd,null,options);
    }
    public int caclculateInSampleSize(BitmapFactory.Options options,int reqWidth,int reqHeight){
        if(reqHeight == 0||reqWidth == 0){
            return 1;
        }

        final int heigth = options.outHeight;
        final int width = options.outWidth;
        Log.d(TAG,"origin,w="+width+"h="+heigth);

        int sampleSize = 1;

        if(heigth > reqHeight || width > reqWidth){
            final int halfWidth = width / 2;
            final int halfHeight = heigth / 2;

            while((halfHeight /  sampleSize) >= reqHeight &&( halfWidth / sampleSize) >= reqWidth){
                sampleSize *= 2;

            }
        }
        return sampleSize;
    }
}
