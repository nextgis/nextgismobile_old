package org.osmdroid.views.overlay;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.ReusableBitmapDrawable;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.TileLooper;
import org.osmdroid.util.TileSystem;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.safecanvas.ISafeCanvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

import static org.osmdroid.constants.UtilConstants.*;

/**
 * These objects are the principle consumer of map tiles.
 *
 * see {@link MapTile} for an overview of how tiles are acquired by this overlay.
 *
 */

public class TilesOverlay extends SafeDrawOverlay{


	public static final int MENU_MAP_MODE = getSafeMenuId();
	public static final int MENU_TILE_SOURCE_STARTING_ID = getSafeMenuIdSequence(TileSourceFactory
			.getTileSources().size());
	public static final int MENU_OFFLINE = getSafeMenuId();

	/** Current tile source */
	protected final MapTileProviderBase mTileProvider;

	/* to avoid allocations during draw */
	protected final Paint mDebugPaint = new Paint();
	private final Rect mTileRect = new Rect();
	private final Rect mViewPort = new Rect();

	private boolean mOptionsMenuEnabled = true;

	private int mWorldSize_2;

	/** For overshooting the tile cache **/
	private int mOvershootTileCache = 0;

	public TilesOverlay(final MapTileProviderBase aTileProvider, final Context aContext) {
		this(aTileProvider, new DefaultResourceProxyImpl(aContext));
	}

	public TilesOverlay(final MapTileProviderBase aTileProvider, final ResourceProxy pResourceProxy) {
		super(pResourceProxy);
		if (aTileProvider == null) {
			throw new IllegalArgumentException(
					"You must pass a valid tile provider to the tiles overlay.");
		}
		this.mTileProvider = aTileProvider;
	}

	@Override
	public void onDetach(final MapView pMapView) {
		this.mTileProvider.detach();
	}

	public int getMinimumZoomLevel() {
		return mTileProvider.getMinimumZoomLevel();
	}

	public int getMaximumZoomLevel() {
		return mTileProvider.getMaximumZoomLevel();
	}

	/**
	 * Whether to use the network connection if it's available.
	 */
	public boolean useDataConnection() {
		return mTileProvider.useDataConnection();
	}

	/**
	 * Set whether to use the network connection if it's available.
	 *
	 * @param aMode
	 *            if true use the network connection if it's available. if false don't use the
	 *            network connection even if it's available.
	 */
	public void setUseDataConnection(final boolean aMode) {
		mTileProvider.setUseDataConnection(aMode);
	}

	@Override
	protected void drawSafe(final ISafeCanvas c, final MapView osmv, final boolean shadow) {

		if (DEBUGMODE) {
			Log.v(OSMD_TAG, "onDraw(" + shadow + ")");
		}

		if (shadow) {
			return;
		}

		// Calculate the half-world size
		final Projection pj = osmv.getProjection();
		final int zoomLevel = pj.getZoomLevel();
		mWorldSize_2 = TileSystem.MapSize(zoomLevel) >> 1;

		// Get the area we are drawing to
		mViewPort.set(pj.getScreenRect());

		// Translate the Canvas coordinates into Mercator coordinates
		mViewPort.offset(mWorldSize_2, mWorldSize_2);

		// Draw the tiles!
		drawTiles(c.getSafeCanvas(), pj.getZoomLevel(), TileSystem.getTileSize(), mViewPort);
	}

	/**
	 * This is meant to be a "pure" tile drawing function that doesn't take into account
	 * osmdroid-specific characteristics (like osmdroid's canvas's having 0,0 as the center rather
	 * than the upper-left corner). Once the tile is ready to be drawn, it is passed to
	 * onTileReadyToDraw where custom manipulations can be made before drawing the tile.
	 */
	public void drawTiles(final Canvas c, final int zoomLevel, final int tileSizePx,
			final Rect viewPort) {

		mTileLooper.loop(c, zoomLevel, tileSizePx, viewPort);

		// draw a cross at center in debug mode
		if (DEBUGMODE) {
			// final GeoPoint center = osmv.getMapCenter();
			final Point centerPoint = new Point(viewPort.centerX() - mWorldSize_2,
					viewPort.centerY() - mWorldSize_2);
			c.drawLine(centerPoint.x, centerPoint.y - 9, centerPoint.x, centerPoint.y + 9, mDebugPaint);
			c.drawLine(centerPoint.x - 9, centerPoint.y, centerPoint.x + 9, centerPoint.y, mDebugPaint);
		}

	}

