/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
 *   Copyright (C) 2014 NextGIS
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package com.nextgis.mobile.map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.nextgis.mobile.GeoJsonLayersListAdapter;
import com.nextgis.mobile.MainActivity;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.Feature;
import com.nextgis.mobile.datasource.GeoEnvelope;
import com.nextgis.mobile.datasource.GeoPoint;
import com.nextgis.mobile.display.EditMarkerStyle;
import com.nextgis.mobile.util.FileUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.mobile.util.Constants.*;

public class MapViewEditable extends MapView {

    public static final int toleranceDP = 20;
    public final float tolerancePX =
            getContext().getResources().getDisplayMetrics().density * toleranceDP;

    protected LocalGeoJsonEditLayer mEditLayer;
    protected float mAnchorOffsetX, mAnchorOffsetY;

    public MapViewEditable(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); // must run first

        if (mDisplay != null && mEditLayer != null) {

            switch (mDrawingState) {

                case DRAW_SATE_panning:
                    GeoEnvelope bounds = mDisplay.getScreenBounds();
                    bounds.offset(mCurrentMouseLocation.x, mCurrentMouseLocation.y);
                    GeoEnvelope mapBounds = mDisplay.screenToMap(bounds);
                    GeoPoint pt = mapBounds.getCenter();

                    mDisplay.setTransformMatrix(mDisplay.getZoomLevel(), pt);
                    break;

                case DRAW_SATE_zooming:
                    GeoPoint focusPt = new GeoPoint(
                            -mCurrentFocusLocation.x, -mCurrentFocusLocation.y);
                    double invertScale = 1 / mScaleFactor;
                    double offX = (1 - invertScale) * focusPt.getX();
                    double offY = (1 - invertScale) * focusPt.getY();

                    GeoEnvelope env = mDisplay.getScreenBounds();
                    env.scale(invertScale);
                    env.offset(offX, offY);

                    GeoPoint center = env.getCenter();
                    GeoPoint geoCenter = mDisplay.screenToMap(center);

                    mDisplay.setTransformMatrix(getZoomForScaleFactor(mScaleFactor), geoCenter);
                    break;

                case DRAW_SATE_edit_drawing:
                    canvas.drawBitmap(mDisplay.getDisplay(false), 0, 0, null);
                    break;
            }

            mEditLayer.draw();
            canvas.drawBitmap(mDisplay.getEditDisplay(), 0, 0, null);
        }
    }

    @Override
    protected void processMessage(Bundle bundle) {
        switch (bundle.getInt(BUNDLE_TYPE_KEY)) {
            case MSGTYPE_EDIT_LAYER_ADDED: //the new edit layer was create and need to be added on map
                File path = (File) bundle.getSerializable(BUNDLE_PATH_KEY);
                addLayer(path);
                saveMap();

                if (isEditModeActive()) {
                    switchEditMode();
                }

                break;

            case MSGTYPE_EDIT_DRAWING_DONE:
                onEditLayerDrawFinished(bundle.getFloat(BUNDLE_DONE_KEY));
                break;

            default:
                super.processMessage(bundle);
        }
    }

    protected void onEditLayerDrawFinished(float percent) {
        if (percent >= 100) {
            invalidate();
        }
    }

    /**
     * Create existed layer from path and add it to the map
     *
     * @param path A path to layer directory
     */
    @Override
    protected void addLayer(File path) {
        super.addLayer(path);

        File config_file = new File(path, LAYER_CONFIG);
        try {
            String sData = FileUtil.readFromFile(config_file);
            JSONObject rootObject = new JSONObject(sData);
            int nType = rootObject.getInt(JSON_TYPE_KEY);

            switch (nType) {
                case LAYERTYPE_LOCAL_EDIT_GEOJSON:
                    mEditLayer = new LocalGeoJsonEditLayer(this, path, rootObject);

                    EditMarkerStyle drawStyle = (EditMarkerStyle) ((EditFeatureRenderer) mEditLayer
                            .getRenderer()).getStyle();
                    mAnchorOffsetX = drawStyle.getAnchorCenterX();
                    mAnchorOffsetY = drawStyle.getAnchorCenterY();
                    break;
            }

        } catch (IOException e) {
            reportError(e.getLocalizedMessage());
        } catch (JSONException e) {
            reportError(e.getLocalizedMessage());
        }

    }

    protected void saveEditableLayer() {
        if (mEditLayer != null) {

            GeoJsonLayer editableLayer =
                    (GeoJsonLayer) getLayerByName(mEditLayer.getEditableLayerName());

            if (editableLayer != null) {
                Feature editFeature = mEditLayer.getEditFeature();
                Feature editableFeature = editableLayer.getFeatureById(editFeature.getID());

                if (editableFeature != null) {
                    editableFeature.setGeometry(editFeature.getGeometry());
                    editableLayer.save();

                    Toast.makeText(getContext(), getContext().getString(R.string.layer_is_saved),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    protected void deleteEditLayer() {
        if (mEditLayer != null) {
            int id = mEditLayer.getId();
            mEditLayer.delete();
            mEditLayer = null;
            onLayerDeleted(id);
        }
    }

    /**
     * Delete layer by identifictor
     *
     * @param id An identificator
     * @return true on success or false
     */
    public boolean deleteLayerById(int id) {

        Layer layer = getLayerById(id);

        if (layer != null) {
            String delLayerName = layer.getName();
            boolean retVal = super.deleteLayerById(id);

            if (retVal && mEditLayer != null
                    && mEditLayer.getEditableLayerName().equals(delLayerName)) {

                if (isEditModeActive()) {
                    onCancelEditLayer();
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Load map properties and layers from map.json file
     */
    @Override
    protected synchronized void loadMap() {
        super.loadMap();

        if (isEditModeActive()) {
            switchEditMode();
        }
    }

    /**
     * Save map properties and layers to map.json file
     */
    @Override
    public synchronized void saveMap() {
        Log.d(TAG, "save map");
        try {
            JSONObject rootObject = new JSONObject();
            rootObject.put(JSON_NAME_KEY, mName);
            JSONArray jsonArray = new JSONArray();
            rootObject.put(JSON_LAYERS_KEY, jsonArray);
            for (Layer layer : mLayers) {
                JSONObject layerObject = new JSONObject();
                layerObject.put(JSON_PATH_KEY, layer.getRelativePath());
                jsonArray.put(layerObject);
                layer.save();
            }

            if (mEditLayer != null) {
                JSONObject layerObject = new JSONObject();
                layerObject.put(JSON_PATH_KEY, mEditLayer.getRelativePath());
                jsonArray.put(layerObject);
                mEditLayer.save();
            }

            File config_file = new File(mMapPath, MAP_CONFIG);
            FileUtil.writeToFile(config_file, rootObject.toString());

        } catch (IOException e) {
            reportError(e.getLocalizedMessage());
        } catch (JSONException e) {
            reportError(e.getLocalizedMessage());
        }
    }

    protected void editStart(MotionEvent event) {
        if (mDrawingState == DRAW_SATE_edit_drawing)
            return;

        if (mEditLayer != null) {
            Feature selectedFeature = getSelectedFeature(
                    new GeoPoint(event.getX() - mAnchorOffsetX, event.getY() - mAnchorOffsetY),
                    mEditLayer);

            if (selectedFeature != null) {
                mEditLayer.setEditFeature(selectedFeature);
                mHandler.removeMessages(MSGTYPE_EDIT_DRAWING_DONE);
                mDrawingState = DRAW_SATE_edit_drawing;
            }
        }
    }

    protected void editFeatureMoveTo(MotionEvent event) {
        if (mDrawingState == DRAW_SATE_edit_drawing) {
            GeoPoint screenPt =
                    new GeoPoint(event.getX() - mAnchorOffsetX, event.getY() - mAnchorOffsetY);
            GeoPoint geoPt = mDisplay.screenToMap(screenPt);

            mEditLayer.getEditFeature().setGeometry(geoPt);

            invalidate();
        }
    }

    protected void editStop() {
        if (mDrawingState == DRAW_SATE_edit_drawing) {
            mDrawingState = DRAW_SATE_drawing_noclearbk;

            Bundle bundle = new Bundle();
            bundle.putBoolean(BUNDLE_HASERROR_KEY, false);
            bundle.putInt(BUNDLE_TYPE_KEY, MSGTYPE_EDIT_DRAWING_DONE);
            bundle.putFloat(BUNDLE_DONE_KEY, 100.0f);
            bundle.putInt(BUNDLE_DRAWSTATE_KEY, DRAW_SATE_drawing_noclearbk);

            Message msg = new Message();
            msg.setData(bundle);
            msg.what = MSGTYPE_EDIT_DRAWING_DONE;
            mHandler.sendMessageDelayed(msg, DISPLAY_REDRAW_TIMEOUT);
        }
    }

    // delegate the event to the gesture detector
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);

        if (!mGestureDetector.onTouchEvent(event)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    break;

                case MotionEvent.ACTION_MOVE:
                    break;

                case MotionEvent.ACTION_UP:
                    editStop();
                    panStop();
                    break;

                default:
                    break;
            }
        }

        return true;
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
//        Log.d(TAG, "onScroll: " + event1.toString() + ", " + event2.toString() + ", "
//                + distanceX + ", " + distanceY);

        editStart(event1); // editStart() must run before panStart()
        editFeatureMoveTo(event2);
        return super.onScroll(event1, event2, distanceX, distanceY); // must run last
    }

    @Override
    public void onLongPress(MotionEvent event) {

        if (isEditModeActive()) {
            return;
        }

        List<Layer> geoJsonLayers = getLayersByType(LAYERTYPE_LOCAL_GEOJSON);

        switch (geoJsonLayers.size()) {
            case 0:
                return;

            case 1:
                createLayerEditor((GeoJsonLayer) geoJsonLayers.get(0),
                        new GeoPoint(event.getX(), event.getY()));
                break;

            default:
                final GeoPoint screenGeoPoint = new GeoPoint(event.getX(), event.getY());
                final GeoJsonLayersListAdapter listAdapter = new GeoJsonLayersListAdapter(getSelf());
                listAdapter.getFilter().filter(null);

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                builder.setTitle(R.string.select_layer_for_edit);
                builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        GeoJsonLayer layer = (GeoJsonLayer) listAdapter.getItem(which);
                        createLayerEditor(layer, screenGeoPoint);
                    }

                });
                builder.show();
        }

        super.onLongPress(event);
    }

    private void createLayerEditor(final GeoJsonLayer layer, GeoPoint screenPoint) {
        final Feature selectedFeature = getSelectedFeature(screenPoint, layer);

        if (selectedFeature != null) {
            LinearLayout dialogView = (LinearLayout) ((MainActivity) getContext()).getLayoutInflater()
                    .inflate(R.layout.dialog_choose_action_for_feature, null);
            Button btnShowAttributes = (Button) dialogView.findViewById(R.id.btn_show_attributes);
            Button btnDeleteFeature = (Button) dialogView.findViewById(R.id.btn_delete_feature);
            Button btnEditFeature = (Button) dialogView.findViewById(R.id.btn_edit_feature);

            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.choose_action_for_feature);
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();

            btnEditFeature.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        List<Feature> features = new ArrayList<Feature>(1);
                        features.add(selectedFeature);
                        LocalGeoJsonEditLayer.create(getSelf(), layer.getName(), features);

                        mHandler.removeMessages(MSGTYPE_EDIT_DRAWING_DONE);
                        mDrawingState = DRAW_SATE_edit_drawing;

                    } catch (JSONException e) {
                        reportError(e.getLocalizedMessage());
                    } catch (IOException e) {
                        reportError(e.getLocalizedMessage());
                    }

                    dialog.dismiss();
                }
            });

            dialog.show();

        } else {
            reportError(getContext().getString(R.string.object_is_not_selected));
        }
    }

    protected Feature getSelectedFeature(GeoPoint screenPoint, GeoJsonLayer layer) {
        GeoEnvelope screenEnvelope = new GeoEnvelope(
                screenPoint.getX() - tolerancePX, screenPoint.getX() + tolerancePX,
                screenPoint.getY() - tolerancePX, screenPoint.getY() + tolerancePX);
        GeoEnvelope geoEnvelope = mDisplay.screenToMap(screenEnvelope);

        Feature selectedFeature = layer.getSelectedFeature(geoEnvelope);

        if (selectedFeature != null) {
            return selectedFeature;
        }

        return null;
    }

    public boolean isEditModeActive() {
        return mEditLayer != null;
    }

    public void onSaveEditLayer() {
        saveEditableLayer();
        deleteEditLayer();
        switchEditMode();
    }

    public void onCancelEditLayer() {
        deleteEditLayer();
        switchEditMode();
    }

    public void switchEditMode() {
        ((MainActivity) getContext()).supportInvalidateOptionsMenu();
    }
}
