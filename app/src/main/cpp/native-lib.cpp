#include <jni.h>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <vector>
using namespace cv;
using namespace std;

extern "C" {

    JNIEXPORT  jintArray   JNICALL Java_com_example_hp_gestureapp_MainActivity_Bitmap2Grey(
            JNIEnv *env,jobject ,jintArray buf,jint w,jint h){
       jint *cbuf;
       jboolean ptfalse = false;
       cbuf=env->GetIntArrayElements(buf, &ptfalse);
       if(cbuf == NULL){
            return 0;
        }
        Mat imgData(h, w, CV_8UC4, (unsigned char*)cbuf);
        //注意，Android的Bitmap是ARGB四通道,而不是RGB三通道
        cvtColor(imgData,imgData,CV_BGRA2GRAY);
        cvtColor(imgData,imgData,CV_GRAY2BGRA);

        int size=w * h;
        jintArray result = env->NewIntArray(size);
        env->SetIntArrayRegion(result, 0, size, (jint*)imgData.data);
        env->ReleaseIntArrayElements(buf, cbuf, 0);
        return result;
    }
    JNIEXPORT  void  JNICALL Java_com_example_hp_gestureapp_MainActivity_blur(
                JNIEnv *,jobject ,jlong matAddrSrcImage, jlong matAddrDestImage) {
        Mat& srcImage  = *(Mat*)matAddrSrcImage;
        Mat& destImage = *(Mat*)matAddrDestImage;
        //cvtColor(srcImage,destImage,COLOR_RGB2GRAY);
        //cvtColor(destImage,destImage,CV_GRAY2BGRA);
        boxFilter(srcImage,destImage,-1,Size(5,5));
    }
     JNIEXPORT  jfloat JNICALL Java_com_example_hp_gestureapp_MainActivity_HogPredict(
                JNIEnv *env,jobject ,jintArray buf,jint w,jint h,jstring path){
         jint *cbuf;
         jboolean ptfalse = false;
         jfloat label=0;
         cbuf = env->GetIntArrayElements(buf, &ptfalse);
         if(cbuf == NULL){
                     return 0;
         }
         const char *nativeString = env->GetStringUTFChars(path,0);
         Mat test(h, w, CV_8UC4, (unsigned char*)cbuf);
         Size imageSize = Size(64, 64);
         //CvSVM *mySVM = new CvSVM();
         //mySVM.load(nativeString);
         env->ReleaseStringUTFChars(path,nativeString);

         resize(test, test, imageSize);
         vector<float> imageDescriptor;

         //HOGDescriptor myHog = HOGDescriptor(imageSize, Size(32, 32), cvSize(16, 16), cvSize(16, 16), 12);
         //myHog.compute(test.clone(),imageDescriptor,Size(1,1),Size(0,0));

         //Mat testDescriptor = Mat::zeros(1, imageDescriptor.size(), CV_32FC1);
         //for (size_t i = 0; i < imageDescriptor.size(); i++){
          			//testDescriptor.at<float>(0, i) = imageDescriptor[i];
         //}
         //jfloat  label = mySVM->predict(testDescriptor, false);
         return label;
      }
}


