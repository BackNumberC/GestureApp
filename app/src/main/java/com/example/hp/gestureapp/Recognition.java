package com.example.hp.gestureapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

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
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.TermCriteria.EPS;

public class Recognition extends AppCompatActivity {
    public int hullnum;
    public float cr;
    public int cx;
    public int cy;
    public double x=0,y=0;
    private static final String TAG = "Recognition";

    //处理预测算法汇总
    public Mat skinsplit(Mat src) {
        Mat Dst=new Mat();
        Dst.create(src.size(),src.type());
        //均值漂移滤波
        Imgproc.pyrMeanShiftFiltering(src,src,5,30);
        //将src转化为YCrCb
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2YCrCb);
        List<Mat> mv=new ArrayList<>();
        Core.split(src,mv);
        Mat dst=new Mat();
        //对Cr量进行OTSU阈值分割
        Imgproc.threshold(mv.get(1),dst,127,255, Imgproc.THRESH_BINARY| Imgproc.THRESH_OTSU);
        //膨胀腐蚀操作
        Mat k= Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(5,5),new Point(-1,-1));
        Imgproc.morphologyEx(dst,dst, Imgproc.MORPH_ERODE,k);
        Imgproc.morphologyEx(dst,dst, Imgproc.MORPH_DILATE,k);
        //获取轮廓
        Mat hierarchy=new Mat();
        List<MatOfPoint> contours=new ArrayList<MatOfPoint>();
        Imgproc.findContours(dst,contours,hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE,new Point(0,0));

        for(int i=0;i<contours.size();i++)
        {
            double area= Imgproc.contourArea(contours.get(i));
            if(area>3000)
            {
                Log.i("wsy","轮廓面积： " + area);
                Imgproc.drawContours(Dst,contours,i,new Scalar(255,255,255),-1);
            }
        }
        Imgproc.cvtColor(Dst,Dst,Imgproc.COLOR_BGR2GRAY);
        //释放所使用的dst
        dst.release();
        src.release();
        return Dst;
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
    //肤色聚类
    public List<Mat> kmeans(Mat src){
        Mat copy = src.clone();
        Mat reshaped_image = copy.reshape(1, src.cols()*src.rows());
        Mat reshaped_image32f = new Mat();
        reshaped_image.convertTo(reshaped_image32f, CvType.CV_32F, 1.0 / 255.0);

        Mat centers = new Mat();
        Mat labels = new Mat();
        TermCriteria criteria = new TermCriteria(EPS,300,0.1);
        Core.kmeans(reshaped_image32f,3,labels,criteria,5, Core.KMEANS_PP_CENTERS,centers);

        List<Mat> result=new ArrayList<>();
        List<Mat> Result=new ArrayList<>();
        //Mat Result = new Mat();
        for(int i=0;i<3;i++){
            Mat c = new Mat(src.size(),CvType.CV_8UC1);
            result.add(c);
        }

        int height = src.height();
        int width = src.width();
        int channels = src.channels();
        int index ;
        byte[] data = new byte[channels];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                index = row*width + col;
                double[] label =labels.get(index,0);
                //Log.e("area","标签 " +label[0]);
                if(label[0]==0){
                    data[0] = (byte)255;
                    data[1] = (byte)255;
                    data[2] = (byte)255;
                    result.get(0).put(row,col,data);
                }else if(label[0]==1){
                    data[0] = (byte)255;
                    data[1] = (byte)255;
                    data[2] = (byte)255;
                    result.get(1).put(row,col,data);
                }else{
                    data[0] = (byte)255;
                    data[1] = (byte)255;
                    data[2] = (byte)255;
                    result.get(2).put(row,col,data);
                }
            }
        }
        for(int i=0;i<3;i++){
            Mat temp = result.get(i);
            //膨胀腐蚀操作
            Mat k= Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(5,5),new Point(-1,-1));
            Imgproc.morphologyEx(temp,temp, Imgproc.MORPH_ERODE,k);
            Imgproc.morphologyEx(temp,temp, Imgproc.MORPH_DILATE,k);
            //获取轮廓
            double Maxarea=0;
            int number=0;
            Mat hierarchy=new Mat();
            List<MatOfPoint> contours=new ArrayList<MatOfPoint>();
            Imgproc.findContours(temp,contours,hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE,new Point(0,0));
            Log.i("wsy","轮廓： " + contours.size()+ "个");
            for(int j=0;j<contours.size();j++){
                double area= Imgproc.contourArea(contours.get(j));
                if(area>Maxarea){
                    Maxarea=area;
                    number=j;
                }
            }
            //绘制轮廓
            Mat dst = new Mat(result.get(i).size(),result.get(i).type());
            Imgproc.drawContours(dst,contours,number,new Scalar(255,255,255),-1);
            Result.add(dst);
        }
        return Result;
    }
    //计算手指个数（凸点）
    public void findHull(Mat src){
        Mat hierarchy=new Mat();
        List<MatOfPoint> contours=new ArrayList<>();
        Point center=new Point();
        float[] radius={0};

        //找寻轮廓
        Imgproc.findContours(src,contours,hierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE,new Point(0,0));
        MatOfPoint contour=contours.get(0);
        MatOfPoint2f point2f=new MatOfPoint2f(contour.toArray());
        Imgproc.minEnclosingCircle(point2f,center,radius);

        //初始凸点集
        ArrayList<Point>  convexHullPointArrayList = new ArrayList<Point>();
        MatOfInt convexHullMatOfInt = new MatOfInt();
        Imgproc.convexHull( contour, convexHullMatOfInt, false);
        for(int j=0; j < convexHullMatOfInt.toList().size(); j++){
            convexHullPointArrayList.add(contour.toList().get(convexHullMatOfInt.toList().get(j)));
        }

        //筛除相邻凸点集
        ArrayList<Point>  convexHullPoint=new ArrayList<>();
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

        //手指凸点集
        ArrayList<Point>  convexHull=new ArrayList<>();
        //cx，cy为质心坐标
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

        //打印凸点手指图
        /*Mat dst=new Mat();
        dst.create(height,width, CvType.CV_8UC3);
        Imgproc.drawContours(dst,contours,0,new Scalar(255,255,255),1);
        for(int i=0;i<convexHull.size();i++) {//连接凸点质心线
            Imgproc.line(dst,new Point(convexHull.get(i).x,convexHull.get(i).y),new Point(x,y),new Scalar(255,255,0),1);
        }
        for(int i=0;i<convexHull.size();i++) {//凸点
            Imgproc.circle(dst,new Point(convexHull.get(i).x,convexHull.get(i).y),2,new Scalar(255,0,0),2);
        }
        Imgproc.circle(dst,center,(int)radius[0],new Scalar(255,255,0),2);
        Imgproc.circle(dst,new Point(cx,cy),(int)cr,new Scalar(255,255,0),2);*/

        hullnum=convexHull.size();
        Log.i("result","手指个数：" +hullnum );
    }
    //计算质心
    public void Calculate(Mat src){ //计算中心距和七个不变矩
        x=0;
        y=0;
        double params=0;
        double m00,m10,m01;
        Imgproc.matchShapes(src,src,2,params);
        Moments mo;
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
    //保存图片至相册
    public void saveBitmap(Bitmap bitmap, String bitName){
        String fileName ;
        File file ;
        if(Build.BRAND .equals("Xiaomi") )
        { // 小米手机
            fileName = Environment.getExternalStorageDirectory().getPath()+"/DCIM/Camera/"+bitName ;
        }
        else{  // Meizu 、Oppo
            fileName = Environment.getExternalStorageDirectory().getPath()+"/DCIM/"+bitName ;
        }
        file = new File(fileName);

        if(file.exists())
        {
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
}
