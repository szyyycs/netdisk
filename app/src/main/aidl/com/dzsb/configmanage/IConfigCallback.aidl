// IConfigCallback.aidl
package com.dzsb.configmanage;


// Declare any non-default types here with import statements

interface IConfigCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
	void onDownloadResponse(int type,int result, String filepath,int percent);
	void onDataContentResponse(int type,int result,in byte[] data);
}
