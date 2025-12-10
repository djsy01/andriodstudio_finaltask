package com.cookandroid.finaltask;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

// 누락된 import 구문 추가
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// ItemTouchHelper.Callback을 구현하여 드래그 앤 드롭 기능을 통합합니다.
public class LocationManageActivity extends AppCompatActivity
        implements LocationAdapter.OnItemClickListener, LocationAdapter.OnStartDragListener {

    private static final int LOCATION_PERMISSION_REQUEST = 101;

    private Button buttonBack, buttonCurrentLocation, buttonAddLocation;
    private TextView textViewCurrentLocation;

    private RecyclerView recyclerViewLocations; // ListView 대신 RecyclerView 사용

    // FusedLocationProviderClient 선언 유지
    private FusedLocationProviderClient fusedLocationClient;

    private MySQLManager mysqlManager;
    private SharedPreferences sharedPreferences;

    private List<SavedLocation> savedLocations;
    private LocationAdapter locationAdapter;
    private String currentUserId;

    private ItemTouchHelper itemTouchHelper; // ItemTouchHelper 추가

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_manage);

        // LocationServices 초기화 시 FusedLocationProviderClient를 사용합니다.
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

        // ID 변경: listViewLocations -> recyclerViewLocations
        recyclerViewLocations = findViewById(R.id.recyclerViewLocations);
    }

    private void setupLocationList() {
        savedLocations = new ArrayList<>();

        // ItemClickListener와 OnStartDragListener를 현재 Activity로 설정
        locationAdapter = new LocationAdapter(savedLocations, this, this);

        // RecyclerView 설정
        recyclerViewLocations.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLocations.setAdapter(locationAdapter);

        // 1. ItemTouchHelperCallback 생성 및 설정 (드래그 앤 드롭 핵심)
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(locationAdapter);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerViewLocations);
    }

    // ItemTouchHelper.Callback 구현 (LocationAdapter.OnStartDragListener 인터페이스의 유일한 메서드)
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }

    private void updateListView() {
        locationAdapter.notifyDataSetChanged();
    }

    // 아이템 클릭 시 순서 이동 다이얼로그 (ListView의 기존 로직 유지)
    @Override
    public void onItemClick(int position) {
        showMoveDialog(position);
    }

    private void showMoveDialog(int position) {
        if (savedLocations.size() <= 1) return;

        // RecyclerView를 사용해도, 이 로직은 여전히 List를 기반으로 하므로 재활용 가능합니다.
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
                if (which == 0) { // 아래로 이동
                    Collections.swap(savedLocations, position, position + 1);
                    moved = true;
                } else if (which == 1) { // 맨 아래로
                    savedLocations.remove(position);
                    savedLocations.add(item);
                    moved = true;
                }
            } else if (position == savedLocations.size() - 1) {
                if (which == 0) { // 위로 이동
                    Collections.swap(savedLocations, position, position - 1);
                    moved = true;
                } else if (which == 1) { // 맨 위로
                    savedLocations.remove(position);
                    savedLocations.add(0, item);
                    moved = true;
                }
            } else {
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

    // LocationAdapter.OnItemClickListener 인터페이스 구현
    @Override
    public void onDeleteClick(int position) {
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
                            loadUserLocations(); // 목록을 다시 로드하여 업데이트
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
    public void saveLocationOrder() {
        List<String> orderedNames = new ArrayList<>();
        // savedLocations 리스트는 이미 변경된 순서를 가지고 있습니다.
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

    /**
     * ItemTouchHelper.Callback 구현 클래스 (RecyclerView 드래그 앤 드롭 로직)
     */
    public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private final LocationAdapter mAdapter;

        public ItemTouchHelperCallback(LocationAdapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            // 위/아래 드래그만 허용 (LEFT, RIGHT는 스와이프를 의미)
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            final int swipeFlags = 0; // 스와이프 비활성화
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            // 리스트에서 실제 위치를 바꿉니다.
            mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // 스와이프를 비활성화했으므로 비워둡니다.
        }

        @Override
        public boolean isLongPressDragEnabled() {
            // 드래그 핸들 터치 시에만 드래그를 시작하도록 하기 위해 LongPress는 비활성화합니다.
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        // 드래그 상태가 변경될 때 (드래그 시작/이동 중/드래그 종료)
        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                // 드래그 시작 시 배경색 변경 등의 시각적 피드백을 줄 수 있습니다.
            } else {
                // 드래그가 끝났을 때 (손을 뗐을 때) 최종 순서를 서버에 저장합니다.
                if (viewHolder != null) {
                    // 순서 저장
                    LocationManageActivity.this.saveLocationOrder();
                }
            }
            super.onSelectedChanged(viewHolder, actionState);
        }
    }
}