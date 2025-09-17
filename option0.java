import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Opcion0 {

    public void opcion0(String archivoAUX) {
        int tp = 0;
        int nproc = 0;
        ArrayList<Integer> tamanos = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivoAUX))) {
            String linea;
            int contadorLineas = 0;
            while ((linea = br.readLine()) != null && contadorLineas < 3) {
                String[] partes = linea.split("=");
                if (partes.length == 2) {
                    String clave = partes[0].trim();
                    String valor = partes[1].trim();
                    if (clave.equalsIgnoreCase("TP")) {
                        tp = Integer.parseInt(valor);
                    } else if (clave.equalsIgnoreCase("NPROC")) {
                        nproc = Integer.parseInt(valor);
                    } else if (clave.equalsIgnoreCase("TAMS")) {
                        String[] tamanosStr = valor.split(",");
                        for (String tamanoStr : tamanosStr) {
                            String[] partesTamano = tamanoStr.trim().split("x");
                            if (partesTamano.length == 2) {
                                int total = Integer.parseInt(partesTamano[0]) * Integer.parseInt(partesTamano[1]);
                                tamanos.add(total);
                            }
                        }
                    }
                }
                contadorLineas++;
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error: Archivo no encontrado o no se puede leer. Por favor, revisa la ruta y el contenido del archivo.");
            e.printStackTrace();
            return;
        }

        // BUCLE PRINCIPAL PARA GENERAR UN ARCHIVO POR CADA TAMAÑO DE MATRIZ
        for (int i = 0; i < nproc && i < tamanos.size(); i++) {
            String nombreArchivo = "proces" + i + ".txt";
            try (FileWriter writer = new FileWriter(nombreArchivo)) {

                // --- Preparación de variables para esta simulación ---
                int nf = (int) Math.sqrt(tamanos.get(i));
                int nc = nf;
                int totalElementosMatriz = nf * nc;

                writer.write("TP: " + tp + "\n");
                writer.write("NF: " + nf + "\n");
                writer.write("NC: " + nc + "\n");
                int nr = totalElementosMatriz * 3;
                writer.write("NR: " + nr + "\n");
                int np = (int) Math.ceil((double) (nr * 4) / tp);
                writer.write("NP: " + np + "\n");

                int countsFILA = 0;
                int countsCOL = 0;
                String[] r_w = {"r", "r", "w"};

                int[] desplazamientos = new int[3];
                desplazamientos[0] = 0;                             
                desplazamientos[1] = totalElementosMatriz * 4;      
                desplazamientos[2] = totalElementosMatriz * 4 * 2;  

                for (int elemento = 0; elemento < totalElementosMatriz; elemento++) {
                    for (int matriz = 0; matriz < 3; matriz++) {
                        
                        // --- LÓGICA DE PAGINACIÓN ABSOLUTA ---
                        int desplazamientoAbsoluto = desplazamientos[matriz];
                        int paginaActual = desplazamientoAbsoluto / tp;
                        int desplazamientoEnPagina = desplazamientoAbsoluto % tp;

                        String imp = "M" + (matriz + 1) + ": " + "[" + countsFILA + "-" + countsCOL + "]" + "," + paginaActual + "," + desplazamientoEnPagina + "," + r_w[matriz];
                        writer.write(imp + "\n");
                        
                        desplazamientos[matriz] += 4;
                    }

                    // Actualizamos las coordenadas [fila-columna] para el siguiente ciclo
                    countsCOL++;
                    if (countsCOL == nf) {
                        countsCOL = 0;
                        countsFILA++;
                    }
                }
                System.out.println("Archivo creado exitosamente: " + nombreArchivo);
            } catch (IOException e) {
                System.err.println("Error al escribir el archivo: " + nombreArchivo);
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Opcion0 opcion0 = new Opcion0();
        // IMPORTANTE: Asegúrate de que esta ruta sea la correcta en tu computador
        String ruta = "caso2//arc.txt";
        opcion0.opcion0(ruta);
    }
}