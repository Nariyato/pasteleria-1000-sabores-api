package cl.duoc.pasteleria.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final RespuestaNoAutenticado noAutenticado;
    private final RespuestaAccesoDenegado accesoDenegado;

    public SecurityConfig(JwtFilter jwtFilter, RespuestaNoAutenticado noAutenticado,
            RespuestaAccesoDenegado accesoDenegado) {
        this.jwtFilter = jwtFilter;
        this.noAutenticado = noAutenticado;
        this.accesoDenegado = accesoDenegado;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(noAutenticado).accessDeniedHandler(accesoDenegado))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/registro",
                                "/api/v1/auth/refresh")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/auth/perfil").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/criticos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/todos").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/productos/**", "/api/v1/categorias/**",
                                "/api/v1/blog/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/mensajes").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/boletas").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/boletas/mis-boletas").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/boletas/usuario/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/boletas").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/boletas/*/estado").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/boletas/{id}").authenticated()
                        .anyRequest().hasRole("ADMIN"))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Orígenes permitidos vía `cors.origenes` / variable `CORS_ORIGENES`. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.origenes}") List<String> origenes) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenes);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }
}