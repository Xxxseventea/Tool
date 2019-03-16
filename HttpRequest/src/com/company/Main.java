package com.company;

public class Main{

    public static void main(String[] args) {
        HttpContent httpContent = new HttpContent.Builder()
                .setUrl("https://news-at.zhihu.com/api/4/news/latest")
                .setMethod("GET")
                .setMap(null)
                .build();
       ThreadPollProxyManage.getInstance().execute(httpContent, new CallBack() {
           @Override
           public void Finish(String response) {
               System.out.println(response);
           }
       });
    }
}
