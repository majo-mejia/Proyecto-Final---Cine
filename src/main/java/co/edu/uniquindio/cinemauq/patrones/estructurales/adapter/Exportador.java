package co.edu.uniquindio.cinemauq.patrones.estructurales.adapter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Patrón ADAPTER (interfaz objetivo). Es la interfaz común de exportación de reportes: el resto
 * del sistema no sabe si exporta a TXT (ExportadorTXT) o a PDF (ExportadorPDFAdapter, que
 * adapta la librería OpenPDF a esta interfaz).
 */
public interface Exportador {

    /** Escribe el reporte en "destino" (sin extensión; el exportador agrega la suya) y devuelve el archivo creado. */
    Path exportar(String titulo, List<String> lineas, Path destino) throws IOException;

    String obtenerExtension();
}
