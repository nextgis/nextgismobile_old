// Created by plusminus on 17:45:56 - 25.09.2008
package org.osmdroid.views;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import microsoft.mappoint.TileSystem;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.api.IMapView;
import org.osmdroid.api.IProjection;
import org.osmdroid.api.IZoomController;
import org.osmdroid.constants.GeoConstants;
import org.osmdroid.constants.MapViewConstants;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.GeometryMath;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.safecanvas.ISafeCanvas;
import org.osmdroid.views.safecanvas.ISafeCanvas.UnsafeCanvasHandler;
import org.osmdroid.views.safecanvas.SafeTranslatedCanvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;
import android.widget.ZoomButtonsController.OnZoomListener;


import android.util.Log;
import static org.osmdroid.constants.UtilConstants.*;

public class MapView extends ViewGroup implements IMapView, MapViewConstants,
		MultiTouchObjectCanvas<Object> {

	protected static final double ZOOM_SENSITIVITY = 1.0;
	protected static final double ZOOM_LOG_BASE_INV = 1.0 / Math.log(2.0 / ZOOM_SENSITIVITY);
	protected static Method sMotionEventTransformMethod;

	/** Current zoom level for map tiles. */
	protected int mZoomLevel = 0;

	protected final OverlayManager mOverlayManager;

	protected Projection mProjection;

	protected final GestureDetector mGestureDetector;

	/** Handles map scrolling */
	protected final Scroller mScroller;
	protected boolean mIsFlinging;

	protected final AtomicInteger mTargetZoomLevel = new AtomicInteger();
	protected final AtomicBoolean mIsAnimating = new AtomicBoolean(false);

	protected Integer mMinimumZoomLevel;
	protected Integer mMaximumZoomLevel;

	protected final MapController mController;
	protected MultiTouchController<Object> mMultiTouchController;
	protected float mMultiTouchScale = 1.0f;
	protected PointF mMultiTouchScalePoint = new PointF();

	protected MapListener mListener;

	// For rotation
	protected float mapOrientation = 0;
	protected final Matrix mRotateMatrix = new Matrix();
	protected final float[] mRotatePoints = new float[2];
	protected final Rect mInvalidateRect = new Rect();

	protected BoundingBoxE6 mScrollableAreaBoundingBox;
	protected Rect mScrollableAreaLimit;

	//TODO: private final Handler mTileRequestCompleteHandler;
	protected IZoomController mZoomController;

	/* a point that will be reused to design added views */
	protected final Point mPoint = new Point();

	/** A drawable loading tile **/
	protected BitmapDrawable mLoadingTile = null;
	protected int mLoadingBackgroundColor = Color.rgb(216, 208, 208);
	protected int mLoadingLineColor = Color.rgb(200, 192, 192);
	

	protected MapView(final Context context, final int tileSizePixels, final AttributeSet attrs) {
		super(context, attrs);
		this.mController = new MapController(this);
		this.mScroller = new Scroller(context);
		TileSystem.setTileSize(tileSizePixels);

		//TODO: mTileRequestCompleteHandler = tileRequestCompleteHandler == null
		//TODO: 		? new SimpleInvalidationHandler(this)
		//TODO: 		: tileRequestCompleteHandler;
		//TODO: mTileProvider = tileProvider;
		//TODO: mTileProvider.setTileRequestCompleteHandler(mTileRequestCompleteHandler);

		mOverlayManager = new OverlayManager();

		mGestureDetector = new GestureDetector(context, new MapViewGestureDetectorListener());
		mGestureDetector.setOnDoubleTapListener(new MapViewDoubleClickListener());
	}

	/**
	 * Constructor used by XML layout resource (uses default tile source).
	 */
	public MapView(final Context context, final AttributeSet attrs) {
		this(context, 256, attrs);
	}

	/**
	 * Standard Constructor.
	 */
	public MapView(final Context context, final int tileSizePixels) {
		this(context, tileSizePixels, null);
	}

	@Override
	public IMapController getController() {
		return this.mController;
	}

	/**
	 * You can add/remove/reorder your Overlays using the List of {@link Overlay}. The first (index
	 * 0) Overlay gets drawn first, the one with the highest as the last one.
	 */
	public List<Overlay> getOverlays() {
		return this.getOverlayManager();
	}

	public OverlayManager getOverlayManager() {
		return mOverlayManager;
	}

	public Scroller getScroller() {
		return mScroller;
	}

	//TODO: public Handler getTileRequestCompleteHandler() {
	//TODO: 	return mTileRequestCompleteHandler;
	//TODO: }

	@Override
	public int getLatitudeSpan() {
		return this.getBoundingBox().getLatitudeSpanE6();
	}

	@Override
	public int getLongitudeSpan() {
		return this.getBoundingBox().getLongitudeSpanE6();
	}

	public BoundingBoxE6 getBoundingBox() {
		return getBoundingBox(getWidth(), getHeight());
	}

	public BoundingBoxE6 getBoundingBox(final int pViewWidth, final int pViewHeight) {

		final int world_2 = TileSystem.MapSize(mZoomLevel) / 2;
		final Rect screenRect = getScreenRect(null);
		screenRect.offset(world_2, world_2);
		final IGeoPoint neGeoPoint = TileSystem.PixelXYToLatLong(screenRect.right, screenRect.top,
				mZoomLevel, null);
		final IGeoPoint swGeoPoint = TileSystem.PixelXYToLatLong(screenRect.left,
				screenRect.bottom, mZoomLevel, null);

		return new BoundingBoxE6(neGeoPoint.getLatitudeE6(), neGeoPoint.getLongitudeE6(),
				swGeoPoint.getLatitudeE6(), swGeoPoint.getLongitudeE6());
	}

	/**
	 * Gets the current bounds of the screen in <I>screen coordinates</I>.
	 */
	public Rect getScreenRect(final Rect reuse) {
		final Rect out = getIntrinsicScreenRect(reuse);
		if (this.getMapOrientation() != 0 && this.getMapOrientation() != 180) {
			// Since the canvas is shifted by getWidth/2, we can just return our natural scrollX/Y
			// value since that is the same as the shifted center.
			int centerX = this.getScrollX();
			int centerY = this.getScrollY();
			GeometryMath.getBoundingBoxForRotatatedRectangle(out, centerX, centerY,
					this.getMapOrientation(), out);
		}
		return out;
	}

	public Rect getIntrinsicScreenRect(final Rect reuse) {
		final Rect out = reuse == null ? new Rect() : reuse;
		out.set(getScrollX() - getWidth() / 2, getScrollY() - getHeight() / 2, getScrollX()
				+ getWidth() / 2, getScrollY() + getHeight() / 2);
		return out;
	}

	/**
	 * Get a projection for converting between screen-pixel coordinates and latitude/longitude
	 * coordinates. You should not hold on to this object for more than one draw, since the
	 * projection of the map could change.
	 *
	 * @return The Projection of the map in its current state. You should not hold on to this object
	 *         for more than one draw, since the projection of the map could change.
	 */
	@Override
	public Projection getProjection() {
		if (mProjection == null) {
			mProjection = new Projection();
		}
		return mProjection;
	}

	void setMapCenter(final IGeoPoint aCenter) {
		getController().animateTo(aCenter);
	}

	/**
	 * @param aZoomLevel
	 *            the zoom level bound by the tile source
	 */
	int setZoomLevel(final int aZoomLevel) {
		final int minZoomLevel = getMinZoomLevel();
		final int maxZoomLevel = getMaxZoomLevel();

		final int newZoomLevel = Math.max(minZoomLevel, Math.min(maxZoomLevel, aZoomLevel));
		final int curZoomLevel = this.mZoomLevel;

		if (newZoomLevel != curZoomLevel) {
			mScroller.forceFinished(true);
			mIsFlinging = false;
		}

		this.mZoomLevel = newZoomLevel;
		this.checkZoomButtons();

		if (newZoomLevel > curZoomLevel) {
			// We are going from a lower-resolution plane to a higher-resolution plane, so we have
			// to do it the hard way.
			final int worldSize_current_2 = TileSystem.MapSize(curZoomLevel) / 2;
			final int worldSize_new_2 = TileSystem.MapSize(newZoomLevel) / 2;
			final IGeoPoint centerGeoPoint = TileSystem.PixelXYToLatLong(getScrollX()
					+ worldSize_current_2, getScrollY() + worldSize_current_2, curZoomLevel, null);
			final Point centerPoint = TileSystem.LatLongToPixelXY(
					centerGeoPoint.getLatitude(), centerGeoPoint.getLongitude(),
					newZoomLevel, null);
			scrollTo(centerPoint.x - worldSize_new_2, centerPoint.y - worldSize_new_2);
		} else if (newZoomLevel < curZoomLevel) {
			// We are going from a higher-resolution plane to a lower-resolution plane, so we can do
			// it the easy way.
			scrollTo(getScrollX() >> curZoomLevel - newZoomLevel,
					 getScrollY() >> curZoomLevel - newZoomLevel);
		}

		// snap for all snappables
		final Point snapPoint = new Point();
		mProjection = new Projection();
		if (this.getOverlayManager().onSnapToItem(getScrollX(), getScrollY(), snapPoint, this)) {
			scrollTo(snapPoint.x, snapPoint.y);
		}

		// do callback on listener
		if (newZoomLevel != curZoomLevel && mListener != null) {
			final ZoomEvent event = new ZoomEvent(this, newZoomLevel);
			mListener.onZoom(event);
		}
		// Allows any views fixed to a Location in the MapView to adjust
		this.requestLayout();
		return this.mZoomLevel;
	}

	/**
	 * Zoom the map to enclose the specified bounding box, as closely as possible.
	 * Must be called after display layout is complete, or screen dimensions are not known, and
	 * will always zoom to center of zoom  level 0.
	 * Suggestion: Check getScreenRect(null).getHeight() > 0
	 */
	public void zoomToBoundingBox(final BoundingBoxE6 boundingBox) {
		final BoundingBoxE6 currentBox = getBoundingBox();

		// Calculated required zoom based on latitude span
		final double maxZoomLatitudeSpan = mZoomLevel == getMaxZoomLevel() ?
				currentBox.getLatitudeSpanE6() :
				currentBox.getLatitudeSpanE6() / Math.pow(2, getMaxZoomLevel() - mZoomLevel);

		final double requiredLatitudeZoom =
			getMaxZoomLevel() -
			Math.ceil(Math.log(boundingBox.getLatitudeSpanE6() / maxZoomLatitudeSpan) / Math.log(2));


		// Calculated required zoom based on longitude span
		final double maxZoomLongitudeSpan = mZoomLevel == getMaxZoomLevel() ?
				currentBox.getLongitudeSpanE6() :
				currentBox.getLongitudeSpanE6() / Math.pow(2, getMaxZoomLevel() - mZoomLevel);

		final double requiredLongitudeZoom =
			getMaxZoomLevel() -
			Math.ceil(Math.log(boundingBox.getLongitudeSpanE6() / maxZoomLongitudeSpan) / Math.log(2));


		// Zoom to boundingBox center, at calculated maximum allowed zoom level
		getController().setZoom((int)(
				requiredLatitudeZoom < requiredLongitudeZoom ?
				requiredLatitudeZoom : requiredLongitudeZoom));

		getController().setCenter(
				new GeoPoint(boundingBox.getCenter().getLatitudeE6(), boundingBox.getCenter()
						.getLongitudeE6()));
	}

	/**
	 * Get the current ZoomLevel for the map tiles.
	 *
	 * @return the current ZoomLevel between 0 (equator) and 18/19(closest), depending on the tile
	 *         source chosen.
	 */
	@Override
	public int getZoomLevel() {
		return getZoomLevel(true);
	}

	/**
	 * Get the current ZoomLevel for the map tiles.
	 *
	 * @param aPending
	 *            if true and we're animating then return the zoom level that we're animating
	 *            towards, otherwise return the current zoom level
	 * @return the zoom level
	 */
	public int getZoomLevel(final boolean aPending) {
		if (aPending && isAnimating()) {
			return mTargetZoomLevel.get();
		} else {
			return mZoomLevel;
		}
	}

	/**
	 * Get the minimum allowed zoom level for the maps.
	 */
	public int getMinZoomLevel() {
		return mMinimumZoomLevel;
	}

	/**
	 * Get the maximum allowed zoom level for the maps.
	 */
	@Override
	public int getMaxZoomLevel() {
		return mMaximumZoomLevel;
	}

	/**
	 * Set the minimum allowed zoom level, or pass null to use the minimum zoom level from the tile
	 * provider.
	 */
	public void setMinZoomLevel(Integer zoomLevel) {
		mMinimumZoomLevel = zoomLevel;
	}

	/**
	 * Set the maximum allowed zoom level, or pass null to use the maximum zoom level from the tile
	 * provider.
	 */
	public void setMaxZoomLevel(Integer zoomLevel) {
		mMaximumZoomLevel = zoomLevel;
	}

	public boolean canZoomIn() {
		final int maxZoomLevel = getMaxZoomLevel();
		if ((isAnimating() ? mTargetZoomLevel.get() : mZoomLevel) >= maxZoomLevel) {
			return false;
		}
		return true;
	}

	public boolean canZoomOut() {
		final int minZoomLevel = getMinZoomLevel();
		if ((isAnimating() ? mTargetZoomLevel.get() : mZoomLevel) <= minZoomLevel) {
			return false;
		}
		return true;
	}

	/**
	 * Zoom in by one zoom level.
	 */
	boolean zoomIn() {
		return getController().zoomIn();
	}

	boolean zoomInFixing(final IGeoPoint point) {
		Point coords = getProjection().toMapPixels(point, null);
		return getController().zoomInFixing(coords.x, coords.y);
	}

	boolean zoomInFixing(final int xPixel, final int yPixel) {
		return getController().zoomInFixing(xPixel, yPixel);
	}

	/**
	 * Zoom out by one zoom level.
	 */
	boolean zoomOut() {
		return getController().zoomOut();
	}

	boolean zoomOutFixing(final IGeoPoint point) {
		Point coords = getProjection().toMapPixels(point, null);
		return zoomOutFixing(coords.x, coords.y);
	}

	boolean zoomOutFixing(final int xPixel, final int yPixel) {
		return getController().zoomOutFixing(xPixel, yPixel);
	}

	/**
	 * Returns the current center-point position of the map, as a GeoPoint (latitude and longitude).
	 *
	 * @return A GeoPoint of the map's center-point.
	 */
	@Override
	public IGeoPoint getMapCenter() {
		final int world_2 = TileSystem.MapSize(mZoomLevel) / 2;
		final Rect screenRect = getScreenRect(null);
		screenRect.offset(world_2, world_2);
		return TileSystem.PixelXYToLatLong(screenRect.centerX(), screenRect.centerY(), mZoomLevel,
				null);
	}

	public void setMapOrientation(float degrees) {
		this.mapOrientation = degrees % 360.0f;
		this.invalidate();
	}

	public float getMapOrientation() {
		return mapOrientation;
	}

	/**
	 * Set the map to limit it's scrollable view to the specified BoundingBoxE6. Note this does not
	 * limit zooming so it will be possible for the user to zoom to an area that is larger than the
	 * limited area.
	 *
	 * @param boundingBox
	 *            A lat/long bounding box to limit scrolling to, or null to remove any scrolling
	 *            limitations
	 */
	public void setScrollableAreaLimit(BoundingBoxE6 boundingBox) {
		final int worldSize_2 = TileSystem.MapSize(MapViewConstants.MAXIMUM_ZOOMLEVEL) / 2;

		mScrollableAreaBoundingBox = boundingBox;

		// Clear scrollable area limit if null passed.
		if (boundingBox == null) {
			mScrollableAreaLimit = null;
			return;
		}

		// Get NW/upper-left
		final Point upperLeft = TileSystem.LatLongToPixelXY(boundingBox.getLatNorthE6() / 1E6,
				boundingBox.getLonWestE6() / 1E6, MapViewConstants.MAXIMUM_ZOOMLEVEL, null);
		upperLeft.offset(-worldSize_2, -worldSize_2);

		// Get SE/lower-right
		final Point lowerRight = TileSystem.LatLongToPixelXY(boundingBox.getLatSouthE6() / 1E6,
				boundingBox.getLonEastE6() / 1E6, MapViewConstants.MAXIMUM_ZOOMLEVEL, null);
		lowerRight.offset(-worldSize_2, -worldSize_2);
		mScrollableAreaLimit = new Rect(upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y);
	}

	public BoundingBoxE6 getScrollableAreaLimit() {
		return mScrollableAreaBoundingBox;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================
	public void invalidateMapCoordinates(Rect dirty) {
		invalidateMapCoordinates(dirty.left, dirty.top, dirty.right, dirty.bottom);
	}

	public void invalidateMapCoordinates(int left, int top, int right, int bottom) {
		mInvalidateRect.set(left, top, right, bottom);
		final int width_2 = this.getWidth() / 2;
		final int height_2 = this.getHeight() / 2;

		// Since the canvas is shifted by getWidth/2, we can just return our natural scrollX/Y value
		// since that is the same as the shifted center.
		int centerX = this.getScrollX();
		int centerY = this.getScrollY();

		if (this.getMapOrientation() != 0)
			GeometryMath.getBoundingBoxForRotatatedRectangle(mInvalidateRect, centerX, centerY,
					this.getMapOrientation() + 180, mInvalidateRect);
		mInvalidateRect.offset(width_2, height_2);

		super.invalidate(mInvalidateRect);
	}

	/**
	 * Returns a set of layout parameters with a width of
	 * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}, a height of
	 * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT} at the {@link GeoPoint} (0, 0) align
	 * with {@link MapView.LayoutParams#BOTTOM_CENTER}.
	 */
	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, null, MapView.LayoutParams.BOTTOM_CENTER, 0, 0);
	}

	@Override
	public ViewGroup.LayoutParams generateLayoutParams(final AttributeSet attrs) {
		return new MapView.LayoutParams(getContext(), attrs);
	}

	// Override to allow type-checking of LayoutParams.
	@Override
	protected boolean checkLayoutParams(final ViewGroup.LayoutParams p) {
		return p instanceof MapView.LayoutParams;
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(final ViewGroup.LayoutParams p) {
		return new MapView.LayoutParams(p);
	}

	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		final int count = getChildCount();

		int maxHeight = 0;
		int maxWidth = 0;

		// Find out how big everyone wants to be
		measureChildren(widthMeasureSpec, heightMeasureSpec);

		// Find rightmost and bottom-most child
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {

				final MapView.LayoutParams lp = (MapView.LayoutParams) child.getLayoutParams();
				final int childHeight = child.getMeasuredHeight();
				final int childWidth = child.getMeasuredWidth();
				getProjection().toMapPixels(lp.geoPoint, mPoint);
				final int x = mPoint.x + getWidth() / 2;
				final int y = mPoint.y + getHeight() / 2;
				int childRight = x;
				int childBottom = y;
				switch (lp.alignment) {
				case MapView.LayoutParams.TOP_LEFT:
					childRight = x + childWidth;
					childBottom = y;
					break;
				case MapView.LayoutParams.TOP_CENTER:
					childRight = x + childWidth / 2;
					childBottom = y;
					break;
				case MapView.LayoutParams.TOP_RIGHT:
					childRight = x;
					childBottom = y;
					break;
				case MapView.LayoutParams.CENTER_LEFT:
					childRight = x + childWidth;
					childBottom = y + childHeight / 2;
					break;
				case MapView.LayoutParams.CENTER:
					childRight = x + childWidth / 2;
					childBottom = y + childHeight / 2;
					break;
				case MapView.LayoutParams.CENTER_RIGHT:
					childRight = x;
					childBottom = y + childHeight / 2;
					break;
				case MapView.LayoutParams.BOTTOM_LEFT:
					childRight = x + childWidth;
					childBottom = y + childHeight;
					break;
				case MapView.LayoutParams.BOTTOM_CENTER:
					childRight = x + childWidth / 2;
					childBottom = y + childHeight;
					break;
				case MapView.LayoutParams.BOTTOM_RIGHT:
					childRight = x;
					childBottom = y + childHeight;
					break;
				}
				childRight += lp.offsetX;
				childBottom += lp.offsetY;

				maxWidth = Math.max(maxWidth, childRight);
				maxHeight = Math.max(maxHeight, childBottom);
			}
		}

		// Account for padding too
		maxWidth += getPaddingLeft() + getPaddingRight();
		maxHeight += getPaddingTop() + getPaddingBottom();

		// Check against minimum height and width
		maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
		maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

		setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec),
				resolveSize(maxHeight, heightMeasureSpec));
	}

	@Override
	protected void onLayout(final boolean changed, final int l, final int t, final int r,
			final int b) {
		final int count = getChildCount();

		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != GONE) {

				final MapView.LayoutParams lp = (MapView.LayoutParams) child.getLayoutParams();
				final int childHeight = child.getMeasuredHeight();
				final int childWidth = child.getMeasuredWidth();
				getProjection().toMapPixels(lp.geoPoint, mPoint);
				final int x = mPoint.x + getWidth() / 2;
				final int y = mPoint.y + getHeight() / 2;
				int childLeft = x;
				int childTop = y;
				switch (lp.alignment) {
				case MapView.LayoutParams.TOP_LEFT:
					childLeft = getPaddingLeft() + x;
					childTop = getPaddingTop() + y;
					break;
				case MapView.LayoutParams.TOP_CENTER:
					childLeft = getPaddingLeft() + x - childWidth / 2;
					childTop = getPaddingTop() + y;
					break;
				case MapView.LayoutParams.TOP_RIGHT:
					childLeft = getPaddingLeft() + x - childWidth;
					childTop = getPaddingTop() + y;
					break;
				case MapView.LayoutParams.CENTER_LEFT:
					childLeft = getPaddingLeft() + x;
					childTop = getPaddingTop() + y - childHeight / 2;
					break;
				case MapView.LayoutParams.CENTER:
					childLeft = getPaddingLeft() + x - childWidth / 2;
					childTop = getPaddingTop() + y - childHeight / 2;
					break;
				case MapView.LayoutParams.CENTER_RIGHT:
					childLeft = getPaddingLeft() + x - childWidth;
					childTop = getPaddingTop() + y - childHeight / 2;
					break;
				case MapView.LayoutParams.BOTTOM_LEFT:
					childLeft = getPaddingLeft() + x;
					childTop = getPaddingTop() + y - childHeight;
					break;
				case MapView.LayoutParams.BOTTOM_CENTER:
					childLeft = getPaddingLeft() + x - childWidth / 2;
					childTop = getPaddingTop() + y - childHeight;
					break;
				case MapView.LayoutParams.BOTTOM_RIGHT:
					childLeft = getPaddingLeft() + x - childWidth;
					childTop = getPaddingTop() + y - childHeight;
					break;
				}
				childLeft += lp.offsetX;
				childTop += lp.offsetY;
				child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
			}
		}
	}

	public void onDetach() {
		this.getOverlayManager().onDetach(this);
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		final boolean result = this.getOverlayManager().onKeyDown(keyCode, event, this);

		return result || super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(final int keyCode, final KeyEvent event) {
		final boolean result = this.getOverlayManager().onKeyUp(keyCode, event, this);

		return result || super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onTrackballEvent(final MotionEvent event) {

		if (this.getOverlayManager().onTrackballEvent(event, this)) {
			return true;
		}

		scrollBy((int) (event.getX() * 25), (int) (event.getY() * 25));

		return super.onTrackballEvent(event);
	}

	@Override
	public boolean dispatchTouchEvent(final MotionEvent event) {

		if (DEBUGMODE) {
			Log.d(OSMD_TAG, "dispatchTouchEvent(" + event + ")");
		}

		// Get rotated event for some touch listeners.
		MotionEvent rotatedEvent = rotateTouchEvent(event);

		try {
			if (super.dispatchTouchEvent(event)) {
				if (DEBUGMODE) {
					Log.d(OSMD_TAG, "super handled onTouchEvent");
				}
				return true;
			}

			if (this.getOverlayManager().onTouchEvent(rotatedEvent, this)) {
				return true;
			}

			if (mMultiTouchController != null && mMultiTouchController.onTouchEvent(event)) {
				if (DEBUGMODE) {
					Log.d(OSMD_TAG, "mMultiTouchController handled onTouchEvent");
				}
				return true;
			}

			if (mGestureDetector.onTouchEvent(rotatedEvent)) {
				if (DEBUGMODE) {
					Log.d(OSMD_TAG, "mGestureDetector handled onTouchEvent");
				}
				return true;
			}
		} finally {
			if (rotatedEvent != event)
				rotatedEvent.recycle();
		}

		if (DEBUGMODE) {
			Log.d(OSMD_TAG, "no-one handled onTouchEvent");
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return false;
	}

	private MotionEvent rotateTouchEvent(MotionEvent ev) {
		if (this.getMapOrientation() == 0)
			return ev;

		mRotateMatrix.setRotate(-getMapOrientation(), this.getWidth() / 2, this.getHeight() / 2);

		MotionEvent rotatedEvent = MotionEvent.obtain(ev);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			mRotatePoints[0] = ev.getX();
			mRotatePoints[1] = ev.getY();
			mRotateMatrix.mapPoints(mRotatePoints);
			rotatedEvent.setLocation(mRotatePoints[0], mRotatePoints[1]);
		} else {
			// This method is preferred since it will rotate historical touch events too
			try {
				if (sMotionEventTransformMethod == null) {
					sMotionEventTransformMethod = MotionEvent.class.getDeclaredMethod("transform",
							new Class[] { Matrix.class });
				}
				sMotionEventTransformMethod.invoke(rotatedEvent, mRotateMatrix);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return rotatedEvent;
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			if (mScroller.isFinished()) {
				// One last scrollTo to get to the final destination
				scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
				// This will facilitate snapping-to any Snappable points.
				setZoomLevel(mZoomLevel);
				mIsFlinging = false;
			} else {
				scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			}
			postInvalidate(); // Keep on drawing until the animation has
			// finished.
		}
	}

	@Override
	public void scrollTo(int x, int y) {
		final int worldSize_2 = TileSystem.MapSize(this.getZoomLevel(false)) / 2;
		while (x < -worldSize_2) {
			x += worldSize_2 * 2;
		}
		while (x > worldSize_2) {
			x -= worldSize_2 * 2;
		}
		while (y < -worldSize_2) {
			y += worldSize_2 * 2;
		}
		while (y > worldSize_2) {
			y -= worldSize_2 * 2;
		}

		if (mScrollableAreaLimit != null) {
			final int zoomDiff = MapViewConstants.MAXIMUM_ZOOMLEVEL - getZoomLevel(false);
			final int minX = (mScrollableAreaLimit.left >> zoomDiff);
			final int minY = (mScrollableAreaLimit.top >> zoomDiff);
			final int maxX = (mScrollableAreaLimit.right >> zoomDiff);
			final int maxY = (mScrollableAreaLimit.bottom >> zoomDiff);

			final int scrollableWidth = maxX - minX;
			final int scrollableHeight = maxY - minY;
			final int width = this.getWidth();
			final int height = this.getHeight();

			// Adjust if we are outside the scrollable area
			if (scrollableWidth <= width) {
				if (x - (width / 2) > minX)
					x = minX + (width / 2);
				else if (x + (width / 2) < maxX)
					x = maxX - (width / 2);
			} else if (x - (width / 2) < minX)
				x = minX + (width / 2);
			else if (x + (width / 2) > maxX)
				x = maxX - (width / 2);

			if (scrollableHeight <= height) {
				if (y - (height / 2) > minY)
					y = minY + (height / 2);
				else if (y + (height / 2) < maxY)
					y = maxY - (height / 2);
			} else if (y - (height / 2) < minY)
				y = minY + (height / 2);
			else if (y + (height / 2) > maxY)
				y = maxY - (height / 2);
		}
		super.scrollTo(x, y);

		// do callback on listener
		if (mListener != null) {
			final ScrollEvent event = new ScrollEvent(this, x, y);
			mListener.onScroll(event);
		}
	}

	@Override
	public void setBackgroundColor(final int pColor) {
		//TODO: mMapOverlay.setLoadingBackgroundColor(pColor);
		invalidate();
	}

	/**
	 * This will "invert" the canvas for an overlay so that it is appropriate for fixed-screen
	 * drawing where (0, 0) is the upper-left corner of the screen. You can use this to draw to a
	 * fixed position on the screen regardless of the scroll position.<br/>
	 * <br/>
	 * <b>Note:</b> You should save the state of your canvas before calling this method and restore
	 * it when you are done drawing. To use this with a {@link SafeTranslatedCanvas} you should use
	 * it with {@link SafeTranslatedCanvas#getUnsafeCanvas(UnsafeCanvasHandler)}.
	 * 
	 * @param c
	 *            a canvas which will be modified
	 */
	public void invertCanvas(final Canvas c) {
		c.rotate(-mapOrientation, mProjection.getScreenRect().exactCenterX(), mProjection
				.getScreenRect().exactCenterY());
		c.scale(1 / mMultiTouchScale, 1 / mMultiTouchScale, mMultiTouchScalePoint.x,
				mMultiTouchScalePoint.y);
		c.translate(getScrollX(), getScrollY());
		c.translate(-getWidth() / 2, -getHeight() / 2);
	}

	@Override
	protected void dispatchDraw(final Canvas c) {
		final long startMs = System.currentTimeMillis();

		mProjection = new Projection();

		// Save the current canvas matrix
		c.save();

		c.translate(getWidth() / 2, getHeight() / 2);
		c.scale(mMultiTouchScale, mMultiTouchScale, mMultiTouchScalePoint.x,
				mMultiTouchScalePoint.y);

		/* rotate Canvas */
		c.rotate(mapOrientation, mProjection.getScreenRect().exactCenterX(), mProjection
				.getScreenRect().exactCenterY());

		/* Draw background */
		// c.drawColor(mBackgroundColor);

		/* Draw all Overlays. */
		this.getOverlayManager().draw(c, this);

		// Restore the canvas matrix
		c.restore();

		super.dispatchDraw(c);

		if (DEBUGMODE) {
			final long endMs = System.currentTimeMillis();
			Log.d(OSMD_TAG, "Rendering overall: " + (endMs - startMs) + "ms");
		}
	}

	/**
	 * Returns true if the safe drawing canvas is being used.
	 *
	 * @see {@link ISafeCanvas}
	 */
	public boolean isUsingSafeCanvas() {
		return this.getOverlayManager().isUsingSafeCanvas();
	}

	/**
	 * Sets whether the safe drawing canvas is being used.
	 *
	 * @see {@link ISafeCanvas}
	 */
	public void setUseSafeCanvas(boolean useSafeCanvas) {
		this.getOverlayManager().setUseSafeCanvas(useSafeCanvas);
	}

	@Override
	protected void onDetachedFromWindow() {
		this.onDetach();
		super.onDetachedFromWindow();
	}

	// ===========================================================
	// Animation
	// ===========================================================

	/**
	 * Determines if maps are animating a zoom operation. Useful for overlays to avoid recalculating
	 * during an animation sequence.
	 *
	 * @return boolean indicating whether view is animating.
	 */
	public boolean isAnimating() {
		return mIsAnimating.get();
	}

	// ===========================================================
	// Implementation of MultiTouchObjectCanvas
	// ===========================================================

	@Override
	public Object getDraggableObjectAtPoint(final PointInfo pt) {
		if (this.isAnimating()) {
			// Zoom animations use the mMultiTouchScale variables to perform their animations so we
			// don't want to step on that.
			return null;
		} else {
			mMultiTouchScalePoint.x = pt.getX() + getScrollX() - (this.getWidth() / 2);
			mMultiTouchScalePoint.y = pt.getY() + getScrollY() - (this.getHeight() / 2);
			return this;
		}
	}

	@Override
	public void getPositionAndScale(final Object obj, final PositionAndScale objPosAndScaleOut) {
		objPosAndScaleOut.set(0, 0, true, mMultiTouchScale, false, 0, 0, false, 0);
	}

	@Override
	public void selectObject(final Object obj, final PointInfo pt) {
		// if obj is null it means we released the pointers
		// if scale is not 1 it means we pinched
		if (obj == null && mMultiTouchScale != 1.0f) {
			final float scaleDiffFloat = (float) (Math.log(mMultiTouchScale) * ZOOM_LOG_BASE_INV);
			final int scaleDiffInt = Math.round(scaleDiffFloat);
			// If we are changing zoom levels,
			// adjust the center point in respect to the scaling point
			if (scaleDiffInt != 0) {
				Matrix m = new Matrix();
				m.setScale(1 / mMultiTouchScale, 1 / mMultiTouchScale, mMultiTouchScalePoint.x,
						mMultiTouchScalePoint.y);
				m.postRotate(-mapOrientation, mProjection.getScreenRect().centerX(), mProjection
						.getScreenRect().centerY());
				float[] pts = new float[2];
				pts[0] = getScrollX();
				pts[1] = getScrollY();
				m.mapPoints(pts);
				scrollTo((int) pts[0], (int) pts[1]);
			}

			// Adjust the zoomLevel
			setZoomLevel(mZoomLevel + scaleDiffInt);
		}

		// reset scale
		mMultiTouchScale = 1.0f;
	}

	@Override
	public boolean setPositionAndScale(final Object obj, final PositionAndScale aNewObjPosAndScale,
			final PointInfo aTouchPoint) {
		float multiTouchScale = aNewObjPosAndScale.getScale();
		// If we are at the first or last zoom level, prevent pinching/expanding
		if (multiTouchScale > 1 && !canZoomIn()) {
			multiTouchScale = 1;
		}
		if (multiTouchScale < 1 && !canZoomOut()) {
			multiTouchScale = 1;
		}
		mMultiTouchScale = multiTouchScale;
		invalidate(); // redraw
		return true;
	}

	/*
	 * Set the MapListener for this view
	 */
	public void setMapListener(final MapListener ml) {
		mListener = ml;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private void checkZoomButtons() {
		if(mZoomController != null){
			mZoomController.setZoomInEnabled(canZoomIn());
			mZoomController.setZoomOutEnabled(canZoomOut());
			mZoomController.onZoomChanged();
		}		
	}

	public void setZoomController(IZoomController mZoomController) {
		this.mZoomController = mZoomController;
		checkZoomButtons();
	}

	public void setMultiTouchControls(final boolean on) {
		mMultiTouchController = on ? new MultiTouchController<Object>(this, false) : null;
	}



	/**
	 * A Projection serves to translate between the coordinate system of x/y on-screen pixel
	 * coordinates and that of latitude/longitude points on the surface of the earth. You obtain a
	 * Projection from MapView.getProjection(). You should not hold on to this object for more than
	 * one draw, since the projection of the map could change. <br />
	 * <br />
	 * <I>Screen coordinates</I> are in the coordinate system of the screen's Canvas. The origin is
	 * in the center of the plane. <I>Screen coordinates</I> are appropriate for using to draw to
	 * the screen.<br />
	 * <br />
	 * <I>MapBase coordinates</I> are in the coordinate system of the standard Mercator projection. The
	 * origin is in the upper-left corner of the plane. <I>MapBase coordinates</I> are appropriate for
	 * use in the TileSystem class.<br />
	 * <br />
	 * <I>Intermediate coordinates</I> are used to cache the computationally heavy part of the
	 * projection. They aren't suitable for use until translated into <I>screen coordinates</I> or
	 * <I>map coordinates</I>.
	 *
	 * @author Nicolas Gramlich
	 * @author Manuel Stahl
	 */
	public class Projection implements IProjection, GeoConstants {

		private final int viewWidth_2 = getWidth() / 2;
		private final int viewHeight_2 = getHeight() / 2;
		private final int worldSize_2 = TileSystem.MapSize(mZoomLevel) / 2;
		private final int offsetX = -worldSize_2;
		private final int offsetY = -worldSize_2;

		private final BoundingBoxE6 mBoundingBoxProjection;
		private final int mZoomLevelProjection;
		private final Rect mScreenRectProjection;
		private final Rect mIntrinsicScreenRectProjection;
		private final float mMapOrientation;

		private Projection() {

			/*
			 * Do some calculations and drag attributes to local variables to save some performance.
			 */
			mZoomLevelProjection = MapView.this.mZoomLevel;
			mBoundingBoxProjection = MapView.this.getBoundingBox();
			mScreenRectProjection = MapView.this.getScreenRect(null);
			mIntrinsicScreenRectProjection = MapView.this.getIntrinsicScreenRect(null);
			mMapOrientation = MapView.this.getMapOrientation();
		}

		public int getZoomLevel() {
			return mZoomLevelProjection;
		}

		public BoundingBoxE6 getBoundingBox() {
			return mBoundingBoxProjection;
		}

		public Rect getScreenRect() {
			return mScreenRectProjection;
		}

		public Rect getIntrinsicScreenRect() {
			return mIntrinsicScreenRectProjection;
		}

		public float getMapOrientation() {
			return mMapOrientation;
		}

		/**
		 * @deprecated Use TileSystem.getTileSize() instead.
		 */
		@Deprecated
		public int getTileSizePixels() {
			return TileSystem.getTileSize();
		}

		/**
		 * @deprecated Use
		 *             <code>Point out = TileSystem.PixelXYToTileXY(screenRect.centerX(), screenRect.centerY(), null);</code>
		 *             instead.
		 */
		@Deprecated
		public Point getCenterMapTileCoords() {
			final Rect rect = getScreenRect();
			return TileSystem.PixelXYToTileXY(rect.centerX(), rect.centerY(), null);
		}

		/**
		 * @deprecated Use
		 *             <code>final Point out = TileSystem.TileXYToPixelXY(centerMapTileCoords.x, centerMapTileCoords.y, null);</code>
		 *             instead.
		 */
		@Deprecated
		public Point getUpperLeftCornerOfCenterMapTile() {
			final Point centerMapTileCoords = getCenterMapTileCoords();
			return TileSystem.TileXYToPixelXY(centerMapTileCoords.x, centerMapTileCoords.y, null);
		}

		/**
		 * Converts <I>screen coordinates</I> to the underlying GeoPoint.
		 *
		 * @param x
		 * @param y
		 * @return GeoPoint under x/y.
		 */
		public IGeoPoint fromPixels(final float x, final float y) {
			final Rect screenRect = getIntrinsicScreenRect();
			return TileSystem.PixelXYToLatLong(screenRect.left + (int) x + worldSize_2,
					screenRect.top + (int) y + worldSize_2, mZoomLevelProjection, null);
		}

		public Point fromMapPixels(final int x, final int y, final Point reuse) {
			final Point out = reuse != null ? reuse : new Point();
			out.set(x - viewWidth_2, y - viewHeight_2);
			out.offset(getScrollX(), getScrollY());
			return out;
		}

		/**
		 * Converts a GeoPoint to its <I>screen coordinates</I>.
		 *
		 * @param in
		 *            the GeoPoint you want the <I>screen coordinates</I> of
		 * @param reuse
		 *            just pass null if you do not have a Point to be 'recycled'.
		 * @return the Point containing the <I>screen coordinates</I> of the GeoPoint passed.
		 */
		public Point toMapPixels(final IGeoPoint in, final Point reuse) {
			final Point out = reuse != null ? reuse : new Point();
			TileSystem.LatLongToPixelXY(
							in.getLatitude(),
							in.getLongitude(),
							getZoomLevel(), out);
			out.offset(offsetX, offsetY);
			if (Math.abs(out.x - getScrollX()) >
					Math.abs(out.x - TileSystem.MapSize(getZoomLevel()) - getScrollX())) {
				out.x -= TileSystem.MapSize(getZoomLevel());
			}
			if (Math.abs(out.x - getScrollX()) >
					Math.abs(out.x + TileSystem.MapSize(getZoomLevel()) - getScrollX())) {
				out.x += TileSystem.MapSize(getZoomLevel());
			}
			if (Math.abs(out.y - getScrollY()) >
					Math.abs(out.y - TileSystem.MapSize(getZoomLevel()) - getScrollY())) {
				out.y -= TileSystem.MapSize(getZoomLevel());
			}
			if (Math.abs(out.y - getScrollY()) >
					Math.abs(out.y + TileSystem.MapSize(getZoomLevel()) - getScrollY())) {
				out.y += TileSystem.MapSize(getZoomLevel());
			}
			return out;
		}

		/**
		 * Performs only the first computationally heavy part of the projection. Call
		 * toMapPixelsTranslated to get the final position.
		 *
		 * @param latituteE6
		 *            the latitute of the point
		 * @param longitudeE6
		 *            the longitude of the point
		 * @param reuse
		 *            just pass null if you do not have a Point to be 'recycled'.
		 * @return intermediate value to be stored and passed to toMapPixelsTranslated.
		 */
		public Point toMapPixelsProjected(final int latituteE6, final int longitudeE6, final Point reuse) {
			final Point out = reuse != null ? reuse : new Point();

			TileSystem.LatLongToPixelXY(latituteE6 * 1E-6, longitudeE6 * 1E-6, MAXIMUM_ZOOMLEVEL, out);
			return out;
		}

		/**
		 * Performs the second computationally light part of the projection. Returns results in
		 * <I>screen coordinates</I>.
		 *
		 * @param in
		 *            the Point calculated by the toMapPixelsProjected
		 * @param reuse
		 *            just pass null if you do not have a Point to be 'recycled'.
		 * @return the Point containing the <I>Screen coordinates</I> of the initial GeoPoint passed
		 *         to the toMapPixelsProjected.
		 */
		public Point toMapPixelsTranslated(final Point in, final Point reuse) {
			final Point out = reuse != null ? reuse : new Point();

			final int zoomDifference = MAXIMUM_ZOOMLEVEL - getZoomLevel();
			out.set((in.x >> zoomDifference) + offsetX, (in.y >> zoomDifference) + offsetY);
			return out;
		}

		/**
		 * Translates a rectangle from <I>screen coordinates</I> to <I>intermediate coordinates</I>.
		 *
		 * @param in
		 *            the rectangle in <I>screen coordinates</I>
		 * @return a rectangle in </I>intermediate coordindates</I>.
		 */
		public Rect fromPixelsToProjected(final Rect in) {
			final Rect result = new Rect();

			final int zoomDifference = MAXIMUM_ZOOMLEVEL - getZoomLevel();

			final int x0 = in.left - offsetX << zoomDifference;
			final int x1 = in.right - offsetX << zoomDifference;
			final int y0 = in.bottom - offsetY << zoomDifference;
			final int y1 = in.top - offsetY << zoomDifference;

			result.set(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1));
			return result;
		}

		// not presently used
		public Rect toPixels(final BoundingBoxE6 pBoundingBoxE6) {
			final Rect rect = new Rect();

			final Point reuse = new Point();

			toMapPixels(
					new GeoPoint(pBoundingBoxE6.getLatNorthE6(), pBoundingBoxE6.getLonWestE6()),
					reuse);
			rect.left = reuse.x;
			rect.top = reuse.y;

			toMapPixels(
					new GeoPoint(pBoundingBoxE6.getLatSouthE6(), pBoundingBoxE6.getLonEastE6()),
					reuse);
			rect.right = reuse.x;
			rect.bottom = reuse.y;

			return rect;
		}

		@Override
		public float metersToEquatorPixels(final float meters) {
			return meters / (float) TileSystem.GroundResolution(0, mZoomLevelProjection);
		}

		@Override
		public IGeoPoint getNorthEast() {
			return fromPixels(getWidth(), 0);
		}

		@Override
		public IGeoPoint getSouthWest() {
			return fromPixels(0, getHeight());
		}

		@Override
		public Point toPixels(final IGeoPoint in, final Point out) {
			return toMapPixels(in, out);
		}

		@Override
		public IGeoPoint fromPixels(final int x, final int y) {
			return fromPixels((float) x, (float) y);
		}
	}

	private class MapViewGestureDetectorListener implements OnGestureListener {

		@Override
		public boolean onDown(final MotionEvent e) {

			// Stop scrolling if we are in the middle of a fling!
			if (mIsFlinging) {
				mScroller.abortAnimation();
				mIsFlinging = false;
			}

			if (MapView.this.getOverlayManager().onDown(e, MapView.this)) {
				return true;
			}

			if(mZoomController != null){
				mZoomController.setVisible(true);
			}
			return true;
		}

		@Override
		public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX,
				final float velocityY) {
			if (MapView.this.getOverlayManager()
					.onFling(e1, e2, velocityX, velocityY, MapView.this)) {
				return true;
			}

			final int worldSize = TileSystem.MapSize(MapView.this.getZoomLevel(false));
			mIsFlinging = true;
			mScroller.fling(getScrollX(), getScrollY(), (int) -velocityX, (int) -velocityY,
                    -worldSize, worldSize, -worldSize, worldSize);
			return true;
		}

		@Override
		public void onLongPress(final MotionEvent e) {
			if (mMultiTouchController != null && mMultiTouchController.isPinching()) {
				return;
			}
			MapView.this.getOverlayManager().onLongPress(e, MapView.this);
		}

		@Override
		public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,
				final float distanceY) {
			if (MapView.this.getOverlayManager().onScroll(e1, e2, distanceX, distanceY,
					MapView.this)) {
				return true;
			}

			scrollBy((int) distanceX, (int) distanceY);
			return true;
		}

		@Override
		public void onShowPress(final MotionEvent e) {
			MapView.this.getOverlayManager().onShowPress(e, MapView.this);
		}

		@Override
		public boolean onSingleTapUp(final MotionEvent e) {
			if (MapView.this.getOverlayManager().onSingleTapUp(e, MapView.this)) {
				return true;
			}

			return false;
		}

	}

	private class MapViewDoubleClickListener implements GestureDetector.OnDoubleTapListener {
		@Override
		public boolean onDoubleTap(final MotionEvent e) {
			if (MapView.this.getOverlayManager().onDoubleTap(e, MapView.this)) {
				return true;
			}

			final IGeoPoint center = getProjection().fromPixels(e.getX(), e.getY());
			return zoomInFixing(center);
		}

		@Override
		public boolean onDoubleTapEvent(final MotionEvent e) {
			if (MapView.this.getOverlayManager().onDoubleTapEvent(e, MapView.this)) {
				return true;
			}

			return false;
		}

		@Override
		public boolean onSingleTapConfirmed(final MotionEvent e) {
			if (MapView.this.getOverlayManager().onSingleTapConfirmed(e, MapView.this)) {
				return true;
			}

			return false;
		}
	}

	private class MapViewZoomListener implements OnZoomListener {
		@Override
		public void onZoom(final boolean zoomIn) {
			if (zoomIn) {
				getController().zoomIn();
			} else {
				getController().zoomOut();
			}
		}

		@Override
		public void onVisibilityChanged(final boolean visible) {
		}
	}

	// ===========================================================
	// Public Classes
	// ===========================================================

	/**
	 * Per-child layout information associated with OpenStreetMapView.
	 */
	public static class LayoutParams extends ViewGroup.LayoutParams {

		/**
		 * Special value for the alignment requested by a View. TOP_LEFT means that the location
		 * will at the top left the View.
		 */
		public static final int TOP_LEFT = 1;
		/**
		 * Special value for the alignment requested by a View. TOP_RIGHT means that the location
		 * will be centered at the top of the View.
		 */
		public static final int TOP_CENTER = 2;
		/**
		 * Special value for the alignment requested by a View. TOP_RIGHT means that the location
		 * will at the top right the View.
		 */
		public static final int TOP_RIGHT = 3;
		/**
		 * Special value for the alignment requested by a View. CENTER_LEFT means that the location
		 * will at the center left the View.
		 */
		public static final int CENTER_LEFT = 4;
		/**
		 * Special value for the alignment requested by a View. CENTER means that the location will
		 * be centered at the center of the View.
		 */
		public static final int CENTER = 5;
		/**
		 * Special value for the alignment requested by a View. CENTER_RIGHT means that the location
		 * will at the center right the View.
		 */
		public static final int CENTER_RIGHT = 6;
		/**
		 * Special value for the alignment requested by a View. BOTTOM_LEFT means that the location
		 * will be at the bottom left of the View.
		 */
		public static final int BOTTOM_LEFT = 7;
		/**
		 * Special value for the alignment requested by a View. BOTTOM_CENTER means that the
		 * location will be centered at the bottom of the view.
		 */
		public static final int BOTTOM_CENTER = 8;
		/**
		 * Special value for the alignment requested by a View. BOTTOM_RIGHT means that the location
		 * will be at the bottom right of the View.
		 */
		public static final int BOTTOM_RIGHT = 9;
		/**
		 * The location of the child within the map view.
		 */
		public IGeoPoint geoPoint;

		/**
		 * The alignment the alignment of the view compared to the location.
		 */
		public int alignment;

		public int offsetX;
		public int offsetY;

		/**
		 * Creates a new set of layout parameters with the specified width, height and location.
		 *
		 * @param width
		 *            the width, either {@link #FILL_PARENT}, {@link #WRAP_CONTENT} or a fixed size
		 *            in pixels
		 * @param height
		 *            the height, either {@link #FILL_PARENT}, {@link #WRAP_CONTENT} or a fixed size
		 *            in pixels
		 * @param geoPoint
		 *            the location of the child within the map view
		 * @param alignment
		 *            the alignment of the view compared to the location {@link #BOTTOM_CENTER},
		 *            {@link #BOTTOM_LEFT}, {@link #BOTTOM_RIGHT} {@link #TOP_CENTER},
		 *            {@link #TOP_LEFT}, {@link #TOP_RIGHT}
		 * @param offsetX
		 *            the additional X offset from the alignment location to draw the child within
		 *            the map view
		 * @param offsetY
		 *            the additional Y offset from the alignment location to draw the child within
		 *            the map view
		 */
		public LayoutParams(final int width, final int height, final IGeoPoint geoPoint,
				final int alignment, final int offsetX, final int offsetY) {
			super(width, height);
			if (geoPoint != null) {
				this.geoPoint = geoPoint;
			} else {
				this.geoPoint = new GeoPoint(0, 0);
			}
			this.alignment = alignment;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}

		/**
		 * Since we cannot use XML files in this project this constructor is useless. Creates a new
		 * set of layout parameters. The values are extracted from the supplied attributes set and
		 * context.
		 *
		 * @param c
		 *            the application environment
		 * @param attrs
		 *            the set of attributes fom which to extract the layout parameters values
		 */
		public LayoutParams(final Context c, final AttributeSet attrs) {
			super(c, attrs);
			this.geoPoint = new GeoPoint(0, 0);
			this.alignment = BOTTOM_CENTER;
		}

		/**
		 * {@inheritDoc}
		 */
		public LayoutParams(final ViewGroup.LayoutParams source) {
			super(source);
		}
	}

	public int getLoadingBackgroundColor() {
		return mLoadingBackgroundColor;
	}

	/**
	 * Set the color to use to draw the background while we're waiting for the tile to load.
	 *
	 * @param pLoadingBackgroundColor
	 *            the color to use. If the value is {@link Color#TRANSPARENT} then there will be no
	 *            loading tile.
	 */
	public void setLoadingBackgroundColor(final int pLoadingBackgroundColor) {
		if (mLoadingBackgroundColor != pLoadingBackgroundColor) {
			mLoadingBackgroundColor = pLoadingBackgroundColor;
			clearLoadingTile();
		}
	}

	public int getLoadingLineColor() {
		return mLoadingLineColor;
	}

	public void setLoadingLineColor(final int pLoadingLineColor) {
		if (mLoadingLineColor != pLoadingLineColor) {
			mLoadingLineColor = pLoadingLineColor;
			clearLoadingTile();
		}
	}

	private Drawable getLoadingTile() {
		if (mLoadingTile == null && mLoadingBackgroundColor != Color.TRANSPARENT) {
			try {
				final int tileSize = mTileProvider.getTileSource() != null ? mTileProvider
						.getTileSource().getTileSizePixels() : 256;
				final Bitmap bitmap = Bitmap.createBitmap(tileSize, tileSize,
						Bitmap.Config.ARGB_8888);
				final Canvas canvas = new Canvas(bitmap);
				final Paint paint = new Paint();
				canvas.drawColor(mLoadingBackgroundColor);
				paint.setColor(mLoadingLineColor);
				paint.setStrokeWidth(0);
				final int lineSize = tileSize / 16;
				for (int a = 0; a < tileSize; a += lineSize) {
					canvas.drawLine(0, a, tileSize, a, paint);
					canvas.drawLine(a, 0, a, tileSize, paint);
				}
				mLoadingTile = new BitmapDrawable(bitmap);
			} catch (final OutOfMemoryError e) {
				Log.e(OSMD_TAG, "OutOfMemoryError getting loading tile");
				System.gc();
			}
		}
		return mLoadingTile;
	}

	private void clearLoadingTile() {
		final BitmapDrawable bitmapDrawable = mLoadingTile;
		mLoadingTile = null;
		// Only recycle if we are running on a project less than 2.3.3 Gingerbread.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
			if (bitmapDrawable != null) {
				bitmapDrawable.getBitmap().recycle();
			}
		}
	}	
}
