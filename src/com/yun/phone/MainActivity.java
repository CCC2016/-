package com.yun.phone;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.linphone.LinphoneManager;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallLog;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PayloadType;
import org.linphone.core.Reason;

import com.yun.phone.PhonePreferences.AccountBuilder;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static MainActivity instance;
    private LinphoneCoreListenerBase mListener;

    Handler a = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);

	LinphoneCoreFactory.instance().setDebugMode(true, "Android");

	PhoneManager.createAndStart(getApplicationContext());

	mListener = new LinphoneCoreListenerBase() {

	    @Override
	    public void registrationState(LinphoneCore lc, LinphoneProxyConfig cfg, RegistrationState state,
		    String smessage) {
		if (state.equals(RegistrationState.RegistrationOk)) {
		    Log.i("TAG", "登录成功");
		    if (PhonePreferences.newInstance().getAccountCount() > 1) {
			PhonePreferences.newInstance().deleteAccount(0);
		    }
		}

		if (state.equals(RegistrationState.RegistrationCleared)) {
		    if (lc != null) {
			LinphoneAuthInfo authInfo = lc.findAuthInfo(cfg.getIdentity(), cfg.getRealm(), cfg.getDomain());
			if (authInfo != null) {
			    lc.removeAuthInfo(authInfo);
			}
		    }
		}
	    }

	    @Override
	    public void callState(LinphoneCore lc, LinphoneCall call, State state, String message) {
		Log.i("TAG", "通话状态 + " + state.toString());
		if (instance == null) {
		    return;
		}

		if (state == State.IncomingReceived) {
		    Log.i("TAG", "收到来电");
		    // answer(call);
		}

		if (state == State.StreamsRunning) {
		    Log.i("TAG", "运行流");
		    // PhoneManager.getLC().enableSpeaker(PhoneManager.getLC().isSpeakerEnabled());
		    // PhoneManager.getLC().enableSpeaker(true);

		    // if (PhoneManager.getLC().isMicMuted()) {
		    // Log.i("TAG", "静音");
		    // PhoneManager.getLC().muteMic(false);
		    // }
		}

		if (state == State.CallEnd || state == State.CallReleased || state == State.Error) {
		    if (message != null && call.getReason() == Reason.Declined) {
			Toast.makeText(MainActivity.this, "拒绝", Toast.LENGTH_SHORT).show();
		    }
		}
	    }
	};

	findViewById(R.id.ss).setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View v) {
		Log.i("TAG", "下线");

		PhoneManager.getInstance().newOutgoingCall("820");

	    }
	});

	a.postDelayed(new Runnable() {

	    @Override
	    public void run() {
		if (PhonePreferences.newInstance().getAccountCount() != 1) {
		    AccountBuilder builder = new AccountBuilder(PhoneManager.getLC());
		    try {
			builder.saveNewAccount();
		    } catch (LinphoneCoreException e) {
			e.printStackTrace();
		    }
		}

		LinphoneCore lc = PhoneManager.getLC();
		for (PayloadType pt : PhoneManager.getLC().getAudioCodecs()) {
		    try {
			if ("opus".equals(pt.getMime())) {
			    if (!lc.isPayloadTypeEnabled(pt)) {
				lc.enablePayloadType(pt, true);
			    }
			} else {
			    lc.enablePayloadType(pt, false);
			}
		    } catch (LinphoneCoreException e) {
			e.printStackTrace();
		    } finally {
			Log.i("TAG", "pt: " + pt.getMime() + " : " + lc.isPayloadTypeEnabled(pt));
		    }
		}

		if (lc != null) {
		    lc.addListener(mListener);
		}

		lc.getCallLogs();
	    }
	}, 5000);

	instance = this;
    }

    @SuppressWarnings("unused")
    private void answer(LinphoneCall aCall) {
	LinphoneCallParams params = PhoneManager.getLC().createDefaultCallParameters();

	boolean isLowBandwidthConnection = PhoneUtils.isHighBandwidthConnection(this);
	if (isLowBandwidthConnection) {
	    params.enableLowBandwidth(isLowBandwidthConnection);
	}

	try {
	    PhoneManager.getLC().acceptCallWithParams(aCall, params);
	} catch (LinphoneCoreException e) {
	    e.printStackTrace();
	}
    }

    @Override
    protected void onDestroy() {
	super.onDestroy();

	PhoneManager.getInstance().destory();
    }

    private String timestampToHumanDate(String timestamp) {
	Calendar cal = Calendar.getInstance();
	cal.setTimeInMillis(Long.parseLong(timestamp));

	SimpleDateFormat dateFormat;
	dateFormat = new SimpleDateFormat("EEEE MMM d HH:mm", Locale.CHINA);
	return dateFormat.format(cal.getTime());
    }

    private String secondsToDisplayableString(int secs) {
	SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
	Calendar cal = Calendar.getInstance();
	cal.set(0, 0, 0, 0, 0, secs);
	return dateFormat.format(cal.getTime());
    }

    @SuppressWarnings("unused")
    private void log() {
	List<LinphoneCallLog> ls = Arrays.asList(LinphoneManager.getLc().getCallLogs());
	for (LinphoneCallLog c : ls) {
	    String callDate = String.valueOf(c.getTimestamp());
	    Log.i("TAG", timestampToHumanDate(callDate));
	    Log.i("TAG", secondsToDisplayableString(c.getCallDuration()));
	}
    }
}
