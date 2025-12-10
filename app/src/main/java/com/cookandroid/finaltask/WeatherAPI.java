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

    // OpenWeatherMap API 키
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

    /**
     * 위도/경도로 날씨 정보 가져오기
     */
    public void getWeatherByCoordinates(double lat, double lon, WeatherCallback callback) {
        // API 키가 없으면 더미 데이터 사용
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
                    // API 실패 시 더미 데이터
                    getDummyWeather("현재 위치", callback);
                }
            } catch (IOException e) {
                e.printStackTrace();
                android.util.Log.e("WeatherAPI", "Network Error: " + e.getMessage());
                // 네트워크 오류 시 더미 데이터
                getDummyWeather("현재 위치", callback);
            }
        });
    }

    /**
     * 도시 이름으로 날씨 정보 가져오기
     */
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
                    // API 실패 시 더미 데이터
                    getDummyWeather(cityName, callback);
                }
            } catch (IOException e) {
                e.printStackTrace();
                // 네트워크 오류 시 더미 데이터
                getDummyWeather(cityName, callback);
            }
        });
    }

    /**
     * JSON 데이터를 WeatherData 객체로 파싱
     */
    private WeatherData parseWeatherData(String jsonData) {
        JsonObject json = gson.fromJson(jsonData, JsonObject.class);

        WeatherData weatherData = new WeatherData();

        // 위치 이름
        weatherData.setLocationName(json.get("name").getAsString());

        // 날씨 정보
        JsonObject main = json.getAsJsonObject("main");
        weatherData.setTemperature(main.get("temp").getAsDouble());
        weatherData.setFeelsLike(main.get("feels_like").getAsDouble());
        weatherData.setHumidity(main.get("humidity").getAsInt());

        // 날씨 상태
        JsonObject weather = json.getAsJsonArray("weather").get(0).getAsJsonObject();
        weatherData.setWeatherStatus(weather.get("description").getAsString());

        // 강수량 (있는 경우)
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

    /**
     * 테스트용 더미 데이터 생성
     */
    public void getDummyWeather(String location, WeatherCallback callback) {
        executorService.execute(() -> {
            // 네트워크 지연 시뮬레이션
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            WeatherData dummyData = new WeatherData(
                    location,
                    15.0 + (Math.random() * 20), // 15-35도
                    13.0 + (Math.random() * 20),
                    50 + (int)(Math.random() * 40), // 50-90%
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

    /**
     * ExecutorService 종료
     */
    public void shutdown() {
        executorService.shutdown();
    }
}