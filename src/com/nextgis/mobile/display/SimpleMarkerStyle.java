package com.nextgis.mobile.display;

import android.graphics.Paint;

import com.nextgis.mobile.datasource.GeoGeometry;
import com.nextgis.mobile.datasource.GeoPoint;

import static com.nextgis.mobile.util.DisplayConstants.*;

public class SimpleMarkerStyle extends Style{
    protected int mType;
    protected float mSize;
    protected float mWidth;
    protected int mOutColor;

    public SimpleMarkerStyle(int fillColor, int outColor, float size, int type) {
        super(fillColor);
        mType = type;
        mSize = size;
        mOutColor = outColor;
        mWidth = 1;
    }

    @Override
    public void onDraw(GeoGeometry geoGeometry, GISDisplay display) {
        GeoPoint pt = (GeoPoint) geoGeometry;
        switch (mType){
            case MarkerStylePoint:
                Paint ptPaint = new Paint();
                ptPaint.setColor(mColor);
                ptPaint.setStrokeWidth((float) (mSize / display.getScale()));
                ptPaint.setStrokeCap(Paint.Cap.ROUND);
                ptPaint.setAntiAlias(true);

                display.drawPoint((float)pt.getX(), (float)pt.getY(), ptPaint);
                break;
            case MarkerStyleCircle:
                Paint fillCirclePaint = new Paint();
                fillCirclePaint.setColor(mColor);
                fillCirclePaint.setStrokeCap(Paint.Cap.ROUND);

                display.drawCircle((float)pt.getX(), (float)pt.getY(), mSize, fillCirclePaint);

                Paint outCirclePaint = new Paint();
                outCirclePaint.setColor(mOutColor);
                outCirclePaint.setStrokeWidth((float) (mWidth / display.getScale()));
                outCirclePaint.setStyle(Paint.Style.STROKE);
                outCirclePaint.setAntiAlias(true);
                display.drawCircle((float)pt.getX(), (float)pt.getY(), mSize, outCirclePaint);

                break;
            case MarkerStyleDiamond:
                break;
            case MarkerStyleCross:
                break;
            case MarkerStyleTriangle:
                break;
            case MarkerStyleBox:
                break;
        }
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }

    public float getSize() {
        return mSize;
    }

    public void setSize(float size) {
        mSize = size;
    }

    public float getWidth() {
        return mWidth;
    }

    public void setWidth(float width) {
       mWidth = width;
    }

    public int getOutlineColor() {
        return mOutColor;
    }

    public void setOutlineColor(int outColor) {
        mOutColor = outColor;
    }
}
