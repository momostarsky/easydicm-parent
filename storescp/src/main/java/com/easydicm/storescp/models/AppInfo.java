package com.easydicm.storescp.models;

public class AppInfo {

    private  String appId;
    private  String appSeckey;

    public  AppInfo(){

    }

    public AppInfo(String appid, String appSeckey){
        this.setAppId(appid);
        this.setAppSeckey(appSeckey);
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSeckey() {
        return appSeckey;
    }

    public void setAppSeckey(String appSeckey) {
        this.appSeckey = appSeckey;
    }
}
