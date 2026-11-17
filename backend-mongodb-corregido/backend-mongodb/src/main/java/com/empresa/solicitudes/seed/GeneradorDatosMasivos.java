package com.empresa.solicitudes.seed;

import com.empresa.solicitudes.entity.Categoria;
import com.empresa.solicitudes.entity.Cliente;
import com.empresa.solicitudes.entity.Empleado;
import com.empresa.solicitudes.entity.Seguimiento;
import com.empresa.solicitudes.entity.Solicitud;
import com.empresa.solicitudes.entity.TipoSolicitud;
import com.empresa.solicitudes.repository.CategoriaRepository;
import com.empresa.solicitudes.repository.TipoSolicitudRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * GENERADOR DE DATOS MASIVOS (Base de Datos II).
 *
 * Llena MongoDB con unos 20.000 documentos VARIADOS y RELACIONADOS entre si:
 *   - 10 empleados      (coleccion "empleados")
 *   - 2.000 clientes    (coleccion "clientes")
 *   - 18.000 solicitudes (coleccion "solicitudes"), cada una con:
 *       cliente al azar, tipo (Queja, Reclamo...), 1 o 2 categorias,
 *       sede, zona, maquina, prioridad, estado, fechas de los ultimos 18 meses,
 *       empleado asignado segun el tipo, respuesta y 0 a 3 seguimientos embebidos.
 *
 * Como se activa (en application.properties):
 *   app.seed.masivo.activo=true
 * Arranca el backend una vez y vuelve a ponerlo en false.
 *
 * Solo corre UNA vez: deja una marca en la coleccion "semillas".
 * Usa una semilla fija (Random 2026): siempre genera los mismos datos.
 */
