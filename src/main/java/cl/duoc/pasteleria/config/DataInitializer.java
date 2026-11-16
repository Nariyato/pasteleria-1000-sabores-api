package cl.duoc.pasteleria.config;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pasteleria.model.BlogPost;
import cl.duoc.pasteleria.model.Boleta;
import cl.duoc.pasteleria.model.Categoria;
import cl.duoc.pasteleria.model.DetalleBoleta;
import cl.duoc.pasteleria.model.EstadoBoleta;
import cl.duoc.pasteleria.model.Producto;
import cl.duoc.pasteleria.model.Rol;
import cl.duoc.pasteleria.model.Tamano;
import cl.duoc.pasteleria.model.Usuario;
import cl.duoc.pasteleria.repository.BlogPostRepository;
import cl.duoc.pasteleria.repository.BoletaRepository;
import cl.duoc.pasteleria.repository.CategoriaRepository;
import cl.duoc.pasteleria.repository.ProductoRepository;
import cl.duoc.pasteleria.repository.UsuarioRepository;

/**
 * Carga los datos iniciales del front (src/data/semillas) cuando las tablas están vacías.
 * Si la tabla ya tiene datos no se toca, así que es seguro dejarlo corriendo en cada arranque.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final String LOGO = "/img/logo-pasteleria-1000-sabores.png";

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final BoletaRepository boletaRepository;
    private final BlogPostRepository blogPostRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CategoriaRepository categoriaRepository, ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository, BoletaRepository boletaRepository,
            BlogPostRepository blogPostRepository, PasswordEncoder passwordEncoder) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.boletaRepository = boletaRepository;
        this.blogPostRepository = blogPostRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoriaRepository.count() == 0) {
            cargarCategorias();
        }
        if (usuarioRepository.count() == 0) {
            cargarUsuarios();
        }
        if (productoRepository.count() == 0) {
            cargarProductos();
        }
        if (boletaRepository.count() == 0) {
            cargarBoletas();
        }
        if (blogPostRepository.count() == 0) {
            cargarBlog();
        }
    }

    private void cargarCategorias() {
        categoriaRepository.saveAll(List.of(
                categoria("Tortas Cuadradas", "Tortas cuadradas para compartir.", "/img/Torta cuadrada de chocolate.jpg"),
                categoria("Tortas Circulares", "Las clásicas tortas redondas.", "/img/Torta circular manjar.jpg"),
                categoria("Postres Individuales", "Porciones individuales.", "/img/Tiramisu clasico.jpg"),
                categoria("Productos Sin Azúcar", "Endulzados sin azúcar.", "/img/Torta sin azucar de naranja.jpg"),
                categoria("Pastelería Tradicional", "Recetas de siempre.", "/img/Empanada de manzana.jpg"),
                categoria("Productos sin gluten", "Aptos para celíacos.", "/img/torta celiacos.jpg"),
                categoria("Productos Vegana", "Sin ingredientes de origen animal.", "/img/Tartaleta-frutos-rojos.jpg"),
                categoria("Tortas Especiales", "Para cumpleaños, bodas y más.", "/img/pastel gigante.jpg")));
    }

    private void cargarUsuarios() {
        // Camila (Duoc) cumple años hoy: así se puede probar la torta gratis de cumpleaños
        LocalDate hoy = LocalDate.now();
        LocalDate cumpleCamila = LocalDate.of(2004, hoy.getMonth(), hoy.getDayOfMonth());
        LocalDate registro = LocalDate.of(2026, 10, 22);

        usuarioRepository.saveAll(List.of(
                usuario("111111111", "Admin", "Mil Sabores", "admin@duoc.cl", "admin123",
                        LocalDate.of(1990, 1, 1), Rol.ADMIN, null, registro),
                usuario("123456785", "Rosa", "Muñoz Díaz", "rosa@gmail.com", "1234",
                        LocalDate.of(1965, 3, 15), Rol.CLIENTE, null, registro),
                usuario("198765430", "Pedro", "Soto Rojas", "pedro@gmail.com", "1234",
                        LocalDate.of(1998, 7, 20), Rol.CLIENTE, "FELICES50", registro),
                usuario("212345679", "Camila", "Pérez Soto", "camila@duocuc.cl", "1234",
                        cumpleCamila, Rol.CLIENTE, null, registro),
                usuario("177777771", "Vera", "Vendedora Sabores", "vendedor@pasteleria.cl", "vendedor123",
                        LocalDate.of(1995, 5, 5), Rol.VENDEDOR, null, registro)));
    }

    private void cargarProductos() {
        List<Categoria> categorias = categoriaRepository.findAll().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
        List<Producto> productos = new ArrayList<>();
        productos.add(torta("TC001", "Torta Cuadrada de Chocolate", categorias.get(0), "cuadrada", 45000, 10, 10, true,
                "Deliciosa torta de chocolate con capas de ganache y un toque de avellanas.",
                "/img/Torta cuadrada de chocolate.jpg"));
        productos.add(torta("TC002", "Torta Cuadrada de Frutas", categorias.get(0), "cuadrada", 50000, 8, 0, false,
                "Mezcla de frutas frescas y crema chantilly sobre un suave bizcocho de vainilla.",
                LOGO));
        productos.add(torta("TT001", "Torta Circular de Vainilla", categorias.get(1), "circular", 40000, 12, 0, false,
                "Bizcocho de vainilla relleno con crema pastelera y glaseado dulce.",
                LOGO));
        productos.add(torta("TT002", "Torta Circular de Manjar", categorias.get(1), "circular", 42000, 4, 0, true,
                "Torta tradicional chilena con manjar y nueces.",
                "/img/Torta circular manjar.jpg"));
        productos.add(otro("PI001", "Mousse de Chocolate", categorias.get(2), 5000, 20, 0, false,
                "Postre individual cremoso hecho con chocolate de alta calidad.",
                LOGO));
        productos.add(otro("PI002", "Tiramisú Clásico", categorias.get(2), 5500, 15, 20, false,
                "Postre italiano con capas de café, mascarpone y cacao.",
                "/img/Tiramisu clasico.jpg"));
        productos.add(torta("PSA001", "Torta Sin Azúcar de Naranja", categorias.get(3), "circular", 48000, 3, 0, true,
                "Torta ligera de naranja endulzada naturalmente.",
                "/img/Torta sin azucar de naranja.jpg"));
        productos.add(otro("PSA002", "Cheesecake Sin Azúcar", categorias.get(3), 47000, 6, 0, false,
                "Suave y cremoso, ideal para disfrutar sin culpa.",
                LOGO));
        productos.add(otro("PT001", "Empanada de Manzana", categorias.get(4), 3000, 30, 0, false,
                "Pastelería tradicional rellena de manzana especiada.",
                "/img/Empanada de manzana.jpg"));
        productos.add(otro("PT002", "Tarta de Santiago", categorias.get(4), 6000, 0, 0, false,
                "Tradicional tarta española de almendras, azúcar y huevos.",
                LOGO));
        productos.add(otro("PG001", "Brownie Sin Gluten", categorias.get(5), 4000, 18, 0, false,
                "Rico y denso, perfecto para quienes evitan el gluten.",
                "/img/torta celiacos.jpg"));
        productos.add(otro("PG002", "Pan Sin Gluten", categorias.get(5), 3500, 2, 0, false,
                "Suave y esponjoso, ideal para sándwiches.",
                LOGO));
        productos.add(torta("PV001", "Torta Vegana de Chocolate", categorias.get(6), "circular", 50000, 7, 15, false,
                "Torta de chocolate húmeda sin ingredientes de origen animal.",
                LOGO));
        productos.add(otro("PV002", "Galletas Veganas de Avena", categorias.get(6), 4500, 25, 0, false,
                "Crujientes y sabrosas, una gran opción saludable.",
                "/img/Tartaleta-frutos-rojos.jpg"));
        productos.add(torta("TE001", "Torta Especial de Cumpleaños", categorias.get(7), "circular", 55000, 9, 0, true,
                "Personalizable con decoraciones y mensajes únicos.",
                "/img/pastel gigante.jpg"));
        productos.add(torta("TE002", "Torta Especial de Boda", categorias.get(7), "circular", 60000, 5, 0, false,
                "Elegante y deliciosa, pensada para el gran día.",
                LOGO));
        productoRepository.saveAll(productos);
    }

    private void cargarBoletas() {
        Usuario camila = usuarioRepository.findByEmailIgnoreCase("camila@duocuc.cl").orElse(null);
        if (camila == null) {
            return;
        }

        Boleta primera = boleta("B-000001", camila, EstadoBoleta.ENTREGADO, "2026-10-10T15:00:00Z", 60000);
        primera.agregarDetalle(detalle("PSA001", 1, 48000));
        primera.agregarDetalle(detalle("PT001", 4, 3000));

        Boleta segunda = boleta("B-000002", camila, EstadoBoleta.PREPARACION, "2026-10-22T18:30:00Z", 45000);
        segunda.agregarDetalle(detalle("TC001", 1, 45000));

        boletaRepository.saveAll(List.of(primera, segunda));
    }

    private void cargarBlog() {
        blogPostRepository.save(BlogPost.builder()
                .titulo("Secretos del Manjar Casero: De la Leche al Punto Caramelo")
                .categoria("Recetas tradicionales")
                .fecha(LocalDate.parse("2026-09-13"))
                .autor("Freddy Camposo, Jefe de Horno")
                .imagen("/img/manjar casero.jpg")
                .resumen("La alquimia de reducir leche entera con azúcar a fuego lento exige paciencia. Revelamos los secretos para lograr el punto perfecto.")
                .contenido(new ArrayList<>(List.of(
                "El manjar es el corazón de muchas de nuestras tortas, y hacerlo en casa es más simple de lo que parece: solo necesitas leche entera, azúcar y mucha paciencia.",
                "El secreto está en el fuego bajo y en revolver constantemente con cuchara de palo, sobre todo al final, cuando la mezcla empieza a espesar y tomar color.",
                "Para saber si está listo, pasa la cuchara por el fondo de la olla: si se ve el fondo por un par de segundos antes de que el manjar vuelva a cubrirlo, alcanzaste el punto caramelo.")))
                .build());

        blogPostRepository.save(BlogPost.builder()
                .titulo("Adaptando Tortas Clásicas para Diabéticos y Celíacos")
                .categoria("Aportes Duoc UC")
                .fecha(LocalDate.parse("2026-08-29"))
                .autor("Alan Bugueño, Duoc UC")
                .imagen("/img/torta celiacos.jpg")
                .resumen("¿Es posible conservar el alma de una torta tradicional sin gluten ni azúcar? Así trabajamos las recetas junto a estudiantes de Duoc UC.")
                .contenido(new ArrayList<>(List.of(
                "Junto a estudiantes de Gastronomía de Duoc UC probamos cómo adaptar nuestras recetas clásicas para personas con diabetes o celiaquía.",
                "Para reemplazar el azúcar usamos alulosa, que carameliza de forma parecida y mantiene la humedad del bizcocho. Para el gluten, una mezcla de harinas de arroz y almendra.",
                "El resultado son tortas como nuestra Torta Sin Azúcar de Naranja y el Brownie Sin Gluten, que ya puedes encontrar en el catálogo.")))
                .build());

        blogPostRepository.save(BlogPost.builder()
                .titulo("La Historia de Nuestro Récord Guinness de 1995")
                .categoria("Nuestra historia")
                .fecha(LocalDate.parse("2026-08-20"))
                .autor("Familia 1000 Sabores")
                .imagen("/img/pastel gigante.jpg")
                .resumen("48 horas sin dormir y más de dos mil hojas de hojarasca. Revive la jornada en que nuestra tradición familiar llegó al libro de récords.")
                .contenido(new ArrayList<>(List.of(
                "En 1995 nos propusimos algo que parecía imposible: hornear la torta más grande del mundo.",
                "Fueron 48 horas de trabajo sin descanso, más de dos mil hojas de hojarasca y litros de manjar preparados por toda la familia y nuestros vecinos.",
                "Ese día entramos al Libro Guinness de los Récords, y hasta hoy es uno de los recuerdos que más nos enorgullece.")))
                .build());

        blogPostRepository.save(BlogPost.builder()
                .titulo("5 Errores Comunes al Batir Merengue")
                .categoria("Consejos de maestros")
                .fecha(LocalDate.parse("2026-10-05"))
                .autor("Freddy Camposo, Jefe de Horno")
                .imagen("/img/amasando.jpg")
                .resumen("Si tu merengue se baja o queda líquido, probablemente estás cometiendo alguno de estos errores. Te contamos cómo evitarlos.")
                .contenido(new ArrayList<>(List.of(
                "Primer error: usar un bol con restos de grasa. Cualquier gota de aceite o de yema impide que las claras suban.",
                "Segundo y tercer error: agregar el azúcar de golpe o demasiado pronto. Hay que incorporarla de a poco, cuando las claras ya están espumosas.",
                "Cuarto y quinto error: batir de más o hacerlo en un día muy húmedo. El merengue está listo cuando forma picos firmes y brillantes.")))
                .build());
    }

    // ---- Atajos para no repetir campos ----

    private static Categoria categoria(String nombre, String descripcion, String imagen) {
        return Categoria.builder().nombre(nombre).descripcion(descripcion).imagen(imagen).build();
    }

    private Usuario usuario(String run, String nombre, String apellidos, String email, String password,
            LocalDate fechaNacimiento, Rol rol, String codigoDescuento, LocalDate fechaRegistro) {
        return Usuario.builder()
                .run(run)
                .nombre(nombre)
                .apellidos(apellidos)
                .email(email)
                .password(passwordEncoder.encode(password))
                .fechaNacimiento(fechaNacimiento)
                .rol(rol)
                .codigoDescuento(codigoDescuento)
                .fechaRegistro(fechaRegistro)
                .build();
    }

    /** Las tortas tienen forma, permiten mensaje y vienen en tres tamaños. */
    private static Producto torta(String codigo, String nombre, Categoria categoria, String forma, int precio,
            int stock, int oferta, boolean destacado, String descripcion, String imagen) {
        return Producto.builder()
                .codigo(codigo)
                .nombre(nombre)
                .categoria(categoria)
                .forma(forma)
                .precio(precio)
                .stock(stock)
                .oferta(oferta)
                .destacado(destacado)
                .activo(true)
                .permiteMensaje(true)
                .descripcion(descripcion)
                .imagen(imagen)
                .tamanos(new ArrayList<>(List.of(
                        new Tamano("chica", 8, 0),
                        new Tamano("mediana", 15, 10000),
                        new Tamano("grande", 25, 20000))))
                .build();
    }

    /** Los demás productos no tienen forma, ni mensaje, ni tamaños. */
    private static Producto otro(String codigo, String nombre, Categoria categoria, int precio, int stock,
            int oferta, boolean destacado, String descripcion, String imagen) {
        return Producto.builder()
                .codigo(codigo)
                .nombre(nombre)
                .categoria(categoria)
                .precio(precio)
                .stock(stock)
                .oferta(oferta)
                .destacado(destacado)
                .activo(true)
                .permiteMensaje(false)
                .descripcion(descripcion)
                .imagen(imagen)
                .build();
    }

    private Boleta boleta(String numero, Usuario usuario, EstadoBoleta estado, String fecha, int total) {
        return Boleta.builder()
                .numero(numero)
                .usuario(usuario)
                .estado(estado)
                .fecha(Instant.parse(fecha))
                .subtotal(total)
                .descuento(0)
                .costoDespacho(0)
                .total(total)
                .build();
    }

    private DetalleBoleta detalle(String codigoProducto, int cantidad, int precioUnitario) {
        Producto producto = productoRepository.findById(codigoProducto).orElseThrow();
        return DetalleBoleta.builder()
                .producto(producto)
                .nombre(producto.getNombre())
                .cantidad(cantidad)
                .precioUnitario(precioUnitario)
                .subtotal(cantidad * precioUnitario)
                .build();
    }
}
