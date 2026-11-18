package co.edu.uniquindio.cinemauq.patrones.estructurales.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Los dos exportadores se usan igual a través de la interfaz Exportador (Adapter). */
class ExportadoresTest {

    private static final List<String> LINEAS = List.of("Película         Entradas", "Dune                    12", "",
            "Total                   12");

    @Test
    void exportadorTXTEscribeTituloYLineas(@TempDir Path carpeta) throws Exception {
        Exportador exportador = new ExportadorTXT();
        Path archivo = exportador.exportar("Entradas por película", LINEAS, carpeta.resolve("reporte"));

        assertEquals("reporte.txt", archivo.getFileName().toString());
        List<String> contenido = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        assertEquals("Entradas por película", contenido.get(0));
        assertEquals("Total                   12", contenido.get(contenido.size() - 1));
    }

    @Test
    void exportadorPDFAdaptaOpenPDFYGeneraUnPDFValido(@TempDir Path carpeta) throws Exception {
        Exportador exportador = new ExportadorPDFAdapter();
        Path archivo = exportador.exportar("Entradas por película", LINEAS, carpeta.resolve("reporte"));

        assertEquals("reporte.pdf", archivo.getFileName().toString());
        byte[] bytes = Files.readAllBytes(archivo);
        String inicio = new String(bytes, 0, 5, StandardCharsets.US_ASCII);
        String fin = new String(bytes, bytes.length - 6, 6, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", inicio);   // cabecera de todo PDF
        assertTrue(fin.contains("%%EOF")); // el documento se cerró bien
    }
}
