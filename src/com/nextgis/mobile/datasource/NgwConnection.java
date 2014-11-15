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

import java.util.Random;

public class NgwConnection implements Comparable<NgwConnection> {

    public static final int LOAD_PARENT = 1;
    public static final int LOAD_RESOURCE = 2;
    public static final int LOAD_CHILDREN = 3;

    protected Integer mId;
    protected String mName;
    protected String mUrl;
    protected String mLogin;
    protected String mPassword;

    protected NgwResource mRootNgwResource;
    protected NgwResource mCurrentNgwResource;

    protected Integer mChildId;

    protected int mThatLoad;


    public NgwConnection(String name, String url, String login, String password) {
        Integer id = (new Random()).nextInt();
        Init(id, name, url, login, password);
    }

    public NgwConnection(Integer id, String name, String url, String login, String password) {
        Init(id, name, url, login, password);
    }

    protected void Init(Integer id, String name, String url, String login, String password) {
        mId = id;
        mName = name;
        mUrl = url;
        mLogin = login;
        mPassword = password;

        mCurrentNgwResource = null;
        mChildId = 0;

        mThatLoad = LOAD_RESOURCE;

        mRootNgwResource = new NgwResource(this);
    }

    public Integer getId() {
        return mId;
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
                || mCurrentNgwResource.getParent() == null
                || mCurrentNgwResource.getParent().getId() == null
                ? "-"
                : mCurrentNgwResource.getParent().getId())
                + "/child/";
    }

    public String getResourceArrayUrl() {
        return mUrl + "/resource/"
                + (mCurrentNgwResource == null || mCurrentNgwResource.getId() == null
                ? "-"
                : mCurrentNgwResource.getId())
                + "/child/";
    }

    public String getChildObjectUrl() {
        return mUrl + "/resource/"
                + (mCurrentNgwResource == null || mCurrentNgwResource.getId() == null
                ? "-"
                : mCurrentNgwResource.getId())
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
        mChildId = 0;
        mThatLoad = LOAD_PARENT;
    }

    public void setLoadResourceArray(NgwResource resource) {
        mCurrentNgwResource = resource;
        mChildId = 0;
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

    @Override
    public int compareTo(NgwConnection connection) {
        return this.mId.compareTo(connection.mId);
    }
}
