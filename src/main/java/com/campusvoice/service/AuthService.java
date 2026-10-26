package com.campusvoice.service;

import com.campusvoice.config.JwtTokenProvider;
import com.campusvoice.dto.AuthRequest;
import com.campusvoice.dto.AuthResponse;
import com.campusvoice.dto.SignupRequest;
import com.campusvoice.dto.UserDto;
import com.campusvoice.entity.Role;
import com.campusvoice.entity.User;
import com.campusvoice.exception.BadRequestException;
import com.campusvoice.exception.ResourceNotFoundException;
import com.campusvoice.repository.DepartmentRepository;
import com.campusvoice.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       DepartmentRepository departmentRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse signup(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("An account with this email already exists.");
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters.");
        }

        if (request.getConfirmPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("The passwords do not match.");
        }

        Role requestedRole = Role.fromString(request.getRole());
        // Enforce Rule 3: A user cannot gain administrator privileges just by selecting that role in the public sign-up form
        if (requestedRole == Role.DEPARTMENT_ADMIN) {
            throw new BadRequestException("Department Administrator accounts cannot be created via public sign-up. Please contact the administrator.");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(requestedRole);
        user.setDepartment(null);

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(token, UserDto.fromEntity(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Email or password is incorrect."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email or password is incorrect.");
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            Role requestedRole = Role.fromString(request.getRole());
            if (user.getRole() != requestedRole) {
                throw new BadRequestException("The selected role does not match this account.");
            }
        }

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, UserDto.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(User currentUser) {
        if (currentUser == null) {
            throw new ResourceNotFoundException("No active session found.");
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserDto.fromEntity(user);
    }
}
