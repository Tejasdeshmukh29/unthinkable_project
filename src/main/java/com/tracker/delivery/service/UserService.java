package com.tracker.delivery.service;

import com.tracker.delivery.entity.Role;
import com.tracker.delivery.entity.User;
import com.tracker.delivery.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == Role.ROLE_AGENT) {
            user.setStatus("AVAILABLE");
            if (user.getLatitude() == null || user.getLongitude() == null) {
                // Initialize default coordinates if not provided (e.g. Center of Delhi)
                user.setLatitude(28.6139);
                user.setLongitude(77.2090);
            }
        } else {
            user.setStatus("ACTIVE");
        }

        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> getAvailableAgents() {
        return userRepository.findByRoleAndStatus(Role.ROLE_AGENT, "AVAILABLE");
    }

    public List<User> getAllAgents() {
        return userRepository.findByRole(Role.ROLE_AGENT);
    }

    public List<User> getAllCustomers() {
        return userRepository.findByRole(Role.ROLE_CUSTOMER);
    }

    public User updateAgentLocation(Long agentId, Double latitude, Double longitude) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));

        if (agent.getRole() != Role.ROLE_AGENT) {
            throw new IllegalArgumentException("User is not a delivery agent.");
        }

        agent.setLatitude(latitude);
        agent.setLongitude(longitude);
        return userRepository.save(agent);
    }

    public User updateAgentStatus(Long agentId, String status) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));

        if (agent.getRole() != Role.ROLE_AGENT) {
            throw new IllegalArgumentException("User is not a delivery agent.");
        }

        if (!status.equals("AVAILABLE") && !status.equals("BUSY") && !status.equals("OFFLINE")) {
            throw new IllegalArgumentException("Invalid agent status. Must be AVAILABLE, BUSY, or OFFLINE.");
        }

        agent.setStatus(status);
        return userRepository.save(agent);
    }
}
