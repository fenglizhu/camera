package com.custom.julyum;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.security.Permission;

public class Camera extends CordovaPlugin {
    public static String TAG = "cameraPlugin";

    //打开相机
    public final static int CAMERA_TYPE_CAMERA = 0;
    //打开相册
    public final static int CAMERA_TYPE_ALBUM = 1;

    //压缩码
    public static int IS_COMPRESS_CODE = 0;

    //相机返回码
    static final int REQUEST_IMAGE_CAPTURE = 10;

    //相册返回码
    static final int REQUEST_ALBUM_CAPTURE = 20;

    //权限检查返回码
    static final  int RESULT_CODE_PERMISSION = 30;


    //图片压缩质量
    public static int mQuality = 50;

    //插件动作(打开相机/打开相册)
    public static int cameraType = CAMERA_TYPE_CAMERA;

    //插件动作是否压缩动（0：压缩，1：不压缩）
    public static int cameraCompress = IS_COMPRESS_CODE;

    //文件前缀名
    public static String preName = "";


    private CallbackContext callbackContext;
    

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        Log.d(TAG,action);
        if (action.equals("coolMethod")) {
            Log.d(TAG,args.length() + "");

            if(args.length() > 0){
                mQuality = args.getInt(0);
                cameraType = args.getInt(1);
            }
            if(args.length() > 1){
                cameraCompress = args.getInt(2);
            }
            if(args.length() > 2){
                preName = args.getString(3);
            }
            
            try {
                // cordova.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
                if(!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        || !PermissionHelper.hasPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || !PermissionHelper.hasPermission(this,Manifest.permission.CAMERA)){
                    PermissionHelper.requestPermissions(this,RESULT_CODE_PERMISSION,new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA
                    });
                }else{
                    camera();
                }

            }catch (Exception e){
                callbackContext.error("照相机开启异常");
                return true;
            }
            return true;
        }
        return super.execute(action, args, callbackContext);
    }


    private void camera() {


        //设置图片名称和路径
        CameraUtils.setImgName(preName.equals("") ? (CameraUtils.getCustomDate("yyyyMMdd_HHmmss") + ".jpg") : (preName + "_" +CameraUtils.getCustomDate("yyyyMMdd_HHmmss") + ".jpg"));
        CameraUtils.setImgPath(CameraUtils.getPhotoPath() + CameraUtils.imgName);
        //打开相机
        if(cameraType == CAMERA_TYPE_CAMERA){
            CameraUtils.openCamera(cordova,this,REQUEST_IMAGE_CAPTURE);
        }
        //打开相册
        else if (cameraType == CAMERA_TYPE_ALBUM) {
            CameraUtils.openAlbum(cordova,this,REQUEST_ALBUM_CAPTURE);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        System.out.println("执行函数onSaveInstanceState()");
        return super.onSaveInstanceState();
    }

    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        System.out.println("执行函数onRestoreStateForActivityResult()");
        this.callbackContext = callbackContext;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        System.out.println("执行函数onActivityResult()");
        Log.d(TAG,"resultCode===" + resultCode + "requestCode===" + requestCode);
        if(requestCode == REQUEST_ALBUM_CAPTURE){
            if(intent != null){
                //绝对路径
                String localPath = CameraUtils.getRealPathFromUri(this.cordova.getActivity(),intent.getData());

                String filePath = CameraUtils.saveAlbumFile(localPath, CameraUtils.imgPath, mQuality,cameraCompress);

                Log.d(TAG + "filePath===",filePath);
                PluginResult result;
                JSONArray array = new JSONArray();
                try {
                    array.put(0,filePath);
                    array.put(1,CameraUtils.imgName);
                    result = new PluginResult(PluginResult.Status.OK,array);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else {
                sendResultForError("图片损坏，请重新选择",true);
            }

        } else if(requestCode == REQUEST_IMAGE_CAPTURE){
            new Thread(new Runnable() {
                @Override
                public void run() {

                    Message message = new Message();
                    if(TextUtils.isEmpty(CameraUtils.imgPath) || !new File(CameraUtils.imgPath).exists()){
                        message.obj = resultCode == 0 ? "拍照已取消" : "图片本地存储路径丢失";
                        sendResultForError((String) message.obj,true);
                        return;
                    }
                    String filePath = CameraUtils.saveCameraFile(CameraUtils.getPhotoPath(),mQuality,cameraCompress);
                    Log.d(TAG + "filePath===",filePath);
                    PluginResult result;
                    JSONArray array = new JSONArray();
                    try {
                        array.put(0,filePath);
                        array.put(1,CameraUtils.imgName);
                        result = new PluginResult(PluginResult.Status.OK,array);
                        result.setKeepCallback(true);
                        callbackContext.sendPluginResult(result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    //向JS回调插件执行异常的数据
    public void sendResultForError(String message,boolean keep){
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, message);
        pluginResult.setKeepCallback(keep);
        callbackContext.sendPluginResult(pluginResult);
    }

    /**
     * @param requestCode   权限返回码
     * @param permissions   权限集合
     * @param grantResults
     * @throws JSONException
     */
    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        System.out.println("执行函数onRequestPermissionResult(),requestCode==" + requestCode);
        for (int r : grantResults){
            if(r == PackageManager.PERMISSION_DENIED){
                //这是禁止没有权限时的回调
                if(requestCode == RESULT_CODE_PERMISSION){
                    sendResultForError(cameraType == CAMERA_TYPE_CAMERA ? "缺少权限，无法打开相机" : "缺少权限，无法打开相册",true);
                }
                return;
            }
        }
        switch (requestCode){
            case RESULT_CODE_PERMISSION:
                camera();
                break;
            default:
                break;
        }
    }
}

/**
 * 用于启动活动的事件的完整顺序如下
 *  1.Cordova应用程序调用您的插件
 *  2.您的插件会启动一个Activity以产生结果
 *  3.Android操作系统销毁了Cordova Activity和您的插件实例，叫做
 *    onSaveInstanceState()
 *  4.用户与您的活动进行交互，活动完成
 *  5.重新创建Cordova活动并收到活动结果，叫做
 *    onRestoreStateForActivityResult()
 *  6.onActivityResult() 被调用，您的插件将结果传递给新的CallbackContext
 *  7.该resume事件由Cordova应用程序触发并接收
 */

