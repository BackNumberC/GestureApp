package com.example.hp.gestureapp;
import android.app.Application;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.lang.reflect.Field;

public class HomeActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button startCount=findViewById(R.id.count);
        startCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(HomeActivity.this, CountActivity.class);
                startActivity(intent);
            }
        });
        Button startCall=findViewById(R.id.call);
        startCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(HomeActivity.this,CallActivity.class);
                startActivity(intent);
            }
        });
        Button explainGame=findViewById(R.id.explain_game);
        explainGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(HomeActivity.this,ExplainActivity.class);
                startActivity(intent);
            }
        });
    }
}
