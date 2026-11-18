package co.edu.uniquindio.cinemauq.patrones.estructurales.adapter;

import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Patrón ADAPTER.
 * Problema: el sistema exporta reportes a través de la interfaz Exportador
 * (exportar(titulo, lineas, destino)), pero la librería externa OpenPDF tiene otra interfaz:
 * hay que crear un Document, asociarle un PdfWriter, abrirlo, agregar Paragraph con fuentes y
 * cerrarlo, y sus errores son DocumentException. No podemos cambiar la librería para que
 * implemente Exportador, así que esta clase la envuelve y traduce una interfaz a la otra.
 * Quien exporta solo conoce Exportador: puede usar ExportadorTXT o este adaptador sin cambiar
 * su código (OCP y DIP).
 */
public class ExportadorPDFAdapter implements Exportador {

    private static final Font FUENTE_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    /** Fuente de ancho fijo para que las columnas del reporte queden alineadas como en el TXT. */
    private static final Font FUENTE_LINEA = FontFactory.getFont(FontFactory.COURIER, 9);

    @Override
    public Path exportar(String titulo, List<String> lineas, Path destino) throws IOException {
        Path archivo = destino.resolveSibling(destino.getFileName() + "." + obtenerExtension());
        Document documento = new Document(PageSize.LETTER, 50, 50, 50, 50);
        try (OutputStream salida = Files.newOutputStream(archivo)) {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Paragraph encabezado = new Paragraph(titulo, FUENTE_TITULO);
            encabezado.setSpacingAfter(12);
            documento.add(encabezado);
            for (String linea : lineas) {
                // Un párrafo vacío no ocupa espacio; un espacio conserva las líneas en blanco del reporte.
                documento.add(new Paragraph(linea.isEmpty() ? " " : linea, FUENTE_LINEA));
            }
            // Al cerrar, OpenPDF escribe el final del PDF; debe pasar antes de cerrar el archivo.
            documento.close();
        } catch (DocumentException e) {
            // Se traduce la excepción de la librería a la que espera la interfaz Exportador.
            throw new IOException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
        return archivo;
    }

    @Override
    public String obtenerExtension() { return "pdf"; }
}
