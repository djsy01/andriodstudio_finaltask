package com.cookandroid.finaltask;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editTextUserId, editTextPassword, editTextPasswordConfirm;
    private TextInputEditText editTextName, editTextPhone;
    private Button buttonCheckId, buttonSignUp, buttonCancel;

    private RedisManager redisManager;
    private boolean isIdChecked = false;
    private String checkedUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        redisManager = RedisManager.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        editTextUserId = findViewById(R.id.editTextUserId);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPasswordConfirm = findViewById(R.id.editTextPasswordConfirm);
        editTextName = findViewById(R.id.editTextName);
        editTextPhone = findViewById(R.id.editTextPhone);
        buttonCheckId = findViewById(R.id.buttonCheckId);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        buttonCancel = findViewById(R.id.buttonCancel);
    }

    private void setupListeners() {
        buttonCheckId.setOnClickListener(v -> checkIdDuplication());

        editTextUserId.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isIdChecked = false;
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        buttonSignUp.setOnClickListener(v -> handleSignUp());
        buttonCancel.setOnClickListener(v -> finish());
    }

    private void checkIdDuplication() {
        String userId = editTextUserId.getText().toString().trim();

        if (userId.isEmpty()) {
            Toast.makeText(this, "아이디를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userId.length() < 4) {
            Toast.makeText(this, "아이디는 4자 이상이어야 합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        redisManager.checkUserIdExists(userId, new RedisManager.RedisCallback() {
            @Override
            public void onSuccess(String result) {
                isIdChecked = true;
                checkedUserId = userId;
                Toast.makeText(RegisterActivity.this, result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                isIdChecked = false;
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSignUp() {
        String userId = editTextUserId.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String passwordConfirm = editTextPasswordConfirm.getText().toString().trim();
        String name = editTextName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        if (!validateInput(userId, password, passwordConfirm, name, phone)) {
            return;
        }

        redisManager.registerUser(userId, password, name, phone, new RedisManager.RedisCallback() {
            @Override
            public void onSuccess(String result) {
                Toast.makeText(RegisterActivity.this, result, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInput(String userId, String password, String passwordConfirm,
                                  String name, String phone) {
        if (userId.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty() ||
                name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력하세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isIdChecked || !checkedUserId.equals(userId)) {
            Toast.makeText(this, "아이디 중복 확인을 해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "비밀번호는 소문자, 숫자, 특수기호(~!@#등)를 포함한 8자 이상이어야 합니다",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidPhone(phone)) {
            Toast.makeText(this, "올바른 전화번호 형식을 입력하세요 (예: 01012345678)",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            return false;
        }

        Pattern lowercasePattern = Pattern.compile("[a-z]");
        if (!lowercasePattern.matcher(password).find()) {
            return false;
        }

        Pattern digitPattern = Pattern.compile("[0-9]");
        if (!digitPattern.matcher(password).find()) {
            return false;
        }

        Pattern specialPattern = Pattern.compile("[~!@#$%^&*]");
        if (!specialPattern.matcher(password).find()) {
            return false;
        }

        return true;
    }

    private boolean isValidPhone(String phone) {
        Pattern phonePattern = Pattern.compile("^01[0-9]{8,9}$");
        return phonePattern.matcher(phone).matches();
    }
}