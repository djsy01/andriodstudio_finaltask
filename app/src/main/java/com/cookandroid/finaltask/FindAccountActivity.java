package com.cookandroid.finaltask;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

public class FindAccountActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private LinearLayout layoutFindId, layoutFindPassword;

    private TextInputEditText editTextFindIdName, editTextFindIdPhone;
    private Button buttonFindId;
    private TextView textViewFindIdResult;

    private TextInputEditText editTextFindPasswordId, editTextFindPasswordName, editTextFindPasswordPhone;
    private Button buttonFindPassword;
    private TextView textViewFindPasswordResult;

    private Button buttonBack;
    private RedisManager redisManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_account);

        redisManager = RedisManager.getInstance();
        redisManager.init(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        layoutFindId = findViewById(R.id.layoutFindId);
        layoutFindPassword = findViewById(R.id.layoutFindPassword);

        editTextFindIdName = findViewById(R.id.editTextFindIdName);
        editTextFindIdPhone = findViewById(R.id.editTextFindIdPhone);
        buttonFindId = findViewById(R.id.buttonFindId);
        textViewFindIdResult = findViewById(R.id.textViewFindIdResult);

        editTextFindPasswordId = findViewById(R.id.editTextFindPasswordId);
        editTextFindPasswordName = findViewById(R.id.editTextFindPasswordName);
        editTextFindPasswordPhone = findViewById(R.id.editTextFindPasswordPhone);
        buttonFindPassword = findViewById(R.id.buttonFindPassword);
        textViewFindPasswordResult = findViewById(R.id.textViewFindPasswordResult);

        buttonBack = findViewById(R.id.buttonBack);
    }

    private void setupListeners() {
        // 탭 전환
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutFindId.setVisibility(View.VISIBLE);
                    layoutFindPassword.setVisibility(View.GONE);
                    clearFindIdFields();
                } else {
                    layoutFindId.setVisibility(View.GONE);
                    layoutFindPassword.setVisibility(View.VISIBLE);
                    clearFindPasswordFields();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        buttonFindId.setOnClickListener(v -> handleFindId());
        buttonFindPassword.setOnClickListener(v -> handleFindPassword());
        buttonBack.setOnClickListener(v -> finish());
    }

    private void handleFindId() {
        String name = editTextFindIdName.getText().toString().trim();
        String phone = editTextFindIdPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "이름과 전화번호를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        redisManager.findUserId(name, phone, new RedisManager.RedisCallback() {
            @Override
            public void onSuccess(String userId) {
                textViewFindIdResult.setText("회원님의 아이디는 \"" + userId + "\" 입니다.");
                textViewFindIdResult.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String error) {
                textViewFindIdResult.setText(error);
                textViewFindIdResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                textViewFindIdResult.setVisibility(View.VISIBLE);

                textViewFindIdResult.postDelayed(() -> {
                    textViewFindIdResult.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                }, 3000);
            }
        });
    }

    private void handleFindPassword() {
        String userId = editTextFindPasswordId.getText().toString().trim();
        String name = editTextFindPasswordName.getText().toString().trim();
        String phone = editTextFindPasswordPhone.getText().toString().trim();

        if (userId.isEmpty() || name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        redisManager.findPassword(userId, name, phone, new RedisManager.RedisCallback() {
            @Override
            public void onSuccess(String password) {
                textViewFindPasswordResult.setText("회원님의 비밀번호는 \"" + password + "\" 입니다.");
                textViewFindPasswordResult.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String error) {
                textViewFindPasswordResult.setText(error);
                textViewFindPasswordResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                textViewFindPasswordResult.setVisibility(View.VISIBLE);

                textViewFindPasswordResult.postDelayed(() -> {
                    textViewFindPasswordResult.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                }, 3000);
            }
        });
    }

    private void clearFindIdFields() {
        editTextFindIdName.setText("");
        editTextFindIdPhone.setText("");
        textViewFindIdResult.setVisibility(View.GONE);
    }

    private void clearFindPasswordFields() {
        editTextFindPasswordId.setText("");
        editTextFindPasswordName.setText("");
        editTextFindPasswordPhone.setText("");
        textViewFindPasswordResult.setVisibility(View.GONE);
    }
}