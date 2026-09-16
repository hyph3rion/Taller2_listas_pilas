package servicios;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import model.Cliente;

public class GestionAgendaDiaria {

    private Queue<Cliente> colaAtencion = new LinkedList<>();
    private final SimpleDateFormat formatoDia = new SimpleDateFormat("yyyyMMdd");
    private final SimpleDateFormat formatoDiaReporte = new SimpleDateFormat("dd/MM/yyyy");
    private String fechaAgendaReporte = null;

    public void cargarAgendaDelDia(List<Cliente> todosLosClientes, Date fechaObjetivo) {
        colaAtencion.clear();

        if (todosLosClientes == null || todosLosClientes.isEmpty()) {
            fechaAgendaReporte = null;
            return;
        }

        List<Cliente> agendaCargada;

        // La cola de atención diaria (FIFO) gestiona únicamente citas regulares de calendario
        // Las citas urgentes pertenecen al Plan de Contingencia (Pila LIFO)
        List<Cliente> citasRegulares = todosLosClientes.stream()
            .filter(c -> c != null && c.getFechaCita() != null)
            .filter(c -> !c.isUrgente())
            .collect(Collectors.toList());

        if (fechaObjetivo != null) {
            String diaCompararStr = formatoDia.format(fechaObjetivo);
            agendaCargada = citasRegulares.stream()
                .filter(c -> formatoDia.format(c.getFechaCita()).equals(diaCompararStr))
                .sorted(Comparator.comparing(Cliente::getFechaCita))
                .collect(Collectors.toList());
            fechaAgendaReporte = formatoDiaReporte.format(fechaObjetivo);
        } else {
            String hoyStr = formatoDia.format(new Date());
            List<Cliente> agendaHoy = citasRegulares.stream()
                .filter(c -> formatoDia.format(c.getFechaCita()).equals(hoyStr))
                .sorted(Comparator.comparing(Cliente::getFechaCita))
                .collect(Collectors.toList());

            if (!agendaHoy.isEmpty()) {
                agendaCargada = agendaHoy;
                fechaAgendaReporte = formatoDiaReporte.format(new Date());
            } else {
                agendaCargada = citasRegulares.stream()
                    .sorted(Comparator.comparing(Cliente::getFechaCita))
                    .collect(Collectors.toList());
                fechaAgendaReporte = "Todas las citas regulares (Orden Cronológico)";
            }
        }

        colaAtencion.addAll(agendaCargada);
    }

    public Cliente atenderSiguiente() {
        if (colaAtencion.isEmpty()) {
            return null;
        }
        return colaAtencion.poll();
    }

    public Cliente verSiguiente() {
        return colaAtencion.peek();
    }

    public String generarReporteCola(String nombreConsultorio) {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("           AGENDA DE ATENCION DIARIA - COLA DE TURNOS (FIFO)                   \n");
        if (nombreConsultorio != null && !nombreConsultorio.trim().isEmpty()) {
            sb.append("           Consultorio: ").append(nombreConsultorio).append("\n");
        }
        if (fechaAgendaReporte != null) {
            sb.append("           Jornada: ").append(fechaAgendaReporte).append("\n");
        }
        sb.append("================================================================================\n");
        sb.append(String.format("%-8s %-10s %-22s %-16s %-14s %-8s\n", "TURNO", "CEDULA", "PACIENTE", "FECHA/HORA", "PROCEDIMIENTO", "PRIORIDAD"));
        sb.append("--------------------------------------------------------------------------------\n");

        if (colaAtencion.isEmpty()) {
            sb.append("No hay pacientes pendientes en la cola de atencion para esta jornada.\n");
            return sb.toString();
        }

        SimpleDateFormat sdfHora = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        int turno = 1;
        for (Cliente c : colaAtencion) {
            String atencion = (c.getTipoDeAtencion() != null && !c.getTipoDeAtencion().isEmpty())
                    ? c.getTipoDeAtencion().get(0) : "Diagnostico";
            String fechaStr = c.getFechaCita() != null ? sdfHora.format(c.getFechaCita()) : "Sin fecha";

            sb.append(String.format("Turno #%-2d %-10d %-22s %-16s %-14s %-8s\n",
                turno++,
                c.getCedula(),
                c.getNombre(),
                fechaStr,
                atencion,
                c.getPrioridad()
            ));
        }
        sb.append("================================================================================\n");
        sb.append("Total pacientes en espera: ").append(colaAtencion.size()).append("\n");
        return sb.toString();
    }

    public String generarReporteCola() {
        return generarReporteCola(null);
    }

    public Queue<Cliente> getColaAtencion() {
        return colaAtencion;
    }

    public int totalEnCola() {
        return colaAtencion.size();
    }
}