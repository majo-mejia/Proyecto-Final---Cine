package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.exceptions.DatosInvalidosException;
import co.edu.uniquindio.cinemauq.exceptions.OperacionNoPermitidaException;
import co.edu.uniquindio.cinemauq.exceptions.UsuarioYaExisteException;
import co.edu.uniquindio.cinemauq.model.enums.MetodoPago;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.Combo;
import co.edu.uniquindio.cinemauq.patrones.estructurales.composite.ItemVendible;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Datos del cine y todas sus colecciones en memoria (persistencia con listas). */
public class Cine {

    private final AtomicInteger secuencia = new AtomicInteger();

    private String nombreComercial;
    private String nit;
    private String direccion;
    private String telefono;
    private String correoElectronico;

    private final List<Usuario> listaUsuarios = new ArrayList<>();
    private final List<Pelicula> listaPeliculas = new ArrayList<>();
    private final List<Sala> listaSalas = new ArrayList<>();
    private final List<Funcion> listaFunciones = new ArrayList<>();
    private final List<ItemVendible> listaItems = new ArrayList<>();
    private final List<Promocion> listaPromociones = new ArrayList<>();
    private final List<Compra> listaCompras = new ArrayList<>();
    private final List<SolicitudRecarga> listaSolicitudesRecarga = new ArrayList<>();

    public Cine(String nombreComercial, String nit, String direccion, String telefono, String correoElectronico) {
        this.nombreComercial = nombreComercial;
        this.nit = nit;
        this.direccion = direccion;
        this.telefono = telefono;
        this.correoElectronico = correoElectronico;
    }

    public String generarId(String prefijo) {
        return prefijo + "-" + secuencia.incrementAndGet();
    }

    // ---------------- Usuarios ----------------

    public void registrarUsuario(Usuario usuario) throws UsuarioYaExisteException {
        boolean existe = listaUsuarios.stream().anyMatch(u -> u.getDocumento().equals(usuario.getDocumento())
                || u.getCorreoElectronico().equalsIgnoreCase(usuario.getCorreoElectronico()));
        if (existe) {
            throw new UsuarioYaExisteException("Ya existe un usuario con ese documento o correo.");
        }
        listaUsuarios.add(usuario);
    }

    public Optional<Usuario> buscarUsuarioPorCorreo(String correo) {
        return listaUsuarios.stream().filter(u -> u.getCorreoElectronico().equalsIgnoreCase(correo)).findFirst();
    }

    public Optional<Cliente> buscarClientePorDocumento(String documento) {
        return obtenerClientes().stream().filter(c -> c.getDocumento().equals(documento)).findFirst();
    }

    public List<Cliente> obtenerClientes() {
        return listaUsuarios.stream().filter(u -> u instanceof Cliente).map(u -> (Cliente) u).toList();
    }

    // ---------------- Películas ----------------

    public void agregarPelicula(Pelicula pelicula) throws DatosInvalidosException {
        if (buscarPelicula(pelicula.getId()).isPresent()) {
            throw new DatosInvalidosException("Ya existe una película con id " + pelicula.getId() + ".");
        }
        listaPeliculas.add(pelicula);
    }

    /** Quita la película del cine; no se permite si todavía tiene funciones programadas a futuro. */
    public void retirarPelicula(String id, LocalDateTime ahora) throws DatosInvalidosException, OperacionNoPermitidaException {
        Pelicula pelicula = buscarPelicula(id)
                .orElseThrow(() -> new DatosInvalidosException("No existe la película " + id + "."));
        boolean tieneFunciones = pelicula.getListaFunciones().stream()
                .anyMatch(f -> f.estaProgramada() && !f.yaComenzo(ahora));
        if (tieneFunciones) {
            throw new OperacionNoPermitidaException("La película tiene funciones programadas; cancélelas primero.");
        }
        listaPeliculas.remove(pelicula);
    }

