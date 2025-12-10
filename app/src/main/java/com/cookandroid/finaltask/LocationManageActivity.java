package com.cookandroid.finaltask;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LocationManageActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 101;

    private Button buttonBack, buttonCurrentLocation, buttonAddLocation;
    private TextView textViewCurrentLocation;
    private ListView listViewLocations;

    private FusedLocationProviderClient fusedLocationClient;
    private MySQLManager mysqlManager;
    private SharedPreferences sharedPreferences;

    private List<SavedLocation> savedLocations;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_manage);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mysqlManager = MySQLManager.getInstance();
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("userId", "");

        initViews();
        setupLocationList();
        setupListeners();
        loadUserLocations();
    }

    private void initViews() {
        buttonBack = findViewById(R.id.buttonBack);
        buttonCurrentLocation = findViewById(R.id.buttonCurrentLocation);
        buttonAddLocation = findViewById(R.id.buttonAddLocation);
        textViewCurrentLocation = findViewById(R.id.textViewCurrentLocation);
        listViewLocations = findViewById(R.id.listViewLocations);
    }

    private void setupLocationList() {
        savedLocations = new ArrayList<>();

        // 간단한 ArrayAdapter 사용
        final android.widget.ArrayAdapter<String> simpleAdapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<String>()
        );
        listViewLocations.setAdapter(simpleAdapter);

        // 아이템 클릭으로 위치 이동
        listViewLocations.setOnItemClickListener((parent, view, position, id) -> {
            showMoveDialog(position);
        });

        // 롱클릭으로 삭제
        listViewLocations.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteDialog(position);
            return true;
        });
    }

    private void updateListView() {
        List<String> locationNames = new ArrayList<>();
        for (SavedLocation loc : savedLocations) {
            locationNames.add(loc.getLocationName());
        }

        android.widget.ArrayAdapter<String> adapter =
                (android.widget.ArrayAdapter<String>) listViewLocations.getAdapter();
        adapter.clear();
        adapter.addAll(locationNames);
        adapter.notifyDataSetChanged();
    }

    private void showMoveDialog(int position) {
        if (savedLocations.size() <= 1) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(savedLocations.get(position).getLocationName());

        String[] options;
        if (position == 0) {
            options = new String[]{"아래로 이동", "맨 아래로", "취소"};
        } else if (position == savedLocations.size() - 1) {
            options = new String[]{"위로 이동", "맨 위로", "취소"};
        } else {
            options = new String[]{"위로 이동", "아래로 이동", "맨 위로", "맨 아래로", "취소"};
        }

        builder.setItems(options, (dialog, which) -> {
            boolean moved = false;
            SavedLocation item = savedLocations.get(position);

            if (position == 0) {
                // 첫 번째 아이템
                if (which == 0) { // 아래로 이동
                    Collections.swap(savedLocations, position, position + 1);
                    moved = true;
                } else if (which == 1) { // 맨 아래로
                    savedLocations.remove(position);
                    savedLocations.add(item);
                    moved = true;
                }
            } else if (position == savedLocations.size() - 1) {
                // 마지막 아이템
                if (which == 0) { // 위로 이동
                    Collections.swap(savedLocations, position, position - 1);
                    moved = true;
                } else if (which == 1) { // 맨 위로
                    savedLocations.remove(position);
                    savedLocations.add(0, item);
                    moved = true;
                }
            } else {
                // 중간 아이템
                if (which == 0) { // 위로 이동
                    Collections.swap(savedLocations, position, position - 1);
                    moved = true;
                } else if (which == 1) { // 아래로 이동
                    Collections.swap(savedLocations, position, position + 1);
                    moved = true;
                } else if (which == 2) { // 맨 위로
                    savedLocations.remove(position);
                    savedLocations.add(0, item);
                    moved = true;
                } else if (which == 3) { // 맨 아래로
                    savedLocations.remove(position);
                    savedLocations.add(item);
                    moved = true;
                }
            }

            if (moved) {
                updateListView();
                saveLocationOrder();
            }
        });

        builder.show();
    }

    private void loadUserLocations() {
        mysqlManager.getUserLocations(currentUserId, new MySQLManager.LocationListCallback() {
            @Override
            public void onSuccess(List<SavedLocation> locations) {
                savedLocations.clear();
                savedLocations.addAll(locations);
                updateListView();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(LocationManageActivity.this, "위치 로드 실패: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        buttonBack.setOnClickListener(v -> finish());
        buttonCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        buttonAddLocation.setOnClickListener(v -> showAddLocationDialog());
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        textViewCurrentLocation.setText("위치를 가져오는 중...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                Geocoder geocoder = new Geocoder(this, Locale.KOREAN);
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String locationName = address.getAdminArea();
                        if (locationName == null) {
                            locationName = address.getLocality();
                        }
                        if (locationName == null) {
                            locationName = "알 수 없는 위치";
                        }

                        textViewCurrentLocation.setText("현재 위치: " + locationName +
                                "\n위도: " + String.format("%.4f", latitude) +
                                ", 경도: " + String.format("%.4f", longitude));

                        showSaveCurrentLocationDialog(locationName, latitude, longitude);
                    } else {
                        textViewCurrentLocation.setText("위도: " + latitude + ", 경도: " + longitude);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    textViewCurrentLocation.setText("위도: " + latitude + ", 경도: " + longitude);
                }
            } else {
                textViewCurrentLocation.setText("현재 위치를 가져올 수 없습니다");
                Toast.makeText(this, "위치를 가져올 수 없습니다. GPS를 확인하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSaveCurrentLocationDialog(String locationName, double lat, double lon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("현재 위치 저장");
        builder.setMessage(locationName + "을(를) 저장하시겠습니까?");

        builder.setPositiveButton("저장", (dialog, which) -> {
            mysqlManager.saveLocation(currentUserId, locationName, lat, lon,
                    new MySQLManager.DatabaseCallback() {
                        @Override
                        public void onSuccess(String result) {
                            Toast.makeText(LocationManageActivity.this, result, Toast.LENGTH_SHORT).show();
                            loadUserLocations();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(LocationManageActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showAddLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("위치 추가");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("예: Seoul");
        builder.setView(input);

        builder.setPositiveButton("추가", (dialog, which) -> {
            String newLocation = input.getText().toString().trim();
            if (!newLocation.isEmpty()) {
                mysqlManager.saveLocation(currentUserId, newLocation, null, null,
                        new MySQLManager.DatabaseCallback() {
                            @Override
                            public void onSuccess(String result) {
                                Toast.makeText(LocationManageActivity.this, newLocation + " 추가됨",
                                        Toast.LENGTH_SHORT).show();
                                loadUserLocations();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(LocationManageActivity.this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteDialog(int position) {
        SavedLocation location = savedLocations.get(position);
        String locationName = location.getLocationName();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("위치 삭제");
        builder.setMessage(locationName + "을(를) 삭제하시겠습니까?");

        builder.setPositiveButton("삭제", (dialog, which) -> {
            mysqlManager.deleteLocation(currentUserId, locationName,
                    new MySQLManager.DatabaseCallback() {
                        @Override
                        public void onSuccess(String result) {
                            Toast.makeText(LocationManageActivity.this, result, Toast.LENGTH_SHORT).show();
                            loadUserLocations();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(LocationManageActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * 위치 순서를 서버에 저장
     */
    private void saveLocationOrder() {
        List<String> orderedNames = new ArrayList<>();
        for (SavedLocation location : savedLocations) {
            orderedNames.add(location.getLocationName());
        }

        mysqlManager.updateLocationOrder(currentUserId, orderedNames,
                new MySQLManager.DatabaseCallback() {
                    @Override
                    public void onSuccess(String result) {
                        Toast.makeText(LocationManageActivity.this, "위치 순서가 저장되었습니다",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(LocationManageActivity.this, "순서 저장 실패: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                textViewCurrentLocation.setText("위치 권한이 거부되었습니다");
            }
        }
    }
}