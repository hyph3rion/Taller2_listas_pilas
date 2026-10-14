package servicios;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import estructuras.Cola;
import model.Cliente;

public class GestionAgendaDiaria {

    private final Cola<Cliente> colaAtencion = new Cola<>();
    private final List<Cliente> atendidos = new ArrayList<>();
    private final SimpleDateFormat formatoDia = new SimpleDateFormat("yyyyMMdd");
    private final SimpleDateFormat formatoDiaReporte = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm");
    private Date fechaAgenda;

    public void cargarAgendaDelDia(List<Cliente> clientes, Date fecha) {
        colaAtencion.vaciar();
        atendidos.clear();
        fechaAgenda = fecha != null ? fecha : new Date();

        if (clientes == null || clientes.isEmpty()) {
            return;
        }

        String dia = formatoDia.format(fechaAgenda);
        List<Cliente> agenda = new ArrayList<>();
        for (Cliente c : clientes) {
            if (c != null && c.getFechaCita() != null && formatoDia.format(c.getFechaCita()).equals(dia)) {
                agenda.add(c);
            }
        }

        ordenarPorHora(agenda);

        for (Cliente c : agenda) {
            colaAtencion.encolar(c);
        }
    }

    private void ordenarPorHora(List<Cliente> lista) {
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

    public Cliente atenderSiguiente() {
        Cliente atendido = colaAtencion.desencolar();
        if (atendido != null) {
            atendidos.add(atendido);
        }
        return atendido;
    }

    public Cliente verSiguiente() {
        return colaAtencion.verFrente();
    }

    public String generarReporteCola(String nombreConsultorio) {
        StringBuilder sb = new StringBuilder();
        sb.append("==========================================================================================\n");
        sb.append("              AGENDA DIARIA DE ATENCION - COLA DE CLIENTES\n");
        if (nombreConsultorio != null && !nombreConsultorio.trim().isEmpty()) {
            sb.append("              Consultorio: ").append(nombreConsultorio).append("\n");
        }
        if (fechaAgenda != null) {
            sb.append("              Jornada: ").append(formatoDiaReporte.format(fechaAgenda)).append("\n");
        }
        sb.append("==========================================================================================\n");
        sb.append(String.format("%-6s %-7s %-12s %-22s %-12s %-12s %-9s\n",
                "TURNO", "HORA", "CEDULA", "PACIENTE", "TELEFONO", "ATENCION", "PRIORIDAD"));
        sb.append("------------------------------------------------------------------------------------------\n");

        if (colaAtencion.estaVacia()) {
            sb.append("No hay clientes pendientes en la cola de atencion para esta jornada.\n");
        } else {
            int turno = 1;
            for (Cliente c : colaAtencion.recorrer()) {
                String atencion = (c.getTipoDeAtencion() != null && !c.getTipoDeAtencion().isEmpty())
                        ? c.getTipoDeAtencion().get(0) : "Diagnostico";
                sb.append(String.format("%-6d %-7s %-12d %-22s %-12d %-12s %-9s\n",
                        turno++,
                        formatoHora.format(c.getFechaCita()),
                        c.getCedula(),
                        c.getNombre(),
                        c.getTelefono(),
                        atencion,
                        c.getPrioridad()));
            }
        }
        sb.append("==========================================================================================\n");
        sb.append("Clientes en espera: ").append(colaAtencion.tamano()).append("\n");
        sb.append("Clientes atendidos: ").append(atendidos.size()).append("\n");
        if (!colaAtencion.estaVacia()) {
            sb.append("Siguiente en ser atendido (frente): ").append(colaAtencion.verFrente().getNombre()).append("\n");
        }

        if (!atendidos.isEmpty()) {
            sb.append("\n------------------------------- CLIENTES ATENDIDOS ---------------------------------------\n");
            int n = 1;
            for (Cliente c : atendidos) {
                sb.append(String.format("%-6d %-7s %-12d %-22s\n",
                        n++, formatoHora.format(c.getFechaCita()), c.getCedula(), c.getNombre()));
            }
        }
        return sb.toString();
    }

    public int totalEnCola() {
        return colaAtencion.tamano();
    }

    public int totalAtendidos() {
        return atendidos.size();
    }
}