package cl.duoc.pasteleria.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.duoc.pasteleria.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Genera y valida los tokens JWT (access y refresh). La clave sale de application.properties, nunca es aleatoria. */
@Service
public class JwtService {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final SecretKey clave;
    private final Duration duracionAccess;
    private final Duration duracionRefresh;

    public JwtService(@Value("${jwt.secret}") String secreto,
            @Value("${jwt.access-minutos}") long accessMinutos,
            @Value("${jwt.refresh-dias}") long refreshDias) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.duracionAccess = Duration.ofMinutes(accessMinutos);
        this.duracionRefresh = Duration.ofDays(refreshDias);
    }

    public String generarAccessToken(Usuario usuario) {
        return generar(usuario, TIPO_ACCESS, duracionAccess);
    }

    public String generarRefreshToken(Usuario usuario) {
        return generar(usuario, TIPO_REFRESH, duracionRefresh);
    }

    public long segundosAccess() {
        return duracionAccess.toSeconds();
    }

    private String generar(Usuario usuario, String tipo, Duration duracion) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .claim("tipo", tipo)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracion.toMillis()))
                .signWith(clave)
                .compact();
    }

    /** Lanza JwtException si el token está mal firmado, vencido o mal formado. */
    public Claims leer(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }

    /** Devuelve el email (subject) si el token es válido y es del tipo pedido; si no, null. */
    public String emailSiValido(String token, String tipoEsperado) {
        try {
            Claims claims = leer(token);
            return tipoEsperado.equals(claims.get("tipo", String.class)) ? claims.getSubject() : null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}