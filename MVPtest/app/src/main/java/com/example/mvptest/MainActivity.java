package com.example.mvptest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.mvptest.presenter.ILoginPresenterCompl;
import com.example.mvptest.view.IView;

public class MainActivity extends AppCompatActivity implements IView {
    ILoginPresenterCompl iLoginPresenterCompl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onClearText() {

    }

    @Override
    public void onLoginResuit(Boolean res, int code) {
                   switch (code){
                       case 0:
                       case -1:
                       case -2:
                   }
    }
}
