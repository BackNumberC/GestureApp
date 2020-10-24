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
import android.widget.Button;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CallActivity extends AppCompatActivity implements SurfaceHolder.Callback,Camera.PreviewCallback {
    public static int camerachoose = 0;
    public Bitmap testimg;
    public Bitmap photo;
    public Bitmap end;
    public Bitmap start;
    public Bitmap middle;
    public Matrix matrix;
    public HandTask handTask;
    public LoadHandler mLoadhandler;
    public Recognition recognition;

    private Camera mCamera;
    public int answer=0;
    public int result=0;
    public String stringanswer=" ";
    public Camera.Parameters parameters;
    public Camera.Size size;
    public YuvImage image;
    public ByteArrayOutputStream os;
    //public Camera.PreviewCallback mPreviewCallback;
    private SurfaceHolder mHolder;
    private SurfaceView mPreview;
    private static final String TAG = "CallActivity";
    private TextView textView,recognize_result;
    public int time=0;
    public String phonenumber="";
    public Button callbutton;
    public Button deletebutton;
    public BlockingQueue<Integer> queue;
    public HashMap<Integer,Integer> map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        setContentView(R.layout.activity_call);

        mLoadhandler = new LoadHandler();
        textView=findViewById(R.id.random);
        recognize_result=findViewById(R.id.recognize_result);
        queue = new LinkedBlockingQueue<>(10);
        map = new HashMap<Integer,Integer>();
        callbutton=findViewById(R.id.recognize_call);

        deletebutton=findViewById(R.id.recognize_delete);
        recognition = new Recognition();

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
        callbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                call(Intent.ACTION_DIAL);
                queue.clear();
                map.clear();
                phonenumber="";
            }
        });
        deletebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                queue.clear();
                map.clear();
                phonenumber = phonenumber.substring(0,phonenumber.length()-1);
            }
        });

        Thread phoneThread=new Thread(new PhoneThread());
        phoneThread.start();
        Thread handThread=new Thread(new HandThread());
        handThread.start();
        Thread clearTheead=new Thread(new ClearThread());
        clearTheead.start();
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
            end = Bitmap.createScaledBitmap(middle, 240, 320, true); //创建新的图像大小
            //将end图像压缩
            end.compress(Bitmap.CompressFormat.PNG, 70, os);
            long middleTime = System.currentTimeMillis(); // 获取中间时间
            Log.i("wsy","代码运行时间： " + (middleTime - startTime) + "ms");
            Mat deal=new Mat();
            Utils.bitmapToMat(end,deal);

            //肤色分割
            Mat dst = recognition.skinsplit(deal);
            //去除手腕
            dst=recognition.wrist(dst);
            //计算质心坐标
            recognition.Calculate(dst);
            //计算手指个数
            recognition.findHull(dst);
            //识别数字手势
            result=(int)predictd(dst);

            deal.release();
            long endTime = System.currentTimeMillis(); // 获取结束时间
            Log.i("wsy","代码运行时间： " + (endTime - middleTime) + "ms");
            if(queue.offer(result)){
                //Log.i("wsy","已插入" );
                if(map.containsKey(result)){
                    int temp = map.get(result);
                    map.put(result,++temp);
                }else{
                    map.put(result,1);
                }
            }else{
                int max=0;
                Iterator<Map.Entry<Integer, Integer>> iterator = map.entrySet().iterator();
                while(iterator.hasNext()){
                    Map.Entry<Integer, Integer> entry = iterator.next();
                    Integer value = entry.getValue();
                    if(value>max){
                        max=value;
                        answer=entry.getKey();
                        Log.i("wsy","暂时结果" +answer);
                    }
                }
                stringanswer = Integer.toString(answer);
            }
            long sysTime = System.currentTimeMillis();//获取系统时间
            CharSequence sysTimeStr = DateFormat.format("hh:mm:ss:mm", sysTime);//时间显示格式
            Log.i("CVCamera", "已获取"+sysTimeStr);
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
                        mCamera.setOneShotPreviewCallback(CallActivity.this);
                    }
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    //线程PhoneThread执行的任务
    public class LoadHandler extends Handler{
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            textView.setText(phonenumber);
            if(msg.obj.equals("10"))
                msg.obj = "无";
            recognize_result.setText("识别结果："+msg.obj);
        }
    }
    //线程PhoneThread（用于显示识别结果）
    class PhoneThread implements  Runnable{
        public void run(){
            while (!Thread.currentThread().isInterrupted()){
                try {
                    Message message = Message.obtain();
                    message.obj =stringanswer;;
                    //message.what=phonenumber;
                    mLoadhandler.sendMessage(message);
                    Thread.sleep(500);
                }catch (InterruptedException e){
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    //线程ClearThread(用于清空确认队列）
    class ClearThread implements Runnable{
        public void run(){
            while (!Thread.currentThread().isInterrupted()){
                try {
                   if(queue.remainingCapacity()==0){
                       if(!stringanswer.equals("10"))
                           phonenumber+=stringanswer;
                       stringanswer ="";
                       queue.clear();
                       map.clear();
                   }
                   Thread.sleep(2500);
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
            camera=Camera.open(camerachoose);
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


    //SVM检测手势
    public float dectored(Mat src){
        float label=0;
        SVM mClassifier=SVM.create();
        try {
            InputStream is = this.getResources().openRawResource(R.raw.hogsvm);
            File svm_modelDir = getDir("svm_model", Context.MODE_PRIVATE);
            File mSvmModel = new File(svm_modelDir, "svm.xml");
            FileOutputStream os = new FileOutputStream(mSvmModel);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            mClassifier=SVM.load(mSvmModel.getAbsolutePath());
            svm_modelDir.delete();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
        HOGDescriptor hog = new HOGDescriptor(new Size(96, 128), new Size(96, 64), new Size(96, 32), new Size(48, 32), 4);
        MatOfFloat descriptor=new MatOfFloat();

        Mat hierarchy1=new Mat();
        List<MatOfPoint> contours1=new ArrayList<MatOfPoint>();
        Imgproc.findContours(src,contours1,hierarchy1, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE,new Point(0,0));
        Mat imgRectROI;
        try{
            MatOfPoint contour=contours1.get(0);
            org.opencv.core.Rect R= Imgproc.boundingRect(contour);
            imgRectROI= new Mat(src, R);
        }catch (Exception e){
            imgRectROI = src.clone();
        }
        Imgproc.resize(imgRectROI,imgRectROI,new Size(96,128));

        //计算src图像的hog特征，放入descriptor数组中
        hog.compute(imgRectROI,descriptor);
        Mat testDescriptor = new Mat(1,descriptor.rows(),CvType. CV_32FC1);
        //将hog特征列向量转换为行向量
        for (int i = 0; i<descriptor.rows(); i++) {
            testDescriptor.put(0, i, descriptor.get(i,0));
        }
        label=mClassifier.predict(testDescriptor);
        Log.i("result","识别结果：" +label );
        //Toast.makeText(this, "识别结果："+result, Toast.LENGTH_SHORT).show();
        return  label;
    }
    //SVM预测手势
    public float predictd(Mat src){
        float label=0;
        SVM mClassifier=SVM.create();
        try {
            // load cascade file from application resources
            InputStream is1 = getResources().openRawResource(R.raw.svm1);
            InputStream is2 = getResources().openRawResource(R.raw.svm2);
            InputStream is3 = getResources().openRawResource(R.raw.svm3);
            InputStream is;
            File mnist_modelDir = getDir("mnist_model", Context.MODE_PRIVATE);
            File mSvmModel = new File(mnist_modelDir, "mysvm.xml");
            FileOutputStream os = new FileOutputStream(mSvmModel);

            byte[] buffer = new byte[4096];
            int bytesRead;
            if(recognition.hullnum==1||recognition.hullnum==0)
                is=is1;
            else if(recognition.hullnum==2)
                is=is2;
            else if(recognition.hullnum==3)
                is=is3;
            else if(recognition.hullnum==4)
                return label=4;
            else
                return label=5;
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

        Mat hierarchy1=new Mat();
        List<MatOfPoint> contours1=new ArrayList<MatOfPoint>();
        Imgproc.findContours(src,contours1,hierarchy1, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE,new Point(0,0));
        Mat imgRectROI;
        try{
            MatOfPoint contour=contours1.get(0);
            org.opencv.core.Rect R= Imgproc.boundingRect(contour);
            imgRectROI= new Mat(src, R);
        }catch (Exception e){
            imgRectROI = src.clone();
        }
        Imgproc.resize(imgRectROI,imgRectROI,new Size(96,128));

        //计算src图像的hog特征，放入descriptor数组中
        hog.compute(imgRectROI,descriptor);
        Mat testDescriptor = new Mat(1,descriptor.rows(),CvType. CV_32FC1);
        //将hog特征列向量转换为行向量
        for (int i = 0; i<descriptor.rows(); i++)
        {
            testDescriptor.put(0, i, descriptor.get(i,0));

        }
        label=mClassifier.predict(testDescriptor);
        Log.i("result","识别结果：" +label );
        //Toast.makeText(this, "识别结果："+result, Toast.LENGTH_SHORT).show();
        return  label;
    }
    //拨打电话
    private void call(String action){
        //phonenumber = Integer.toString(phonenumber);
        if(phonenumber!=null&&phonenumber.trim().length()>0){
            //这里"tel:"+电话号码 是固定格式，系统一看是以"tel:"开头的，就知道后面应该是电话号码。
            Intent intent = new Intent(action, Uri.parse("tel:" + phonenumber.trim()));
            startActivity(intent);//调用上面这个intent实现拨号
        }else{
            Toast.makeText(this, "电话号码不能为空", Toast.LENGTH_LONG).show();
        }
    }
}

