package cl.duoc.pasteleria.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import cl.duoc.pasteleria.repository.UsuarioRepository;

/** Spring Security busca al usuario por correo. El rol se convierte en la autoridad ROLE_ADMIN, ROLE_VENDEDOR o ROLE_CLIENTE. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .map(u -> User.withUsername(u.getEmail())
                        .password(u.getPassword())
                        .authorities(new SimpleGrantedAuthority("ROLE_" + u.getRol().name()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario con email: " + email));
    }
}