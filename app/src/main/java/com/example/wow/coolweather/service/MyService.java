package com.example.wow.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.wow.coolweather.WeatherActivity;
import com.example.wow.coolweather.gson.Weather;
import com.example.wow.coolweather.util.HttpUtil;
import com.example.wow.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MyService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service","开启自动更新");
        updateWeather();
        updateBingPic();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Long anHour = prefs.getLong("updateTime",0);
        Log.d("service",anHour.toString());
        AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Long anhour = anHour*60*60*1000;
        long updateTime = SystemClock.elapsedRealtime() + anhour ;
        Intent in = new Intent(this,MyService.class);
        PendingIntent pi = PendingIntent.getService(this,0,in,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME,updateTime,pi);
        return super.onStartCommand(intent, flags, startId);
    }
    /**
     * 更新天气信息
     */
    private void updateWeather(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        if (weatherString != null){
            //有缓存时直接解析数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String weatherUrl = "http://guolin.tech/api/weather?cityid=" +
                    weatherId + "&key=8b2cb24557a94df1980e56dbdf9b9e4d";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    final String responseText = response.body().string();
                    final Weather weather = Utility.handleWeatherResponse(responseText);
                            if (weather != null && "ok".equals(weather.status)) {
                                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MyService.this)
                                        .edit();
                                editor.putString("weather", responseText);
                                editor.apply();
                            }
                }
            });
        }
    }

    /**
     * 更新Bing每日一图
     */
    private void updateBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MyService.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Service","关闭自动更新");
    }
}
