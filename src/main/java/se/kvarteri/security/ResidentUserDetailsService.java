package se.kvarteri.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import se.kvarteri.models.Resident;
import se.kvarteri.repositories.ResidentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResidentUserDetailsService implements UserDetailsService {

    private final ResidentRepository residents;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Resident r = residents.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return new User(r.getEmail(), r.getPasswordHash(), r.isEnabled(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + r.getRole().name())));
    }
}
