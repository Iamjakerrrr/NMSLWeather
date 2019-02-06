package com.example.nmslweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.nmslweather.MyApplication;
import com.example.nmslweather.R;
import com.example.nmslweather.gson.Weather;
import com.example.nmslweather.util.HttpUtil;
import com.example.nmslweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

public class AutoUpdateService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 8 * 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.location;
            String weatherUrl = "https://free-api.heweather.net/s6/weather?location=" +
                    weatherId + "&key=" + MyApplication.getContext().getResources().
                    getString(R.string.weather_key);
            HttpUtil.sendOkHttpRequest(weatherUrl, new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d("AutoUpdateService", Log.getStackTraceString(e));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather1 = Utility.handleWeatherResponse(responseText);
                    if (weather1 != null && weather1.status.equals("ok")) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                    }
                }
            });
        }
    }

    private void updateBingPic() {
        String url = getResources().getString(R.string.url_of_bing_pic);
        HttpUtil.sendOkHttpRequest(url, new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("AutoUpdateService", Log.getStackTraceString(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.
                        getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
