package com.company;

import javax.security.auth.callback.Callback;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class ThreadPollProxyManage {
    //通过ThreadPoolExecutor的代理类来对线程池的管理
    private static ThreadPollProxy threadPoolExecutor;

    //单例对象
    public static ThreadPollProxy getInstance() {
        synchronized (ThreadPollProxy.class) {
            if (threadPoolExecutor == null) {
                threadPoolExecutor = new ThreadPollProxy(3, 10, 1000);
            }
        }
        return threadPoolExecutor;
    }

    //通过ThreadPoolExecutor的代理来对线程池的管理
    public static class ThreadPollProxy {
        private ThreadPoolExecutor threadPoolExecutor;//线程池执行者
        private int corePoolSize;
        private int maximumPoolSize;
        private long keepAliveTime;

        public ThreadPollProxy(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
            this.corePoolSize = corePoolSize;
            this.maximumPoolSize = maximumPoolSize;
            this.keepAliveTime = keepAliveTime;
        }

        //对外提供一个执行任务的方法
        public void execute(HttpContent httpContent,CallBack callBack) {
            String method = httpContent.getMethod();
            String url = httpContent.getUrl();
            Map<String, String> map = httpContent.getMap();
            if (threadPoolExecutor == null || threadPoolExecutor.isShutdown()) {
                threadPoolExecutor = new ThreadPoolExecutor(
                        //核心线程数量
                        corePoolSize,
                        //最大线程数量
                        maximumPoolSize,
                        //当线程空闲时，保持活跃时间
                        keepAliveTime,
                        //时间单元，毫秒级
                        TimeUnit.MILLISECONDS,
                        //线程任务队列
                        new LinkedBlockingQueue<Runnable>(),
                        //创建线程的工厂
                        Executors.defaultThreadFactory()
                );
            }
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    HttpURLConnection httpURLConnection = null;
                    BufferedReader reader = null;
                    String response = null;
                    try {
                        URL url1 = new URL(url);
                        httpURLConnection = (HttpURLConnection) url1.openConnection();
                        httpURLConnection.setRequestMethod(method);
                        httpURLConnection.setReadTimeout(8000);
                        httpURLConnection.setConnectTimeout(8000);
                        httpURLConnection.setDoOutput(true); //设置输出可用
                        httpURLConnection.setDoInput(true);//设置输入可用
                        httpURLConnection.setUseCaches(false);//设置不可缓存
                        httpURLConnection.connect();

                        if (method.equals("POST")) {
                            StringBuffer sbRequest = new StringBuffer();
                            if (map != null && map.size() > 0) {
                                for (String key : map.keySet()) {
                                    sbRequest.append(key + "=" + map.get(key) + "&");
                                }
                                String params = sbRequest.substring(0,sbRequest.length()-1);
                                OutputStream os = httpURLConnection.getOutputStream();
                                os.write(params.getBytes());
                                os.flush();
                            }
                        }
                        if (httpURLConnection.getResponseCode() == 200) {
                            InputStream is = httpURLConnection.getInputStream();
                            reader = new BufferedReader(new InputStreamReader(is));
                            StringBuffer stringBuffer = new StringBuffer();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                stringBuffer.append(line);
                            }
                            response = stringBuffer.toString();
                            callBack.Finish(response);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        if(reader != null){
                            try{
                                reader.close();
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                        if(httpURLConnection != null){
                            httpURLConnection.disconnect();
                        }
                    }
                }
            };
            threadPoolExecutor.execute(runnable);
        }
    }
}
