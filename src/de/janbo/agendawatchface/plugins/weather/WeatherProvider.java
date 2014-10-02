package de.janbo.agendawatchface.plugins.weather;

import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.janbo.agendawatchface.api.AgendaItem;
import de.janbo.agendawatchface.api.AgendaWatchfacePlugin;
import de.janbo.agendawatchface.api.LineOverflowBehavior;
import de.janbo.agendawatchface.api.TimeDisplayType;

public class WeatherProvider extends AgendaWatchfacePlugin {

	@Override
	public String getPluginId() {
		return "de.janbo.agendwatchface.plugins.weather";
	}

	@Override
	public String getPluginDisplayName() {
		return "OpenWeatherMap";
	}

	@Override
	public void onRefreshRequest(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!prefs.getBoolean("pref_key_weather_activate", true))
			return;
		
		SharedPreferences dataPrefs = context.getSharedPreferences("weatherData", 0);
		if (Math.abs(System.currentTimeMillis() - dataPrefs.getLong("lastUpdate", 0)) >= 1000 * 60 * 60)
			context.startService(new Intent(context, WeatherFetchService.class));

		publishWeather(context);
	}

	/**
	 * Sends current weather data to the AgendaWatchfaceService
	 */
	public void publishWeather(Context context) {
		ArrayList<AgendaItem> result = new ArrayList<AgendaItem>();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!prefs.getBoolean("pref_key_weather_activate", true)) {
			publishData(context, result, false);
			return;
		}
		
		try {
			JSONObject data = new JSONObject(context.getSharedPreferences("weatherData", 0).getString("currentForecastDataset", ""));
			JSONArray list = data.getJSONArray("list");

			for (int i = 0; i < list.length(); i++) {
				AgendaItem item = weatherDataToItem(context, list.getJSONObject(i));
				if (item != null)
					result.add(item);
			}
		} catch (RuntimeException e) {
			Log.e("WeatherProvider", "Error parsing", e);
			result.clear();
		} catch (JSONException e) {
			Log.e("WeatherProvider", "Error parsing", e);
			result.clear();
		}

		publishData(context, result, false);
	}

	/**
	 * Creates an AgendaItem from the JSONObject that's the weather data
	 */
	private AgendaItem weatherDataToItem(Context context, JSONObject obj) {
		try {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			AgendaItem item = new AgendaItem(getPluginId());

			// Set times
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(obj.getLong("dt") * 1000);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);

			item.startTime = cal.getTime();

			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			item.endTime = cal.getTime();

			// Make it show above all-day events
			item.priority = 100;

			// Set lines
			item.line1.timeDisplay = TimeDisplayType.NONE;
			item.line1.textBold = false;
			item.line1.overflow = LineOverflowBehavior.OVERFLOW_IF_NECESSARY;

			double min = (obj.getJSONObject("temp").getDouble("min") - 273.15);
			double max = (obj.getJSONObject("temp").getDouble("max") - 273.15);
			if (prefs.getBoolean("pref_fahrenheit", false)) {
				min = min*(9.0/5.0)+32;
				max = max*(9.0/5.0)+32;
			}
			long displayMin = Math.round(min);
			long displayMax = Math.round(max);
			
			item.line1.text = (obj.has("weather") && obj.getJSONArray("weather").length() > 0 ? obj.getJSONArray("weather").getJSONObject(0).getString("description") + " " : "") + displayMin + "°-"
					+ displayMax + "°";
			item.line1.text = item.line1.text.replace("intensity ", "");

			return item;

		} catch (JSONException e) {
			Log.e("WeatherProvider", "Something went wrong creating the AgendaItem " + obj, e);
		} catch (RuntimeException e) {
			Log.e("WeatherProvider", "Something went wrong creating the AgendaItem " + obj, e);
		}

		return null;
	}

	@Override
	public void onShowSettingsRequest(Context context) {
		// Start our settings activity
		Intent intent = new Intent(context, SettingsActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
