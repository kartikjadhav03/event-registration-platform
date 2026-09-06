package com.event_registration_platform.service;

import com.event_registration_platform.dto.RegisterRequest;
import com.event_registration_platform.dto.UserResponseDTO;

public interface UserService {
    UserResponseDTO registerUser(RegisterRequest request);

}
