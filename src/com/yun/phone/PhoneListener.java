package com.yun.phone;

import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.LinphoneCore.RegistrationState;

public class PhoneListener extends LinphoneCoreListenerBase {
    public static final int LOGIN_SUCCESS = 1;
    public static final int LOGIN_FAIL = 2;

    @Override
    public void registrationState(LinphoneCore lc, LinphoneProxyConfig cfg, RegistrationState state, String smessage) {
        super.registrationState(lc, cfg, state, smessage);
        if (state.equals(RegistrationState.RegistrationCleared)) {
            if (lc != null) {
        	LinphoneAuthInfo authInfo = lc.findAuthInfo(cfg.getIdentity(), cfg.getRealm(), cfg.getDomain());
        	if (authInfo != null) {
        	    lc.removeAuthInfo(authInfo);
        	}
            }
        }
        
        if (state.equals(RegistrationState.RegistrationOk)) {
            if (PhonePreferences.newInstance().getAccountCount() > 1) {
        	PhonePreferences.newInstance().deleteAccount(0);
            }
            loginState(LOGIN_SUCCESS);
        } else {
            loginState(LOGIN_FAIL);
        }
    }
    
    public void loginState(int state) {
    }
}
