package com.example.sdpproject.service;

import com.example.sdpproject.entity.User;

public interface VerificationService {
    Boolean verifyUser(String email, String verificationCode);

    void verificationCodeSending(User user);

    String generateVerificationCode();
}
