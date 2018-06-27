package com.arcgis_feature_layer_selection_sample;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.arcgis_feature_layer_selection_sample.view.AttributeListActivity;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements MapSelectionView.IMapSelectionViewListener {
    
    private MapView mMapView;
    private ArcGISMap map;
    
    private MapSelectionView mapSelectionView;
    private GraphicsOverlay selectionOverlay;
    private Point initialMapPoint;
    private Graphic selectionGraphic;
    private FeatureLayer selectableLayer;
    private Button btnStart, btnResetSelection;
    private ProgressDialog dialog;
    private Button btnOpen;
    
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mMapView = findViewById(R.id.mapView);
        
        mapSelectionView = findViewById(R.id.map_activity_selection_view);
        mapSelectionView.listener = this;
        
        map = new ArcGISMap(Basemap.createStreets());
        
        mMapView.setMap(map);
        
        ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(getString(R.string.sample_service_url));
        
        FeatureLayer featureLayer = new FeatureLayer(serviceFeatureTable);
        
        // add the layer to the map
        map.getOperationalLayers().add(featureLayer);
        
        selectionOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(selectionOverlay);
        
        btnOpen = findViewById(R.id.btnOpen);
        btnOpen.setEnabled(false);
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                startActivity(new Intent(MainActivity.this, AttributeListActivity.class));
            }
        });
        
        
        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                mapSelectionView.setVisibility(View.VISIBLE);
                btnStart.setEnabled(false);
            }
        });
        
        btnResetSelection = findViewById(R.id.btnResetSelection);
        btnResetSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                if (selectableLayer != null) {
                    // Remove selectable layer from the map.
                    map.getOperationalLayers().remove(selectableLayer);
                    
                    // Clear the selectable layer.
                    selectableLayer = null;
                    
                    btnResetSelection.setVisibility(View.GONE);
                    btnStart.setVisibility(View.VISIBLE);
                    btnStart.setEnabled(true);
                    btnOpen.setEnabled(false);
                    
                }
            }
        });
        
        showInfoDialog();
    }
    
    private void showProgress () {
        dialog = new ProgressDialog(this);
        dialog.setMessage("Selecting Features");
        dialog.setCancelable(false);
        dialog.show();
    }
    
    private void dismissProgress () {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_info:
                showInfoDialog();
                break;
            
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showInfoDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("How to use");
        builder.setMessage("1. I have used the from your ArcGIS sample.\n" +
                "2. Please zoom-in to the shown layer on the map.\n" +
                "3. Click on Start Selection to enable selection of features.\n" +
                "4. Now move your finger on map to draw a box over the features you want to select.\n" +
                "5. When you release your finger it will start selecting the layers and after completion you can see blue boundary around selected features.\n" +
                "6. Also In the Logcat you can see, it will print all the available attributes along with there values from the selected features.\n" +
                "7. You can click on Reset Selection button to select again.\n" +
                "8. After selection, when you click on Open button it will open new screen which shows all the attributes with there values.\n" +
                "9. Here you can see most of the attributes has null values.\n" +
                "10. Please try this scenario multiple times by reselection of features and even by reopen the application because some time it shows data and some time it doesn't.\n");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
    
    @Override
    protected void onPause () {
        super.onPause();
        mMapView.pause();
    }
    
    @Override
    protected void onResume () {
        super.onResume();
        mMapView.resume();
    }
    
    @Override
    protected void onDestroy () {
        super.onDestroy();
        mMapView.dispose();
    }
    
    @Override
    public void startSelectAtPoint (float x, float y) {
        if (selectionOverlay != null) {
            android.graphics.Point screenPoint = new android.graphics.Point(Math.round(x), Math.round(y));
            initialMapPoint = mMapView.screenToLocation(screenPoint);
            
            // Create symbol for selection layer
            SimpleFillSymbol innerSymbol = new SimpleFillSymbol();
            innerSymbol.setColor(ContextCompat.getColor(this, R.color.map_selection_background));
            SimpleLineSymbol lineSymbol = new SimpleLineSymbol();
            lineSymbol.setColor(ContextCompat.getColor(this, R.color.map_selection_outline));
            innerSymbol.setOutline(lineSymbol);
            
            Envelope envelope = new Envelope(initialMapPoint.getX(), initialMapPoint.getY(), initialMapPoint.getX(), initialMapPoint.getY(), mMapView.getSpatialReference());
            
            selectionGraphic = new Graphic(envelope, innerSymbol);
            
            selectionOverlay.getGraphics().add(selectionGraphic);
        }
    }
    
    @Override
    public void moveSelectToPoint (float x, float y) {
        if (selectionOverlay != null) {
            android.graphics.Point screenPoint = new android.graphics.Point(Math.round(x), Math.round(y));
            Point mapPoint = mMapView.screenToLocation(screenPoint);
            
            Envelope envelope = new Envelope(initialMapPoint.getX(), initialMapPoint.getY(), mapPoint.getX(), mapPoint.getY(), mMapView.getSpatialReference());
            
            selectionGraphic.setGeometry(envelope);
        }
    }
    
    @Override
    public void endSelectAtPoint (float x, float y) {
        if (selectionOverlay != null) {
            
            // Clear the selection layer.
            selectionOverlay.getGraphics().clear();
            
            mapSelectionView.setVisibility(View.INVISIBLE);
            
            executeSelection();
            
            btnStart.setVisibility(View.GONE);
            btnResetSelection.setVisibility(View.VISIBLE);
            
            showProgress();
        }
    }
    
    private void executeSelection () {
        Geometry geometry = GeometryEngine.simplify(selectionGraphic.getGeometry());
        QueryParameters query = new QueryParameters();
        query.setGeometry(geometry);
        
        ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(getString(R.string.sample_service_url));
        
        selectableLayer = new FeatureLayer(serviceFeatureTable);
        selectableLayer.setSelectionWidth(4);
        
        map.getOperationalLayers().add(selectableLayer);
        
        final ListenableFuture<FeatureQueryResult> selectFuture = serviceFeatureTable.queryFeaturesAsync(query, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL);
        
        selectFuture.addDoneListener(new Runnable() {
            @Override
            public void run () {
                try {
                    FeatureQueryResult features = selectFuture.get();
                    selectableLayer.selectFeatures(features);
                    
                    selectFeatures(features);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    
                }
            }
        });
    }
    
    private void selectFeatures (FeatureQueryResult features) {
        if (features != null) {
            // Save the raw selected features to the app state.
            AppState.SelectedFeatures = features;
            
            for (Feature feature : features) {
                for (String key : feature.getAttributes().keySet()) {
                    Object value = feature.getAttributes().get(key);
                    if (value instanceof GregorianCalendar) {
                        Log.d("Feature Attribute", key + ": " + dateToDateDisplayString(new Date(((GregorianCalendar) value).getTimeInMillis())));
                    } else {
                        Log.d("Feature Attribute", key + ": " + String.valueOf(value));
                    }
                }
            }
            btnOpen.setEnabled(true);
        }
        dismissProgress();
    }
    
    public String dateToDateDisplayString (Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(date);
    }
    
}