	private final TileLooper mTileLooper = new TileLooper() {
		@Override
		public void initialiseLoop(final int pZoomLevel, final int pTileSizePx) {
			// make sure the cache is big enough for all the tiles
			final int numNeeded = (mLowerRight.y - mUpperLeft.y + 1) * (mLowerRight.x - mUpperLeft.x + 1);
			mTileProvider.ensureCapacity(numNeeded + mOvershootTileCache);
		}
		@Override
		public void handleTile(final Canvas pCanvas, final int pTileSizePx, final MapTile pTile, final int pX, final int pY) {
			Drawable currentMapTile = mTileProvider.getMapTile(pTile);
			boolean isReusable = currentMapTile instanceof ReusableBitmapDrawable;
			final ReusableBitmapDrawable reusableBitmapDrawable =
					isReusable ? (ReusableBitmapDrawable) currentMapTile : null;
			if (currentMapTile == null) {
				currentMapTile = getLoadingTile();
			}

			if (currentMapTile != null) {
				mTileRect.set(pX * pTileSizePx, pY * pTileSizePx, pX * pTileSizePx + pTileSizePx,
						pY * pTileSizePx + pTileSizePx);
				if (isReusable) {
					reusableBitmapDrawable.beginUsingDrawable();
				}
				try {
					if (isReusable && !((ReusableBitmapDrawable) currentMapTile).isBitmapValid()) {
						currentMapTile = getLoadingTile();
						isReusable = false;
					}
					onTileReadyToDraw(pCanvas, currentMapTile, mTileRect);
				} finally {
					if (isReusable)
						reusableBitmapDrawable.finishUsingDrawable();
				}
			}

			if (DEBUGMODE) {
				mTileRect.set(pX * pTileSizePx, pY * pTileSizePx, pX * pTileSizePx + pTileSizePx, pY
						* pTileSizePx + pTileSizePx);
				mTileRect.offset(-mWorldSize_2, -mWorldSize_2);
				pCanvas.drawText(pTile.toString(), mTileRect.left + 1,
						mTileRect.top + mDebugPaint.getTextSize(), mDebugPaint);
				pCanvas.drawLine(mTileRect.left, mTileRect.top, mTileRect.right, mTileRect.top,
						mDebugPaint);
				pCanvas.drawLine(mTileRect.left, mTileRect.top, mTileRect.left, mTileRect.bottom,
						mDebugPaint);
			}
		}
		@Override
		public void finaliseLoop() {
		}
	};

	protected void onTileReadyToDraw(final Canvas c, final Drawable currentMapTile,
			final Rect tileRect) {
		tileRect.offset(-mWorldSize_2, -mWorldSize_2);
		currentMapTile.setBounds(tileRect);
		currentMapTile.draw(c);
	}


	/**
	 * Set this to overshoot the tile cache. By default the TilesOverlay only creates a cache large
	 * enough to hold the minimum number of tiles necessary to draw to the screen. Setting this
	 * value will allow you to overshoot the tile cache and allow more tiles to be cached. This
	 * increases the memory usage, but increases drawing performance.
	 *
	 * @param overshootTileCache
	 *            the number of tiles to overshoot the tile cache by
	 */
	public void setOvershootTileCache(int overshootTileCache) {
		mOvershootTileCache = overshootTileCache;
	}

	/**
	 * Get the tile cache overshoot value.
	 *
	 * @return the number of tiles to overshoot tile cache
	 */
	public int getOvershootTileCache() {
		return mOvershootTileCache;
	}
}
