package com.example.mvptest.bean;

public class UserModel implements User{
    private String name;
    private String password;
    public UserModel(String name,String password){
        this.name = name;
        this.password = password;
    }
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPaddword() {
        return password;
    }

    @Override
    public int checkUser() {
        if(!password.equals("txx")){
            return -1;
        }
        if(!name.equals("txx")){
            return -2;
        }
        return 0;
    }
}
