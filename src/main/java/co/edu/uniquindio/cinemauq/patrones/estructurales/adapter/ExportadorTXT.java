package co.edu.uniquindio.cinemauq.patrones.estructurales.adapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExportadorTXT implements Exportador {

    @Override
    public Path exportar(String titulo, List<String> lineas, Path destino) throws IOException {
        Path archivo = destino.resolveSibling(destino.getFileName() + "." + obtenerExtension());
        List<String> contenido = new ArrayList<>();
        contenido.add(titulo);
        contenido.add("=".repeat(titulo.length()));
        contenido.addAll(lineas);
        return Files.write(archivo, contenido, StandardCharsets.UTF_8);
    }

    @Override
    public String obtenerExtension() { return "txt"; }
}
