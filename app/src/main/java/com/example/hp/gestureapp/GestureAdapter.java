package com.example.hp.gestureapp;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class GestureAdapter extends RecyclerView.Adapter<GestureAdapter.ViewHolder>{
    private static final String TAG = "GestureAdapter";
    private List<Gesture> mGestureList;
    static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView gestureImage;
        TextView gestureName;
        View gestureView;
        public ViewHolder(View view){
            super(view);
            gestureView=view;
            gestureImage=view.findViewById(R.id.gesture_image);
            gestureName=view.findViewById(R.id.gesture_name);
        }
    }
    public GestureAdapter(List<Gesture> gestureList){
        mGestureList=gestureList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.gesture_item,parent,false);
        final ViewHolder viewHolder=new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Gesture fruit=mGestureList.get(position);
        holder.gestureImage.setImageResource(fruit.getImageId());
        holder.gestureName.setText(fruit.getName());
    }

    @Override
    public int getItemCount() {
        return mGestureList.size();
    }
}
