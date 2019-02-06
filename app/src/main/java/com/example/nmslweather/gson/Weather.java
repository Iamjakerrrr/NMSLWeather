package com.example.nmslweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {
    public Basic basic;
    public Update update;
    public String status;
    public Now now;

    @SerializedName("daily_forecast")
    public List<DailyForecast> dailyForecastList;

    @SerializedName("lifestyle")
    public List<LifeStyle> lifeStyleList;
}
