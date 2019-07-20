package com.example.mvptest.presenter;

import com.example.mvptest.bean.User;
import com.example.mvptest.bean.UserModel;
import com.example.mvptest.view.IView;

public class ILoginPresenterCompl implements ILoginPresenter {

    IView iView;
    User user;

    public ILoginPresenterCompl(IView iView){
        this.iView = iView;
    }
    @Override
    public void clear() {
         iView.onClearText();
    }

    @Override
    public void doLogin(String name, String password) {
       user = new UserModel(name,password);
       boolean res = false;
       int code = user.checkUser();
       if(code == 1){
           res = true;
       }
       iView.onLoginResuit(res,code);
    }
}
