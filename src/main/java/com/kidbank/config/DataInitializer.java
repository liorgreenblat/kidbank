package com.kidbank.config;

import com.kidbank.dto.Dtos.UserRequest;
import com.kidbank.model.User.Role;
import com.kidbank.repository.UserRepository;
import com.kidbank.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;
    private final UserRepository userRepository;

    public DataInitializer(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("הדס",   "hadas", Role.PARENT);
        seed("ליאור", "lior",  Role.PARENT);
        seed("בר",    "bar",   Role.KID);
        seed("גל",    "gal",   Role.KID);
    }

    private void seed(String name, String username, Role role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            userService.createUser(new UserRequest(name, username, role));
        }
    }
}
