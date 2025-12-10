package com.cookandroid.finaltask;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MySQLManager {

    private static final String API_BASE_URL = "http://10.0.2.2:3000/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static MySQLManager instance;
    private final OkHttpClient client;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Gson gson;

    private MySQLManager() {
        this.client = new OkHttpClient();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
    }

    public static synchronized MySQLManager getInstance() {
        if (instance == null) {
            instance = new MySQLManager();
        }
        return instance;
    }

    public interface DatabaseCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public interface LocationListCallback {
        void onSuccess(List<SavedLocation> locations);
        void onError(String error);
    }

    public void testConnection(DatabaseCallback callback) {
        executorService.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(API_BASE_URL.replace("/api", "") + "/health")
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess("MySQL 연결 성공!"));
                } else {
                    mainHandler.post(() -> callback.onError("서버 응답 오류"));
                }
            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("서버 연결 실패: " + e.getMessage()));
            }
        });
    }

    public void initializeTables(DatabaseCallback callback) {
        mainHandler.post(() -> callback.onSuccess("테이블 초기화 완료"));
    }

    public void saveLocation(String userId, String locationName, Double latitude, Double longitude,
                             DatabaseCallback callback) {
        executorService.execute(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("userId", userId);
                json.addProperty("locationName", locationName);
                if (latitude != null) {
                    json.addProperty("latitude", latitude);
                }
                if (longitude != null) {
                    json.addProperty("longitude", longitude);
                }

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(API_BASE_URL + "/locations")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    String message = result.get("message").getAsString();
                    mainHandler.post(() -> callback.onSuccess(message));
                } else {
                    JsonObject error = gson.fromJson(responseBody, JsonObject.class);
                    String errorMsg = error.get("error").getAsString();
                    mainHandler.post(() -> callback.onError(errorMsg));
                }
            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("네트워크 오류: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("오류: " + e.getMessage()));
            }
        });
    }

    public void getUserLocations(String userId, LocationListCallback callback) {
        executorService.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(API_BASE_URL + "/locations/" + userId)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray locationsArray = result.getAsJsonArray("locations");

                    List<SavedLocation> locations = new ArrayList<>();
                    for (int i = 0; i < locationsArray.size(); i++) {
                        JsonObject loc = locationsArray.get(i).getAsJsonObject();
                        SavedLocation location = new SavedLocation();
                        location.setLocationName(loc.get("location_name").getAsString());

                        if (loc.has("latitude") && !loc.get("latitude").isJsonNull()) {
                            location.setLatitude(loc.get("latitude").getAsDouble());
                        }
                        if (loc.has("longitude") && !loc.get("longitude").isJsonNull()) {
                            location.setLongitude(loc.get("longitude").getAsDouble());
                        }

                        locations.add(location);
                    }

                    mainHandler.post(() -> callback.onSuccess(locations));
                } else {
                    JsonObject error = gson.fromJson(responseBody, JsonObject.class);
                    String errorMsg = error.get("error").getAsString();
                    mainHandler.post(() -> callback.onError(errorMsg));
                }
            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("네트워크 오류: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("오류: " + e.getMessage()));
            }
        });
    }

    public void deleteLocation(String userId, String locationName, DatabaseCallback callback) {
        executorService.execute(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("userId", userId);
                json.addProperty("locationName", locationName);

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(API_BASE_URL + "/locations")
                        .delete(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    String message = result.get("message").getAsString();
                    mainHandler.post(() -> callback.onSuccess(message));
                } else {
                    JsonObject error = gson.fromJson(responseBody, JsonObject.class);
                    String errorMsg = error.get("error").getAsString();
                    mainHandler.post(() -> callback.onError(errorMsg));
                }
            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("네트워크 오류: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("오류: " + e.getMessage()));
            }
        });
    }

    public void updateLocationOrder(String userId, List<String> orderedLocationNames, DatabaseCallback callback) {
        android.util.Log.d("MySQLManager", "updateLocationOrder - userId: " + userId);
        android.util.Log.d("MySQLManager", "Locations: " + orderedLocationNames.toString());

        executorService.execute(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("userId", userId);

                JsonArray locationsArray = new JsonArray();
                for (String locationName : orderedLocationNames) {
                    locationsArray.add(locationName);
                }
                json.add("locations", locationsArray);

                android.util.Log.d("MySQLManager", "Request: " + json.toString());

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(API_BASE_URL + "/locations/order")
                        .put(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                android.util.Log.d("MySQLManager", "Response: " + response.code() + " - " + responseBody);

                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    String message = result.get("message").getAsString();
                    mainHandler.post(() -> callback.onSuccess(message));
                } else {
                    JsonObject error = gson.fromJson(responseBody, JsonObject.class);
                    String errorMsg = error.get("error").getAsString();
                    mainHandler.post(() -> callback.onError(errorMsg));
                }
            } catch (IOException e) {
                e.printStackTrace();
                android.util.Log.e("MySQLManager", "Error: " + e.getMessage());
                mainHandler.post(() -> callback.onError("네트워크 오류: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("MySQLManager", "Error: " + e.getMessage());
                mainHandler.post(() -> callback.onError("오류: " + e.getMessage()));
            }
        });
    }

    public void saveWeatherCache(WeatherData weatherData, DatabaseCallback callback) {
        mainHandler.post(() -> callback.onSuccess("캐시 저장 완료"));
    }

    public void getWeatherCache(String locationName, DatabaseCallback callback) {
        mainHandler.post(() -> callback.onError("캐시 없음"));
    }

    public void shutdown() {
        executorService.shutdown();
    }
}