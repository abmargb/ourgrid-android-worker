package org.ourgrid.androidworker;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.ourgrid.androidworker.services.ConnectivityController;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import de.mindpipe.android.logging.log4j.LogCatAppender;

public class MainActivity extends Activity {

	static {
	    org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
	    root.addAppender(new LogCatAppender());
	    root.setLevel(Level.DEBUG);
	}

	private static final String COMPONENT_CREATED = "COMPONENT_STATE";
	private boolean created = false;

	private class PowerConnectedReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityController.getInstance().setCharging(
					true, getApplicationContext());
		}
	}
	
	private class PowerDisconnectedReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityController.getInstance().setCharging(
					false, getApplicationContext());
		}
	}
	
	private class WifiBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(WifiBroadcastReceiver.class.toString(), "Connectivy changed.");
			wifiStateChanged(context);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.created = savedInstanceState != null && 
				savedInstanceState.getBoolean(COMPONENT_CREATED, false);
		
		final TextView textView = (TextView) findViewById(R.id.logField);
		textView.setMovementMethod(new ScrollingMovementMethod());
		
		if (created) {
			return;
		}
		
		Logger.getRootLogger().addAppender(createAppender());
		
		registerReceiver(new PowerConnectedReceiver(), 
				new IntentFilter(Intent.ACTION_POWER_CONNECTED));
		registerReceiver(new PowerDisconnectedReceiver(), 
				new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
		registerReceiver(new WifiBroadcastReceiver(), 
				new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		registerReceiver(new WifiBroadcastReceiver(), 
				new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		
		wifiStateChanged(this);
		
		Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		setCharging(batteryIntent);
		
		this.created = true;
	}

	private void setCharging(Intent batteryIntent) {
		int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
		                     status == BatteryManager.BATTERY_STATUS_FULL;
		ConnectivityController.getInstance().setCharging(isCharging, this);
	}
	
	private void wifiStateChanged(Context context) {
		ConnectivityManager conMngr = (ConnectivityManager) context.getSystemService(
				Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifi = conMngr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		ConnectivityController.getInstance().setConnectedToWiFi(
				wifi.isConnected(), getApplicationContext());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(COMPONENT_CREATED, created);
	}
	
	private AppenderSkeleton createAppender() {
		return new AppenderSkeleton() {
			
			@Override
			public boolean requiresLayout() {
				return false;
			}
			
			@Override
			public void close() {
				
			}
			
			@Override
			protected void append(final LoggingEvent arg0) {
				final TextView textView = (TextView) findViewById(R.id.logField);
				if (textView == null) {
					return;
				}
				textView.getHandler().post(new Runnable() {
					@Override
					public void run() {
						textView.setText(textView.getText().toString() + "\n" + arg0.getMessage());
					}
				});
			}
		};
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
