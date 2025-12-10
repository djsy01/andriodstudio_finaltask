package com.cookandroid.finaltask;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextUserId, editTextPassword;
    private Button buttonLogin, buttonRegister;
    private TextView textViewFindAccount;
    private SharedPreferences sharedPreferences;
    private RedisManager redisManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        redisManager = RedisManager.getInstance();
        redisManager.init(this); // RedisManager 초기화

        // 이미 로그인된 세션이 있는지 확인
        if (hasValidSession()) {
            navigateToMain();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        editTextUserId = findViewById(R.id.editTextUserId);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewFindAccount = findViewById(R.id.textViewFindAccount);
    }

    private void setupListeners() {
        buttonLogin.setOnClickListener(v -> handleLogin());
        buttonRegister.setOnClickListener(v -> navigateToRegister());
        textViewFindAccount.setOnClickListener(v -> navigateToFindAccount());
    }

    private void handleLogin() {
        String userId = editTextUserId.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (userId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // Redis로 로그인 처리
        redisManager.login(userId, password, new RedisManager.RedisCallback() {
            @Override
            public void onSuccess(String sessionId) {
                // 세션 ID 저장
                saveSession(sessionId, userId);
                Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSession(String sessionId, String userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("sessionId", sessionId);
        editor.putString("userId", userId);
        editor.putLong("loginTime", System.currentTimeMillis());
        editor.apply();
    }

    private boolean hasValidSession() {
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String sessionId = sharedPreferences.getString("sessionId", "");

        return isLoggedIn && !sessionId.isEmpty();
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    private void navigateToFindAccount() {
        Intent intent = new Intent(LoginActivity.this, FindAccountActivity.class);
        startActivity(intent);
    }
}