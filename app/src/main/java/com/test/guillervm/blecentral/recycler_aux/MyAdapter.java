package com.test.guillervm.blecentral.recycler_aux;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.test.guillervm.blecentral.R;
import com.test.guillervm.blecentral.core.Peripheral;

import java.util.ArrayList;

/**
 * Created by guillervm on 14/4/15.
 */
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private ArrayList<Peripheral> dataset;
    private Context context;

    // Provide a reference to the views for each data item.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView deviceName;
        public TextView deviceAddress;
        public ImageView iconRight;

        public ViewHolder(View v) {
            super(v);
            deviceName = (TextView)v.findViewById(R.id.device_name);
            deviceAddress = (TextView)v.findViewById(R.id.device_address);
            iconRight = (ImageView)v.findViewById(R.id.icon_right);
        }
    }

    // Constructor.
    public MyAdapter(ArrayList<Peripheral> dataset, Context context) {
        this.dataset = dataset;
        this.context = context;
    }

    // Create new views (invoked by the layout manager).
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.my_list_item_view, parent, false);

        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager).
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Update interface.
        if (dataset.get(position).getDevice().getName() != null && dataset.get(position).getDevice().getName().length() > 0) {
            holder.deviceName.setText(dataset.get(position).getDevice().getName());
        } else {
            holder.deviceName.setText(dataset.get(position).getDevice().getAddress());
        }
        holder.deviceAddress.setText(dataset.get(position).getDistanceString());
        if (dataset.get(position).isAvailable() > 0) {
            holder.iconRight.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_available_green));
        } else if (dataset.get(position).isAvailable() < 0) {
            holder.iconRight.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_available_red));
        } else {
            holder.iconRight.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_available_grey));
        }
    }

    // Return the size the dataset (invoked by the layout manager).
    @Override
    public int getItemCount() {
        return dataset.size();
    }
}