    public Optional<Pelicula> buscarPelicula(String id) {
        return listaPeliculas.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public List<Pelicula> obtenerPeliculas() { return Collections.unmodifiableList(listaPeliculas); }

    // ---------------- Salas y funciones ----------------

    public void agregarSala(Sala sala) throws DatosInvalidosException {
        if (buscarSala(sala.getId()).isPresent()) {
            throw new DatosInvalidosException("Ya existe una sala con id " + sala.getId() + ".");
        }
        listaSalas.add(sala);
    }

    public Optional<Sala> buscarSala(String id) {
        return listaSalas.stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    /** Valida que la sala no tenga otra función programada que se cruce en horario. */
    public void programarFuncion(Funcion funcion) throws DatosInvalidosException {
        for (Funcion otra : listaFunciones) {
            boolean mismaSala = otra.getSala().equals(funcion.getSala());
            boolean seCruzan = funcion.getFechaHora().isBefore(otra.obtenerFechaHoraFin())
                    && otra.getFechaHora().isBefore(funcion.obtenerFechaHoraFin());
            if (mismaSala && seCruzan && otra.estaProgramada()) {
                throw new DatosInvalidosException("La sala " + funcion.getSala().getNombre()
                        + " ya tiene una función en ese horario.");
            }
        }
        listaFunciones.add(funcion);
        funcion.getPelicula().agregarFuncion(funcion);
        funcion.getSala().agregarFuncion(funcion);
    }

    public Optional<Funcion> buscarFuncion(String id) {
        return listaFunciones.stream().filter(f -> f.getId().equals(id)).findFirst();
    }

    // ---------------- Confitería y promociones ----------------

    public void agregarItem(ItemVendible item) throws DatosInvalidosException {
        if (buscarItem(item.obtenerId()).isPresent()) {
            throw new DatosInvalidosException("Ya existe un producto o combo con id " + item.obtenerId() + ".");
        }
        listaItems.add(item);
    }

    public Optional<ItemVendible> buscarItem(String id) {
        return listaItems.stream().filter(i -> i.obtenerId().equals(id)).findFirst();
    }

    /** Quita un producto o combo del menú; un producto que hace parte de un combo no se puede quitar. */
    public void retirarItem(String id) throws DatosInvalidosException, OperacionNoPermitidaException {
        ItemVendible item = buscarItem(id)
                .orElseThrow(() -> new DatosInvalidosException("No existe el producto o combo " + id + "."));
        boolean enCombo = listaItems.stream().anyMatch(i -> i instanceof Combo c && c.contiene(item));
        if (enCombo) {
            throw new OperacionNoPermitidaException(item.obtenerNombre() + " hace parte de un combo; quítelo del combo primero.");
        }
        listaItems.remove(item);
    }

    public void agregarPromocion(Promocion promocion) throws DatosInvalidosException {
        if (listaPromociones.contains(promocion)) {
            throw new DatosInvalidosException("Ya existe una promoción con id " + promocion.getId() + ".");
        }
        listaPromociones.add(promocion);
    }

    public Optional<Promocion> buscarPromocion(String id) {
        return listaPromociones.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public void retirarPromocion(String id) throws DatosInvalidosException {
        Promocion promocion = buscarPromocion(id)
                .orElseThrow(() -> new DatosInvalidosException("No existe la promoción " + id + "."));
        listaPromociones.remove(promocion);
    }

    // ---------------- Compras y recargas ----------------

    public void registrarCompra(Compra compra) { listaCompras.add(compra); }

    public void registrarSolicitudRecarga(SolicitudRecarga solicitud) { listaSolicitudesRecarga.add(solicitud); }

    public Optional<SolicitudRecarga> buscarSolicitudRecarga(String id) {
        return listaSolicitudesRecarga.stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    /**
     * Dinero recibido en el periodo: compras pagadas con tarjeta menos lo reembolsado.
     * Las compras con puntos no generan ingreso en dinero.
     */
    public double calcularIngresoPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        return listaCompras.stream()
                .filter(c -> c.getMetodo() == MetodoPago.TARJETA)
                .filter(c -> !c.getFechaHora().isBefore(desde) && !c.getFechaHora().isAfter(hasta))
                .filter(c -> c.estaPagada() || c.getReembolso() != null)
                .mapToDouble(c -> c.getTotal() - (c.getReembolso() == null ? 0 : c.getReembolso().getMonto()))
                .sum();
    }

    // ---------------- Getters / setters ----------------

    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }
    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }

    public List<Usuario> getListaUsuarios() { return Collections.unmodifiableList(listaUsuarios); }
    public List<Pelicula> getListaPeliculas() { return Collections.unmodifiableList(listaPeliculas); }
    public List<Sala> getListaSalas() { return Collections.unmodifiableList(listaSalas); }
    public List<Funcion> getListaFunciones() { return Collections.unmodifiableList(listaFunciones); }
    public List<ItemVendible> getListaItems() { return Collections.unmodifiableList(listaItems); }
    public List<Promocion> getListaPromociones() { return Collections.unmodifiableList(listaPromociones); }
    public List<Compra> getListaCompras() { return Collections.unmodifiableList(listaCompras); }
    public List<SolicitudRecarga> getListaSolicitudesRecarga() { return Collections.unmodifiableList(listaSolicitudesRecarga); }
}
