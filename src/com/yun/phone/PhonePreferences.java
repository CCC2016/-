package com.yun.phone;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAddress.TransportType;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneProxyConfig;

import android.text.TextUtils;
import android.util.Log;

public class PhonePreferences {
    private static final String TAG = PhonePreferences.class.getSimpleName();
    private static PhonePreferences mInstance;
    
    private PhonePreferences() {}
    
    public synchronized static final PhonePreferences newInstance() {
	if (mInstance == null) {
	    mInstance = new PhonePreferences();
	}
	return mInstance;
    }
    
    public static class AccountBuilder {
	private LinphoneCore lc;
	private String tempUsername;
	private String tempUserId;
	private String tempPassword;
	private String tempDomain;
	private boolean tempAvpfEnabled = false;
	private int tempAvpfRRInterval = 0;
	private String tempQualityReportingCollector;
	private boolean tempQualityReportingEnabled = false;
	private int tempQualityReportingInterval = 0;
	private boolean tempEnabled = true;
	private boolean tempNoDefault = false;
	
	public AccountBuilder(LinphoneCore lc) {
	    this.lc = lc;
	}
	
	public AccountBuilder setUsername(String username) {
	    tempUsername = username;
	    return this;
	}
	
	public AccountBuilder setPassword(String password) {
	    tempPassword = password;
	    return this;
	}
	
	public AccountBuilder setDomain(String domain) {
	    tempDomain = domain;
	    return this;
	}
	
	public void saveNewAccount() throws LinphoneCoreException {
	    if (TextUtils.isEmpty(tempUsername) || TextUtils.isEmpty(tempDomain)) {
		return;
	    }
	    
	    String identity = "sip:" + tempUsername + "@" + tempDomain;
	    String proxy = "sip:" + tempDomain;
	    
	    LinphoneAddress proxyAddr = LinphoneCoreFactory.instance().createLinphoneAddress(proxy);
	    LinphoneAddress identityAddr = LinphoneCoreFactory.instance().createLinphoneAddress(identity);
	    
	    proxyAddr.setTransport(TransportType.LinphoneTransportTcp);
	    
	    LinphoneProxyConfig prxCfg = lc.createProxyConfig(identityAddr.asString(),
		    proxyAddr.asStringUriOnly(), null, tempEnabled);
	    prxCfg.setExpires(180);
	    prxCfg.enableAvpf(tempAvpfEnabled);
	    prxCfg.setAvpfRRInterval(tempAvpfRRInterval);
	    prxCfg.enableQualityReporting(tempQualityReportingEnabled);
	    prxCfg.setQualityReportingCollector(tempQualityReportingCollector);
	    prxCfg.setQualityReportingInterval(tempQualityReportingInterval);
	    
	    LinphoneAuthInfo authInfo = LinphoneCoreFactory.instance()
		    .createAuthInfo(tempUsername, tempUserId, tempPassword, null, null, tempDomain);
	    
	    lc.addProxyConfig(prxCfg);
	    lc.addAuthInfo(authInfo);
	    
	    Log.i(TAG, "AccountCount: " + PhonePreferences.newInstance().getAccountCount());
	    if (!tempNoDefault && PhonePreferences.newInstance().getAccountCount() == 1)
		lc.setDefaultProxyConfig(prxCfg);
	}
    }
    
    private LinphoneCore getLC() {
	if (!PhoneManager.isInstanced()) {
	    return null;
	}
	return PhoneManager.getLC();
    }
    
    public int getAccountCount() {
	if (getLC() == null || getLC().getProxyConfigList() == null)
	    return 0;
	
	return getLC().getProxyConfigList().length;
    }
    
    public void deleteAccount(int n) {
	LinphoneProxyConfig proxyCfg = getProxyConfig(n);
	if (proxyCfg != null)
	    getLC().removeProxyConfig(proxyCfg);
	if (getLC().getProxyConfigList().length != 0) {
	    resetDefaultProxyConfig();
	    getLC().refreshRegisters();
	}
    }
    
    private LinphoneProxyConfig getProxyConfig(int n) {
	LinphoneProxyConfig[] prxCfgs = getLC().getProxyConfigList();
	if (n < 0 || n > prxCfgs.length)
	    return null;
	return prxCfgs[n];
    }
    
    public boolean isAccountEnabled(int n) {
	return getProxyConfig(n).registerEnabled();
    }
    
    public void resetDefaultProxyConfig() {
	int count = getLC().getProxyConfigList().length;
	for (int i = 0; i < count; i++) {
	    if (isAccountEnabled(i)) {
		getLC().setDefaultProxyConfig(getProxyConfig(i));
		break;
	    }
	}
	
	if (getLC().getDefaultProxyConfig() == null) {
	    getLC().setDefaultProxyConfig(getProxyConfig(0));
	}
    }
}
