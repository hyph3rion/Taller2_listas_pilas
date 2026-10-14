package servicios;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import estructuras.Pila;
import model.Cliente;

public class GestionContingencia {

    public static final String ARCHIVO_INFORME = "informe_pila_contingencia.txt";

    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public Pila<Cliente> generarPilaUrgencias(List<Cliente> clientes) {
        Pila<Cliente> pila = new Pila<>();
        if (clientes == null || clientes.isEmpty()) {
            return pila;
        }

        List<Cliente> seleccionados = new ArrayList<>();
        for (Cliente c : clientes) {
            if (c != null && c.esExtraccion() && c.isUrgente()) {
                if (c.getFechaCita() == null) {
                    c.setFechaCita(new java.util.Date());
                }
                seleccionados.add(c);
            }
        }

        ordenarPorFechaAscendente(seleccionados);

        for (int i = seleccionados.size() - 1; i >= 0; i--) {
            pila.apilar(seleccionados.get(i));
        }
        return pila;
    }

    private void ordenarPorFechaAscendente(List<Cliente> lista) {
        for (int i = 1; i < lista.size(); i++) {
            Cliente actual = lista.get(i);
            int j = i - 1;
            while (j >= 0 && lista.get(j).getFechaCita().after(actual.getFechaCita())) {
                lista.set(j + 1, lista.get(j));
                j--;
            }
            lista.set(j + 1, actual);
        }
    }

    public String generarReportePila(Pila<Cliente> pila, String nombreConsultorio) {
        StringBuilder sb = new StringBuilder();
        sb.append("==========================================================================================\n");
        sb.append("        INFORME PLAN DE CONTINGENCIA - EXTRACCIONES DENTALES URGENTES (PILA)\n");
        if (nombreConsultorio != null && !nombreConsultorio.trim().isEmpty()) {
            sb.append("        Consultorio: ").append(nombreConsultorio).append("\n");
        }
        sb.append("        Generado: ").append(formatoFecha.format(new java.util.Date())).append("\n");
        sb.append("        Orden: fecha de cita de la mas cercana a la mas lejana\n");
        sb.append("==========================================================================================\n");
        sb.append(String.format("%-6s %-12s %-22s %-12s %-17s %-11s %-9s\n",
                "ORDEN", "CEDULA", "PACIENTE", "TELEFONO", "FECHA CITA", "ATENCION", "PRIORIDAD"));
        sb.append("------------------------------------------------------------------------------------------\n");

        if (pila == null || pila.estaVacia()) {
            sb.append("No hay clientes con citas de extraccion urgentes pendientes.\n");
            return sb.toString();
        }

        int orden = 1;
        for (Cliente c : pila.recorrer()) {
            sb.append(String.format("%-6d %-12d %-22s %-12d %-17s %-11s %-9s\n",
                    orden++,
                    c.getCedula(),
                    c.getNombre(),
                    c.getTelefono(),
                    formatoFecha.format(c.getFechaCita()),
                    c.getTipoDeAtencion().get(0),
                    c.getPrioridad()));
        }
        sb.append("==========================================================================================\n");
        sb.append("Total clientes en la pila: ").append(pila.tamano()).append("\n");
        sb.append("Siguiente a llamar (cima): ").append(pila.verCima().getNombre()).append("\n");
        return sb.toString();
    }

    public boolean guardarInforme(String contenido) {
        try (FileWriter fw = new FileWriter(ARCHIVO_INFORME, false)) {
            fw.write(contenido);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}