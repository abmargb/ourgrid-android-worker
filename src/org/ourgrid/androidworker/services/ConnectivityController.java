package org.ourgrid.androidworker.services;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.SmackAndroid;
import org.ourgrid.androidworker.AndroidCertificationDataProvider;
import org.ourgrid.androidworker.WorkerComponentAndroid;

import android.content.Context;
import android.os.AsyncTask;
import br.edu.ufcg.lsd.commune.context.DefaultContextFactory;
import br.edu.ufcg.lsd.commune.context.ModuleContext;
import br.edu.ufcg.lsd.commune.context.PropertiesParser;
import br.edu.ufcg.lsd.commune.network.certification.CertificationProperties;
import br.edu.ufcg.lsd.commune.network.xmpp.CommuneNetworkException;
import br.edu.ufcg.lsd.commune.network.xmpp.XMPPProperties;

public class ConnectivityController {

	private static ConnectivityController instance;
	
	private boolean charging = false;
	private boolean connectedToWiFi = false;

	private WorkerComponentAndroid component;

	private boolean componentStarted;
	
	public static ConnectivityController getInstance() {
		if (instance == null) {
			instance = new ConnectivityController();
		}
		return instance;
	}

	public void setCharging(boolean charging, Context context) {
		this.charging = charging;
		connectivityChanged(context);
	}

	public void setConnectedToWiFi(boolean connectedToWiFi, Context context) {
		this.connectedToWiFi = connectedToWiFi;
		connectivityChanged(context);
	}
	
	private void connectivityChanged(Context context) {
		if (!componentStarted && connectedToWiFi && charging) {
			start(context);
			return;
		} 
		
		if (componentStarted && (!connectedToWiFi || !charging)) {
			stop(context);
			return;
		}
	}
	
	private void stop(Context context) {
		componentStarted = false;
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					component.stop();
				} catch (CommuneNetworkException e) {
					e.printStackTrace();
				}
				
				return null;
			}
		}.execute();
	}

	private void start(final Context context) {
		this.componentStarted = true;
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				SmackAndroid.init(context);
				startComponent(context);
				return null;
			}
		}.execute();
	}
	
	private void startComponent(Context appContext) {
		
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(XMPPProperties.PROP_PASSWORD, "xmpp-password");
		properties.put(XMPPProperties.PROP_XMPP_SERVERNAME, "xmpp.ourgrid.org");
		properties.put(XMPPProperties.PROP_USERNAME, "abmar-worker-android-nexus");
		properties.put(CertificationProperties.PROP_CERT_PROVIDER_CLASS, 
				AndroidCertificationDataProvider.class.getName());
		
		PropertiesParser parser = new PropertiesParser(properties);
		DefaultContextFactory factory = new DefaultContextFactory(parser);
		ModuleContext context = factory.createContext();
		
		try {
			this.component = new WorkerComponentAndroid(appContext, context);
		} catch (Exception e) {
			componentStarted = false;
		}
	}
	
}
