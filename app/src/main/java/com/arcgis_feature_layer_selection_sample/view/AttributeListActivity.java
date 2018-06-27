package com.arcgis_feature_layer_selection_sample.view;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.arcgis_feature_layer_selection_sample.AppState;
import com.arcgis_feature_layer_selection_sample.R;
import com.esri.arcgisruntime.data.Feature;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class AttributeListActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attribute_list);
        
        List<Feature> list = new ArrayList<>();
        
        for (Feature feature : AppState.SelectedFeatures) {
            list.add(feature);
        }
        
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, layoutManager.getOrientation()));
        recyclerView.setAdapter(new MyAdapter(list));
    }
    
    class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        
        private List<Feature> list;
        
        MyAdapter (List<Feature> list) {
            this.list = list;
        }
        
        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
            return new MyViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder (@NonNull MyViewHolder holder, int position) {
            StringBuilder text = new StringBuilder();
            for (String key : list.get(position).getAttributes().keySet()) {
                Object value = list.get(position).getAttributes().get(key);
                if (value instanceof GregorianCalendar) {
                    text.append(key).append(": ").append(dateToDateDisplayString(new Date(((GregorianCalendar) value).getTimeInMillis()))).append("\n");
                } else {
                    text.append(key).append(": ").append(String.valueOf(value)).append("\n");
                }
            }
            holder.textView.setText(text.toString());
        }
        
        @Override
        public int getItemCount () {
            return list.size();
        }
        
        class MyViewHolder extends RecyclerView.ViewHolder {
            
            private TextView textView;
            
            MyViewHolder (View itemView) {
                super(itemView);
                
                textView = itemView.findViewById(R.id.textView);
            }
        }
    }
    
    public String dateToDateDisplayString(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(date);
    }
}
