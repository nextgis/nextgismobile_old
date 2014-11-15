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

public class NgwConnection {

    public static final int LOAD_PARENT = 1;
    public static final int LOAD_RESOURCE = 2;
    public static final int LOAD_CHILDREN = 3;

    protected String mName;
    protected String mUrl;
    protected String mLogin;
    protected String mPassword;

    protected NgwResource mRootNgwResource;
    protected NgwResource mCurrentNgwResource;

    protected Integer mChildId;

    protected int mThatLoad;


    public NgwConnection(String name, String url, String login, String password) {
        mName = name;
        mUrl = url;
        mLogin = login;
        mPassword = password;

        mCurrentNgwResource = null;
        mChildId = 0;

        mThatLoad = LOAD_RESOURCE;

        mRootNgwResource = new NgwResource(null);
    }

    public String getName() {
        return mName;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getLogin() {
        return mLogin;
    }

    public String getPassword() {
        return mPassword;
    }

    public NgwResource getRootNgwResource() {
        return mRootNgwResource;
    }

    public NgwResource getCurrentNgwResource() {
        return mCurrentNgwResource;
    }

    public String getLoadUrl() {
        switch (mThatLoad) {
            case LOAD_PARENT:
                return getParentArrayUrl();
            case LOAD_RESOURCE:
            default:
                return getResourceArrayUrl();
            case LOAD_CHILDREN:
                return getChildObjectUrl();
        }
    }

    public String getParentArrayUrl() {
        return mUrl + "/resource/"
                + (mCurrentNgwResource == null
                || mCurrentNgwResource.mParent == null || mCurrentNgwResource.mParent.mId == null
                ? "-"
                : mCurrentNgwResource.mParent.mId)
                + "/child/";
    }

    public String getResourceArrayUrl() {
        return mUrl + "/resource/"
                + (mCurrentNgwResource == null || mCurrentNgwResource.mId == null
                ? "-"
                : mCurrentNgwResource.mId)
                + "/child/";
    }

    public String getChildObjectUrl() {
        return mUrl + "/resource/"
                + (mCurrentNgwResource == null || mCurrentNgwResource.mId == null
                ? "-"
                : mCurrentNgwResource.mId)
                + "/child/" + mChildId;
    }

    public void setLoadRootArray() {
        setLoadResourceArray(mRootNgwResource);
    }

    public void setLoadRootObject() {
        setLoadChildrenObject(mRootNgwResource, 0);
    }

    public void setLoadParentArray(NgwResource resource) {
        mCurrentNgwResource = resource;
        mThatLoad = LOAD_PARENT;
    }

    public void setLoadResourceArray(NgwResource resource) {
        mCurrentNgwResource = resource;
        mThatLoad = LOAD_RESOURCE;
    }

    public void setLoadChildrenObject(NgwResource resource, Integer childId) {
        mCurrentNgwResource = resource;
        mChildId = childId;
        mThatLoad = LOAD_CHILDREN;
    }

    public boolean isForJsonArray() {
        switch (mThatLoad) {

            case LOAD_PARENT:
            case LOAD_RESOURCE:
            default:
                return true;

            case LOAD_CHILDREN:
                return false;
        }
    }
}
