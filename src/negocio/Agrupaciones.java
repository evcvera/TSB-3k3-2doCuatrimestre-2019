package negocio;

import soporte.TSBHashtableDA;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Agrupaciones {
    private TSBHashtableDA votacion;

    public Agrupaciones(String carpeta) throws FileNotFoundException
    {
        votacion = identificarAgrupaciones(carpeta + "\\descripcion_postulaciones.dsv");
    }

    public TSBHashtableDA generarVacia() {
        TSBHashtableDA tabla = new TSBHashtableDA();
        for (Object a : votacion.values()) {
            Agrupacion agrupacion = (Agrupacion) a;
            tabla.put(agrupacion.getCodigo(), new Agrupacion(agrupacion));
        }
        return tabla;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Agrupaciones{");
        sb.append("agrupaciones=").append(votacion);
        sb.append('}');
        return sb.toString();
    }

    public TSBHashtableDA identificarAgrupaciones(String path) throws FileNotFoundException {
        String linea, campos[], categoria, codigo, nombre;
        TSBHashtableDA table = new TSBHashtableDA();
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(path));
            while (scanner.hasNextLine()) {
                linea = scanner.nextLine();
                campos = linea.split("\\|");
                categoria = campos[0];
                if (categoria.compareTo("000100000000000") == 0) {
                    codigo = campos[2];
                    nombre = campos[3];
                    table.put(codigo, new Agrupacion(codigo,nombre,0));
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Archivo no encontrado " + e);
            throw e;
        }
        return table;
    }



}
