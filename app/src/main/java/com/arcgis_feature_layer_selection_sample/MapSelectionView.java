package com.arcgis_feature_layer_selection_sample;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class MapSelectionView extends View {

    public interface IMapSelectionViewListener {
        void startSelectAtPoint (float x, float y);

        void moveSelectToPoint (float x, float y);

        void endSelectAtPoint (float x, float y);
    }

    public IMapSelectionViewListener listener;

    public MapSelectionView (Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent (MotionEvent event) {
        if (listener == null)
            return super.onTouchEvent(event);

        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            listener.startSelectAtPoint(event.getX(), event.getY());
        } else if (action == MotionEvent.ACTION_MOVE) {
            listener.moveSelectToPoint(event.getX(), event.getY());
        } else if (action == MotionEvent.ACTION_UP) {
            listener.endSelectAtPoint(event.getX(), event.getY());
        } else
            return super.onTouchEvent(event);

        return true;
    }
}
