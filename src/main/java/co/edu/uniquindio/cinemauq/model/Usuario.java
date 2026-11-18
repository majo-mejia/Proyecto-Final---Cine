package co.edu.uniquindio.cinemauq.model;

import co.edu.uniquindio.cinemauq.model.enums.TipoUsuario;

import java.util.Objects;

public abstract class Usuario {

    private String nombre;
    private final String documento;
    private String correoElectronico;
    private String contrasena;
    private final TipoUsuario tipo;

    protected Usuario(String nombre, String documento, String correoElectronico, String contrasena, TipoUsuario tipo) {
        this.nombre = nombre;
        this.documento = documento;
        this.correoElectronico = correoElectronico;
        this.contrasena = contrasena;
        this.tipo = tipo;
    }

    public boolean validarCredenciales(String correoElectronico, String contrasena) {
        return this.correoElectronico.equalsIgnoreCase(correoElectronico) && this.contrasena.equals(contrasena);
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDocumento() { return documento; }
    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public TipoUsuario getTipo() { return tipo; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Usuario otro && documento.equals(otro.documento));
    }

    @Override
    public int hashCode() { return Objects.hash(documento); }

    @Override
    public String toString() { return nombre + " (" + documento + ")"; }
}
