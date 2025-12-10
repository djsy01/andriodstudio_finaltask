package com.cookandroid.finaltask;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RedisManager {
    private static final String API_BASE_URL = "http://10.0.2.2:3000/api";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static RedisManager instance;
    private final OkHttpClient client;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Gson gson;

    private RedisManager() {
        this.client = new OkHttpClient();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
    }

    public static synchronized RedisManager getInstance() {
        if (instance == null) {
            instance = new RedisManager();
        }
        return instance;
    }

    public void init(android.content.Context context) {
    }

    public interface RedisCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public void registerUser(String userId, String password, String name, String phone, RedisCallback callback) {
        executorService.execute(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("userId", userId);
                json.addProperty("password", password);
                json.addProperty("name", name);
                json.addProperty("phone", phone);

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(API_BASE_URL + "/register")
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

    public void checkUserIdExists(String userId, RedisCallback callback) {
        executorService.execute(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("userId", userId);

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(API_BASE_URL + "/check-id")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                JsonObject result = gson.fromJson(responseBody, JsonObject.class);

                boolean available = result.get("available").getAsBoolean();
                String message = result.get("message").getAsString();

                if (available) {
                    mainHandler.post(() -> callback.onSuccess(message));
                } else {
                    mainHandler.post(() -> callback.onError(message));
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

    public void login(String userId, String password, RedisCallback callback) {
        executorService.execute(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("userId", userId);
                json.addProperty("password", password);

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(API_BASE_URL + "/login")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    String sessionId = result.get("sessionId").getAsString();
                    mainHandler.post(() -> callback.onSuccess(sessionId));
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

    public void logout(String sessionId, RedisCallback callback) {
        executorService.execute(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("sessionId", sessionId);

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(API_BASE_URL + "/logout")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                String message = result.get("message").getAsString();

                mainHandler.post(() -> callback.onSuccess(message));
            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("네트워크 오류: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("오류: " + e.getMessage()));
            }
        });
    }

    public void findUserId(String name, String phone, RedisCallback callback) {
        executorService.execute(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("name", name);
                json.addProperty("phone", phone);

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(API_BASE_URL + "/find-id")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    String userId = result.get("userId").getAsString();
                    mainHandler.post(() -> callback.onSuccess(userId));
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

    public void findPassword(String userId, String name, String phone, RedisCallback callback) {
        executorService.execute(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("userId", userId);
                json.addProperty("name", name);
                json.addProperty("phone", phone);

                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url(API_BASE_URL + "/find-password")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    String password = result.get("password").getAsString();
                    mainHandler.post(() -> callback.onSuccess(password));
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

    public void validateSession(String sessionId, RedisCallback callback) {
        mainHandler.post(() -> callback.onSuccess("valid"));
    }

    public void getUserInfo(String userId, RedisCallback callback) {
        mainHandler.post(() -> callback.onSuccess("info"));
    }

    public void testConnection(RedisCallback callback) {
        executorService.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(API_BASE_URL.replace("/api", "") + "/health")
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess("서버 연결 성공!"));
                } else {
                    mainHandler.post(() -> callback.onError("서버 응답 오류"));
                }
            } catch (IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError("서버 연결 실패: " + e.getMessage()));
            }
        });
    }

    public void close() {
        executorService.shutdown();
    }
}