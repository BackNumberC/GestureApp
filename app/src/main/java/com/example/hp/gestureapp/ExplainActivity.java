package com.example.hp.gestureapp;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

public class ExplainActivity extends AppCompatActivity{
    private List<Gesture> gestureList=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        setContentView(R.layout.activity_explain);
        /*
        initGesture();
        RecyclerView recyclerView=findViewById(R.id.recycler_view);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        GestureAdapter adapter=new GestureAdapter(gestureList);
        recyclerView.setAdapter(adapter);
        */
    }

    private void initGesture(){
        Gesture gesture0=new Gesture("gesture0",R.drawable.gesture0);
        Gesture gesture1=new Gesture("gesture1",R.drawable.gesture1);
        Gesture gesture2=new Gesture("gesture2",R.drawable.gesture2);
        Gesture gesture3=new Gesture("gesture3",R.drawable.gesture3);
        Gesture gesture4=new Gesture("gesture4",R.drawable.gesture4);
        Gesture gesture5=new Gesture("gesture5",R.drawable.gesture5);
        Gesture gesture6=new Gesture("gesture6",R.drawable.gesture6);
        Gesture gesture7=new Gesture("gesture7",R.drawable.gesture7);
        Gesture gesture8=new Gesture("gesture8",R.drawable.gesture8);
        Gesture gesture9=new Gesture("gesture9",R.drawable.gesture_nine);
        gestureList.add(gesture0);
        gestureList.add(gesture1);
        gestureList.add(gesture2);
        gestureList.add(gesture3);
        gestureList.add(gesture4);
        gestureList.add(gesture5);
        gestureList.add(gesture6);
        gestureList.add(gesture7);
        gestureList.add(gesture8);
        gestureList.add(gesture9);
    }
}
