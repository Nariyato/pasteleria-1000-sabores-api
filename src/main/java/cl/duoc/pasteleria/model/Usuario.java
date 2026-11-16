package cl.duoc.pasteleria.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 12)
    private String run;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 120)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** Siempre se guarda encriptada con BCrypt (60 caracteres), nunca en texto plano. */
    @Column(nullable = false, length = 100)
    private String password;

    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Código de descuento del registro (por ejemplo FELICES50). */
    @Column(length = 30)
    private String codigoDescuento;

    private LocalDate fechaRegistro;

    @Column(length = 150)
    private String direccion;

    @Column(length = 60)
    private String comuna;

    @Column(length = 60)
    private String region;

    @Column(length = 20)
    private String telefono;

    /** Foto de perfil (en el front es una imagen reducida en base64). */
    @Lob
    private String foto;
}
