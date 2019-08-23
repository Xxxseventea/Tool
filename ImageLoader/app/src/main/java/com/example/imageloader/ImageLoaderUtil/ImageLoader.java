package com.example.imageloader.ImageLoaderUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.example.imageloader.R;
import com.example.imageloader.io.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageLoader {

    private static final int DISK_CACHE_INDEX = 0;
    private static final int DISK_CACHE_SIZE = 1024*1024*50;
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    private boolean mIsDiskLruCacheCreated = false;
    private static String TAG = "ImageLoader";

    private static final int TAG_KEY_URI = R.id.image_uri;
    private static final int MESSAGE_POST_RESULT = 1;
    private static final long KEEP_ALIVE = 10L;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2+1;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r,"ImageLoader#"+mCount.getAndIncrement());
        }
    };

    private static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,KEEP_ALIVE, TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(),sThreadFactory);

    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(Message msg) {
        LoaderResult result = (LoaderResult) msg.obj;
        ImageView imageView = result.imageView;
        imageView.setImageBitmap(result.bitmap);
        String uri = (String) imageView.getTag(TAGE_KEY_URI);
        if (uri.equals(result.uri)) {
            imageView.setImageBitmap(result.bitmap);
        } else {
            Log.w(TAG, "set image bitmap,but url has chaned,ignored!");
        }
        super.handleMessage(msg);
    }
};

    private Context context;
    private ImageResizer imageResizer = new ImageResizer();
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    /**
     * 分别用LruCache和DiskLruCache来完成内存缓存和磁盘缓存工作
     * 内存缓存和磁盘缓存
     * @param context
     */
    private ImageLoader(Context context){
        context = this.context.getApplicationContext();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory()/1024);
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String,Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };
        File diskCacheDir = getDiskCacheDir(context,"bitmap");
        if(!diskCacheDir.exists()){
            diskCacheDir.mkdirs();
        }
        if(getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE){
            try{
                mDiskLruCache = DiskLruCache.open(diskCacheDir,1,1,DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取内存缓存
     *
     */

    public static ImageLoader build(Context context){
        return new ImageLoader(context);
    }
    private void addBitmapToMemoryCache(String key,Bitmap bitmap){
        if(getBitmapFromMemCache(key) == null){
            mMemoryCache.put(key,bitmap);
        }
    }
    private Bitmap getBitmapFromMemCache(String key){
        return mMemoryCache.get(key);
    }

    /**
     * 获取磁盘缓存
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     * @throws IOException
     */
    private Bitmap loadBitmapFromHttp(String url,int reqWidth,int reqHeight) throws IOException{
        if(Looper.myLooper() == Looper.getMainLooper()){
            throw new RuntimeException("can not visit network from UI Thread");
        }
        if(mDiskLruCache == null){
            return null;
        }
        String key = hashKeyFormURL(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if(editor != null){
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            if(downloadUrlToString(url,outputStream)){
                editor.commit();
            }else {
                editor.abort();
            }
            mDiskLruCache.flush();
        }
        return loadBitmapFromDiskCache(url,reqWidth,reqHeight);
    }

    private Bitmap loadBitmapFromDiskCache(String url,int reqWidth,int reqHeight) throws IOException{
        if(Looper.myLooper() == Looper.getMainLooper()){
            Log.w(TAG,"load bitmap from UI Thread,it is not recommended!");
        }
        if(mDiskLruCache == null){
            return null;
        }
        Bitmap bitmap = null;
        String key = hashKeyFormURL(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if(snapshot != null){
            FileInputStream fileInputStream = (FileInputStream)snapshot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = imageResizer.decodeSampledBitmapFromFileDescriptor(fileDescriptor,reqWidth,reqHeight);
            if(bitmap != null){
                addBitmapToMemoryCache(key,bitmap);
            }
        }
        return bitmap;
    }

    /**
     * 同步加载
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     */

    private Bitmap loadBitmap(String url,int reqWidth,int reqHeight){
        Bitmap bitmap = loadBitmapFromMemCache(url);
        if(bitmap != null){
            Log.d(TAG,"loadbitmapfrommemcache,url:"+url);
            return bitmap;
        }
        try{
            bitmap = loadBitmapFromDiskCache(url,reqWidth,reqHeight);
            if(bitmap != null){
                Log.d(TAG,"loadbitmapfromDisk,url:"+url);
                return bitmap;
            }
            bitmap = loadBitmapFromHttp(url,reqWidth,reqHeight);
            Log.d(TAG,"loadBitmapFromHttp,url:"+url);
        }catch (IOException e){
            e.printStackTrace();
        }
        if(bitmap != null && !mIsDiskLruCacheCreated){
            Log.d(TAG,"encounter error,DiskLruCache is not created");
            bitmap = downloadBitmapFromUrl(url);
        }
        return bitmap;
    }

    /**
     * 异步加载
     * @param url
     * @return
     */
    private Bitmap loadBitmapFromMemCache(String url){
        final String key = hashKeyFormURL(url);
        Bitmap bitmap = getBitmapFromMemCache(key);
        return bitmap;
    }

    private void bindBitmap(final String url,final ImageView imageView){
        bindBitmap(url,imageView,0,0);
    }
    private void bindBitmap(final String uri,final ImageView imageView,final int reqWidth,final int reqHeight){
        imageView.setTag(TAG_KEY_URI,uri);
        final Bitmap bitmap = loadBitmapFromMemCache(uri);
        if(bitmap != null){
            imageView.setImageBitmap(bitmap);
            return ;
        }

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap1 = loadBitmap(uri,reqWidth,reqHeight);
                if(bitmap != null){
                    LoaderResult result = new LoaderResult(imageView,uri,bitmap);
                    mMainHandler.obtainMessage(MESSAGE_POST_RESULT,result).sendToTarget();

                }
            }
        };
        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }

    private static class LoaderResult{
        public ImageView imageView;
        public String uri;
        public Bitmap bitmap;

        public LoaderResult(ImageView imageView, String uri, Bitmap bitmap){
            this.imageView = imageView;
            this.uri = uri;
            this.bitmap = bitmap;
        }
    }

    public Bitmap downloadBitmapFromUrl(String urlString){
     Bitmap bitmap = null;
        HttpURLConnection httpURLConnection = null;
        BufferedInputStream in = null;
        try{
            final URL url = new URL(urlString);
            httpURLConnection = (HttpURLConnection)url.openConnection();
            in = new BufferedInputStream(httpURLConnection.getInputStream(),IO_BUFFER_SIZE);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(httpURLConnection != null){
                httpURLConnection.disconnect();
            }
        }
        return bitmap;
    }

    private String hashKeyFormURL(String url){
        String cacheKey;
        try{
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(url.getBytes());
            cacheKey = byteToHexString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
           cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    private String byteToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for(int i = 0;i<bytes.length;i++){
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if(hex.length() == 1){
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public File getDiskCacheDir(Context context,String uniqueName){
        boolean externalStorageAvailable = Environment.getExternalStorageDirectory().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if(externalStorageAvailable){
            cachePath = context.getExternalCacheDir().getPath();
        }else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    private long getUsableSpace(File path){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long)stats.getBlockSize() * (long)stats.getAvailableBlocks();
    }

    public boolean downloadUrlToString(String url, OutputStream outputStream){
        HttpURLConnection httpURLConnection = null;
        BufferedOutputStream bufferedOutputStream = null;
        BufferedInputStream bufferedInputStream = null;

        try{
            final URL url1 = new URL(url);
            httpURLConnection = (HttpURLConnection)url1.openConnection();
            bufferedInputStream = new BufferedInputStream(httpURLConnection.getInputStream(),IO_BUFFER_SIZE);
            bufferedOutputStream = new BufferedOutputStream(outputStream,IO_BUFFER_SIZE);

            int b;
            while((b = bufferedInputStream.read()) != -1){
                bufferedOutputStream.write(b);
            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(httpURLConnection != null){
                httpURLConnection.disconnect();
            }
        }
        return false;
    }
}
