package com.example.myapplication.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.myapplication.R;

import java.util.List;

public class MeasureDataAdapter extends RecyclerView.Adapter<MeasureDataAdapter.VH> {
    private List<String> mDatas;

    public MeasureDataAdapter(List<String> data){
        this.mDatas=data;
    }

    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_measure_data,viewGroup,false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH viewHolder, final int position) {
        viewHolder.measureData.setText("第" + (position+1)+ "次：" + mDatas.get(position));
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击事件
                mDatas.remove(position);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    public static class VH extends RecyclerView.ViewHolder{
        public final TextView measureData;
        public VH(View v){
            super(v);
            measureData=v.findViewById(R.id.measure_data);
        }
    }
}
