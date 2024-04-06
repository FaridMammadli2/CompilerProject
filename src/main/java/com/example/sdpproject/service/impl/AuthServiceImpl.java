package com.example.sdpproject.service.impl;

import com.example.sdpproject.dto.auth.request.LoginRequest;
import com.example.sdpproject.dto.auth.request.RegisterRequest;
import com.example.sdpproject.email.MailSenderService;
import com.example.sdpproject.entity.Role;
import com.example.sdpproject.entity.User;
import com.example.sdpproject.entity.enums.RoleEnum;
import com.example.sdpproject.repository.RoleRepository;
import com.example.sdpproject.repository.UserRepository;
import com.example.sdpproject.service.AuthService;
import com.example.sdpproject.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional(rollbackOn = Exception.class)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final MailSenderService mailSenderService;
    private final VerificationService verificationService;
    private final RoleRepository roleRepository;

    @Value(value = "${admin.email}")
    private String ADMIN_EMAIL;

    @Override
    public String register(RegisterRequest request) {
        User user = getUser(request);
        user.addRole(
                Role
                        .builder()
                        .role(RoleEnum.USER)
                        .build()
        );
        if(ADMIN_EMAIL.equals(user.getEmail())) {
            user.addRole(
                    Role
                            .builder()
                            .role(RoleEnum.ADMIN)
                            .build()
            );
        }

        userRepository.save(user);
        verificationService.verificationCodeSending(user);
        return "Register";
    }

    @Override
    public String authentication(LoginRequest request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        if(!user.isEnabled()) {
            // TODO throw user defined exception
            throw new RuntimeException("User not verified");
        }
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim(), request.getPassword().trim())
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticate);
        securityContextRepository.saveContext(
                context, httpServletRequest, httpServletResponse);

        return "Login";
    }


    private User getUser(RegisterRequest request) {
        return User.builder()
                .userName(request.getUserName())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .build();
    }


}
