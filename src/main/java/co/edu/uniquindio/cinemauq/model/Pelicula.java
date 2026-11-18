package co.edu.uniquindio.cinemauq.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Pelicula {

    private final String id;
    private String titulo;
    private String sinopsis;
    private int duracionMinutos;
    private String genero;
    private String clasificacion;
    /** Ruta del póster dentro de resources (o URL). */
    private String imagenPelicula;
    private boolean enCartelera = true;
    private final List<Funcion> listaFunciones = new ArrayList<>();

    public Pelicula(String id, String titulo, String sinopsis, int duracionMinutos,
                    String genero, String clasificacion, String imagenPelicula) {
        this.id = id;
        this.titulo = titulo;
        this.sinopsis = sinopsis;
        this.duracionMinutos = duracionMinutos;
        this.genero = genero;
        this.clasificacion = clasificacion;
        this.imagenPelicula = imagenPelicula;
    }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getSinopsis() { return sinopsis; }
    public void setSinopsis(String sinopsis) { this.sinopsis = sinopsis; }
    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
    public String getClasificacion() { return clasificacion; }
    public void setClasificacion(String clasificacion) { this.clasificacion = clasificacion; }
    public String getImagenPelicula() { return imagenPelicula; }
    public void setImagenPelicula(String imagenPelicula) { this.imagenPelicula = imagenPelicula; }
    public List<Funcion> getListaFunciones() { return Collections.unmodifiableList(listaFunciones); }

    public void publicarEnCartelera() { enCartelera = true; }
    public void retirarDeCartelera() { enCartelera = false; }
    public boolean estaEnCartelera() { return enCartelera; }

    /** Lo llama Cine.programarFuncion para mantener la relación Pelicula 1 — 0..* Funcion. */
    void agregarFuncion(Funcion funcion) { listaFunciones.add(funcion); }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Pelicula p && id.equals(p.id));
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return titulo; }
}
