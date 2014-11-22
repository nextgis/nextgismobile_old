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
package com.nextgis.mobile.datasource;

import java.util.ArrayList;
import java.util.List;

public class NgwConnectionList {

    protected List<NgwConnection> mNgwConnections;

    public NgwConnectionList() {
        mNgwConnections = new ArrayList<NgwConnection>();
    }

    public int size() {
        return mNgwConnections.size();
    }

    public boolean add(NgwConnection connection) {
        return mNgwConnections.add(connection);
    }

    public NgwConnection remove(int location) {
        return mNgwConnections.remove(location);
    }

    public boolean removeByConnectionId(int connectionId) {
        for (NgwConnection resourceRoot : mNgwConnections) {
            if (resourceRoot.getId() == connectionId) {
                return mNgwConnections.remove(resourceRoot);
            }
        }
        return false;
    }

    public NgwConnection get(int location) {
        return mNgwConnections.get(location);
    }

    public NgwConnection getByConnectionId(int connectionId) {
        for (NgwConnection connection : mNgwConnections) {
            if (connection.getId() == connectionId) {
                return connection;
            }
        }
        return null;
    }
}
