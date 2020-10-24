package com.example.hp.gestureapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;

public class CustomButton extends android.support.v7.widget.AppCompatButton {

    String tag;
    String text;

    public CustomButton(Context context) {
        super(context);
    }

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        //获取自定义的属性
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.test);
        tag = ta.getString(R.styleable.test_tag);
        text= ta.getString(R.styleable.test_text);
        ta.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制第一行文字
        Paint paint = new Paint();
        paint.setTextSize(65);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setColor(Color.WHITE);
        float tagWidth = paint.measureText(tag);
        int x = 80;
        int y = this.getHeight()/3;
        canvas.drawText(tag, x, y, paint);
        //绘制第二行文字
        Paint paint1 = new Paint();
        paint1.setTextSize(40);
        paint1.setColor(Color.WHITE);
        float numWidth = paint.measureText(text );
        /*int x1 = (int) (this.getWidth() - numWidth)/2;*/
        int x1 =80;
        int y1 = this.getHeight()/2 + 80;
        canvas.drawText(text, x1, y1, paint1);
//        canvas.translate(0,(this.getMeasuredHeight()/2) - (int) this.getTextSize());
    }
}
