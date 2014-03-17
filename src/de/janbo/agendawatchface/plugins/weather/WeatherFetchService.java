package de.janbo.agendawatchface.plugins.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Downloads weather information and simply stores them in the SharedPreferences
 * 
 * @author Jan
 * 
 */
public class WeatherFetchService extends IntentService {

	/**
	 * Boolean extra for intents starting this service. If set to true, will get data and then broadcast the city name or an error message
	 */
	public static final String INTENT_EXTRA_BROADCAST_CITY = "de.janbo.agendawatchface.plugins.weather.intent.extra.broadcastcity";
	
	/**
	 * The intent action of the broadcast following the request for giving the city
	 */
	public static final String INTENT_ACTION_BROADCAST_CITY = "de.janbo.agendawatchface.plugins.weather.intent.action.broadcastcity";
	public static final String INTENT_EXTRA_BROADCAST_CITY_MESSAGE = "de.janbo.agendawatchface.plugins.weather.intent.extra.citymessage";

	public WeatherFetchService() {
		super("Agenda Watchface Weather Service");
	}

	@Override
	protected void onHandleIntent(Intent arg0) {
		try {
			SharedPreferences weatherDataPrefs = getSharedPreferences("weatherData", 0);
			SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			//Download file
			URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q="+URLEncoder.encode(userPrefs.getString("pref_town", "Paderborn,de"), "UTF-8")+"&APPID=37748da5a153099c5cadb2aa2b72e65c");
			
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			String str;
			StringBuilder builder = new StringBuilder();
			while ((str = in.readLine()) != null)
				builder.append(str);
			in.close();

			//Save
			Editor edit = weatherDataPrefs.edit();
			edit.putString("currentForecastDataset", builder.toString());
			edit.putLong("lastUpdate", System.currentTimeMillis());
			edit.apply();
			
			Log.d("WeatherFetchService", "Downloaded new weather data");
			
			if (arg0.getBooleanExtra(INTENT_EXTRA_BROADCAST_CITY, false))
				broadcastCity(builder.toString());
			
			// Publish data to watch
			new WeatherProvider().publishWeather(this);
		} catch (MalformedURLException e) {
		} catch (IOException e) {
			Log.e("WeatherFetcher", "Error downloading file", e);
		}
	}
	
	private void broadcastCity(String result) {
		Intent intent = new Intent(INTENT_ACTION_BROADCAST_CITY);

		try {
			JSONObject obj = new JSONObject(result);
			intent.putExtra(INTENT_EXTRA_BROADCAST_CITY_MESSAGE, "Using city "+obj.getJSONObject("city").get("name")+", "+obj.getJSONObject("city").get("country"));
		} catch (JSONException e) {
			Log.e("WeatherFetcher", "Couldn't parse city from output", e);
			intent.putExtra(INTENT_EXTRA_BROADCAST_CITY_MESSAGE, "Error getting weather data. Is the city correct?");
		}
		
		sendBroadcast(intent);
	}
}
