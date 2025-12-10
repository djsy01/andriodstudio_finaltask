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
        redisManager.init(this); // RedisManager 초기화

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
        // 아이디 중복 확인
        buttonCheckId.setOnClickListener(v -> checkIdDuplication());

        // 아이디 변경 시 중복 체크 초기화
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

        // Redis로 중복 확인
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

        // 유효성 검사
        if (!validateInput(userId, password, passwordConfirm, name, phone)) {
            return;
        }

        // Redis에 회원가입
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
        // 모든 필드 입력 확인
        if (userId.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty() ||
                name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력하세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 아이디 중복 체크 확인
        if (!isIdChecked || !checkedUserId.equals(userId)) {
            Toast.makeText(this, "아이디 중복 확인을 해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 비밀번호 유효성 검사
        if (!isValidPassword(password)) {
            Toast.makeText(this, "비밀번호는 소문자, 숫자, 특수기호(~!@#등)를 포함한 8자 이상이어야 합니다",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // 비밀번호 확인 일치
        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 전화번호 유효성 검사
        if (!isValidPhone(phone)) {
            Toast.makeText(this, "올바른 전화번호 형식을 입력하세요 (예: 01012345678)",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * 비밀번호 유효성 검사
     * - 8자 이상
     * - 소문자 포함
     * - 숫자 포함
     * - 특수기호(~!@#$%^&*) 포함
     */
    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            return false;
        }

        // 소문자 포함 확인
        Pattern lowercasePattern = Pattern.compile("[a-z]");
        if (!lowercasePattern.matcher(password).find()) {
            return false;
        }

        // 숫자 포함 확인
        Pattern digitPattern = Pattern.compile("[0-9]");
        if (!digitPattern.matcher(password).find()) {
            return false;
        }

        // 특수기호 포함 확인
        Pattern specialPattern = Pattern.compile("[~!@#$%^&*]");
        if (!specialPattern.matcher(password).find()) {
            return false;
        }

        return true;
    }

    /**
     * 전화번호 유효성 검사
     * - 01로 시작
     * - 9~11자리 숫자
     */
    private boolean isValidPhone(String phone) {
        Pattern phonePattern = Pattern.compile("^01[0-9]{8,9}$");
        return phonePattern.matcher(phone).matches();
    }
}