package com.cookandroid.finaltask;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 100;

    private TextView textViewLocationName, textViewDateTime, textViewWeatherStatus;
    private TextView textViewTemperature, textViewHumidity, textViewPrecipitation, textViewFeelsLike;
    private Spinner spinnerLocation;
    private Button buttonLocation, buttonLogout, buttonRefresh;

    private SharedPreferences sharedPreferences;
    private FusedLocationProviderClient fusedLocationClient;
    private RedisManager redisManager;
    private MySQLManager mysqlManager;
    private WeatherAPI weatherAPI;

    private List<SavedLocation> savedLocations;
    private ArrayAdapter<String> locationAdapter;
    private List<String> locationNames;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        redisManager = RedisManager.getInstance();
        mysqlManager = MySQLManager.getInstance();
        weatherAPI = new WeatherAPI();

        currentUserId = sharedPreferences.getString("userId", "");

        mysqlManager.initializeTables(new MySQLManager.DatabaseCallback() {
            @Override
            public void onSuccess(String result) {
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "DB 초기화 실패: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        initViews();
        setupLocationSpinner();
        setupListeners();
        updateDateTime();
        loadUserLocations();
    }

    private void initViews() {
        textViewLocationName = findViewById(R.id.textViewLocationName);
        textViewDateTime = findViewById(R.id.textViewDateTime);
        textViewWeatherStatus = findViewById(R.id.textViewWeatherStatus);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewPrecipitation = findViewById(R.id.textViewPrecipitation);
        textViewFeelsLike = findViewById(R.id.textViewFeelsLike);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        buttonLocation = findViewById(R.id.buttonLocation);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonRefresh = findViewById(R.id.buttonRefresh);
    }

    private void setupLocationSpinner() {
        savedLocations = new ArrayList<>();
        locationNames = new ArrayList<>();
        locationNames.add("현재 위치");

        locationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationNames);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);

        spinnerLocation.setSelection(0, false);
    }

    private void loadUserLocations() {
        mysqlManager.getUserLocations(currentUserId, new MySQLManager.LocationListCallback() {
            @Override
            public void onSuccess(List<SavedLocation> locations) {
                int currentSelection = spinnerLocation.getSelectedItemPosition();
                String selectedLocationName = null;
                if (currentSelection > 0 && currentSelection <= savedLocations.size()) {
                    selectedLocationName = savedLocations.get(currentSelection - 1).getLocationName();
                }

                savedLocations.clear();
                savedLocations.addAll(locations);

                locationNames.clear();
                locationNames.add("현재 위치");
                for (SavedLocation loc : locations) {
                    locationNames.add(loc.getLocationName());
                }

                locationAdapter.notifyDataSetChanged();

                if (selectedLocationName != null) {
                    for (int i = 0; i < savedLocations.size(); i++) {
                        if (savedLocations.get(i).getLocationName().equals(selectedLocationName)) {
                            spinnerLocation.setSelection(i + 1, false);
                            return;
                        }
                    }
                }

                if (!locations.isEmpty() && currentSelection == 0) {
                    spinnerLocation.setSelection(1, false);
                    loadWeatherForLocation(locations.get(0));
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "위치 로드 실패: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        buttonLocation.setOnClickListener(v -> navigateToLocationManage());
        buttonLogout.setOnClickListener(v -> handleLogout());
        buttonRefresh.setOnClickListener(v -> refreshWeather());

        spinnerLocation.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position == 0) {
                    getCurrentLocationWeather();
                } else {
                    SavedLocation location = savedLocations.get(position - 1);
                    loadWeatherForLocation(location);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 a hh:mm", Locale.KOREAN);
        String currentDateTime = sdf.format(new Date());
        textViewDateTime.setText(currentDateTime);
    }

    private void getCurrentLocationWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                loadWeatherByCoordinates(latitude, longitude);
            } else {
                Toast.makeText(this, "현재 위치를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWeatherForLocation(SavedLocation location) {
        if (location.getLatitude() != null && location.getLongitude() != null) {
            loadWeatherByCoordinates(location.getLatitude(), location.getLongitude());
        } else {
            loadWeatherByCity(location.getLocationName());
        }
    }

    private void loadWeatherByCoordinates(double lat, double lon) {
        weatherAPI.getWeatherByCoordinates(lat, lon, new WeatherAPI.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData weatherData) {
                displayWeather(weatherData);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "날씨 조회 실패: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWeatherByCity(String cityName) {
        weatherAPI.getWeatherByCity(cityName, new WeatherAPI.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData weatherData) {
                displayWeather(weatherData);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "날씨 조회 실패: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayWeather(WeatherData weatherData) {
        textViewLocationName.setText(weatherData.getLocationName());
        textViewWeatherStatus.setText(weatherData.getWeatherStatus());
        textViewTemperature.setText(String.format("%.0f°C", weatherData.getTemperature()));
        textViewHumidity.setText(String.format("%d%%", weatherData.getHumidity()));
        textViewPrecipitation.setText(String.format("%.1fmm", weatherData.getPrecipitation()));
        textViewFeelsLike.setText(String.format("%.0f°C", weatherData.getFeelsLike()));
    }

    private void refreshWeather() {
        updateDateTime();
        int position = spinnerLocation.getSelectedItemPosition();

        if (position == 0) {
            getCurrentLocationWeather();
        } else {
            SavedLocation location = savedLocations.get(position - 1);
            loadWeatherForLocation(location);
        }

        Toast.makeText(this, "날씨 정보를 새로고침했습니다", Toast.LENGTH_SHORT).show();
    }

    private void handleLogout() {
        String sessionId = sharedPreferences.getString("sessionId", "");

        redisManager.logout(sessionId, new RedisManager.RedisCallback() {
            @Override
            public void onSuccess(String result) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "로그아웃 실패: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToLocationManage() {
        Intent intent = new Intent(MainActivity.this, LocationManageActivity.class);
        startActivityForResult(intent, 200);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200) {
            loadUserLocations();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationWeather();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDateTime();
        loadUserLocations();
    }
}