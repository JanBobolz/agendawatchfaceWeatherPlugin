package de.janbo.agendawatchface.plugins.weather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Debug;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment {
	SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			Intent intent = new Intent(getActivity(), WeatherFetchService.class);
			intent.putExtra(WeatherFetchService.INTENT_EXTRA_BROADCAST_CITY, true);
			getActivity().startService(intent);
		}
	};
	
	BroadcastReceiver cityReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			Toast.makeText(context, intent.getStringExtra(WeatherFetchService.INTENT_EXTRA_BROADCAST_CITY_MESSAGE), Toast.LENGTH_LONG).show();
		};
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(WeatherFetchService.INTENT_ACTION_BROADCAST_CITY);
		getActivity().registerReceiver(cityReceiver, filter);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(cityReceiver);
	}

	@Override
	public void onResume() {
		super.onResume();
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
	}
}
