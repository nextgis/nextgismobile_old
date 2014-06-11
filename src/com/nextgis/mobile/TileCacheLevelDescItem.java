/******************************************************************************
 * Project:  NextGIS mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), polimax@mail.ru
 ******************************************************************************
*   Copyright (C) 2014 NextGIS
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
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
package com.nextgis.mobile;

import android.os.Parcel;
import android.os.Parcelable;

public class TileCacheLevelDescItem implements Parcelable {
	int minY, maxY;
	int minX, maxX;
	
	public TileCacheLevelDescItem(int nMaxX, int nMinX, int nMaxY, int nMinY) {
		this.minX = nMinX;
		this.minY = nMinY;
		this.maxX = nMaxX;
		this.maxY = nMaxY;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(minX);
		dest.writeInt(minY);
		dest.writeInt(maxX);
		dest.writeInt(maxY);		
	}
	
	public int getMinX(){
		return minX;
	}
	
	public int getMinY(){
		return minY;
	}
	
	public int getMaxX(){
		return maxX;
	}
	
	public int getMaxY(){
		return maxY;
	}
	
	public boolean isInside(int nX, int nY){
		if(nX < minX || nX > maxX)
			return false;
		if(nY < minY || nY > maxY)
			return false;
		return true;
	}
	
	public static final Parcelable.Creator<TileCacheLevelDescItem> CREATOR
    = new Parcelable.Creator<TileCacheLevelDescItem>() {
	    public TileCacheLevelDescItem createFromParcel(Parcel in) {
	        return new TileCacheLevelDescItem(in);
	    }
	
	    public TileCacheLevelDescItem[] newArray(int size) {
	        return new TileCacheLevelDescItem[size];
	    }
	};
	
	private TileCacheLevelDescItem(Parcel in) {
		minX = in.readInt();
		minY = in.readInt();
		maxX = in.readInt();
		maxY = in.readInt();
	}

}
