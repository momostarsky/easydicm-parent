package com.easydicm.storescp;

/***
 * 系统使用的全局变量
 */
public abstract class GlobalConstant {

    private GlobalConstant() {

    }

    /***
     * 本次链接的ClientId
     */
    public static final String AssicationClientId = "ClientId";

    /***
     * 本次链接的ApplicationId
     */
    public static final String AssicationApplicationId = "ApplicationId";

    /***
     * 本次链接的SessionId
     */
    public  static  final  String AssicationSessionId = "SessionId";


    /***
     * 本次连接的Map 对象
     */
    public  static  final  String AssicationSessionData = "SessionData";

    /***
     * 图像数据位置信息
     */
    public  static  final  String AssicationSopPostion = "SopPostion";

}
