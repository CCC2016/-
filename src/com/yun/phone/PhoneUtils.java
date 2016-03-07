package com.yun.phone;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration.AndroidCamera;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class PhoneUtils {
    public static boolean isHighBandwidthConnection(Context context) {
	ConnectivityManager cm = (ConnectivityManager) context
		.getSystemService(Context.CONNECTIVITY_SERVICE);
	NetworkInfo info = cm.getActiveNetworkInfo();
	return (info != null && info.isConnected()
		&& isConnectionFast(info.getType(), info.getSubtype()));
    }
    
    private static boolean isConnectionFast(int type, int subType) {
	if (type == ConnectivityManager.TYPE_MOBILE) {
	    switch (subType) {
	    case TelephonyManager.NETWORK_TYPE_EDGE:
	    case TelephonyManager.NETWORK_TYPE_GPRS:
	    case TelephonyManager.NETWORK_TYPE_IDEN:
		return false;
	    }
	}
	return true;
    }
    
    public static int getFrontCamAsDefault() {
	int camId = 0;
	AndroidCamera[] cameras = AndroidCameraConfiguration.retrieveCameras();
	for (AndroidCamera androidCamera : cameras) {
	    if (androidCamera.frontFacing)
		camId = androidCamera.id;
	}
	return camId;
    }
    
    public static String getUserAgent(Context context) throws NameNotFoundException {
	String versionName = context.getPackageManager()
		.getPackageInfo(context.getPackageName(), 0).versionName;
	if (versionName == null) {
	    versionName = String
		    .valueOf(context.getPackageManager()
			    .getPackageInfo(context.getPackageName(), 0).versionCode);
	}
	return versionName;
    }
    
    public static int getAvailableCores() {
	return Runtime.getRuntime().availableProcessors();
    }
    
    public static void copyIfNotExist(Context context, int ressourceId, String target) throws IOException {
	File lFileToCopy = new File(target);
	if (!lFileToCopy.exists()) {
	    copyFromPackage(context, ressourceId, lFileToCopy.getName());
	}
    }
    
    public static void copyFromPackage(Context context, int ressourceId, String target) throws IOException {
	FileOutputStream lOutputStream = context.openFileOutput(target, 0);
	InputStream lInputStream = context.getResources().openRawResource(ressourceId);
	int readByte;
	byte[] buff = new byte[8048];
	while ((readByte = lInputStream.read(buff)) != -1) {
	    lOutputStream.write(buff, 0, readByte);
	}
	lOutputStream.flush();
	lOutputStream.close();
	lInputStream.close();
    }
}
