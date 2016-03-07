package com.yun.phone;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.EcCalibratorStatus;
import org.linphone.core.LinphoneCore.GlobalState;
import org.linphone.core.LinphoneCore.LogCollectionUploadState;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCore.RemoteProvisioningState;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneInfoMessage;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PresenceActivityType;
import org.linphone.core.PresenceModel;
import org.linphone.core.PublishState;
import org.linphone.core.SubscriptionState;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.widget.Toast;

public class PhoneManager implements LinphoneCoreListener {
    private static final String TAG = PhoneManager.class.getSimpleName();
    private static PhoneManager mInstance;
    private Context mContext;
    private String mBasePath;
    private LinphoneCore mLinphoneCore;
    private Timer mTimer;
    
    public static final boolean isInstanced() {
	return mInstance != null;
    }
    
    public synchronized static final PhoneManager getInstance() {
	return mInstance;
    }
    
    public synchronized static final LinphoneCore getLC() {
	return getInstance().mLinphoneCore;
    }
    
    public PhoneManager(Context context) {
	mContext = context;
	mBasePath = context.getFilesDir().getAbsolutePath();
	try {
	    copyAssetsFromPackage(context, mBasePath);
	} catch (IOException e) {
	    e.printStackTrace();
	}
	
	PhonePreferences.newInstance();
	
    }

    public synchronized static void createAndStart(Context context) {
	if (mInstance != null)
	    throw new RuntimeException(TAG + " 已实例化");
	
	mInstance = new PhoneManager(context);
	mInstance.startLibLinphone(context);
    }
    
    private void startLibLinphone(Context context) {
	try {
	    mLinphoneCore = LinphoneCoreFactory.instance().createLinphoneCore(this, mBasePath + "/.linphonerc",
		    mBasePath + "/linphonerc", null, context);
	    initLinphoneCoreValues(context);
	    
	    startIterate();
	} catch (LinphoneCoreException e) {
	    e.printStackTrace();
	}
    }
    
    private void copyAssetsFromPackage(Context context, String path) throws IOException {
	PhoneUtils.copyFromPackage(context, R.raw.linphonerc_factory, new File(path + "/linphonerc").getName());
	PhoneUtils.copyIfNotExist(context, R.raw.linphonerc_default, path + "/.linphonerc");
	PhoneUtils.copyIfNotExist(context, R.raw.lpconfig, path + "/lpconfig.xsd");
	PhoneUtils.copyIfNotExist(context, R.raw.rootca, path + "/rootca.pem");
	PhoneUtils.copyIfNotExist(context, R.raw.ringback, path + "/ringback.wav");
	PhoneUtils.copyIfNotExist(context, R.raw.oldphone_mono, path + "/oldphone_mono.wav");
	PhoneUtils.copyIfNotExist(context, R.raw.toy_mono, path + "/toy_mono.wav");
    }
    
    private void initLinphoneCoreValues(Context context) {
	mLinphoneCore.setContext(context);
	mLinphoneCore.setRootCA(mBasePath + "/rootca.pem");
	mLinphoneCore.setChatDatabasePath(mBasePath + "/linphone-history.db");
	mLinphoneCore.setCpuCount(PhoneUtils.getAvailableCores());
	mLinphoneCore.setPlayFile(mBasePath + "/toy_mono.wav");
	mLinphoneCore.setRing(null);
	mLinphoneCore.setVideoDevice(PhoneUtils.getFrontCamAsDefault());
	mLinphoneCore.setNetworkReachable(true);
	try {
	    mLinphoneCore.setUserAgent("YuneasyAndroid", PhoneUtils.getUserAgent(context));
	} catch (NameNotFoundException e) {
	    e.printStackTrace();
	}
    }
    
    private void startIterate() {
	TimerTask lTask = new TimerTask() {
	    @Override
	    public void run() {
		mLinphoneCore.iterate();
	    }
	};
	
	mTimer = new Timer("Linphone scheduler");
	mTimer.schedule(lTask, 0, 20);
    }
    
    private boolean isPresenceModelActivitySet() {
	if (isInstanced() && mLinphoneCore != null) {
	    return mLinphoneCore.getPresenceModel() != null
		    && mLinphoneCore.getPresenceModel().getActivity() != null;
	}
	return false;
    }
    
    public void changeStatusToOffline() {
	if (isInstanced() && isPresenceModelActivitySet()
		&& mLinphoneCore.getPresenceModel().getActivity().getType()
		!= PresenceActivityType.Offline) {
	    mLinphoneCore.getPresenceModel().getActivity()
	    .setType(PresenceActivityType.Offline);
	    Log.i(TAG, "Offline + set");
	} else if (isInstanced() && !isPresenceModelActivitySet()) {
	    PresenceModel model = LinphoneCoreFactory.instance()
		    .createPresenceModel(PresenceActivityType.Offline, null);
	    mLinphoneCore.setPresenceModel(model);
	    Log.i(TAG, "Offline + new");
	}
    }
    
