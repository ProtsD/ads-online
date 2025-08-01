package ru.ads_online.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.ads_online.pojo.dto.user.Register;
import ru.ads_online.pojo.entity.UserEntity;
import ru.ads_online.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(
                        () -> new UsernameNotFoundException("User name " + username + " not found")
                );
        return new UserPrincipal(userEntity);
    }

    public boolean userExists(String username) {
        Optional<UserEntity> userEntity = userRepository.findByUsername(username);
        return userEntity.isPresent();
    }

    public void createUser(Register register) {
        UserEntity userEntity = new UserEntity()
                .setUsername(register.getUsername())
                .setPassword(passwordEncoder.encode(register.getPassword()))
                .setFirstName(register.getFirstName())
                .setLastName(register.getLastName())
                .setPhone(register.getPhone())
                .setRole(register.getRole());
        userRepository.save(userEntity);
    }
}
