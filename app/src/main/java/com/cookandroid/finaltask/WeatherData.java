package com.cookandroid.finaltask;

public class WeatherData {
    private String locationName;
    private double temperature;
    private double feelsLike;
    private int humidity;
    private double precipitation;
    private String weatherStatus;
    private long timestamp;

    public WeatherData() {
    }

    public WeatherData(String locationName, double temperature, double feelsLike,
                       int humidity, double precipitation, String weatherStatus) {
        this.locationName = locationName;
        this.temperature = temperature;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.precipitation = precipitation;
        this.weatherStatus = weatherStatus;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getLocationName() {
        return locationName;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public int getHumidity() {
        return humidity;
    }

    public double getPrecipitation() {
        return precipitation;
    }

    public String getWeatherStatus() {
        return weatherStatus;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setFeelsLike(double feelsLike) {
        this.feelsLike = feelsLike;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public void setPrecipitation(double precipitation) {
        this.precipitation = precipitation;
    }

    public void setWeatherStatus(String weatherStatus) {
        this.weatherStatus = weatherStatus;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "locationName='" + locationName + '\'' +
                ", temperature=" + temperature +
                ", feelsLike=" + feelsLike +
                ", humidity=" + humidity +
                ", precipitation=" + precipitation +
                ", weatherStatus='" + weatherStatus + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}