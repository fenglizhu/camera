package com.custom.julyum;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zhufengli on 2020/2/25.
 */

public class CameraUtils {
    public static final String TAG = "cameraPlugin";
    //图片名
    public static String imgName;
    //图片路径
    public static String imgPath;

    public static String FILEPROVIDER  = ".provider";

    /**
     * 打开手机相机
     * @param cordova
     * @param plugin
     * Category
     * Category属性用于指定当前动作（action）被执行的环境。
     * 通过addCategory()方法或在清单文件AndroidManifest.xml中设置。默认为：CATEGORY_DEFAULT
     */
    public static void  openCamera(CordovaInterface cordova, CordovaPlugin plugin,int requestCode){

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        intent.addCategory(Intent.CATEGORY_DEFAULT);
        //调用前置摄像头
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1);

        Uri imageUri = null;
        //判断SDK版本号
        if (Build.VERSION.SDK_INT >=24){
            Log.d(TAG,Build.MODEL);
            try{
                File imageFile = new File(imgPath);
                imageUri = FileProvider.getUriForFile(cordova.getActivity(),cordova.getActivity().getApplication().getPackageName()+FILEPROVIDER,imageFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }catch(Exception e){
                e.printStackTrace();
            }
        }else{
            imageUri = Uri.fromFile(new File(imgPath));
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        cordova.setActivityResultCallback(plugin);
        cordova.getActivity().startActivityForResult(intent,requestCode);
    }

    /**
     * 打开手机相册
     * @param cordova
     * @param plugin
     * @param requestCode
     */
    public static void openAlbum(CordovaInterface cordova,CordovaPlugin plugin,int requestCode){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        cordova.setActivityResultCallback(plugin);
        cordova.getActivity().startActivityForResult(intent,requestCode);
    }

    /**
     * 保存相机拍到的图片
     * mkdirs("E:/project/demo")  生成所有目录
     * mkdir("E:/project/demo")   必须project目录存在才能生成demo目录
     *
     * @param path               图片相对路径
     * @param mQuality          图片质量
     * @param cameraCompress   是否需要压缩图片
     * @return                  返回图片文件绝对路径
     */
    public static String saveCameraFile(String path,int mQuality, int cameraCompress) {
        Log.d(TAG,"path===" + path);
        Log.d(TAG,"imgPath===" + imgPath);

        String filePath = null;
        try {
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                //检查文件路径在不在
                File fPath = new File(path);
                if(!fPath.exists()){
                    fPath.mkdirs();
                }
                //检查文件是否存在
                File file = new File(imgPath);
                if(!file.exists()){
                    file.createNewFile();
                }
                filePath = file.getAbsolutePath();
                if(cameraCompress == 0){
                    try {
                        //判断图片大于1Mb：二次压缩  小于1Mb：不压缩
                        int len = (int) file.length();
                        Bitmap bitmap;
                        if((len / 1024 / 1024) > 1){
                            bitmap = decodeSampleBitmap(filePath);
                        }else{
                            bitmap = BitmapFactory.decodeFile(filePath);
                        }
                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG,mQuality,fos);
                        Log.d(TAG, "宽度为" + bitmap.getWidth() + "高度为" + bitmap.getHeight());
                        fos.flush();
                        fos.close();
                    }catch(FileNotFoundException e){
                        e.printStackTrace();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
            return filePath;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            return filePath;
        }
    }

    /**
     *
     * @param oldPath           原图片绝对路径
     * @param newPath           新图片文件绝对路径
     * @param mQuality          图片质量
     * @param cameraCompress   是否需要压缩图片
     * @return                  返回新图片文件绝对路径
     */
    public static String saveAlbumFile(String oldPath,String newPath, int mQuality, int cameraCompress){
        //1.首先把选择的文件写入对应的路劲文件
        String filePath = null;
        File oldFile = new File(oldPath);
        if(!oldFile.exists() || !oldFile.isFile() || !oldFile.canRead()){
            return filePath;
        }
        try {
            FileInputStream fis = new FileInputStream(oldPath);     //读入原文件
            FileOutputStream fos = new FileOutputStream(newPath);
            byte[] buffer = new byte[1024];
            int byteRead = 0;
            while ((byteRead = fis.read(buffer)) != -1){
                fos.write(buffer,0,byteRead);
            }
            fis.close();
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //2.检查文件是否存在、是否需要压缩文件
        File file = new File(newPath);
        if(file.exists()){
            filePath = file.getAbsolutePath();
            Log.d(TAG,filePath);
            if(cameraCompress == 0){
                try {
                    //判断图片大于1Mb：二次压缩  小于1Mb：不压缩
                    int len = (int) file.length();
                    Bitmap bitmap;
                    if((len / 1024 / 1024) > 1){
                        bitmap = decodeSampleBitmap(filePath);
                    }else{
                        bitmap = BitmapFactory.decodeFile(filePath);
                    }
                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,mQuality,fos);
                    fos.flush();
                    fos.close();
                }catch(FileNotFoundException e){
                    e.printStackTrace();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }else{
            filePath = null;
        }
        return filePath;
    }

    /**
     * 压缩图片
     * @param path  图片的绝对路径
     * @return  返回Bitmap
     */
    public static Bitmap decodeSampleBitmap(String path){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);
        options.inJustDecodeBounds = false;
        options.inSampleSize = 2;
        return  BitmapFactory.decodeFile(path,options);
    }

    /**
     * 根据uri获取图片的绝对路径
     * @param context    上下文对象
     * @param uri        图片的uri
     * @return           如果图片存在，则返回图片的绝对路径，否则返回bull
     */
    public static String getRealPathFromUri(Context context, Uri uri) {
        int sdkVersion = Build.VERSION.SDK_INT;
        if(sdkVersion >= 19){
            return getRealPathAboveApi(context,uri);
        }else{
            return getRealPathBelowApi(context,uri);
        }
    }

    /**
     * 适配api19以下（不包含19），根据uri获取图片的绝对路径
     * @param context   上下文对象
     * @param uri       图片的uri
     * @return          如果图片存在，则返回图片的绝对路径，否则返回bull
     */
    private static String getRealPathBelowApi(Context context, Uri uri) {
        return getDataColumn(context,uri,null,null);
    }

    /**
     * 适配api19及以上，根据uri获取图片的绝对路径
     * @param context   上下文对象
     * @param uri       图片的uri
     * @return          如果图片存在，则返回图片的绝对路径，否则返回bull
     */
    @SuppressLint("NewApi")
    private static String getRealPathAboveApi(Context context, Uri uri) {
        String filePath = null;
        if(DocumentsContract.isDocumentUri(context, uri)){

            String id = DocumentsContract.getDocumentId(uri);

            if(isDownloadsDocument(uri)){
                Uri contentUri = ContentUris.withAppendedId(uri.parse("content://downloads/public_downloads"),Long.valueOf(id));
                filePath = getDataColumn(context, contentUri,null,null);

            }else if(isMediaDocument(uri)){
                String[] split = id.split(":");
                String type = split[0];                 //类型 有image/video/audio
                String seId = split[1];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = new String[]{seId};
                filePath = getDataColumn(context, contentUri,selection,selectionArgs);
            }

        } else if("content".equalsIgnoreCase(uri.getScheme())){
            //如果是content类型的uri
            if(isGooglePhotosUri(uri)){
                filePath = uri.getLastPathSegment();
            }else{
                filePath = getDataColumn(context, uri,null,null);
            }

        }else if("file".equalsIgnoreCase(uri.getScheme())){
            //如果是file类型的uri,可以直接获取路径
            filePath = uri.getPath();
        }
        return filePath;
    }

    /**
     * 返回Uri对应的文件路径
     * @param context
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        String[] projection = new String[]{MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        String path = null;
        try {
            cursor = context.getContentResolver().query(uri,projection,selection,selectionArgs,null,null);
            if (cursor != null && cursor.moveToFirst()){
                int index = cursor.getColumnIndexOrThrow(projection[0]);
                path = cursor.getString(index);
            }
        }catch(Exception e){
            if(cursor != null){
                cursor.close();
            }
        }
        return path;
    }

    //获得照片路径
    public static String getPhotoPath(){
        return Environment.getExternalStorageDirectory() + "/DCIM/";
    }

    //设置图片路径
    public static void setImgPath(String imgPath) {
        CameraUtils.imgPath = imgPath;
    }

    //设置图片名称
    public static void setImgName(String imgName) {
        CameraUtils.imgName = imgName;
    }

    public static String getCustomDate(String format){
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        Date date = new Date();
        return dateFormat.format(date);
    }


    /**
     * @param uri
     * @return      Uri权限是否为DownloadsProvider
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     * @return      Uri权限是否为MediaProvider
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     * @return  Uri授权是否是Google Photos
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

}
/**
 * 关于获取手机相册的图片路径，
 * 参考简书文章
 * https://www.jianshu.com/p/be3dd2749821
 */