    public void newOutgoingCall(String to) {
	LinphoneAddress lAddress;
	try {
	    lAddress = mLinphoneCore.interpretUrl(to);
	    LinphoneProxyConfig lpc = mLinphoneCore.getDefaultProxyConfig();
	    if (lpc != null && lAddress.asStringUriOnly().equals(lpc.getIdentity())) {
		return;
	    }
	} catch (LinphoneCoreException e) {
	    e.printStackTrace();
	    return;
	}
	lAddress.setDisplayName("Android");
	
	boolean isLowBandwidthConnection = !PhoneUtils.isHighBandwidthConnection(mContext);
	if (mLinphoneCore.isNetworkReachable()) {
	    try {
		LinphoneCallParams params = mLinphoneCore.createDefaultCallParameters();
		params.setVideoEnabled(false);
		params.setAudioBandwidth(0);
		if (isLowBandwidthConnection) {
		    params.enableLowBandwidth(true);
		}
		mLinphoneCore.inviteAddressWithParams(lAddress, params);
	    } catch (LinphoneCoreException e) {
		e.printStackTrace();
		return;
	    }
	} else {
	    Toast.makeText(mContext, "网络不可用", Toast.LENGTH_SHORT).show();
	}
    }
    
    public void destory() {
	try {
	    mTimer.cancel();
	    mLinphoneCore.destroy();
	} finally {
	    mLinphoneCore = null;
	    mInstance = null;
	}
    }
    
    @Override
    public void authInfoRequested(LinphoneCore lc, String realm, String username, String Domain) {
    }
    
    @Override
    public void callStatsUpdated(LinphoneCore lc, LinphoneCall call, LinphoneCallStats stats) {
    }
    
    @Override
    public void newSubscriptionRequest(LinphoneCore lc, LinphoneFriend lf, String url) {
    }
    
    @Override
    public void notifyPresenceReceived(LinphoneCore lc, LinphoneFriend lf) {
    }
    
    @Override
    public void dtmfReceived(LinphoneCore lc, LinphoneCall call, int dtmf) {
    }
    
    @Override
    public void notifyReceived(LinphoneCore lc, LinphoneCall call, LinphoneAddress from, byte[] event) {
    }
    
    @Override
    public void transferState(LinphoneCore lc, LinphoneCall call, State new_call_state) {
    }
    
    @Override
    public void infoReceived(LinphoneCore lc, LinphoneCall call, LinphoneInfoMessage info) {
    }
    
    @Override
    public void subscriptionStateChanged(LinphoneCore lc, LinphoneEvent ev, SubscriptionState state) {
    }
    
    @Override
    public void publishStateChanged(LinphoneCore lc, LinphoneEvent ev, PublishState state) {
    }
    
    @Override
    public void show(LinphoneCore lc) {
    }
    
    @Override
    public void displayStatus(LinphoneCore lc, String message) {
    }
    
    @Override
    public void displayMessage(LinphoneCore lc, String message) {
    }
    
    @Override
    public void displayWarning(LinphoneCore lc, String message) {
    }
    
    @Override
    public void fileTransferProgressIndication(LinphoneCore lc, LinphoneChatMessage message, LinphoneContent content,
	    int progress) {
    }
    
    @Override
    public void fileTransferRecv(LinphoneCore lc, LinphoneChatMessage message, LinphoneContent content, byte[] buffer,
	    int size) {
    }
    
    @Override
    public int fileTransferSend(LinphoneCore lc, LinphoneChatMessage message, LinphoneContent content,
	    ByteBuffer buffer, int size) {
	return 0;
    }
    
    @Override
    public void globalState(LinphoneCore lc, GlobalState state, String message) {
    }
    
    @Override
    public void registrationState(LinphoneCore lc, LinphoneProxyConfig cfg, RegistrationState state, String smessage) {
    }
    
    @Override
    public void configuringStatus(LinphoneCore lc, RemoteProvisioningState state, String message) {
    }
    
    @Override
    public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr, LinphoneChatMessage message) {
    }
    
    @Override
    public void callState(LinphoneCore lc, LinphoneCall call, State state, String message) {
    }
    
    @Override
    public void callEncryptionChanged(LinphoneCore lc, LinphoneCall call, boolean encrypted,
	    String authenticationToken) {
    }
    
    @Override
    public void notifyReceived(LinphoneCore lc, LinphoneEvent ev, String eventName, LinphoneContent content) {
    }
    
    @Override
    public void isComposingReceived(LinphoneCore lc, LinphoneChatRoom cr) {
    }
    
    @Override
    public void ecCalibrationStatus(LinphoneCore lc, EcCalibratorStatus status, int delay_ms, Object data) {
    }
    
    @Override
    public void uploadProgressIndication(LinphoneCore lc, int offset, int total) {
    }
    
    @Override
    public void uploadStateChanged(LinphoneCore lc, LogCollectionUploadState state, String info) {
    }
}
