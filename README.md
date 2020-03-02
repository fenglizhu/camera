# camera
A camera plugin about Android.

版本1.0.0

#### 使用说明

```
camera:function(){
    /**
     * 
     * @param success			function	成功回调
     * @param error				function	失败回调
     * @param camera			String		插件名称，固定值
     * @param coolMethod		String		插件方法，固定值
     * @param [100, 0, 1, ""]	 Array		 插件参数
     * [100, 0, 1, ""]:插件方法参数，具体对应如下：
     * 参数1：照片压缩质量：0-100
     * 参数2：类型 		0:相机     1:相册
     * 参数3：是否压缩    0:压缩     1:不压缩   注意:使用压缩文件的话
     * 参数4：照片前缀名
     */
    cordova.exec(success,error,"camera", "coolMethod" , [100, 0, 1, ""]);
}

success:function(result){
	//照片本地路径
    let src = result[0];
    //照片名称
    let name = result[1];
}

```

