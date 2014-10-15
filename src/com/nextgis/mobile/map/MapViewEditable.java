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
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;
import com.nextgis.mobile.GeoJsonLayersListAdapter;
import com.nextgis.mobile.MainActivity;
import com.nextgis.mobile.R;
import com.nextgis.mobile.datasource.Feature;
import com.nextgis.mobile.datasource.GeoEnvelope;
import com.nextgis.mobile.datasource.GeoPoint;
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

                if (mEditLayer != null) {
                    MainActivity mainActivity = (MainActivity) getContext();
                    mActionMode = mainActivity.startSupportActionMode(mActionModeCallback);
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
            Feature editFeature = mEditLayer.getEditFeature();

            GeoJsonLayer editableLayer =
                    (GeoJsonLayer) getLayerByName(mEditLayer.getEditableLayerName());
            Feature editableFeature = editableLayer.getFeatureById(editFeature.getID());

            if (editableFeature != null) {
                editableFeature.setGeometry(editFeature.getGeometry());
                editableLayer.save();
            }
        }
    }

    /**
     * Delete layer by identifictor
     *
     * @param id An identificator
     * @return true on success or false
     */
    public boolean deleteLayerById(int id) {

        if (mEditLayer.getId() == id) {
            mEditLayer.delete();
            mEditLayer = null;
            onLayerDeleted(id);
            return true;
        }

        return super.deleteLayerById(id);
    }

    /**
     * Load map properties and layers from map.json file
     */
    @Override
    protected synchronized void loadMap() {
        super.loadMap();

        if (mEditLayer != null) {
            MainActivity mainActivity = (MainActivity) getContext();
            mActionMode = mainActivity.startSupportActionMode(mActionModeCallback);
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
            Feature selectedFeature =
                    getSelectedFeature(new GeoPoint(event.getX(), event.getY()), mEditLayer);

            if (selectedFeature != null) {
                mEditLayer.setEditFeature(selectedFeature);
                mHandler.removeMessages(MSGTYPE_EDIT_DRAWING_DONE);
                mDrawingState = DRAW_SATE_edit_drawing;
            }
        }
    }

    protected void editFeatureMoveTo(MotionEvent event) {
        if (mDrawingState == DRAW_SATE_edit_drawing) {
            GeoPoint screenPt = new GeoPoint(event.getX(), event.getY());
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

        if (mActionMode != null) {
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

    private void createLayerEditor(GeoJsonLayer layer, GeoPoint screenPoint) {
        // TODO: dialog with "delete", "edit", ...

        Feature selectedFeature = getSelectedFeature(screenPoint, layer);

        if (selectedFeature != null) {

            try {
                List<Feature> features = new ArrayList<Feature>(1);
                features.add(selectedFeature);
                LocalGeoJsonEditLayer.create(this, layer.getName(), features);

                mHandler.removeMessages(MSGTYPE_EDIT_DRAWING_DONE);
                mDrawingState = DRAW_SATE_edit_drawing;

            } catch (JSONException e) {
                reportError(e.getLocalizedMessage());
            } catch (IOException e) {
                reportError(e.getLocalizedMessage());
            }

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

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (!hasWindowFocus) {
            setKeyStateActionMode(MapActionModeCallback.KEY_NONE_FOCUS);
        } else {
            setKeyStateActionMode(MapActionModeCallback.KEY_NONE);
        }
    }

    protected ActionMode mActionMode = null;

    public boolean isActionModeActive() {
        return mActionMode != null;
    }

    protected MapActionModeCallback mActionModeCallback = new MapActionModeCallback();

    public void setKeyStateActionMode(int keyState) {
        if (mActionModeCallback != null) {
            mActionModeCallback.setKeyState(keyState);
        }
    }

    public class MapActionModeCallback implements ActionMode.Callback {

        public static final int KEY_NONE = 0;
        public static final int KEY_NONE_FOCUS = 1;
        public static final int KEY_CANCEL = 2;
        public static final int KEY_SAVE = 3;

        private int mKeyState = KEY_NONE;

        public void setKeyState(int keyState) {
            this.mKeyState = keyState;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_layer, menu);
            mode.setTitle(getContext().getString(R.string.select_layer_for_edit));
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {

                case R.id.menu_save:
                    mKeyState = KEY_SAVE;
                    break;

                case R.id.menu_cancel:
                    mKeyState = KEY_CANCEL;
                    break;

                default:
                    mKeyState = KEY_NONE;
                    break;
            }

            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (mKeyState == KEY_NONE) { // ActionMode Done is pressed
                mKeyState = KEY_SAVE;
            }

            onMakeAction();

            mActionMode = null;
        }

        public void onMakeAction() {
            switch (mKeyState) {

                case KEY_SAVE:
                    saveEditableLayer();
                    Toast.makeText(getContext(), getContext().getString(R.string.layer_is_saved),
                            Toast.LENGTH_LONG).show();

                case KEY_CANCEL:
                    if (mEditLayer != null) {
                        deleteLayerById(mEditLayer.getId());
                    }
                    break;

                case KEY_NONE_FOCUS:
                case KEY_NONE:
                    break;

                default:
                    break;
            }

            mKeyState = KEY_NONE;
        }
    }
}
