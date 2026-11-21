package cl.duoc.pasteleria.dto;

import java.time.LocalDate;

import cl.duoc.pasteleria.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos públicos de un usuario (sin contraseña)")
public record UsuarioResponse(
        Long id,
        String run,
        String nombre,
        String apellidos,
        String email,
        LocalDate fechaNacimiento,
        String rol,
        String codigoDescuento,
        LocalDate fechaRegistro,
        String direccion,
        String comuna,
        String region,
        String telefono,
        String foto,
        boolean esDuoc,
        String tipoUsuario) {

    public static UsuarioResponse desde(Usuario usuario) {
        String email = usuario.getEmail();
        boolean duoc = email != null && email.toLowerCase().endsWith("@duocuc.cl");
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getRun(),
                usuario.getNombre(),
                usuario.getApellidos(),
                email,
                usuario.getFechaNacimiento(),
                usuario.getRol().aJson(),
                usuario.getCodigoDescuento(),
                usuario.getFechaRegistro(),
                usuario.getDireccion(),
                usuario.getComuna(),
                usuario.getRegion(),
                usuario.getTelefono(),
                usuario.getFoto(),
                duoc,
                duoc ? "duoc" : "normal");
    }
}