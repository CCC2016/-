package com.yun.phone;

import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;

import com.yun.phone.PhonePreferences.AccountBuilder;

import android.content.Context;
import android.util.Log;

public class PhoneDevice {
    
    public static void openDebugMode(boolean debug) {
	LinphoneCoreFactory.instance().setDebugMode(debug, "Yuneasy");
    }
    
    public static void init(Context context) {
	openDebugMode(false);
	PhoneManager.createAndStart(context);
    }
    
    public static void addListener(PhoneListener listener) {
	PhoneManager.getLC().addListener(listener);
    }
    
    public static void login(String username, String password, String domain) {
	if (PhonePreferences.newInstance().getAccountCount() != 1) {
	    AccountBuilder builder = new AccountBuilder(PhoneManager.getLC());
	    builder.setDomain(domain).setUsername(username).setPassword(password);
	    try {
		builder.saveNewAccount();
	    } catch (LinphoneCoreException e) {
		Log.i("Yuneasy", "电话注册失败");
	    }
	}
    }
    
    public static void callOut(String phoneNumber) {
	PhoneManager.getInstance().newOutgoingCall(phoneNumber);
    }
    
    public static void destroy() {
	PhoneManager.getInstance().destory();
    }
}
