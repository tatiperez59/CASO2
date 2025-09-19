import java.io.*;
import java.util.ArrayList;

public class Opcion1 {

    private int numProcesos;
    private int marcosTotales;

    // Datos de cada proceso
    private ArrayList<ArrayList<String>> direcciones;
    private int[] marcosAsignados;
    private int[] dvActual;
    private int[] fallosPagina;
    private int[] aciertos;
    private int[] swapSinReemplazo;
    private int[] swapConReemplazo;

    // Info de pag en memoria por proceso
    private ArrayList<ArrayList<Integer>> paginas;
    private ArrayList<ArrayList<Integer>> contadores;
    private ArrayList<ArrayList<Boolean>> bitsR;

    // Para guardar la salida
    private StringBuilder logSimulacion;

    public Opcion1(int numProcesos, int marcosTotales) {
        this.numProcesos = numProcesos;
        this.marcosTotales = marcosTotales;

        direcciones = new ArrayList<>();
        marcosAsignados = new int[numProcesos];
        dvActual = new int[numProcesos];
        fallosPagina = new int[numProcesos];
        aciertos = new int[numProcesos];
        swapSinReemplazo = new int[numProcesos];
        swapConReemplazo = new int[numProcesos];

        paginas = new ArrayList<>();
        contadores = new ArrayList<>();
        bitsR = new ArrayList<>();

        logSimulacion = new StringBuilder();

        cargarProcesos();
    }

    // Cargar archivos
    private void cargarProcesos() {
        int marcosPorProceso = marcosTotales / numProcesos;
        for (int i = 0; i < numProcesos; i++) {
            ArrayList<String> refs = new ArrayList<>();
            String nombreArchivo = "proc" + i + ".txt";
            try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {
                String linea;
                // Ignorar las primeras 5 líneas del encabezado
                for (int j = 0; j < 5; j++) {
                    br.readLine();
                }
                while ((linea = br.readLine()) != null) {
                    refs.add(linea);
                }
                direcciones.add(refs);
                marcosAsignados[i] = marcosPorProceso;
                dvActual[i] = 0;
                paginas.add(new ArrayList<>());
                contadores.add(new ArrayList<>());
                bitsR.add(new ArrayList<>());

                log("Cargado: " + nombreArchivo + " con " + refs.size() + " direcciones.");
            } catch (IOException e) {
                log("Error leyendo archivo: " + nombreArchivo);
            }
        }
    }

    private void log(String msg) {
        System.out.println(msg);
        logSimulacion.append(msg).append("\n");
    }

    private boolean procesoTerminado(int pid) {
        return dvActual[pid] >= direcciones.get(pid).size();
    }

    private int buscarPaginaMemoria(int pid, int pagina) {
        return paginas.get(pid).indexOf(pagina);
    }

    private void actualizarContadores(int pid) {
        for (int j = 0; j < paginas.get(pid).size(); j++) {
            int cont = contadores.get(pid).get(j);
            boolean r = bitsR.get(pid).get(j);
            cont = (cont >> 1) | (r ? 128 : 0);
            contadores.get(pid).set(j, cont);
            bitsR.get(pid).set(j, false);
        }
    }

    private int paginaReemplazar(int pid) {
        int minIdx = 0;
        int minVal = contadores.get(pid).get(0);
        for (int j = 1; j < paginas.get(pid).size(); j++) {
            if (contadores.get(pid).get(j) < minVal) {
                minVal = contadores.get(pid).get(j);
                minIdx = j;
            }
        }
        return minIdx;
    }

