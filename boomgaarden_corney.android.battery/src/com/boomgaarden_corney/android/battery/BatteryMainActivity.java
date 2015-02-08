package com.boomgaarden_corney.android.battery;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class BatteryMainActivity extends Activity {

	private final String DEBUG_TAG = "DEBUG_BATTERY";
	private final String SERVER_URL = "http://54.86.68.241/battery/test.php";

	private TextView txtResults;

	private String errorMsg;
	private String batteryTechnology;

	private Intent mBattery;

	private boolean batteryPresent = false;

	private int batteryHealth = 0;
	private int batteryIconSmall = 0;
	private int batteryLevel = 0;
	private int batteryPlugged = 0;
	private int batteryScale = 0;
	private int batteryTemperature = 0;
	private int batteryVoltage = 0;
	private int batteryCold = 0;
	private int batteryDead = 0;
	private int batteryGood = 0;
	private int batteryOverVoltage = 0;
	private int batteryOverHeat = 0;
	private int batteryUnknown = 0;
	private int batteryUnspecifiedFailure = 0;
	private int batteryPluggedAC = 0;
	private int batteryPluggedUSB = 0;
	private int batteryPluggedWireless = 0;
	private int batteryCharging = 0;
	private int batteryDischarging = 0;
	private int batteryFull = 0;
	private int batteryNotCharging = 0;

	private List<NameValuePair> paramsDevice = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsBattery = new ArrayList<NameValuePair>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);

		setDeviceData();
		showDeviceData();
		sendDeviceData();

		mBattery = this.getApplicationContext().registerReceiver(null,
				new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		if (mBattery == null) {
			setErrorMsg("No Battery Detected");
			showErrorMsg();
			sendErrorMsg();
		} else {
			for (int i = 0; i < 5; i++) {
				setBatteryData();
				showBatteryData();
				sendBatteryData();
			}			
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("DEVICE")) {
			writer.write(buildPostRequest(paramsDevice));
		} else if (postParameters.equals("BATTERY")) {
			writer.write(buildPostRequest(paramsBattery));
			paramsBattery = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		}

		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();

		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Battery
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}
	}

	private void setDeviceData() {
		paramsDevice.add(new BasicNameValuePair("Device", Build.DEVICE));
		paramsDevice.add(new BasicNameValuePair("Brand", Build.BRAND));
		paramsDevice.add(new BasicNameValuePair("Manufacturer",
				Build.MANUFACTURER));
		paramsDevice.add(new BasicNameValuePair("Model", Build.MODEL));
		paramsDevice.add(new BasicNameValuePair("Product", Build.PRODUCT));
		paramsDevice.add(new BasicNameValuePair("Board", Build.BOARD));
		paramsDevice.add(new BasicNameValuePair("Android API", String
				.valueOf(Build.VERSION.SDK_INT)));
	}

	private void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}

	private void showDeviceData() {
		// Display and store (for sending via HTTP POST query) device
		// information
		txtResults.append("Device: " + Build.DEVICE + "\n");
		txtResults.append("Brand: " + Build.BRAND + "\n");
		txtResults.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		txtResults.append("Model: " + Build.MODEL + "\n");
		txtResults.append("Product: " + Build.PRODUCT + "\n");
		txtResults.append("Board: " + Build.BOARD + "\n");
		txtResults.append("Android API: "
				+ String.valueOf(Build.VERSION.SDK_INT) + "\n");

		txtResults.append("\n");

	}

	private void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void showBatteryData() {
		StringBuilder results = new StringBuilder();

		results.append("Battery Health: " + String.valueOf(batteryHealth)
				+ "\n");
		results.append("Battery Icon Small: "
				+ String.valueOf(batteryIconSmall) + "\n");
		results.append("Battery Level: " + String.valueOf(batteryLevel) + "\n");
		results.append("Battery Plugged: " + String.valueOf(batteryPlugged)
				+ "\n");

		results.append("Battery Present: " + String.valueOf(batteryPresent)
				+ "\n");
		results.append("Battery Scale: " + String.valueOf(batteryScale) + "\n");
		results.append("Battery Technology: "
				+ String.valueOf(batteryTechnology) + "\n");
		results.append("BatteryTemperature: "
				+ String.valueOf(batteryTemperature) + "\n");

		results.append("Battery Voltage: " + String.valueOf(batteryVoltage)
				+ "\n");
		results.append("Battery Cold: " + String.valueOf(batteryCold) + "\n");
		results.append("Battery Dead: " + String.valueOf(batteryDead) + "\n");
		results.append("Battery Good: " + String.valueOf(batteryGood) + "\n");

		results.append("Battery Over Voltage: "
				+ String.valueOf(batteryOverVoltage) + "\n");
		results.append("Battery Over Heat: " + String.valueOf(batteryOverHeat)
				+ "\n");
		results.append("Battery Unknown: " + String.valueOf(batteryUnknown)
				+ "\n");
		results.append("Battery Unspecified Failure: "
				+ String.valueOf(batteryUnspecifiedFailure) + "\n");

		results.append("Battery Plugged AC: "
				+ String.valueOf(batteryPluggedAC) + "\n");
		results.append("Battery Plugged USB: "
				+ String.valueOf(batteryPluggedUSB) + "\n");
		results.append("Battery Plugged Wireless: "
				+ String.valueOf(batteryPluggedWireless) + "\n");
		results.append("Battery Charging: " + String.valueOf(batteryCharging)
				+ "\n");

		results.append("Battery Discharging: "
				+ String.valueOf(batteryDischarging) + "\n");
		results.append("Battery Full: " + String.valueOf(batteryFull) + "\n");
		results.append("Battery Not Charging: "
				+ String.valueOf(batteryNotCharging) + "\n");

		txtResults.append(new String(results));
		txtResults.append("\n");

	}

	private void sendDeviceData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "DEVICE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendBatteryData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "BATTERY");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void setBatteryData() {

		batteryHealth = mBattery.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
		batteryIconSmall = mBattery.getIntExtra(
				BatteryManager.EXTRA_ICON_SMALL, 0);
		batteryLevel = mBattery.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
		batteryPlugged = mBattery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

		batteryPresent = mBattery.getExtras().getBoolean(
				BatteryManager.EXTRA_PRESENT);
		batteryScale = mBattery.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
		batteryTechnology = mBattery.getExtras().getString(
				BatteryManager.EXTRA_TECHNOLOGY);
		batteryTemperature = mBattery.getIntExtra(
				BatteryManager.EXTRA_TEMPERATURE, 0);

		batteryVoltage = mBattery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
		batteryCold = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_HEALTH_COLD);
		batteryDead = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_HEALTH_DEAD);
		batteryGood = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_HEALTH_GOOD);

		batteryOverVoltage = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE);
		batteryOverHeat = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_HEALTH_OVERHEAT);
		batteryUnknown = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_HEALTH_UNKNOWN);
		batteryUnspecifiedFailure = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE);

		batteryPluggedAC = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_PLUGGED_AC);
		batteryPluggedUSB = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_PLUGGED_USB);
		batteryPluggedWireless = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_PLUGGED_WIRELESS);
		batteryCharging = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_STATUS_CHARGING);

		batteryDischarging = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_STATUS_DISCHARGING);
		batteryFull = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_STATUS_FULL);
		batteryNotCharging = mBattery.getIntExtra(null,
				BatteryManager.BATTERY_STATUS_NOT_CHARGING);

		paramsBattery.add(new BasicNameValuePair("Battery Health", String
				.valueOf(batteryHealth)));
		paramsBattery.add(new BasicNameValuePair("Battery Icon Small", String
				.valueOf(batteryIconSmall)));
		paramsBattery.add(new BasicNameValuePair("Battery Level", String
				.valueOf(batteryLevel)));
		paramsBattery.add(new BasicNameValuePair("Battery Plugged In", String
				.valueOf(batteryPlugged)));

		paramsBattery.add(new BasicNameValuePair("Battery Present", String
				.valueOf(batteryPresent)));
		paramsBattery.add(new BasicNameValuePair("Battery Scale", String
				.valueOf(batteryScale)));
		paramsBattery.add(new BasicNameValuePair("Battery Technology", String
				.valueOf(batteryTechnology)));
		paramsBattery.add(new BasicNameValuePair("Battery Temperature", String
				.valueOf(batteryTemperature)));

		paramsBattery.add(new BasicNameValuePair("Battery Voltage", String
				.valueOf(batteryVoltage)));
		paramsBattery.add(new BasicNameValuePair("Battery Cold", String
				.valueOf(batteryCold)));
		paramsBattery.add(new BasicNameValuePair("Battery Dead", String
				.valueOf(batteryDead)));
		paramsBattery.add(new BasicNameValuePair("Battery Good", String
				.valueOf(batteryGood)));

		paramsBattery.add(new BasicNameValuePair("Battery Over Voltage", String
				.valueOf(batteryOverVoltage)));
		paramsBattery.add(new BasicNameValuePair("Battery OverHeat", String
				.valueOf(batteryOverHeat)));
		paramsBattery.add(new BasicNameValuePair("Battery Unknown", String
				.valueOf(batteryUnknown)));
		paramsBattery.add(new BasicNameValuePair("Battery Unspecified Failure",
				String.valueOf(batteryUnspecifiedFailure)));

		paramsBattery.add(new BasicNameValuePair("Battery Plugged AC", String
				.valueOf(batteryPluggedAC)));
		paramsBattery.add(new BasicNameValuePair("Battery Plugged USB", String
				.valueOf(batteryPluggedUSB)));
		paramsBattery.add(new BasicNameValuePair("Battery Plugged Wireless",
				String.valueOf(batteryPluggedWireless)));
		paramsBattery.add(new BasicNameValuePair("Battery Charging", String
				.valueOf(batteryCharging)));

		paramsBattery.add(new BasicNameValuePair("Battery Discharging", String
				.valueOf(batteryDischarging)));
		paramsBattery.add(new BasicNameValuePair("Battery Full", String
				.valueOf(batteryFull)));
		paramsBattery.add(new BasicNameValuePair("Battery Not Charging", String
				.valueOf(batteryNotCharging)));

	}
}