@Component
@Order(100) // corre despues de DataSeeder (que crea tipos, categorias y el admin)
public class GeneradorDatosMasivos implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GeneradorDatosMasivos.class);
    private static final String MARCA = "datos-masivos-v1";
    private static final int LOTE = 1000; // se insertan de a 1000 documentos (insertMany)

    private final MongoTemplate mongo;
    private final TipoSolicitudRepository tipoRepo;
    private final CategoriaRepository categoriaRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.masivo.activo:false}")
    private boolean activo;
    @Value("${app.seed.masivo.clientes:2000}")
    private int totalClientes;
    @Value("${app.seed.masivo.solicitudes:18000}")
    private int totalSolicitudes;

    private final Random rnd = new Random(2026);

    public GeneradorDatosMasivos(MongoTemplate mongo, TipoSolicitudRepository tipoRepo,
                                 CategoriaRepository categoriaRepo, PasswordEncoder passwordEncoder) {
        this.mongo = mongo;
        this.tipoRepo = tipoRepo;
        this.categoriaRepo = categoriaRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // ─────────────────────────── Datos base para combinar ───────────────────────────

    private static final String[] NOMBRES = {"Andrés", "Camila", "Juan", "Valentina", "Santiago", "Daniela",
            "Sebastián", "María", "Mateo", "Laura", "Nicolás", "Sofía", "Carlos", "Isabella", "Luis", "Mariana",
            "Diego", "Gabriela", "Jorge", "Natalia", "David", "Paula", "Alejandro", "Juliana", "Miguel", "Sara",
            "Felipe", "Andrea", "Julián", "Carolina", "Ricardo", "Luisa", "Esteban", "Manuela", "Óscar", "Ana",
            "Kevin", "Melissa", "Brayan", "Yuliana", "Steven", "Jhefreis", "Mario", "Daniel", "Lina", "Tatiana"};
    private static final String[] APELLIDOS = {"Gómez", "Rodríguez", "Martínez", "López", "García", "Pérez",
            "González", "Sánchez", "Ramírez", "Torres", "Díaz", "Vargas", "Castro", "Romero", "Herrera", "Medina",
            "Morales", "Ortiz", "Jiménez", "Rojas", "Payares", "Pineda", "Mendoza", "Cabarcas", "Julio", "Marrugo",
            "Barrios", "Arrieta", "Puello", "Caraballo", "Guerrero", "Silva", "Acosta", "Mercado", "Villalba"};
    private static final String[] DOMINIOS = {"gmail.com", "hotmail.com", "outlook.com", "yahoo.com", "unicartagena.edu.co"};

    private static final String[] SEDES = {"Sede Bocagrande", "Sede Manga", "Sede El Cabrero",
            "Sede Pie de la Popa", "Sede Caribe Plaza", "Sede Castillogrande"};
    private static final String[] ZONAS = {"Zona de cardio", "Zona de pesas", "Zona funcional", "Salón de spinning",
            "Piscina", "Vestidores", "Recepción", "Baños", "Parqueadero", "Zona de estiramiento"};
    private static final String[] MAQUINAS = {"Caminadora", "Bicicleta estática", "Elíptica", "Remo",
            "Prensa de piernas", "Máquina de pecho", "Polea alta", "Smith", "Banco de pesas", "Escaladora"};
    private static final String[] CARGOS = {"Administrador", "Coordinador de sede", "Técnico de mantenimiento",
            "Asesor comercial", "Entrenador", "Recepcionista"};

    // Titulos por tipo de solicitud ({m} = maquina, {z} = zona, {s} = sede)
    private static final Map<String, String[]> TITULOS = Map.of(
            "Queja", new String[]{"{m} dañada", "Mala atención en recepción", "Vestidores sucios", "Ruido excesivo en {z}",
                    "Instructor impuntual", "Aire acondicionado no funciona", "Falta de toallas", "{z} muy llena",
                    "Música demasiado alta", "Demora en la atención"},
            "Reclamo", new String[]{"Cobro doble de la mensualidad", "No me congelaron el plan", "{m} sin mantenimiento",
                    "Me cobraron una clase que no tomé", "No funciona mi huella de acceso", "Plan cancelado sin aviso",
                    "No me aplicaron el descuento", "Casillero dañado con mis cosas"},
            "Petición", new String[]{"Solicito certificado de afiliación", "Cambio de sede de mi plan",
                    "Horario extendido en {s}", "Información sobre planes familiares", "Copia de mi factura",
                    "Clases de yoga en la noche", "Congelar mi plan por viaje", "Actualizar mis datos"},
            "Sugerencia", new String[]{"Agregar más {m}", "Más clases de spinning", "Bebederos en {z}",
                    "App para reservar clases", "Más parqueaderos", "Música más variada", "Ampliar {z}"},
            "Felicitación", new String[]{"Excelente atención del entrenador", "Muy limpia la {s}",
                    "Gracias por arreglar la {m}", "Me encantan las clases grupales", "Buen servicio en recepción"});

    private static final String[] DETALLES = {"Esto ha pasado varias veces esta semana.", "Lo noté ayer en la tarde.",
            "Ya lo había reportado antes y no lo han solucionado.", "Espero una respuesta pronto.",
            "Les agradecería revisar la situación.", "Pasa sobre todo en horas pico.", "Adjunto más detalles en recepción.",
            "Soy afiliado hace más de un año.", "Me pasó a las 6 de la mañana.", "Otros usuarios también lo comentan."};
    private static final String[] RESPUESTAS = {"Gracias por tu reporte. Ya se programó el mantenimiento.",
            "Lamentamos lo ocurrido. Hablamos con el personal encargado.", "Tu solicitud fue aprobada.",
            "Ya realizamos el ajuste en tu cuenta.", "Revisamos el caso y aplicamos el reembolso.",
            "Gracias por tu sugerencia, la tendremos en cuenta.", "¡Gracias por tus palabras! Se las compartimos al equipo.",
            "El equipo técnico ya revisó el equipo y quedó funcionando.", "Te contactaremos por correo con más información."};
    private static final String[] COMENTARIOS = {"Se revisó el caso con el coordinador de la sede.",
            "Se contactó al cliente por teléfono.", "Se escaló al área técnica.", "Pendiente de repuesto.",
            "Se verificó en el sistema de pagos.", "Se agendó visita de mantenimiento.", "Caso validado con el cliente."};

    // Que categoria tiene cada tipo de solicitud mas a menudo (para que los datos tengan sentido)
    private static final Map<String, String> CATEGORIA_PROBABLE = Map.of(
            "Queja", "Atención al cliente", "Reclamo", "Facturación y pagos", "Petición", "Otro",
            "Sugerencia", "Instalaciones", "Felicitación", "Instructores");

    // Que cargo atiende cada tipo (asignacion automatica de empleado)
    private static final Map<String, String> CARGO_POR_TIPO = Map.of(
            "Queja", "Coordinador de sede", "Reclamo", "Asesor comercial", "Petición", "Recepcionista",
            "Sugerencia", "Coordinador de sede", "Felicitación", "Entrenador");

    // ─────────────────────────────────── Ejecucion ───────────────────────────────────

    @Override
    public void run(String... args) {
        if (!activo) {
            return;
        }
        if (mongo.exists(query(where("_id").is(MARCA)), "semillas")) {
            log.info("[Datos masivos] Ya se generaron antes. No se repite.");
            return;
        }
        long inicio = System.currentTimeMillis();

        List<TipoSolicitud> tipos = tipoRepo.findAll();
        List<Categoria> categorias = categoriaRepo.findAll();
        if (tipos.isEmpty() || categorias.isEmpty()) {
            log.warn("[Datos masivos] No hay tipos o categorias. Arranca primero sin generador.");
            return;
        }

        List<Empleado> empleados = generarEmpleados(categorias);
        List<String> clienteIds = generarClientes();
        int solicitudes = generarSolicitudes(tipos, categorias, empleados, clienteIds);

        mongo.insert(new Document("_id", MARCA).append("fecha", new Date())
                .append("empleados", empleados.size()).append("clientes", clienteIds.size())
                .append("solicitudes", solicitudes), "semillas");

        log.info("[Datos masivos] Listo: {} empleados, {} clientes, {} solicitudes en {} s",
                empleados.size(), clienteIds.size(), solicitudes, (System.currentTimeMillis() - inicio) / 1000);
    }

    private List<Empleado> generarEmpleados(List<Categoria> categorias) {
        String clave = passwordEncoder.encode("empleado123"); // misma clave para todos (BCrypt es lento)
        List<Empleado> lista = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Empleado e = new Empleado();
            String nombre = elegir(NOMBRES) + " " + elegir(APELLIDOS);
            e.setNombre(nombre);
            e.setEmail("empleado" + i + "@bodytech.com");
            e.setCargo(CARGOS[i % (CARGOS.length - 1) + 1]); // todos menos "Administrador"
            e.setFechaContratacion(fechaHaceDias(200 + rnd.nextInt(1200)));
            e.setContrasena(clave);
            // cada empleado atiende una categoría (se reparten en orden)
            e.setCategoriaId(categorias.get((i - 1) % categorias.size()).getId());
            lista.add(e);
        }
        mongo.insert(lista, Empleado.class);
        log.info("[Datos masivos] {} empleados (clave: empleado123)", lista.size());
        return lista;
    }

    private List<String> generarClientes() {
        String clave = passwordEncoder.encode("cliente123");
        List<String> ids = new ArrayList<>();
        List<Cliente> lote = new ArrayList<>();
        for (int i = 1; i <= totalClientes; i++) {
            String nombre = elegir(NOMBRES);
            String apellido = elegir(APELLIDOS);
            Cliente c = new Cliente();
            c.setId(new ObjectId().toHexString());
            c.setNombre(nombre + " " + apellido + " " + elegir(APELLIDOS));
            // el numero al final garantiza que el correo sea unico
            c.setEmail(sinTildes(nombre + "." + apellido).toLowerCase() + i + "@" + elegir(DOMINIOS));
            c.setTelefono("3" + (10 + rnd.nextInt(14)) + String.format("%07d", rnd.nextInt(10_000_000)));
            c.setFechaRegistro(fechaHaceDias(30 + rnd.nextInt(700)));
            c.setContrasena(clave);
            lote.add(c);
            ids.add(c.getId());
            if (lote.size() == LOTE) {
                mongo.insert(lote, Cliente.class);
                lote.clear();
            }
        }
        if (!lote.isEmpty()) mongo.insert(lote, Cliente.class);
        log.info("[Datos masivos] {} clientes (clave: cliente123)", ids.size());
        return ids;
    }

    private int generarSolicitudes(List<TipoSolicitud> tipos, List<Categoria> categorias,
                                   List<Empleado> empleados, List<String> clienteIds) {
        Map<String, TipoSolicitud> tipoPorNombre = tipos.stream()
                .collect(Collectors.toMap(TipoSolicitud::getNombre, Function.identity(), (a, b) -> a));
        Map<String, Categoria> catPorNombre = categorias.stream()
                .collect(Collectors.toMap(Categoria::getNombre, Function.identity(), (a, b) -> a));
        Map<String, List<Empleado>> empleadosPorCargo = empleados.stream()
                .collect(Collectors.groupingBy(Empleado::getCargo));

        // Pesos: hay mas quejas y peticiones que felicitaciones (como en la vida real)
        String[] tiposPonderados = {"Queja", "Queja", "Queja", "Queja", "Queja", "Queja",
                "Petición", "Petición", "Petición", "Petición", "Petición",
                "Reclamo", "Reclamo", "Reclamo", "Reclamo",
                "Sugerencia", "Sugerencia", "Sugerencia", "Felicitación", "Felicitación"};
        String[] prioridades = {"Baja", "Media", "Media", "Media", "Alta", "Alta"};

        int total = 0;
        List<Solicitud> lote = new ArrayList<>();
        for (int i = 0; i < totalSolicitudes; i++) {
            String nombreTipo = elegir(tiposPonderados);
            TipoSolicitud tipo = tipoPorNombre.getOrDefault(nombreTipo, tipos.get(rnd.nextInt(tipos.size())));

            String maquina = elegir(MAQUINAS);
            String zona = elegir(ZONAS);
            String sede = elegir(SEDES);
            String titulo = elegir(TITULOS.getOrDefault(tipo.getNombre(), TITULOS.get("Queja")))
                    .replace("{m}", maquina).replace("{z}", zona.toLowerCase()).replace("{s}", sede);

            Solicitud s = new Solicitud();
            s.setTitulo(titulo);
            s.setDescripcion(titulo + " en la " + sede + ". " + elegir(DETALLES));
            s.setTipoSolicitudId(tipo.getId());
            s.setClienteId(clienteIds.get(rnd.nextInt(clienteIds.size())));
            s.setPrioridad("Felicitación".equals(tipo.getNombre()) ? "Baja" : elegir(prioridades));
            s.setSede(sede);
            s.setZonaGimnasio(zona);
            boolean esDeMaquina = titulo.contains(maquina);
            s.setMaquinaAfectada(esDeMaquina ? maquina + " " + (1 + rnd.nextInt(12)) : null);

            // Categorias: la mas probable para el tipo + a veces una segunda
            List<String> cats = new ArrayList<>();
            Categoria principal = esDeMaquina ? catPorNombre.get("Máquinas y equipos")
                    : catPorNombre.get(CATEGORIA_PROBABLE.get(tipo.getNombre()));
            if (principal == null || rnd.nextInt(10) < 2) principal = categorias.get(rnd.nextInt(categorias.size()));
            cats.add(principal.getId());
            if (rnd.nextInt(10) < 3) {
                String otra = categorias.get(rnd.nextInt(categorias.size())).getId();
                if (!cats.contains(otra)) cats.add(otra);
            }
            s.setCategoriaIds(cats);

            // Fecha: ultimos 18 meses, con mas solicitudes recientes
            int diasAtras = (int) (540 * Math.pow(rnd.nextDouble(), 1.6));
            Date creada = fechaHaceDias(diasAtras);
            s.setFechaCreacion(creada);

            // Empleado asignado segun el tipo (si no hay de ese cargo, uno al azar)
            List<Empleado> candidatos = empleadosPorCargo.getOrDefault(CARGO_POR_TIPO.get(tipo.getNombre()), empleados);
            Empleado responsable = candidatos.get(rnd.nextInt(candidatos.size()));
            s.setEmpleadoId(responsable.getId());

            // Estado segun la antiguedad: lo viejo casi siempre esta cerrado
            String estado;
            int dado = rnd.nextInt(100);
            if (diasAtras > 60) estado = dado < 85 ? "CERRADA" : (dado < 95 ? "EN_PROCESO" : "ABIERTA");
            else if (diasAtras > 10) estado = dado < 45 ? "CERRADA" : (dado < 80 ? "EN_PROCESO" : "ABIERTA");
            else estado = dado < 10 ? "CERRADA" : (dado < 40 ? "EN_PROCESO" : "ABIERTA");
            s.setEstado(estado);

            // Seguimientos embebidos (0 a 3) y respuesta
            List<Seguimiento> segs = new ArrayList<>();
            if (!"ABIERTA".equals(estado)) {
                int n = 1 + rnd.nextInt(3);
                long t = creada.getTime();
                for (int k = 0; k < n; k++) {
                    t += (long) (1 + rnd.nextInt(72)) * 3_600_000L; // entre 1 y 72 horas despues
                    Seguimiento sg = new Seguimiento();
                    sg.setCodigo(new ObjectId().toHexString());
                    sg.setComentario(elegir(COMENTARIOS));
                    sg.setFecha(new Date(Math.min(t, System.currentTimeMillis())));
                    sg.setEmpleadoId(responsable.getId());
                    segs.add(sg);
                }
                if ("CERRADA".equals(estado)) {
                    s.setRespuestaAdmin(elegir(RESPUESTAS));
                    s.setFechaRespuesta(new Date(Math.min(t + 3_600_000L, System.currentTimeMillis())));
                }
            }
            s.setSeguimientos(segs);

            lote.add(s);
            if (lote.size() == LOTE) {
                mongo.insert(lote, Solicitud.class);
                total += lote.size();
                lote.clear();
                log.info("[Datos masivos] {} / {} solicitudes...", total, totalSolicitudes);
            }
        }
        if (!lote.isEmpty()) {
            mongo.insert(lote, Solicitud.class);
            total += lote.size();
        }
        return total;
    }

    // ─────────────────────────────────── Ayudantes ───────────────────────────────────

    private String elegir(String[] opciones) {
        return opciones[rnd.nextInt(opciones.length)];
    }

    private Date fechaHaceDias(int dias) {
        long ms = (long) dias * 86_400_000L + (long) rnd.nextInt(86_400) * 1000L;
        return new Date(System.currentTimeMillis() - ms);
    }

    private static String sinTildes(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace(" ", "");
    }
}