    public void simularTurnos() {
        ArrayList<Integer> cola = new ArrayList<>();
        for (int i = 0; i < numProcesos; i++) {
            if (!procesoTerminado(i)) cola.add(i);
        }

        while (!cola.isEmpty()) {
            int pid = cola.remove(0);
            if (procesoTerminado(pid)) continue;

            log("\nTurno proc: " + pid);

            String dv = direcciones.get(pid).get(dvActual[pid]);
            log("PROC " + pid + " analizando linea_: " + dvActual[pid] + " -> " + dv);

            String[] partes = dv.split(",");
            int pagina = Integer.parseInt(partes[1].trim());

            actualizarContadores(pid);
            log("PROC " + pid + " envejecimiento aplicado");

            int idx = buscarPaginaMemoria(pid, pagina);
            if (idx != -1) {
                // Hit
                bitsR.get(pid).set(idx, true);
                aciertos[pid]++;
                log("PROC " + pid + " HIT en pagina " + pagina);
                dvActual[pid]++;
            } else {
                // Fallo
                fallosPagina[pid]++;
                log("PROC " + pid + " FALLA de pagina " + pagina);
                if (paginas.get(pid).size() < marcosAsignados[pid]) {
                    // Fallo sin reemplazo
                    paginas.get(pid).add(pagina);
                    contadores.get(pid).add(0);
                    bitsR.get(pid).add(true);
                    swapSinReemplazo[pid]++;
                    log("PROC " + pid + " cargó pagina " + pagina + " en marco libre");
                    dvActual[pid]++;
                } else {
                    // Fallo con reemplazo
                    int victima = paginaReemplazar(pid);
                    int victimaPag = paginas.get(pid).get(victima);
                    paginas.get(pid).set(victima, pagina);
                    contadores.get(pid).set(victima, 0);
                    bitsR.get(pid).set(victima, true);
                    swapConReemplazo[pid]++;
                    log("PROC " + pid + " reemplazó pagina " + victimaPag + " por " + pagina);
                }
            }

            if (!procesoTerminado(pid)) {
                cola.add(pid);
            } else {
                log("Proceso " + pid + " ha terminado.");
                if (!cola.isEmpty() && marcosAsignados[pid] > 0) {
                    int maxFallos = -1;
                    int procObjetivo = -1;
                    for (int otro : cola) {
                        if (fallosPagina[otro] > maxFallos) {
                            maxFallos = fallosPagina[otro];
                            procObjetivo = otro;
                        }
                    }
                    if (procObjetivo != -1) {
                        log("Reasignando " + marcosAsignados[pid] + " marcos del proceso " + pid + " al proceso " + procObjetivo);
                        marcosAsignados[procObjetivo] += marcosAsignados[pid];
                        marcosAsignados[pid] = 0;
                    }
                }
            }
        }

        log("\n=== Simulacion finalizada ===");
        for (int i = 0; i < numProcesos; i++) {
            int totalRefs = (direcciones.get(i).size() > 0) ? direcciones.get(i).size() : 0;
            int totalSwaps = swapSinReemplazo[i] + (swapConReemplazo[i] * 2);
            double tasaFallas = (totalRefs == 0) ? 0 : (double) fallosPagina[i] / totalRefs;
            double tasaExito = (totalRefs == 0) ? 0 : (double) aciertos[i] / totalRefs;

            log("\nProceso: " + i);
            log("- Num referencias: " + totalRefs);
            log("- Fallas: " + fallosPagina[i]);
            log("- Hits: " + aciertos[i]);
            log("- SWAP: " + totalSwaps);
            log(String.format("- Tasa fallas: %.4f", tasaFallas));
            log(String.format("- Tasa exito: %.4f", tasaExito));
        }

        // Guardar salida
        try (FileWriter writer = new FileWriter("resultado.txt")) {
            writer.write(logSimulacion.toString());
            System.out.println("\n=== Resultados guardados en resultado.txt ===");
        } catch (IOException e) {
            System.err.println("Error al escribir resultado.txt");
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java Opcion1 <numProcesos> <marcosTotales>");
            return;
        }
        int numProcesos = Integer.parseInt(args[0]);
        int marcosTotales = Integer.parseInt(args[1]);
        Opcion1 simulador = new Opcion1(numProcesos, marcosTotales);
        simulador.simularTurnos();
    }
}