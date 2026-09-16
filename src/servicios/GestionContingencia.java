package servicios;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import model.Cliente;

public class GestionContingencia {

    public Deque<Cliente> generarPilaUrgencias(List<Cliente> clientes) {
        Deque<Cliente> pilaContingencia = new ArrayDeque<>();

        if (clientes == null || clientes.isEmpty()) {
            return pilaContingencia;
        }

        List<Cliente> filtrados = clientes.stream()
            .filter(Cliente::isUrgente)
            .sorted(Comparator
                .comparing(Cliente::esExtraccion).reversed()
                .thenComparing((Cliente c) -> c.getFechaCita() != null ? c.getFechaCita() : new java.util.Date()).reversed()
            )
            .collect(Collectors.toList());

        for (Cliente c : filtrados) {
            pilaContingencia.push(c);
        }

        return pilaContingencia;
    }

    public String generarReportePila(Deque<Cliente> pila, String nombreConsultorio) {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("         INFORME DE CONTINGENCIA - LLAMADAS PRIORITARIAS (EXTRACCIONES)        \n");
        if (nombreConsultorio != null && !nombreConsultorio.trim().isEmpty()) {
            sb.append("         Origen: ").append(nombreConsultorio).append("\n");
        }
        sb.append("================================================================================\n");
        sb.append(String.format("%-6s %-10s %-20s %-16s %-14s %-10s\n", "ORDEN", "CEDULA", "PACIENTE", "FECHA CITA", "PROCEDIMIENTO", "PRIORIDAD"));
        sb.append("--------------------------------------------------------------------------------\n");

        if (pila == null || pila.isEmpty()) {
            sb.append("No hay pacientes con citas urgentes de extraccion pendientes.\n");
            return sb.toString();
        }

        Deque<Cliente> copiaPila = new ArrayDeque<>(pila);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        int ordenLlamada = 1;
        while (!copiaPila.isEmpty()) {
            Cliente c = copiaPila.pop();
            String fechaStr = c.getFechaCita() != null ? sdf.format(c.getFechaCita()) : "Sin fecha";
            String procStr = (c.getTipoDeAtencion() != null && !c.getTipoDeAtencion().isEmpty())
                    ? c.getTipoDeAtencion().get(0) : "Extraccion";

            sb.append(String.format("#%-5d %-10d %-20s %-16s %-14s %-10s\n",
                ordenLlamada++,
                c.getCedula(),
                c.getNombre(),
                fechaStr,
                procStr,
                c.getPrioridad()
            ));
        }
        sb.append("================================================================================\n");
        sb.append("Total urgencias en pila: ").append(pila.size()).append("\n");
        return sb.toString();
    }

    public String generarReportePila(Deque<Cliente> pila) {
        return generarReportePila(pila, null);
    }
}