package com.event_registration_platform.service;

import com.event_registration_platform.dto.RegisterRequest;
import com.event_registration_platform.dto.UserResponseDTO;
import com.event_registration_platform.entity.Role;
import com.event_registration_platform.entity.User;
import com.event_registration_platform.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,PasswordEncoder passwordEncoder){
        this.userRepository=userRepository;
        this.passwordEncoder =passwordEncoder;
    }
    @Override
    public UserResponseDTO registerUser(RegisterRequest request){

        if (userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new RuntimeException("Email already registered");

        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ATTENDEE);

        User savedUser = userRepository.save(user);

        return new UserResponseDTO(savedUser.getName(),savedUser.getEmail(),savedUser.getRole().toString());




    }
}
