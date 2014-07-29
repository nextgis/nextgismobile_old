package org.osmdroid.api;

public interface IZoomController {
	public void setVisible(boolean bVisible);
	public void setZoomInEnabled(boolean bEnabled);
	public void setZoomOutEnabled(boolean bEnabled);
	public void onZoomChanged();
}
