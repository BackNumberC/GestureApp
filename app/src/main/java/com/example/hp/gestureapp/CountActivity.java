package com.example.hp.gestureapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.ml.SVM;
import org.opencv.objdetect.HOGDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,Camera.PreviewCallback {
    /*static {
        System.loadLibrary("native-lib");
    }*/
    private String change_path = "/peoplechanged";
    public Bitmap testimg;
    public Bitmap photo;
    public Bitmap end;
    public Bitmap start;
    public Bitmap middle;
    public Matrix matrix;
    public int cx=0,cy=0;
    public float cr=0;
    public double x=0,y=0;
    public  int hullnum=0;
    public HandTask handTask;
    public LoadHandler mLoadhandler;

    private Camera mCamera;
    public int result=11;
    public int answer;
    public Camera.Parameters parameters;
    public Camera.Size size;
    public YuvImage image;
    public ByteArrayOutputStream os;
    //public Camera.PreviewCallback mPreviewCallback;
    private SurfaceHolder mHolder;
    private SurfaceView mPreview;
    private static final String TAG = "MainActivity";
    private TextView textView,recognize_result,answer_right;
    public int time=0;
    public String  question;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_count);
        //去掉顶部标题栏
        if (getSupportActionBar()!=null){
            getSupportActionBar().hide();
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mLoadhandler = new LoadHandler();
        textView=findViewById(R.id.random);
        recognize_result=findViewById(R.id.recognize_result);
        answer_right=findViewById(R.id.answer_right);

        testimg= BitmapFactory.decodeResource(getResources(),R.mipmap.test5);

        if (OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCVLoader初始化成功", Toast.LENGTH_SHORT).show();
        }

        mPreview=findViewById(R.id.preview);//相机界面
        mHolder=mPreview.getHolder();
        mHolder.addCallback(this);
        //点击屏幕自动聚焦
        mPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCamera.autoFocus(null);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.CAMERA}, 1);
            }
        }

        mCamera=Camera.open(1);//1是前置摄像头，0是后置摄像头
        /*
        //获取支持的尺寸
        parameters=mCamera.getParameters();
        List<Camera.Size> preview=mCamera.getParameters().getSupportedPreviewSizes();
        for (int i=0;i<preview.size();i++){
            size=preview.get(i);
            Log.d(TAG, "width:"+size.width+", high:"+size.height);
        }
        //parameters.setPreviewSize(240,320);
        */
        //设置练习题
        question="";
        while (result>=10 || result<0){
            question=proQuestion();
        }
        textView.setText(question);
        Thread answerThread=new Thread(new AnswerThread());
        answerThread.start();
        Thread handThread=new Thread(new HandThread());
        handThread.start();
    }

    //从视频流中获取帧
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (null != handTask) {
            switch (handTask.getStatus()) {
                case RUNNING:
                    return;
                case PENDING:
                    handTask.cancel(false);
                    break;
            }
        }
        handTask = new HandTask(data);
        handTask.execute((Void) null);
    }
    //线程HandThread执行的任务
    private class HandTask extends AsyncTask<Void, Void, Void> {
        private byte[] mData;
        public HandTask(byte[] data){
            this.mData = data;
        }//构造函数
        protected Void doInBackground(Void... params) {
            long startTime = System.currentTimeMillis(); // 获取开始时间
            size = mCamera.getParameters().getPreviewSize(); //获取预览大小
            image = new YuvImage(mData, ImageFormat.NV21, size.width, size.height, null);
            os = new ByteArrayOutputStream(mData.length);
            if(!image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, os)){
                return null;
            }
            byte[] tmp = os.toByteArray();
            Bitmap start = BitmapFactory.decodeByteArray(tmp, 0,tmp.length);
            matrix = new Matrix();
            matrix.postRotate(270);
            middle = Bitmap.createBitmap(start, 0,0, start.getWidth(),  start.getHeight(), matrix, true);
            os.reset();
            //从视频流中获取的图像end
            end = Bitmap.createScaledBitmap(middle, 225, 300, true); //创建新的图像大小
            //将end图像压缩
            end.compress(Bitmap.CompressFormat.PNG, 70, os);
            long middleTime = System.currentTimeMillis(); // 获取中间时间
            Log.i("wsy","代码运行时间： " + (middleTime - startTime) + "ms");
            Mat deal=new Mat();
            Utils.bitmapToMat(end,deal);
            //获得预测结果answer
            answer=skinsplit(deal);
            deal.release();
            long endTime = System.currentTimeMillis(); // 获取结束时间
            Log.i("wsy","代码运行时间： " + (endTime - middleTime) + "ms");

            long sysTime = System.currentTimeMillis();//获取系统时间
            CharSequence sysTimeStr = DateFormat.format("hh:mm:ss:mm", sysTime);//时间显示格式
            Log.i("CVCamera", "已获取"+sysTimeStr);
            /*
            if(answer==result){
                question="";
                result=11;
                while (result>=10 || result<0){
                    question=proQuestion();
                }
            }
            */
            return null;
        }
    }
    //线程HandThread（用于获取视频流并处理预测）
    class HandThread implements Runnable{
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    if(null != mCamera)
                    {
                        mCamera.setOneShotPreviewCallback(MainActivity.this);
                        Log.i(TAG, "setOneShotPreview...");
                    }
                    Thread.sleep(1200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    //线程AnswerThread执行的任务
    public class LoadHandler extends Handler{
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            textView.setText((String)msg.obj);
            recognize_result.setText("识别结果："+msg.what);
            Log.d("huidadaan", "answer:"+msg.what+"result:"+result);
            if (msg.what==result){
                answer_right.setText("正确");
                question="";
                result=11;
                while (result>=10 || result<0){
                    question=proQuestion();
                }
            }else {
                answer_right.setText("");
            }
        }
    }
    //线程AnswerThread（用于判断正误并刷新题目）
    class AnswerThread implements  Runnable{
        public void run(){
            while (!Thread.currentThread().isInterrupted()){
                try {
                    Message message = Message.obtain();
                    //message.arg1 = result;
                    message.obj = question;
                    message.what=answer;
                    mLoadhandler.sendMessage(message);
                    Thread.sleep(1200);
                }catch (InterruptedException e){
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    //设置摄像头参数
    private Camera getCamera(){
        Camera camera;//声明局部变量camera
        try{
            camera=Camera.open(0);
        }//根据cameraId的设置打开前置摄像头
        catch (Exception e){
            camera=null;
            e.printStackTrace();
        }
        return camera;
    }
    //开启预览界面
    private void setStartPreview(Camera camera,SurfaceHolder holder){
        try{
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(90);//如果没有这行你看到的预览界面就会是水平的
            camera.startPreview();
            /*camera.setPreviewCallback(new Camera.PreviewCallback(){
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Bitmap endBit;
                    Camera.Size size = mCamera.getParameters().getPreviewSize();
                    try{
                        YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                            time=0;
                            long startTime = System.currentTimeMillis(); // 获取开始时间
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                            //以上为将图片从YUV转化为JPG
                            Matrix matrix = new Matrix();
                            matrix.postRotate(270);
                            Bitmap end = Bitmap.createBitmap(bmp, 0,0, bmp.getWidth(),  bmp.getHeight(), matrix, true);
                            //将图片旋转放正
                            stream.reset();
                            endBit = Bitmap.createScaledBitmap(end, 225, 300, true); //创建新的图像大小
                            endBit.compress(Bitmap.CompressFormat.PNG, 70, stream);
                            //缩放并压缩图片
                            //String result="finally";
                            //saveBitmap(endBit,result);
                            //保存图片
                            Mat deal=new Mat();
                            Utils.bitmapToMat(endBit,deal);
                            skinsplit(deal);
                            deal.release();
                            Log.i("图片"," "+endBit.getWidth()+" "+endBit.getHeight()+" "+endBit.getByteCount());
                            long endTime = System.currentTimeMillis(); // 获取结束时间
                            Log.i("wsy","代码运行时间： " + (endTime - startTime) + "ms");
                            long sysTime = System.currentTimeMillis();//获取系统时间
                            CharSequence sysTimeStr = DateFormat.format("hh:mm:ss:mm", sysTime);//时间显示格式
                            Log.i("CVCamera", "已获取"+sysTimeStr);
                            stream.close();

                    }catch(Exception ex){
                        Log.e("Sys","Error:"+ex.getMessage());
                    }
                }
            });*/
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    //定义释放摄像头的方法
    private void releaseCamera(){
        if(mCamera!=null){//如果摄像头还未释放，则执行下面代码
            mCamera.stopPreview();//1.首先停止预览
            mCamera.setPreviewCallback(null);//2.预览返回值为null
            mCamera.release(); //3.释放摄像头
            mCamera=null;//4.摄像头对象值为null
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera==null){//如果此时摄像头值仍为空
            mCamera=getCamera();//则通过getCamera()方法开启摄像头
        }

        if(mHolder!=null){
            //parameters=mCamera.getParameters();
            setStartPreview(mCamera,mHolder);//开启预览界面
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        setStartPreview(mCamera,mHolder);
    }
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        mCamera.stopPreview();//如果预览界面改变，则首先停止预览界面
        setStartPreview(mCamera,mHolder);//调整再重新打开预览界面
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.setOneShotPreviewCallback(null);
        releaseCamera();//预览界面销毁则释放相机
    }

    //处理预测算法汇总
    public int skinsplit(Mat src) {
        int result1=0;
        Mat Dst=new Mat();
        Dst.create(src.size(),src.type());
        //中值滤波
        Imgproc.medianBlur(src,src,5);
        //将src转化为YCrCb
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2YCrCb);
        List<Mat> mv=new ArrayList<>();
        Core.split(src,mv);
        Mat dst=new Mat();
        //对Cr量进行OTSU阈值分割
        Imgproc.threshold(mv.get(1),dst,127,255,Imgproc.THRESH_BINARY|Imgproc.THRESH_OTSU);
        //膨胀腐蚀操作
        Mat k=Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(5,5),new Point(-1,-1));
        Imgproc.morphologyEx(dst,dst,Imgproc.MORPH_ERODE,k);
        Imgproc.morphologyEx(dst,dst,Imgproc.MORPH_DILATE,k);
        //获取轮廓
        double Maxarea=0;
        int number=0;
        Mat hierarchy=new Mat();
        List<MatOfPoint> contours=new ArrayList<MatOfPoint>();
        Imgproc.findContours(dst,contours,hierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE,new Point(0,0));
        Log.i("wsy","轮廓： " + contours.size()+ "个");
        for(int i=0;i<contours.size();i++){
            double area=Imgproc.contourArea(contours.get(i));
            if(area>Maxarea){
                Maxarea=area;
                number=i;
            }
        }
        //绘制轮廓
        Imgproc.drawContours(Dst,contours,number,new Scalar(255,255,255),-1);
        Imgproc.cvtColor(Dst,Dst,Imgproc.COLOR_BGR2GRAY);//轮廓筛选过滤掉无关部分
        //去除手腕
        Dst=wrist(Dst);
        //计算质心坐标
        Calculate(Dst);
        //对dst进行最小外接矩形裁剪
        dst=Dst.clone();
        Mat hierarchy1=new Mat();
        List<MatOfPoint> contours1=new ArrayList<MatOfPoint>();
        Imgproc.findContours(dst,contours1,hierarchy1,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE,new Point(0,0));
        MatOfPoint contour=contours1.get(0);
        org.opencv.core.Rect R=Imgproc.boundingRect(contour);
        Mat imgRectROI= new Mat(dst, R);
        //计算手指个数
        Dst=findHull(Dst);
        //将imgRectROI resize成指定尺寸再进行预测
        Imgproc.resize(imgRectROI,imgRectROI,new Size(96,128));
        result1=(int)predictd(imgRectROI);
        //释放所使用的dst
        dst.release();
        Dst.release();
        src.release();
        return result1;
    }
    //手腕去除
    public Mat wrist(Mat src){
        Mat mat3=new Mat();
        Imgproc.distanceTransform(src,mat3,Imgproc.CV_DIST_L1,3);
        //寻找圆心和半径
        Mat mat4=new Mat();
        src.copyTo(mat4);
        int channels=mat3.channels();
        cr=0;
        cx=0;
        cy=0;
        float[]data=new float[channels*mat3.cols()];
        for (int i=0;i<mat3.rows();i++){
            mat3.get(i,0,data);
            for (int j=0;j<data.length;j++){
                if (data[j]>cr){
                    cr=data[j];
                    cx=j;
                    cy=i;
                }
            }
        }
        //绘制内切圆
        Imgproc.circle(mat4,new Point(cx,cy),(int) cr,new Scalar(0,0,0),1);
        /*imageView4=findViewById(R.id.image_test4);
        Utils.matToBitmap(mat4,bitmap4);
        imageView4.setImageBitmap(bitmap4);*/
        //手掌手腕分割
        Mat mat5=new Mat();
        src.copyTo(mat5);
        int chann=src.channels();
        int wi=src.cols();
        int hi=src.rows();
        byte[]data3=new byte[chann*wi];
        for (int i=hi/2;i<hi;i++){
            if (i>cy+cr){
                mat5.get(i,0,data3);
                for (int j=0;j<data3.length;j++){
                    data3[j]=(byte)0;
                }
                mat5.put(i,0,data3);
            }
        }
        mat3.release();
        mat4.release();
        return  mat5;
    }
    //计算质心
    public void Calculate(Mat src){ //计算中心距和七个不变矩
        x=0;
        y=0;
        double params=0;
        double m00,m10,m01;
        Imgproc.matchShapes(src,src,2,params);
        Moments mo;
        Mat hu=new Mat();
        mo=Imgproc.moments(src,true);
        m00=mo.m00;
        m01=mo.m01;
        m10=mo.m10;
        x=m10/m00;
        y=m01/m00;
        /*Imgproc.HuMoments(mo,hu);
        Log.i("huhuhu", "hu:"+hu.toString());
        int chan=hu.channels();
        int w=hu.cols();
        int h=hu.rows();
        double[] data=new double[chan*w*h];
        hu.get(0,0,data);
        for (int j=0;j<data.length&&j<=6;j++){
            Log.i("huhuhu", "huju:"+data[j]);
        }*/
    }
    //计算手指个数（凸点）
    public Mat  findHull(Mat src){
        //寻找凸包点
        Mat hierarchy=new Mat();
        int width=src.cols();
        int height=src.rows();
        List<MatOfPoint> contours=new ArrayList<>();
        Point center=new Point();
        float[] radius={0};

        Imgproc.findContours(src,contours,hierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE,new Point(0,0));
        MatOfPoint contour=contours.get(0);
        MatOfPoint2f point2f=new MatOfPoint2f(contour.toArray());
        Imgproc.minEnclosingCircle(point2f,center,radius);

        ArrayList<Point>  convexHullPointArrayList = new ArrayList<Point>();//初始凸点集
        ArrayList<Point>  convexHullPoint=new ArrayList<>();//筛除相邻凸点集
        ArrayList<Point>  convexHull=new ArrayList<>();//手指凸点集
        MatOfInt  convexHullMatOfInt = new MatOfInt();
        Imgproc.convexHull( contour, convexHullMatOfInt, false);

        for(int j=0; j < convexHullMatOfInt.toList().size(); j++){
            convexHullPointArrayList.add(contour.toList().get(convexHullMatOfInt.toList().get(j)));
        }

        convexHullPoint.add(convexHullPointArrayList.get(0));
        for(int j=0;j<convexHullPointArrayList.size()-1;j++){
            double  x1=convexHullPointArrayList.get(j).x;
            double  y1=convexHullPointArrayList.get(j).y;
            double  x2=convexHullPointArrayList.get(j+1).x;
            double  y2=convexHullPointArrayList.get(j+1).y;
            if(Math.abs(x2-x1)>20||Math.abs(y2-y1)>20){
                convexHullPoint.add(convexHullPointArrayList.get(j+1));
            }
        }

        for(int j=0;j<convexHullPoint.size();j++){
            double distancex=convexHullPoint.get(j).x-cx;
            double distancey=convexHullPoint.get(j).y-cy;
            double distance=cr/3*4;
            if(distancex*distancex+distancey*distancey>distance*distance&&convexHullPoint.get(j).y-y<0){
                convexHull.add(convexHullPoint.get(j));
            }
        }

        int convexHullcount=convexHull.size();
        if(convexHullcount>1){
            double  x1=convexHull.get(0).x;
            double  y1=convexHull.get(0).y;
            double  x2=convexHull.get(convexHullcount-1).x;
            double  y2=convexHull.get(convexHullcount-1).y;
            if(Math.abs(x2-x1)<20||Math.abs(y2-y1)<20){
                convexHull.remove(convexHullcount-1);
            }
        }

        Mat dst=new Mat();
        dst.create(height,width,CvType.CV_8UC3);
        Imgproc.drawContours(dst,contours,0,new Scalar(255,255,255),1);
        for(int i=0;i<convexHull.size();i++) {//连接凸点质心线
            Imgproc.line(dst,new Point(convexHull.get(i).x,convexHull.get(i).y),new Point(x,y),new Scalar(255,255,0),1);
        }
        for(int i=0;i<convexHull.size();i++) {//凸点
            Imgproc.circle(dst,new Point(convexHull.get(i).x,convexHull.get(i).y),2,new Scalar(255,0,0),2);
        }
        Imgproc.circle(dst,center,(int)radius[0],new Scalar(255,255,0),2);
        Imgproc.circle(dst,new Point(cx,cy),(int)cr,new Scalar(255,255,0),2);
        hullnum=convexHull.size();
        Log.i("result","手指个数：" +hullnum );
        return  dst;
    }
    //SVM预测手势
    public float predictd(Mat src){
        File mSvmModel;
        float result2=0;
        SVM mClassifier=SVM.create();
        try {
            // load cascade file from application resources
            InputStream is1 = getResources().openRawResource(R.raw.mysvm1);
            InputStream is2 = getResources().openRawResource(R.raw.mysvm2);
            InputStream is3 = getResources().openRawResource(R.raw.mysvm3);
            InputStream is4 = getResources().openRawResource(R.raw.mysvm45);
            InputStream is;
            File mnist_modelDir = getDir("mnist_model", Context.MODE_PRIVATE);
            mSvmModel = new File(mnist_modelDir, "mysvm.xml");
            FileOutputStream os = new FileOutputStream(mSvmModel);

            byte[] buffer = new byte[4096];
            int bytesRead;
            if(hullnum==1||hullnum==0)
                is=is1;
            else if(hullnum==2)
                is=is2;
            else if(hullnum==3)
                is=is3;
            else
                is=is4;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            mClassifier=SVM.load(mSvmModel.getAbsolutePath());
            mnist_modelDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
        HOGDescriptor hog = new HOGDescriptor(new Size(96, 128), new Size(96, 64), new Size(96, 32), new Size(48, 32), 4);
        MatOfFloat descriptor=new MatOfFloat();
        //计算src图像的hog特征，放入descriptor数组中
        hog.compute(src,descriptor);

        Mat testDescriptor = new Mat(1,descriptor.rows(),CvType. CV_32FC1);
        //将hog特征列向量转换为行向量
        for (int i = 0; i<descriptor.rows(); i++)
        {
            testDescriptor.put(0, i, descriptor.get(i,0));

        }
        result2=mClassifier.predict(testDescriptor);
        Log.i("result","识别结果：" +result2 );
        //Toast.makeText(this, "识别结果："+result, Toast.LENGTH_SHORT).show();
        return  result2;
    }
    //保存图片至相册
    public void saveBitmap(Bitmap bitmap, String bitName){
        String fileName ;
        File file ;
        if(Build.BRAND .equals("Xiaomi") ){ // 小米手机
            fileName = Environment.getExternalStorageDirectory().getPath()+"/DCIM/Camera/"+bitName ;
        }else{  // Meizu 、Oppo
            fileName = Environment.getExternalStorageDirectory().getPath()+"/DCIM/"+bitName ;
        }
        file = new File(fileName);

        if(file.exists()){
            file.delete();
        }
        FileOutputStream out;
        try{
            out = new FileOutputStream(file);
            // 格式为 JPEG，照相机拍出的图片为JPEG格式的，PNG格式的不能显示在相册中
            if(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out))
            {
                out.flush();
                out.close();
// 插入图库
                MediaStore.Images.Media.insertImage(this.getContentResolver(), file.getAbsolutePath(), bitName, null);

            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();

        }
        // 发送广播，通知刷新图库的显示
        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + fileName)));
    }
    //设置问题和答案
    private String proQuestion(){
        int num1,num2;
        String symbol;
        num1=produceRandom();
        symbol=randomSymbol();
        num2=produceRandom();
        String question=num1+""+symbol+num2+""+"=?";
        switch (symbol){
            case "+":result=num1+num2;break;
            case "-":result=num1-num2;break;
            case "x":result=num1*num2;break;
            default:break;
        }
        return question;
    }
    //产生随机数
    private int produceRandom(){
        Random random=new Random();
        int num=random.nextInt(9)%(9) + 1;
        return num;
    }
    //产生随机符号
    private String randomSymbol(){
        Random random=new Random();
        String symbol="";
        int num=random.nextInt(3)%(3) + 1;
        switch (num){
            case 1:symbol="+";break;
            case 2:symbol="-";break;
            case 3:symbol="x";break;
            default:break;
        }
        return symbol;
    }
}

