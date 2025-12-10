package com.cookandroid.finaltask;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherAPI {

    private static final String API_KEY = "2ccc0d62398504f1a2041d7d2c5b05d9";
    private static final boolean USE_DUMMY_DATA = false; // 실제 API 사용
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    private final OkHttpClient client;
    private final Gson gson;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public WeatherAPI() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface WeatherCallback {
        void onSuccess(WeatherData weatherData);
        void onError(String error);
    }

    public void getWeatherByCoordinates(double lat, double lon, WeatherCallback callback) {
        if (USE_DUMMY_DATA || API_KEY.equals("YOUR_API_KEY_HERE")) {
            android.util.Log.d("WeatherAPI", "Using dummy data (USE_DUMMY_DATA=" + USE_DUMMY_DATA + ")");
            getDummyWeather("현재 위치", callback);
            return;
        }

        android.util.Log.d("WeatherAPI", "Fetching real weather data for: " + lat + ", " + lon);

        executorService.execute(() -> {
            try {
                String url = String.format("%s?lat=%f&lon=%f&appid=%s&units=metric&lang=kr",
                        BASE_URL, lat, lon, API_KEY);

                Request request = new Request.Builder()
                        .url(url)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    android.util.Log.d("WeatherAPI", "Response: " + jsonData);
                    WeatherData weatherData = parseWeatherData(jsonData);
                    android.util.Log.d("WeatherAPI", "Parsed Temperature: " + weatherData.getTemperature());

                    mainHandler.post(() -> callback.onSuccess(weatherData));
                } else {
                    android.util.Log.e("WeatherAPI", "API Failed, using dummy data");
                    getDummyWeather("현재 위치", callback);
                }
            } catch (IOException e) {
                e.printStackTrace();
                android.util.Log.e("WeatherAPI", "Network Error: " + e.getMessage());
                getDummyWeather("현재 위치", callback);
            }
        });
    }

    public void getWeatherByCity(String cityName, WeatherCallback callback) {
        // API 키가 없으면 더미 데이터 사용
        if (USE_DUMMY_DATA || API_KEY.equals("YOUR_API_KEY_HERE")) {
            getDummyWeather(cityName, callback);
            return;
        }

        executorService.execute(() -> {
            try {
                String url = String.format("%s?q=%s&appid=%s&units=metric&lang=kr",
                        BASE_URL, cityName, API_KEY);

                Request request = new Request.Builder()
                        .url(url)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    WeatherData weatherData = parseWeatherData(jsonData);

                    mainHandler.post(() -> callback.onSuccess(weatherData));
                } else {
                    getDummyWeather(cityName, callback);
                }
            } catch (IOException e) {
                e.printStackTrace();
                getDummyWeather(cityName, callback);
            }
        });
    }

    private WeatherData parseWeatherData(String jsonData) {
        JsonObject json = gson.fromJson(jsonData, JsonObject.class);

        WeatherData weatherData = new WeatherData();

        weatherData.setLocationName(json.get("name").getAsString());

        JsonObject main = json.getAsJsonObject("main");
        weatherData.setTemperature(main.get("temp").getAsDouble());
        weatherData.setFeelsLike(main.get("feels_like").getAsDouble());
        weatherData.setHumidity(main.get("humidity").getAsInt());

        JsonObject weather = json.getAsJsonArray("weather").get(0).getAsJsonObject();
        weatherData.setWeatherStatus(weather.get("description").getAsString());

        if (json.has("rain")) {
            JsonObject rain = json.getAsJsonObject("rain");
            if (rain.has("1h")) {
                weatherData.setPrecipitation(rain.get("1h").getAsDouble());
            }
        } else if (json.has("snow")) {
            JsonObject snow = json.getAsJsonObject("snow");
            if (snow.has("1h")) {
                weatherData.setPrecipitation(snow.get("1h").getAsDouble());
            }
        } else {
            weatherData.setPrecipitation(0);
        }

        weatherData.setTimestamp(System.currentTimeMillis());

        return weatherData;
    }

    public void getDummyWeather(String location, WeatherCallback callback) {
        executorService.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            WeatherData dummyData = new WeatherData(
                    location,
                    15.0 + (Math.random() * 20),
                    13.0 + (Math.random() * 20),
                    50 + (int)(Math.random() * 40),
                    Math.random() * 10, // 0-10mm
                    getRandomWeatherStatus()
            );

            mainHandler.post(() -> callback.onSuccess(dummyData));
        });
    }

    private String getRandomWeatherStatus() {
        String[] statuses = {"맑음", "구름 조금", "흐림", "비", "눈", "안개"};
        return statuses[(int)(Math.random() * statuses.length)];
    }

    public void shutdown() {
        executorService.shutdown();
    }
}