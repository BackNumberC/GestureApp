package com.example.hp.gestureapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
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
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.SVM;
import org.opencv.objdetect.HOGDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.File;
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

public class CountActivity extends AppCompatActivity implements SurfaceHolder.Callback,Camera.PreviewCallback {
    public static int camerachoose=0;
    public Bitmap testimg;
    public Bitmap photo;
    public Bitmap end;
    public Bitmap start;
    public Bitmap middle;
    public Matrix matrix;
    public HandTask handTask;
    public LoadHandler mLoadhandler;

    private Camera mCamera;
    public int result=11;
    public int answer;
    public String stringanswer=" ";
    public int preanswer;
    public Camera.Parameters parameters;
    public Camera.Size size;
    public YuvImage image;
    public ByteArrayOutputStream os;
    private SurfaceHolder mHolder;
    private SurfaceView mPreview;
    private static final String TAG = "CountActivity";
    private TextView textView,recognize_result,answer_right;
    public int time=0;
    public String question;
    public Recognition recognition;
    public BlockingQueue<Integer> queue;
    public HashMap<Integer,Integer> map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        setContentView(R.layout.activity_count);

        mLoadhandler = new LoadHandler();
        textView=findViewById(R.id.random);
        recognize_result=findViewById(R.id.recognize_result);
        answer_right=findViewById(R.id.answer_right);
        recognition =new Recognition();
        queue = new LinkedBlockingQueue<>(10);
        map = new HashMap<Integer,Integer>();
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
            if(!image.compressToJpeg(new Rect(0, 0, size.width, size.height), 50, os)){
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
            end.compress(Bitmap.CompressFormat.JPEG, 30, os);
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
            preanswer=(int)predictd(dst);

            deal.release();
            long endTime = System.currentTimeMillis(); // 获取结束时间
            Log.i("wsy", "代码运行时间： " + (endTime - middleTime) + "ms");
            if (queue.offer(preanswer)) {
                //Log.i("wsy","已插入" );
                if (map.containsKey(preanswer)) {
                    int temp = map.get(preanswer);
                    map.put(preanswer, ++temp);
                } else {
                    map.put(preanswer, 1);
                }
                if (queue.remainingCapacity() == 0){
                    int max = 0;
                    Iterator<Map.Entry<Integer, Integer>> iterator = map.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<Integer, Integer> entry = iterator.next();
                        Integer value = entry.getValue();
                        if (value > max) {
                            max = value;
                            answer = entry.getKey();
                        }
                    }
                    stringanswer = Integer.toString(answer);
                }
            }
            Log.i("wsy", "识别结果" + answer);
            long sysTime = System.currentTimeMillis();//获取系统时间
            CharSequence sysTimeStr = DateFormat.format("hh:mm:ss:mm", sysTime);//时间显示格式
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
                        mCamera.setOneShotPreviewCallback(CountActivity.this);
                        //Log.i(TAG, "setOneShotPreview...");
                    }
                    Thread.sleep(400);
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
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            textView.setText(question);
            answer_right.setText("");
            if(msg.obj.equals("10"))
                msg.obj = "无";
            recognize_result.setText("识别结果："+msg.obj);
            if(msg.what==result){
                answer_right.setText("正确");
                question="";
                result=11;
                while (result>=10 || result<0){
                    question=proQuestion();
                }
                stringanswer=" ";
                queue.clear();
                map.clear();
            }else if((String)msg.obj==" "){
                answer_right.setText(" ");
            }else{
                answer_right.setText("错误");
                if(queue.remainingCapacity()==0){
                    queue.clear();
                    map.clear();
                    stringanswer=" ";
                }
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
                    message.obj = stringanswer;
                    message.what = answer;
                    mLoadhandler.sendMessage(message);
                    Thread.sleep(500);
                }catch (InterruptedException e){
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    //设置摄像头参数
    private Camera getCamera() {
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

    //SVM检测手势
    public float dectored(Mat src){
        float label=0;
        SVM mClassifier=SVM.create();
        try {
            // load cascade file from application resources
            InputStream is = this.getResources().openRawResource(R.raw.hogsvm);
            File mnist_modelDir = getDir("mnist_model", Context.MODE_PRIVATE);
            File mSvmModel = new File(mnist_modelDir, "mysvm.xml");
            FileOutputStream os = new FileOutputStream(mSvmModel);
            byte[] buffer = new byte[4096];
            int bytesRead;
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
        /*Bitmap end  = Bitmap.createBitmap(imgRectROI.width(), imgRectROI.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgRectROI,end);
        saveBitmap(end,Integer.toString(count++)+".jpg");*/
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
            File svm_modelDir = getDir("svm_model", Context.MODE_PRIVATE);
            File mSvmModel = new File(svm_modelDir, "svm.xml");
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
        for (int i = 0; i<descriptor.rows(); i++)
        {
            testDescriptor.put(0, i, descriptor.get(i,0));
        }
        label=mClassifier.predict(testDescriptor);
        Log.i("result","识别结果：" +label );
        //Toast.makeText(this, "识别结果："+result, Toast.LENGTH_SHORT).show();
        return  label;
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

