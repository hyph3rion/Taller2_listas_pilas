package controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import javax.swing.JOptionPane;
import java.util.ArrayDeque;
import java.util.Deque;

import dao.ClienteDao;
import dao.ConsultorioDao;
import dao.DoctorDao;
import model.Cliente;
import model.Consultorio;
import model.Doctor;
import model.ClientType;
import model.ProcedureType;
import model.PricingBreakdown;

import servicios.GestionAgendaDiaria;
import servicios.GestionContingencia;

import view.PanelContingencia;
import view.Frame;
import view.PanelConsultas;
import view.PanelDatos;
import view.PanelDoctores;
import view.PanelOperaciones;

public class Controlador implements ActionListener {

    private static final String ARCHIVO_LOG = "cancelaciones.log";

    private ArrayList<Doctor> doctor;
    private ArrayList<Cliente> pacientes;
    private Frame vista;
    private ClienteDao clienteCrud;
    private ConsultorioDao consultorioCrud;
    private DoctorDao doctorCrud;

    // new stuff
    private GestionContingencia planContingencia;
    private GestionAgendaDiaria agendaDiaria;
    private Deque<Cliente> pilaContingenciaActual;
    private Consultorio consultorioActual;

    public Controlador() {
        this.vista = new Frame();
        this.clienteCrud = new ClienteDao();
        this.consultorioCrud = new ConsultorioDao();
        this.doctorCrud = new DoctorDao();
        this.doctor = new ArrayList<>(doctorCrud.listarTodos());
        this.pacientes = new ArrayList<>(clienteCrud.listarTodos());

        this.planContingencia = new GestionContingencia();
        this.agendaDiaria = new GestionAgendaDiaria();
        this.pilaContingenciaActual = new ArrayDeque<>();

        actionListeners();

        // initial required
        renderPatientsList();
        renderDoctorsList();
        refreshDoctorComboBox();
        cargarLogCancelaciones();

        // consult render stuff
        List<Consultorio> consultorios = consultorioCrud.listarTodos();
        PanelConsultas pc = vista.getPanelConsultas();
        pc.actualizarComboConsultorios(consultorios);
        pc.actualizarComboPacientes(this.pacientes);

        if (!consultorios.isEmpty()) {
            this.consultorioActual = consultorios.get(0);
            renderOfficeDetails(this.consultorioActual);
        }
        refrescarComboConsultoriosContingencia();
        actualizarVistasContingenciaSilencioso();
    }

    public void actionListeners() {
        // Patient search button
        vista.getPanelVista().getBtnBuscar().addActionListener(this);

        // Doctor search button
        vista.getPanelDocsVista().getBtnBuscar().addActionListener(this);

        // Office and appointment buttons
        PanelConsultas pc = vista.getPanelConsultas();
        pc.getBtnBuscarConsultorio().addActionListener(this);
        pc.getBtnGuardarConsultorio().addActionListener(this);
        pc.getBtnActualizarConsultorio().addActionListener(this);
        pc.getBtnEliminarConsultorio().addActionListener(this);
        pc.getBtnLimpiarConsultorio().addActionListener(this);

        pc.getBtnAsignarConsulta().addActionListener(this);
        pc.getBtnActualizarConsulta().addActionListener(this);
        pc.getBtnCancelarConsulta().addActionListener(this);
        pc.getBtnAtenderSiguiente().addActionListener(this);
        pc.getBtnCargarCita().addActionListener(this);
        pc.getBtnRefrescarDoctores().addActionListener(this);
        pc.getBtnLimpiarCita().addActionListener(e -> pc.limpiarFormularioCita());

        // Selector dinámico de consultorios en PanelConsultas
        pc.getComboSeleccionarConsultorio().addActionListener(e -> {
            String item = (String) pc.getComboSeleccionarConsultorio().getSelectedItem();
            if (item != null && !item.isEmpty()) {
                try {
                    int nit = Integer.parseInt(item.split(" - ")[0].trim());
                    Consultorio cons = consultorioCrud.buscarConsultorio(nit);
                    if (cons != null) {
                        this.consultorioActual = cons;
                        renderOfficeDetails(cons);
                    }
                } catch (Exception ignored) {}
            }
        });

        // Selector dinámico de citas existentes en PanelConsultas
        pc.getComboCitasExistentes().addActionListener(e -> {
            String item = (String) pc.getComboCitasExistentes().getSelectedItem();
            if (item != null && !item.startsWith("[")) {
                String codigoCita = item.split(" - ")[0].trim();
                pc.getCodigoCitaF().setText(codigoCita);
                cargarDatosCitaEnFormulario(false);
            }
        });

        // Selector dinámico de pacientes registrados en PanelConsultas
        pc.getComboPacientesRegistrados().addActionListener(e -> {
            String item = (String) pc.getComboPacientesRegistrados().getSelectedItem();
            if (item != null && !item.startsWith("[")) {
                try {
                    int ced = Integer.parseInt(item.split(" - ")[0].trim());
                    pc.getCedulaPacienteCitaF().setText(String.valueOf(ced));
                    Cliente pac = clienteCrud.buscarPaciente(ced);
                    if (pac != null) {
                        pc.getComboPrioridadCita().setSelectedItem(pac.getPrioridad());
                        String proc = (!pac.getTipoDeAtencion().isEmpty()) ? pac.getTipoDeAtencion().get(0) : "Diagnostico";
                        pc.getComboProcedimientoCita().setSelectedItem(proc);
                        if (pac.getFechaCita() != null) {
                            pc.getFechaCitaSpinner().setValue(pac.getFechaCita());
                        }
                    }
                } catch (Exception ignored) {}
            }
        });

        pc.getCedulaPacienteCitaF().addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                autoCompletarPacienteEnFormularioCita();
            }
        });
        pc.getCedulaPacienteCitaF().addActionListener(e -> autoCompletarPacienteEnFormularioCita());

        // Sincronización inteligente entre prioridad y procedimiento para Contingencia (Taller 2)
        pc.getComboPrioridadCita().addActionListener(e -> {
            String prio = (String) pc.getComboPrioridadCita().getSelectedItem();
            if ("Urgente".equalsIgnoreCase(prio)) {
                String proc = (String) pc.getComboProcedimientoCita().getSelectedItem();
                if (proc == null || !proc.toLowerCase().contains("extracc")) {
                    pc.getComboProcedimientoCita().setSelectedItem("Extraccion");
                }
            }
        });

        pc.getComboProcedimientoCita().addActionListener(e -> {
            String proc = (String) pc.getComboProcedimientoCita().getSelectedItem();
            if (proc != null && proc.toLowerCase().contains("extracc")) {
                pc.getComboPrioridadCita().setSelectedItem("Urgente");
            }
        });

        // Carga dinámica en PanelDatos al digitar cédula
        vista.getPanelDatos().getCedulaF().addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                autoCompletarPacienteEnFormularioDatos();
            }
        });
        vista.getPanelDatos().getCedulaF().addActionListener(e -> autoCompletarPacienteEnFormularioDatos());

        // Operation bar buttons
        PanelOperaciones po = vista.getPanelOperaciones();
        po.getBotonGuardar().addActionListener(this);
        po.getEliminarRegistro().addActionListener(this);
        po.getActualizarRegistro().addActionListener(this);

        // New stuff event subscription
        PanelContingencia pcont = vista.getPanelContingencia();
        pcont.getBtnGenerarPila().addActionListener(this);
        pcont.getBtnLlamarUrgencia().addActionListener(this);
        pcont.getBtnCargarAgendaCola().addActionListener(this);
        pcont.getBtnAtenderTurnoCola().addActionListener(this);
        pcont.getBtnRefrescarConsultorios().addActionListener(this);

        pcont.getComboConsultorios().addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                actualizarVistasContingenciaSilencioso();
            }
        });

        // Sincronización viva al cambiar de pestaña
        vista.getModuloPestanas().addChangeListener(e -> {
            int idx = vista.getModuloPestanas().getSelectedIndex();
            if (idx == 0) {
                pc.actualizarComboConsultorios(consultorioCrud.listarTodos());
                pc.actualizarComboPacientes(clienteCrud.listarTodos());
                if (consultorioActual != null) {
                    Consultorio recargado = consultorioCrud.buscarConsultorio(consultorioActual.getNit());
                    if (recargado != null) consultorioActual = recargado;
                    renderOfficeDetails(consultorioActual);
                }
            } else if (idx == 1) {
                renderPatientsList();
            } else if (idx == 2) {
                renderDoctorsList();
                refreshDoctorComboBox();
            } else if (idx == 3) {
                refrescarComboConsultoriosContingencia();
                actualizarVistasContingenciaSilencioso();
            }
        });
    }

    public void run() {
        vista.setVisible(true);
    }

    // Insertion sort algorithm prioritizing available doctors first and ascending
    // IDs
    public void sortDoctorsByAvailabilityAndId(ArrayList<Doctor> list) {
        int size = list.size();
        for (int i = 1; i < size; i++) {
            Doctor current = list.get(i);
            int j = i - 1;

            while (j >= 0 && shouldPrecede(current, list.get(j))) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, current);
        }
    }

    // Comparison logic for doctor sorting
    private boolean shouldPrecede(Doctor d1, Doctor d2) {
        if (d1.isDisponibilidad() && !d2.isDisponibilidad()) {
            return true;
        }
        if (!d1.isDisponibilidad() && d2.isDisponibilidad()) {
            return false;
        }
        return d1.getIdDoctor() < d2.getIdDoctor();
    }

    // Render all registered doctors in the doctors text area
    public void renderDoctorsList() {
        this.doctor = new ArrayList<>(doctorCrud.listarTodos());
        sortDoctorsByAvailabilityAndId(doctor);
        StringBuilder sb = new StringBuilder();
        sb.append("=== LISTA DE DOCTORES REGISTRADOS ===\n\n");

        if (doctor.isEmpty()) {
            sb.append("No hay doctores registrados en el sistema.\n");
        } else {
            for (Doctor d : doctor) {
                String status = d.isDisponibilidad() ? "DISPONIBLE" : "NO DISPONIBLE";
                sb.append("ID: ").append(d.getIdDoctor())
                        .append(" | Nombre: ").append(d.getNombreDoctor())
                        .append("\n  Cédula Profesional: ").append(d.getCedulaProfesional())
                        .append(" | Estado: ").append(status)
                        .append("\n------------------------------------------------------------\n");
            }
        }
        vista.getPanelDocsVista().getVistaResultados().setText(sb.toString());
    }

    public void sortPatientsByTotalFeeDescending(List<Cliente> list) {
        int size = list.size();
        for (int i = 1; i < size; i++) {
            Cliente current = list.get(i);
            double currentFee = getClienteTotalFee(current);
            int j = i - 1;
            while (j >= 0 && getClienteTotalFee(list.get(j)) > currentFee) {
                list.set(j + 1, list.get(i));
                j--;
            }
            list.set(j + 1, current);
        }
    }

    private double getClienteTotalFee(Cliente c) {
        String tCliente = (c.getTipoDeCliente() != null && c.getTipoDeCliente().length > 0) ? c.getTipoDeCliente()[0]
                : "Particular";
        String tAtencion = (!c.getTipoDeAtencion().isEmpty()) ? c.getTipoDeAtencion().get(0) : "Diagnostico";

        PricingBreakdown pricing = calculatePricing(tCliente, tAtencion, c.getCantidad());
        return pricing.totalToPay();
    }

    public void renderPatientsList() {
        List<Cliente> lista = clienteCrud.listarTodos();
        this.pacientes = new ArrayList<>(lista);

        // desc required sort algorithm
        sortPatientsByTotalFeeDescending(lista);

        // statistic required calculus
        int totalClientes = lista.size();
        double ingresosTotales = 0;
        int clientesExtraccion = 0;

        for (Cliente c : lista) {
            ingresosTotales += getClienteTotalFee(c);
            if (!c.getTipoDeAtencion().isEmpty() && "Extraccion".equalsIgnoreCase(c.getTipoDeAtencion().get(0))) {
                clientesExtraccion++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append("               MÉTRICAS GENERALES DEL CONSULTORIO           \n");
        sb.append("============================================================\n");
        sb.append(" • Total Clientes Registrados: ").append(totalClientes).append("\n");
        sb.append(" • Ingresos Totales Recibidos: $").append(String.format("%,.0f", ingresosTotales)).append("\n");
        sb.append(" • Clientes para Extracción de Dientes: ").append(clientesExtraccion).append("\n");
        sb.append("============================================================\n");
        sb.append("  LISTA DE PACIENTES (ORDENADA POR VALOR: MAYOR A MENOR)   \n");
        sb.append("============================================================\n\n");

        if (lista.isEmpty()) {
            sb.append("No hay pacientes registrados en el sistema.\n");
        } else {
            for (Cliente c : lista) {
                String clientType = c.getTipoDeCliente() != null && c.getTipoDeCliente().length > 0
                        ? c.getTipoDeCliente()[0]
                        : "Particular";
                String attentionType = !c.getTipoDeAtencion().isEmpty() ? c.getTipoDeAtencion().get(0) : "Diagnostico";
                double total = getClienteTotalFee(c);

                sb.append("Cédula: ").append(c.getCedula())
                        .append(" | Nombre: ").append(c.getNombre())
                        .append("\n  Teléfono: ").append(c.getTelefono())
                        .append(" | Tipo: ").append(clientType)
                        .append(" | Atención: ").append(attentionType)
                        .append(" | Cant: ").append(c.getCantidad())
                        .append("\n  VALOR TOTAL: $").append(String.format("%,.0f", total))
                        .append("\n------------------------------------------------------------\n");
            }
        }
        vista.getPanelVista().getVistaResultados().setText(sb.toString());
    }

    public PricingBreakdown calculatePricing(String clientTypeStr, String procedureTypeStr, int quantity) {
        // Conversión segura de String a Enum (ignora mayúsculas/minúsculas y espacios)
        ClientType clientType;
        try {
            clientType = ClientType.valueOf(clientTypeStr.trim().toUpperCase());
        } catch (Exception ex) {
            clientType = ClientType.PARTICULAR; // Fallback por defecto
        }

        ProcedureType procedureType;
        try {
            procedureType = ProcedureType.valueOf(procedureTypeStr.trim().toUpperCase());
        } catch (Exception ex) {
            procedureType = ProcedureType.DIAGNOSTICO; // Fallback por defecto
        }

        double baseFee = clientType.getBaseFee();
        double unitFee = procedureType.getUnitFee(clientType);
        int appliedQuantity = Math.max(1, quantity);
        double subtotal = unitFee * appliedQuantity;
        double total = baseFee + subtotal;

        return new PricingBreakdown(baseFee, unitFee, subtotal, total);
    }

    // Dynamic search for the first available doctor
    public Doctor findDynamicAvailableDoctor() {
        this.doctor = new ArrayList<>(doctorCrud.listarTodos());
        sortDoctorsByAvailabilityAndId(doctor);
        for (Doctor doc : doctor) {
            if (doc.isDisponibilidad()) {
                return doc;
            }
        }
        return null;
    }

    // Refresh combo box options in office view
    public void refreshDoctorComboBox() {
        PanelConsultas pc = vista.getPanelConsultas();
        pc.getComboDoctores().removeAllItems();
        pc.getComboDoctores().addItem("[Auto-asignar disponible]");

        this.doctor = new ArrayList<>(doctorCrud.listarTodos());
        sortDoctorsByAvailabilityAndId(doctor);
        for (Doctor doc : doctor) {
            String status = doc.isDisponibilidad() ? "Disponible" : "No Disponible";
            pc.getComboDoctores().addItem(doc.getIdDoctor() + " - " + doc.getNombreDoctor() + " (" + status + ")");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PanelConsultas pc = vista.getPanelConsultas();
        PanelOperaciones po = vista.getPanelOperaciones();

        // Handle save operations (based on active tab to avoid cross-blocking)
        if (e.getSource() == po.getBotonGuardar()) {
            int pestanaActiva = vista.getModuloPestanas().getSelectedIndex();
            if (pestanaActiva == 1) {
                guardarPaciente();
            } else if (pestanaActiva == 2) {
                guardarDoctor();
            } else if (pestanaActiva == 0) {
                guardarConsultorio();
            }
        }

        // Office consultorio direct buttons
        if (e.getSource() == pc.getBtnGuardarConsultorio()) {
            guardarConsultorio();
        }
        if (e.getSource() == pc.getBtnActualizarConsultorio()) {
            actualizarConsultorio();
        }
        if (e.getSource() == pc.getBtnEliminarConsultorio()) {
            eliminarConsultorio();
        }
        if (e.getSource() == pc.getBtnLimpiarConsultorio()) {
            limpiarFormularioConsultorio();
        }

        // Office appointment direct buttons
        if (e.getSource() == pc.getBtnActualizarConsulta()) {
            actualizarConsultaCita();
        }
        if (e.getSource() == pc.getBtnCancelarConsulta()) {
            cancelarConsultaConJustificacion();
        }
        if (e.getSource() == pc.getBtnAtenderSiguiente()) {
            atenderSiguienteTurno();
        }
        if (e.getSource() == pc.getBtnCargarCita()) {
            cargarDatosCitaEnFormulario();
        }

        // Handle update operations (based on active tab)
        if (e.getSource() == po.getActualizarRegistro()) {
            int pestanaActiva = vista.getModuloPestanas().getSelectedIndex();
            if (pestanaActiva == 1) {
                actualizarPaciente();
            } else if (pestanaActiva == 2) {
                actualizarDoctor();
            } else if (pestanaActiva == 0) {
                actualizarConsultorio();
            }
        }

        // Handle delete operations (based on active tab)
        if (e.getSource() == po.getEliminarRegistro()) {
            int pestanaActiva = vista.getModuloPestanas().getSelectedIndex();
            if (pestanaActiva == 1) {
                eliminarPaciente();
            } else if (pestanaActiva == 2) {
                eliminarDoctor();
            } else if (pestanaActiva == 0) {
                eliminarConsultorio();
            }
        }

        PanelContingencia pcont = vista.getPanelContingencia();
        if (e.getSource() == pcont.getBtnGenerarPila()) {
            generarPilaContingencia();
        }
        if (e.getSource() == pcont.getBtnLlamarUrgencia()) {
            llamarUrgenciaPila();
        }
        if (e.getSource() == pcont.getBtnCargarAgendaCola()) {
            cargarAgendaDiariaCola();
        }
        if (e.getSource() == pcont.getBtnAtenderTurnoCola()) {
            atenderTurnoCola();
        }
        if (e.getSource() == pcont.getBtnRefrescarConsultorios()) {
            refrescarComboConsultoriosContingencia();
            JOptionPane.showMessageDialog(vista, "Lista de consultorios actualizada.");
        }

        // Schedule appointment in office
        if (e.getSource() == pc.getBtnAsignarConsulta()) {
            try {
                // 1. Validar cédula del paciente
                if (pc.getCedulaPacienteCitaF().getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(vista, "Complete la Cédula del Paciente o selecciónelo de la lista.",
                            "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int cedula = Integer.parseInt(pc.getCedulaPacienteCitaF().getText().trim());

                // 2. Resolver el consultorio automáticamente
                Consultorio cons = buscarConsultorioPorCitaONit(null);
                if (cons == null) {
                    JOptionPane.showMessageDialog(vista,
                            "No hay ningún consultorio registrado. Registre uno primero en el formulario superior.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String codigoCita = pc.getCodigoCitaF().getText().trim();
                if (codigoCita.isEmpty()) {
                    codigoCita = "CITA-" + String.format("%03d", cons.getConsultas().size() + 1);
                    pc.getCodigoCitaF().setText(codigoCita);
                }

                // 3. Buscar al paciente por su cédula
                Cliente paciente = clienteCrud.buscarPaciente(cedula);
                if (paciente == null) {
                    JOptionPane.showMessageDialog(vista,
                            "El paciente con cédula " + cedula + " no existe. Regístrelo en 'Gestión de Pacientes'.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Doctor assignment selection
                Doctor doctorAsignado = null;
                String selectedDoctor = (String) pc.getComboDoctores().getSelectedItem();

                if (selectedDoctor != null && !selectedDoctor.equals("[Auto-asignar disponible]")) {
                    int idDoc = Integer.parseInt(selectedDoctor.split(" - ")[0]);
                    doctorAsignado = doctorCrud.buscarDoctor(idDoc);
                } else {
                    doctorAsignado = findDynamicAvailableDoctor();
                }

                if (doctorAsignado == null) {
                    JOptionPane.showMessageDialog(vista, "No hay doctores con disponibilidad activa.", "Aviso",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Pricing settlement and form fields
                String tCliente = paciente.getTipoDeCliente() != null && paciente.getTipoDeCliente().length > 0
                        ? paciente.getTipoDeCliente()[0]
                        : "Particular";
                String tAtencion = (String) pc.getComboProcedimientoCita().getSelectedItem();
                if (tAtencion == null || tAtencion.isEmpty()) {
                    tAtencion = (!paciente.getTipoDeAtencion().isEmpty()) ? paciente.getTipoDeAtencion().get(0) : "Diagnostico";
                }
                String prioridad = (String) pc.getComboPrioridadCita().getSelectedItem();
                if (prioridad == null || prioridad.isEmpty()) {
                    prioridad = paciente.getPrioridad();
                }
                if ("Urgente".equalsIgnoreCase(prioridad) && !tAtencion.toLowerCase().contains("extracc")) {
                    tAtencion = "Extraccion";
                    pc.getComboProcedimientoCita().setSelectedItem("Extraccion");
                }
                Date fechaCita = (Date) pc.getFechaCitaSpinner().getValue();
                int cant = paciente.getCantidad();

                PricingBreakdown fees = calculatePricing(tCliente, tAtencion, cant);

                // Create appointment-specific client with chosen procedure, priority and date
                ArrayList<String> atencionCita = new ArrayList<>();
                atencionCita.add(tAtencion);
                String[] prioridadCita = new String[]{prioridad};

                Cliente citaCliente = new Cliente(
                    paciente.getCedula(),
                    paciente.getNombre(),
                    paciente.getTelefono(),
                    paciente.getTipoDeCliente(),
                    atencionCita,
                    cant,
                    prioridadCita,
                    fechaCita
                );

                // Save appointment and enqueue in queue system
                cons.getConsultas().put(codigoCita, citaCliente);
                cons.encolarCita(codigoCita, citaCliente);
                if (!cons.getTelefonoContactoDoctores().contains((long) doctorAsignado.getCedulaProfesional())) {
                    cons.getTelefonoContactoDoctores().add((long) doctorAsignado.getCedulaProfesional());
                }
                consultorioCrud.actualizarConsultorio(cons);
                this.consultorioActual = cons;

                renderOfficeDetails(cons);
                refrescarComboConsultoriosContingencia();
                actualizarVistasContingenciaSilencioso();

                String summary = "========================================\n"
                        + "       CITA AGENDADA EXITOSAMENTE        \n"
                        + "========================================\n"
                        + "Consultorio: " + cons.getNombreConsultorio() + " (NIT: " + cons.getNit() + ")\n"
                        + "Código Cita: " + codigoCita + "\n"
                        + "Doctor Asignado: " + doctorAsignado.getNombreDoctor() + " (ID: "
                        + doctorAsignado.getIdDoctor() + ")\n"
                        + "Paciente: " + paciente.getNombre() + " (CC: " + paciente.getCedula() + ")\n"
                        + "Tipo Cliente: " + tCliente + "\n"
                        + "Procedimiento: " + tAtencion + " (Cant: " + cant + ")\n"
                        + "Prioridad: " + prioridad + "\n"
                        + "----------------------------------------\n"
                        + "Valor Base Cita:    $" + String.format("%,.0f", fees.baseAppointmentFee()) + "\n"
                        + "Valor Atención:     $" + String.format("%,.0f", fees.procedureSubtotal()) + "\n"
                        + "TOTAL A PAGAR:      $" + String.format("%,.0f", fees.totalToPay()) + "\n"
                        + "========================================";

                JOptionPane.showMessageDialog(vista, summary, "Comprobante de Cita", JOptionPane.INFORMATION_MESSAGE);

                // Limpiar campos del formulario de cita
                pc.limpiarFormularioCita();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(vista, "Revise los campos numéricos ingresados.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        // Office lookup
        if (e.getSource() == pc.getBtnBuscarConsultorio()) {
            try {
                int nit = Integer.parseInt(pc.getTxtBuscarNit().getText().trim());
                Consultorio cons = consultorioCrud.buscarConsultorio(nit);
                if (cons != null) {
                    renderOfficeDetails(cons);
                } else {
                    JOptionPane.showMessageDialog(vista, "Consultorio no encontrado.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(vista, "Ingrese un NIT válido.");
            }
        }

        // Refresh doctor list
        if (e.getSource() == pc.getBtnRefrescarDoctores()) {
            refreshDoctorComboBox();
            renderDoctorsList();
            JOptionPane.showMessageDialog(vista, "Lista de doctores actualizada.");
        }

        // Patient search
        if (e.getSource() == vista.getPanelVista().getBtnBuscar()) {
            String query = vista.getPanelVista().getTxtBuscarCedula().getText().trim();
            if (query.isEmpty()) {
                renderPatientsList();
            } else {
                try {
                    int cedula = Integer.parseInt(query);
                    Cliente cli = clienteCrud.buscarPaciente(cedula);
                    if (cli != null) {
                        vista.getPanelDatos().cargarDatosPaciente(cli);
                        String clientType = (cli.getTipoDeCliente() != null && cli.getTipoDeCliente().length > 0)
                                ? cli.getTipoDeCliente()[0]
                                : "Particular";
                        String attentionType = (!cli.getTipoDeAtencion().isEmpty())
                                ? cli.getTipoDeAtencion().get(0)
                                : "Diagnostico";
                        String detalle = "=== DATOS DEL PACIENTE ===\n\n"
                                + "Cédula: " + cli.getCedula() + "\n"
                                + "Nombre: " + cli.getNombre() + "\n"
                                + "Teléfono: " + cli.getTelefono() + "\n"
                                + "Tipo de Cliente: " + clientType + "\n"
                                + "Atención Solicitada: " + attentionType + "\n"
                                + "Cantidad Procedimientos: " + cli.getCantidad() + "\n"
                                + "Fecha Cita: " + cli.getFechaCita() + "\n";
                        vista.getPanelVista().getVistaResultados().setText(detalle);
                    } else {
                        JOptionPane.showMessageDialog(vista, "Paciente no encontrado con cédula: " + cedula);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(vista, "Cédula inválida.");
                }
            }
        }

        // Doctor search
        if (e.getSource() == vista.getPanelDocsVista().getBtnBuscar()) {
            String query = vista.getPanelDocsVista().getTxtBuscarCedula().getText().trim();
            if (query.isEmpty()) {
                renderDoctorsList();
            } else {
                try {
                    int busqueda = Integer.parseInt(query);
                    Doctor match = doctorCrud.buscarPorCedulaProfesional(busqueda);
                    if (match == null) {
                        match = doctorCrud.buscarDoctor(busqueda);
                    }
                    if (match != null) {
                        String status = match.isDisponibilidad() ? "DISPONIBLE" : "NO DISPONIBLE";
                        String detalle = "=== DATOS DEL DOCTOR ===\n\n"
                                + "ID Doctor: " + match.getIdDoctor() + "\n"
                                + "Nombre: " + match.getNombreDoctor() + "\n"
                                + "Cédula Profesional: " + match.getCedulaProfesional() + "\n"
                                + "Estado: " + status + "\n";
                        vista.getPanelDocsVista().getVistaResultados().setText(detalle);
                    } else {
                        JOptionPane.showMessageDialog(vista, "Doctor no encontrado.");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(vista, "Cédula profesional o ID inválido.");
                }
            }
        }
    }

    // --- Save Operations ---

    private void guardarPaciente() {
        PanelDatos pd = vista.getPanelDatos();
        if (pd.getCedulaF().getText().trim().isEmpty() || pd.getNombreF().getText().trim().isEmpty()
                || pd.getTelefonoF().getText().trim().isEmpty() || pd.getCantidadF().getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Por favor complete todos los campos del formulario de paciente.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int cedula = Integer.parseInt(pd.getCedulaF().getText().trim());
            String nombre = pd.getNombreF().getText().trim();
            long telefono = Long.parseLong(pd.getTelefonoF().getText().trim());
            int cantidad = Integer.parseInt(pd.getCantidadF().getText().trim());
            String[] tipoCliente = { (String) pd.getTipoDeClienteC().getSelectedItem() };

            ArrayList<String> tipoAtencion = new ArrayList<>();
            if (pd.getRbCalzas().isSelected())
                tipoAtencion.add("Calzas");
            else if (pd.getRbLimpieza().isSelected())
                tipoAtencion.add("Limpieza");
            else if (pd.getRbExtraccion().isSelected())
                tipoAtencion.add("Extraccion");
            else if (pd.getRbDiagnostico().isSelected())
                tipoAtencion.add("Diagnostico");
            else
                tipoAtencion.add("Diagnostico");

            String[] prioridad = { (String) pd.getPrioridadAtencionC().getSelectedItem() };
            Date fecha = (Date) pd.getFechaCitaS().getValue();

            Cliente nuevoCliente = new Cliente(cedula, nombre, telefono, tipoCliente, tipoAtencion, cantidad, prioridad,
                    fecha);
            if (clienteCrud.registrarPaciente(nuevoCliente)) {
                renderPatientsList();
                vista.getPanelConsultas().actualizarComboPacientes(clienteCrud.listarTodos());
                refrescarComboConsultoriosContingencia();
                actualizarVistasContingenciaSilencioso();
                JOptionPane.showMessageDialog(vista, "Paciente registrado exitosamente.");
                clearPatientForm();
            } else {
                JOptionPane.showMessageDialog(vista, "La cédula ya está registrada.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "Revise los campos numéricos del paciente.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarDoctor() {
        PanelDoctores pdoc = vista.getPanelDoctores();
        if (pdoc.getIdDoctorTextF().getText().trim().isEmpty()
                || pdoc.getCedulaProfesionalTextF().getText().trim().isEmpty()
                || pdoc.getNombreDoctorTextField().getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Por favor complete todos los campos del formulario del doctor.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int idDoctor = Integer.parseInt(pdoc.getIdDoctorTextF().getText().trim());
            int cedulaProf = Integer.parseInt(pdoc.getCedulaProfesionalTextF().getText().trim());
            String nombre = pdoc.getNombreDoctorTextField().getText().trim();
            boolean disponible = pdoc.getRbDisponibilidadTrue().isSelected();

            Doctor nuevoDoc = new Doctor(idDoctor, cedulaProf, nombre, disponible);
            if (doctorCrud.registrarDoctor(nuevoDoc)) {
                renderDoctorsList();
                refreshDoctorComboBox();
                JOptionPane.showMessageDialog(vista, "Doctor registrado exitosamente.");
                clearDoctorForm();
            } else {
                JOptionPane.showMessageDialog(vista, "El ID del doctor ya existe.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "Revise los campos numéricos del doctor.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarConsultorio() {
        PanelConsultas pc = vista.getPanelConsultas();
        if (pc.getNitF().getText().trim().isEmpty() || pc.getNombreConsultorioF().getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Por favor complete al menos el NIT y el Nombre del consultorio.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int nit = Integer.parseInt(pc.getNitF().getText().trim());
            String nombre = pc.getNombreConsultorioF().getText().trim();
            String direccion = pc.getDireccionF().getText().trim();

            ArrayList<Long> telefonos = new ArrayList<>();
            if (!pc.getTelefonoDoctorF().getText().trim().isEmpty()) {
                telefonos.add(Long.parseLong(pc.getTelefonoDoctorF().getText().trim()));
            }

            Consultorio cons = new Consultorio(nombre, direccion, telefonos, new HashMap<>(), nit);
            if (consultorioCrud.registrarConsultorio(cons)) {
                this.consultorioActual = cons;
                vista.getPanelConsultas().actualizarComboConsultorios(consultorioCrud.listarTodos());
                renderOfficeDetails(cons);
                refrescarComboConsultoriosContingencia();
                actualizarVistasContingenciaSilencioso();
                JOptionPane.showMessageDialog(vista, "Consultorio registrado exitosamente.");
            } else {
                JOptionPane.showMessageDialog(vista, "El NIT ya se encuentra registrado.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "Revise los campos numéricos del consultorio.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Update Operations ---

    private void actualizarPaciente() {
        PanelDatos pd = vista.getPanelDatos();
        if (pd.getCedulaF().getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(vista,
                    "Ingrese la cédula del paciente en el formulario para actualizar sus datos.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int cedula = Integer.parseInt(pd.getCedulaF().getText().trim());
            Cliente existente = clienteCrud.buscarPaciente(cedula);
            if (existente == null) {
                JOptionPane.showMessageDialog(vista, "No se encontró ningún paciente con la cédula " + cedula + ".",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String nombre = pd.getNombreF().getText().trim().isEmpty() ? existente.getNombre()
                    : pd.getNombreF().getText().trim();
            long telefono = pd.getTelefonoF().getText().trim().isEmpty() ? existente.getTelefono()
                    : Long.parseLong(pd.getTelefonoF().getText().trim());
            int cantidad = pd.getCantidadF().getText().trim().isEmpty() ? existente.getCantidad()
                    : Integer.parseInt(pd.getCantidadF().getText().trim());
            String[] tipoCliente = { (String) pd.getTipoDeClienteC().getSelectedItem() };

            ArrayList<String> tipoAtencion = new ArrayList<>();
            if (pd.getRbCalzas().isSelected())
                tipoAtencion.add("Calzas");
            else if (pd.getRbLimpieza().isSelected())
                tipoAtencion.add("Limpieza");
            else if (pd.getRbExtraccion().isSelected())
                tipoAtencion.add("Extraccion");
            else if (pd.getRbDiagnostico().isSelected())
                tipoAtencion.add("Diagnostico");
            else
                tipoAtencion = existente.getTipoDeAtencion();

            String[] prioridad = { (String) pd.getPrioridadAtencionC().getSelectedItem() };
            Date fecha = (Date) pd.getFechaCitaS().getValue();

            Cliente actualizado = new Cliente(cedula, nombre, telefono, tipoCliente, tipoAtencion, cantidad, prioridad,
                    fecha);
            if (clienteCrud.actualizarPaciente(actualizado)) {
                renderPatientsList();
                vista.getPanelConsultas().actualizarComboPacientes(clienteCrud.listarTodos());
                refrescarComboConsultoriosContingencia();
                actualizarVistasContingenciaSilencioso();
                JOptionPane.showMessageDialog(vista, "Paciente actualizado exitosamente.");
            } else {
                JOptionPane.showMessageDialog(vista, "Error al actualizar paciente.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "Revise los campos numéricos del paciente.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarDoctor() {
        PanelDoctores pdoc = vista.getPanelDoctores();
        if (pdoc.getIdDoctorTextF().getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Ingrese el ID del doctor en el formulario para actualizar sus datos.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int idDoctor = Integer.parseInt(pdoc.getIdDoctorTextF().getText().trim());
            Doctor existente = doctorCrud.buscarDoctor(idDoctor);
            if (existente == null) {
                JOptionPane.showMessageDialog(vista, "No se encontró ningún doctor con el ID " + idDoctor + ".",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int cedulaProf = pdoc.getCedulaProfesionalTextF().getText().trim().isEmpty()
                    ? existente.getCedulaProfesional()
                    : Integer.parseInt(pdoc.getCedulaProfesionalTextF().getText().trim());
            String nombre = pdoc.getNombreDoctorTextField().getText().trim().isEmpty()
                    ? existente.getNombreDoctor()
                    : pdoc.getNombreDoctorTextField().getText().trim();
            boolean disponible = pdoc.getRbDisponibilidadTrue().isSelected();

            Doctor docActualizado = new Doctor(idDoctor, cedulaProf, nombre, disponible);
            if (doctorCrud.actualizarDoctor(docActualizado)) {
                renderDoctorsList();
                refreshDoctorComboBox();
                JOptionPane.showMessageDialog(vista, "Doctor actualizado exitosamente.");
            } else {
                JOptionPane.showMessageDialog(vista, "Error al actualizar doctor.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "Revise los campos numéricos del doctor.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarConsultorio() {
        PanelConsultas pc = vista.getPanelConsultas();
        if (pc.getNitF().getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Ingrese el NIT del consultorio a actualizar.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int nit = Integer.parseInt(pc.getNitF().getText().trim());
            Consultorio existente = consultorioCrud.buscarConsultorio(nit);
            if (existente == null) {
                JOptionPane.showMessageDialog(vista, "No se encontró ningún consultorio con el NIT " + nit + ".",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String nombre = pc.getNombreConsultorioF().getText().trim().isEmpty() ? existente.getNombreConsultorio()
                    : pc.getNombreConsultorioF().getText().trim();
            String direccion = pc.getDireccionF().getText().trim().isEmpty() ? existente.getDireccion()
                    : pc.getDireccionF().getText().trim();

            ArrayList<Long> telefonos = existente.getTelefonoContactoDoctores();
            if (!pc.getTelefonoDoctorF().getText().trim().isEmpty()) {
                long tel = Long.parseLong(pc.getTelefonoDoctorF().getText().trim());
                if (!telefonos.contains(tel)) {
                    telefonos.add(tel);
                }
            }

            Consultorio actualizado = new Consultorio(nombre, direccion, telefonos, existente.getConsultas(), nit);
            if (consultorioCrud.actualizarConsultorio(actualizado)) {
                this.consultorioActual = actualizado;
                vista.getPanelConsultas().actualizarComboConsultorios(consultorioCrud.listarTodos());
                renderOfficeDetails(actualizado);
                refrescarComboConsultoriosContingencia();
                actualizarVistasContingenciaSilencioso();
                JOptionPane.showMessageDialog(vista, "Consultorio actualizado exitosamente.");
            } else {
                JOptionPane.showMessageDialog(vista, "Error al actualizar consultorio.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "Revise los campos numéricos del consultorio.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Delete Operations ---

    private void eliminarPaciente() {
        PanelDatos pd = vista.getPanelDatos();
        String cedulaStr = pd.getCedulaF().getText().trim();
        if (cedulaStr.isEmpty()) {
            cedulaStr = vista.getPanelVista().getTxtBuscarCedula().getText().trim();
        }
        if (cedulaStr.isEmpty()) {
            JOptionPane.showMessageDialog(vista,
                    "Ingrese la cédula del paciente a eliminar en el formulario o en la barra de búsqueda.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int cedula = Integer.parseInt(cedulaStr);
            int confirm = JOptionPane.showConfirmDialog(vista,
                    "¿Está seguro de eliminar al paciente con cédula " + cedula + "?", "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (clienteCrud.eliminarPaciente(cedula)) {
                    renderPatientsList();
                    vista.getPanelConsultas().actualizarComboPacientes(clienteCrud.listarTodos());
                    refrescarComboConsultoriosContingencia();
                    actualizarVistasContingenciaSilencioso();
                    clearPatientForm();
                    JOptionPane.showMessageDialog(vista, "Paciente eliminado exitosamente.");
                } else {
                    JOptionPane.showMessageDialog(vista, "No se encontró ningún paciente con la cédula " + cedula + ".",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "La cédula ingresada no es válida.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarDoctor() {
        PanelDoctores pdoc = vista.getPanelDoctores();
        String idStr = pdoc.getIdDoctorTextF().getText().trim();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Ingrese el ID del doctor a eliminar en el formulario.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int idDoctor = Integer.parseInt(idStr);
            int confirm = JOptionPane.showConfirmDialog(vista,
                    "¿Está seguro de eliminar al doctor con ID " + idDoctor + "?", "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (doctorCrud.eliminarDoctor(idDoctor)) {
                    renderDoctorsList();
                    refreshDoctorComboBox();
                    clearDoctorForm();
                    JOptionPane.showMessageDialog(vista, "Doctor eliminado exitosamente.");
                } else {
                    JOptionPane.showMessageDialog(vista, "No se encontró ningún doctor con el ID " + idDoctor + ".",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "El ID ingresado no es válido.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarConsultorio() {
        PanelConsultas pc = vista.getPanelConsultas();
        String nitStr = pc.getNitF().getText().trim();
        if (nitStr.isEmpty()) {
            nitStr = pc.getTxtBuscarNit().getText().trim();
        }
        if (nitStr.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Ingrese el NIT del consultorio a eliminar.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int nit = Integer.parseInt(nitStr);
            int confirm = JOptionPane.showConfirmDialog(vista,
                    "¿Está seguro de eliminar el consultorio con NIT " + nit + "?", "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (consultorioCrud.eliminarConsultorio(nit)) {
                    this.consultorioActual = null;
                    vista.getPanelConsultas().actualizarComboConsultorios(consultorioCrud.listarTodos());
                    pc.getVistaDatosConsultorio().setText("");
                    pc.getVistaCitasAgendadas().setText("");
                    pc.actualizarComboCitas(null);
                    refrescarComboConsultoriosContingencia();
                    actualizarVistasContingenciaSilencioso();
                    JOptionPane.showMessageDialog(vista, "Consultorio eliminado exitosamente.");
                } else {
                    JOptionPane.showMessageDialog(vista, "No se encontró ningún consultorio con el NIT " + nit + ".",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "El NIT ingresado no es válido.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Office Appointment Operations (Consultas/Citas) ---

    private void actualizarConsultaCita() {
        PanelConsultas pc = vista.getPanelConsultas();
        String codigoCita = pc.getCodigoCitaF().getText().trim();
        if (codigoCita.isEmpty()) {
            String item = (String) pc.getComboCitasExistentes().getSelectedItem();
            if (item != null && !item.startsWith("[")) {
                codigoCita = item.split(" - ")[0].trim();
                pc.getCodigoCitaF().setText(codigoCita);
            }
        }
        if (codigoCita.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Ingrese el Código de la Cita que desea actualizar o selecciónela de la lista.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Consultorio cons = buscarConsultorioPorCitaONit(codigoCita);
        if (cons == null || !cons.getConsultas().containsKey(codigoCita)) {
            JOptionPane.showMessageDialog(vista, "No se encontró ninguna cita con el código '" + codigoCita + "'.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Cliente pacienteActual = cons.getConsultas().get(codigoCita);

        String cedulaStr = pc.getCedulaPacienteCitaF().getText().trim();
        if (!cedulaStr.isEmpty()) {
            try {
                int cedula = Integer.parseInt(cedulaStr);
                Cliente nuevoPac = clienteCrud.buscarPaciente(cedula);
                if (nuevoPac == null) {
                    JOptionPane.showMessageDialog(vista, "El paciente con cédula " + cedula + " no existe.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                pacienteActual = nuevoPac;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(vista, "La cédula del paciente debe ser numérica.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String selectedDoctor = (String) pc.getComboDoctores().getSelectedItem();
        if (selectedDoctor != null && !selectedDoctor.equals("[Auto-asignar disponible]")) {
            int idDoc = Integer.parseInt(selectedDoctor.split(" - ")[0]);
            Doctor doctorAsignado = doctorCrud.buscarDoctor(idDoc);
            if (doctorAsignado != null) {
                long cedProf = (long) doctorAsignado.getCedulaProfesional();
                if (!cons.getTelefonoContactoDoctores().contains(cedProf)) {
                    cons.getTelefonoContactoDoctores().add(cedProf);
                }
            }
        }

        String prioridad = (String) pc.getComboPrioridadCita().getSelectedItem();
        String procedimiento = (String) pc.getComboProcedimientoCita().getSelectedItem();
        if ("Urgente".equalsIgnoreCase(prioridad) && (procedimiento == null || !procedimiento.toLowerCase().contains("extracc"))) {
            procedimiento = "Extraccion";
            pc.getComboProcedimientoCita().setSelectedItem("Extraccion");
        }
        Date fecha = (Date) pc.getFechaCitaSpinner().getValue();

        ArrayList<String> atencionCita = new ArrayList<>();
        atencionCita.add(procedimiento != null ? procedimiento : "Diagnostico");
        String[] prioridadCita = new String[]{prioridad != null ? prioridad : "Normal"};

        Cliente citaModificada = new Cliente(
            pacienteActual.getCedula(),
            pacienteActual.getNombre(),
            pacienteActual.getTelefono(),
            pacienteActual.getTipoDeCliente(),
            atencionCita,
            pacienteActual.getCantidad(),
            prioridadCita,
            fecha
        );

        cons.getConsultas().put(codigoCita, citaModificada);
        cons.removerDeCola(codigoCita);
        cons.encolarCita(codigoCita, citaModificada);

        if (consultorioCrud.actualizarConsultorio(cons)) {
            this.consultorioActual = cons;
            renderOfficeDetails(cons);
            refrescarComboConsultoriosContingencia();
            actualizarVistasContingenciaSilencioso();
            JOptionPane.showMessageDialog(vista, "Cita '" + codigoCita + "' modificada exitosamente en el consultorio "
                    + cons.getNombreConsultorio() + ".");
        } else {
            JOptionPane.showMessageDialog(vista, "Error al persistir la actualización de la cita.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarDatosCitaEnFormulario() {
        cargarDatosCitaEnFormulario(true);
    }

    private void cargarDatosCitaEnFormulario(boolean mostrarAlerta) {
        PanelConsultas pc = vista.getPanelConsultas();
        String codigoCita = pc.getCodigoCitaF().getText().trim();
        if (codigoCita.isEmpty()) {
            if (mostrarAlerta) {
                JOptionPane.showMessageDialog(vista, "Ingrese o seleccione el Código de la Cita a cargar.", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        Consultorio cons = buscarConsultorioPorCitaONit(codigoCita);
        if (cons == null || !cons.getConsultas().containsKey(codigoCita)) {
            if (mostrarAlerta) {
                JOptionPane.showMessageDialog(vista, "No se encontró ninguna cita con el código '" + codigoCita + "'.",
                        "No encontrada", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        Cliente paciente = cons.getConsultas().get(codigoCita);
        pc.getCedulaPacienteCitaF().setText(String.valueOf(paciente.getCedula()));
        pc.getComboPrioridadCita().setSelectedItem(paciente.getPrioridad());

        String proc = (paciente.getTipoDeAtencion() != null && !paciente.getTipoDeAtencion().isEmpty())
                ? paciente.getTipoDeAtencion().get(0) : "Diagnostico";
        pc.getComboProcedimientoCita().setSelectedItem(proc);
        if (paciente.getFechaCita() != null) {
            pc.getFechaCitaSpinner().setValue(paciente.getFechaCita());
        }

        for (int i = 0; i < pc.getComboPacientesRegistrados().getItemCount(); i++) {
            String item = pc.getComboPacientesRegistrados().getItemAt(i);
            if (item.startsWith(paciente.getCedula() + " - ")) {
                pc.getComboPacientesRegistrados().setSelectedIndex(i);
                break;
            }
        }

        if (mostrarAlerta) {
            JOptionPane.showMessageDialog(vista,
                    "Datos de la cita '" + codigoCita + "' cargados en el formulario.\n\n"
                    + "Paciente: " + paciente.getNombre() + " (CC: " + paciente.getCedula() + ")\n"
                    + "Prioridad: " + paciente.getPrioridad() + " | Procedimiento: " + proc + "\n\n"
                    + "Puede modificar los datos y presionar 'Modificar Cita', o presionar 'Cancelar Cita'.",
                    "Cita Cargada", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void limpiarFormularioConsultorio() {
        PanelConsultas pc = vista.getPanelConsultas();
        pc.getNitF().setText("");
        pc.getNombreConsultorioF().setText("");
        pc.getDireccionF().setText("");
        pc.getTelefonoDoctorF().setText("");
    }

    private void cancelarConsultaConJustificacion() {
        PanelConsultas pc = vista.getPanelConsultas();
        String codigoCita = pc.getCodigoCitaF().getText().trim();
        if (codigoCita.isEmpty()) {
            String item = (String) pc.getComboCitasExistentes().getSelectedItem();
            if (item != null && !item.startsWith("[")) {
                codigoCita = item.split(" - ")[0].trim();
                pc.getCodigoCitaF().setText(codigoCita);
            }
        }
        if (codigoCita.isEmpty()) {
            codigoCita = JOptionPane.showInputDialog(vista, "Ingrese el Código de la Cita que desea cancelar:",
                    "Cancelar Consulta", JOptionPane.QUESTION_MESSAGE);
            if (codigoCita != null) {
                codigoCita = codigoCita.trim();
            }
        }

        if (codigoCita == null || codigoCita.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Debe indicar el Código de la Cita para realizar la cancelación.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Consultorio cons = buscarConsultorioPorCitaONit(codigoCita);
        if (cons == null || !cons.getConsultas().containsKey(codigoCita)) {
            JOptionPane.showMessageDialog(vista, "No se encontró ninguna cita con el código '" + codigoCita + "'.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Cliente paciente = cons.getConsultas().get(codigoCita);

        String motivo = JOptionPane.showInputDialog(vista,
                "⚠️ JUSTIFICACIÓN DE CANCELACIÓN (Requerida para auditoría):\n\n"
                        + "Cita a cancelar: " + codigoCita + "\n"
                        + "Paciente: "
                        + (paciente != null ? paciente.getNombre() + " (CC: " + paciente.getCedula() + ")" : "N/A")
                        + "\n\n"
                        + "Ingrese el motivo de la cancelación:",
                "Justificar Cancelación de Consulta",
                JOptionPane.WARNING_MESSAGE);

        if (motivo == null || motivo.trim().isEmpty()) {
            JOptionPane.showMessageDialog(vista,
                    "Cancelación anulada: Es obligatorio suministrar una justificación válida.",
                    "Cancelación no efectuada", JOptionPane.ERROR_MESSAGE);
            return;
        }

        cons.getConsultas().remove(codigoCita);
        cons.removerDeCola(codigoCita);

        registrarLogCancelacion(codigoCita, paciente, cons, motivo.trim());

        if (consultorioCrud.actualizarConsultorio(cons)) {
            this.consultorioActual = cons;
            renderOfficeDetails(cons);
            cargarLogCancelaciones();
            pc.limpiarFormularioCita();
            refrescarComboConsultoriosContingencia();
            actualizarVistasContingenciaSilencioso();

            JOptionPane.showMessageDialog(vista,
                    "✅ Consulta '" + codigoCita + "' cancelada exitosamente.\n\n"
                            + "La justificación y los datos han sido archivados en el log de auditoría.",
                    "Consulta Cancelada",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(vista, "Error al persistir la cancelación de la cita.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atenderSiguienteTurno() {
        PanelConsultas pc = vista.getPanelConsultas();
        Consultorio cons = buscarConsultorioPorCitaONit(null);
        if (cons == null) {
            JOptionPane.showMessageDialog(vista, "Seleccione o busque un consultorio primero.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String codigoAtender = cons.desencolarSiguiente();
        if (codigoAtender == null) {
            JOptionPane.showMessageDialog(vista, "No hay turnos pendientes en la cola de espera de este consultorio.",
                    "Cola Vacía", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Cliente cli = cons.getConsultas().get(codigoAtender);
        consultorioCrud.actualizarConsultorio(cons);
        this.consultorioActual = cons;
        renderOfficeDetails(cons);
        actualizarVistasContingenciaSilencioso();

        String nombrePac = cli != null ? cli.getNombre() : "N/A";
        String cedulaPac = cli != null ? String.valueOf(cli.getCedula()) : "N/A";
        String prioridad = (cli != null && cli.getPrioridadAtencion() != null && cli.getPrioridadAtencion().length > 0)
                ? cli.getPrioridadAtencion()[0]
                : "Normal";
        String atencion = (cli != null && !cli.getTipoDeAtencion().isEmpty()) ? cli.getTipoDeAtencion().get(0)
                : "Diagnostico";

        JOptionPane.showMessageDialog(vista,
                "🔔 LLAMANDO A TURNO:\n\n"
                        + "Código Cita: " + codigoAtender + "\n"
                        + "Paciente: " + nombrePac + " (CC: " + cedulaPac + ")\n"
                        + "Prioridad: " + prioridad + "\n"
                        + "Procedimiento: " + atencion + "\n\n"
                        + "El paciente ha sido retirado de la cola de espera y pasa al consultorio.",
                "Turno en Atención",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void renderColaCitas(Consultorio cons) {
        PanelConsultas pc = vista.getPanelConsultas();
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append("        COLA DE ESPERA DE CITAS - SISTEMA DE TURNOS\n");
        sb.append("        Consultorio: ").append(cons != null ? cons.getNombreConsultorio() : "N/A").append("\n");
        sb.append("============================================================\n\n");

        if (cons == null || cons.getColaCitas().isEmpty()) {
            sb.append("No hay pacientes en la cola de espera de este consultorio.\n");
        } else {
            int turno = 1;
            for (String cod : cons.getColaCitas()) {
                Cliente cli = cons.getConsultas().get(cod);
                if (cli != null) {
                    String prioridad = (cli.getPrioridadAtencion() != null && cli.getPrioridadAtencion().length > 0)
                            ? cli.getPrioridadAtencion()[0]
                            : "Normal";
                    String atencion = (!cli.getTipoDeAtencion().isEmpty()) ? cli.getTipoDeAtencion().get(0)
                            : "Diagnostico";
                    String etiquetaPrioridad = "Urgente".equalsIgnoreCase(prioridad) ? "🚨 [URGENCIA PRIORITARIA]"
                            : "🟢 [Normal]";

                    sb.append("TURNO #").append(turno++).append(" ").append(etiquetaPrioridad).append("\n")
                            .append("  Código Cita: ").append(cod)
                            .append(" | Paciente: ").append(cli.getNombre()).append(" (CC: ").append(cli.getCedula())
                            .append(")\n")
                            .append("  Procedimiento: ").append(atencion)
                            .append(" | Teléfono: ").append(cli.getTelefono()).append("\n")
                            .append("------------------------------------------------------------\n");
                }
            }
        }
        pc.getVistaColaCitas().setText(sb.toString());
    }

    private void registrarLogCancelacion(String codigoCita, Cliente cli, Consultorio cons, String justificacion) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());

        StringBuilder logEntry = new StringBuilder();
        logEntry.append("[").append(timestamp).append("] CANCELACIÓN DE CONSULTA\n")
                .append("  Código Cita: ").append(codigoCita).append("\n")
                .append("  Consultorio: ").append(cons.getNombreConsultorio()).append(" (NIT: ").append(cons.getNit())
                .append(")\n");
        if (cli != null) {
            logEntry.append("  Paciente: ").append(cli.getNombre()).append(" (CC: ").append(cli.getCedula())
                    .append(")\n")
                    .append("  Teléfono: ").append(cli.getTelefono()).append("\n");
        }
        logEntry.append("  JUSTIFICACIÓN: ").append(justificacion).append("\n")
                .append("--------------------------------------------------------------------------------\n\n");

        try (FileWriter fw = new FileWriter(ARCHIVO_LOG, true)) {
            fw.write(logEntry.toString());
        } catch (IOException e) {
            System.err.println("Error al escribir en " + ARCHIVO_LOG + ": " + e.getMessage());
        }
    }

    private void cargarLogCancelaciones() {
        PanelConsultas pc = vista.getPanelConsultas();
        File f = new File(ARCHIVO_LOG);
        if (!f.exists()) {
            pc.getVistaLogCancelaciones().setText(
                    "=== REGISTRO DE AUDITORÍA DE CANCELACIONES (LOG) ===\n\nNo se registran cancelaciones en el sistema.");
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== REGISTRO DE AUDITORÍA DE CANCELACIONES (LOG) ===\n\n");
            sb.append(Files.readString(f.toPath()));
            pc.getVistaLogCancelaciones().setText(sb.toString());
        } catch (IOException e) {
            pc.getVistaLogCancelaciones().setText("Error al cargar log de cancelaciones: " + e.getMessage());
        }
    }

    private Consultorio buscarConsultorioPorCitaONit(String codigoCita) {
        PanelConsultas pc = vista.getPanelConsultas();
        // 1. Consultorio seleccionado en combo
        String item = (String) pc.getComboSeleccionarConsultorio().getSelectedItem();
        if (item != null && !item.isEmpty()) {
            try {
                int nit = Integer.parseInt(item.split(" - ")[0].trim());
                Consultorio cons = consultorioCrud.buscarConsultorio(nit);
                if (cons != null && (codigoCita == null || cons.getConsultas().containsKey(codigoCita))) {
                    return cons;
                }
            } catch (Exception ignored) {}
        }

        // 2. Intentar por NIT en nitF
        String nitStr = pc.getNitF().getText().trim();
        if (!nitStr.isEmpty()) {
            try {
                int nit = Integer.parseInt(nitStr);
                Consultorio cons = consultorioCrud.buscarConsultorio(nit);
                if (cons != null && (codigoCita == null || cons.getConsultas().containsKey(codigoCita))) {
                    return cons;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // 3. Intentar por NIT en txtBuscarNit
        String buscarNitStr = pc.getTxtBuscarNit().getText().trim();
        if (!buscarNitStr.isEmpty()) {
            try {
                int nit = Integer.parseInt(buscarNitStr);
                Consultorio cons = consultorioCrud.buscarConsultorio(nit);
                if (cons != null && (codigoCita == null || cons.getConsultas().containsKey(codigoCita))) {
                    return cons;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // 4. Buscar en todos los consultorios cuál contiene este codigoCita
        if (codigoCita != null && !codigoCita.isEmpty()) {
            for (Consultorio c : consultorioCrud.listarTodos()) {
                if (c.getConsultas().containsKey(codigoCita)) {
                    return c;
                }
            }
        }

        // 5. Consultorio actualmente activo
        if (this.consultorioActual != null) {
            return this.consultorioActual;
        }

        // 6. Primer consultorio de la lista
        List<Consultorio> todos = consultorioCrud.listarTodos();
        if (!todos.isEmpty()) {
            return todos.get(0);
        }

        return null;
    }

    // Render office information and associated appointments
    private void renderOfficeDetails(Consultorio cons) {
        if (cons == null) return;
        this.consultorioActual = cons;
        PanelConsultas pc = vista.getPanelConsultas();
        renderColaCitas(cons);
        pc.actualizarComboCitas(cons);

        StringBuilder sbOffice = new StringBuilder();
        sbOffice.append("NIT: ").append(cons.getNit()).append("\n");
        sbOffice.append("Nombre: ").append(cons.getNombreConsultorio()).append("\n");
        sbOffice.append("Dirección: ").append(cons.getDireccion()).append("\n");
        sbOffice.append("Contactos Doctores: ").append(cons.getTelefonoContactoDoctores().toString()).append("\n");
        pc.getVistaDatosConsultorio().setText(sbOffice.toString());

        // Pre-cargar datos del consultorio en su formulario
        pc.getNitF().setText(String.valueOf(cons.getNit()));
        pc.getNombreConsultorioF().setText(cons.getNombreConsultorio());
        pc.getDireccionF().setText(cons.getDireccion());
        if (!cons.getTelefonoContactoDoctores().isEmpty()) {
            pc.getTelefonoDoctorF().setText(String.valueOf(cons.getTelefonoContactoDoctores().get(0)));
        }

        StringBuilder sbAppointments = new StringBuilder();
        if (cons.getConsultas().isEmpty()) {
            sbAppointments.append("No hay citas registradas en este consultorio.\n");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            cons.getConsultas().forEach((codigo, cli) -> {
                String tCliente = cli.getTipoDeCliente() != null && cli.getTipoDeCliente().length > 0
                        ? cli.getTipoDeCliente()[0]
                        : "Particular";
                String tAtencion = (!cli.getTipoDeAtencion().isEmpty()) ? cli.getTipoDeAtencion().get(0)
                        : "Diagnostico";
                PricingBreakdown fees = calculatePricing(tCliente, tAtencion, cli.getCantidad());

                String prioridad = cli.getPrioridad() != null ? cli.getPrioridad() : "Normal";
                String etiquetaPrioridad = "Urgente".equalsIgnoreCase(prioridad) ? "🚨 [URGENTE]" : "🟢 [Normal]";
                String fechaStr = cli.getFechaCita() != null ? sdf.format(cli.getFechaCita()) : "Sin fecha";

                sbAppointments.append("• Cita: ").append(codigo).append(" ").append(etiquetaPrioridad)
                        .append("\n  Paciente: ").append(cli.getNombre()).append(" (CC: ").append(cli.getCedula())
                        .append(")\n")
                        .append("  Tipo: ").append(tCliente).append(" | Atenc: ").append(tAtencion)
                        .append(" | Fecha: ").append(fechaStr)
                        .append(" | Total: $").append(String.format("%,.0f", fees.totalToPay()))
                        .append("\n------------------------------------------------------------\n");
            });
        }
        pc.getVistaCitasAgendadas().setText(sbAppointments.toString());
    }

    // Clear patient input fields
    private void clearPatientForm() {
        PanelDatos pd = vista.getPanelDatos();
        pd.limpiarFormulario();
    }

    // Clear doctor input fields
    private void clearDoctorForm() {
        PanelDoctores pdoc = vista.getPanelDoctores();
        pdoc.getIdDoctorTextF().setText("");
        pdoc.getCedulaProfesionalTextF().setText("");
        pdoc.getNombreDoctorTextField().setText("");
    }

    private void autoCompletarPacienteEnFormularioDatos() {
        String cedStr = vista.getPanelDatos().getCedulaF().getText().trim();
        if (cedStr.isEmpty()) return;
        try {
            int ced = Integer.parseInt(cedStr);
            Cliente c = clienteCrud.buscarPaciente(ced);
            if (c != null) {
                vista.getPanelDatos().cargarDatosPaciente(c);
            }
        } catch (NumberFormatException ignored) {}
    }

    private void actualizarVistasContingenciaSilencioso() {
        List<Cliente> clientes = obtenerListaClientesContingencia();
        this.pilaContingenciaActual = planContingencia.generarPilaUrgencias(clientes);

        String origen = obtenerNombreOrigenContingencia();
        String reportePila = planContingencia.generarReportePila(pilaContingenciaActual, origen);
        vista.getPanelContingencia().getVistaPilaReporte().setText(reportePila);

        Date fechaFiltro = null;
        if (vista.getPanelContingencia().getChkFiltrarPorFecha().isSelected()) {
            fechaFiltro = (Date) vista.getPanelContingencia().getFechaAgendaSpinner().getValue();
        }
        agendaDiaria.cargarAgendaDelDia(clientes, fechaFiltro);
        String reporteCola = agendaDiaria.generarReporteCola(origen);
        vista.getPanelContingencia().getVistaColaReporte().setText(reporteCola);
    }

    public void refrescarComboConsultoriosContingencia() {
        PanelContingencia pcont = vista.getPanelContingencia();
        Object prevSelection = pcont.getComboConsultorios().getSelectedItem();
        pcont.getComboConsultorios().removeAllItems();

        List<Consultorio> consultorios = consultorioCrud.listarTodos();
        for (Consultorio c : consultorios) {
            pcont.getComboConsultorios().addItem(c.getNit() + " - " + c.getNombreConsultorio());
        }
        pcont.getComboConsultorios().addItem("[Todos los Pacientes (pacientes.dat)]");

        if (prevSelection != null) {
            pcont.getComboConsultorios().setSelectedItem(prevSelection);
        } else if (pcont.getComboConsultorios().getItemCount() > 0) {
            pcont.getComboConsultorios().setSelectedIndex(0);
        }
    }

    private List<Cliente> obtenerListaClientesContingencia() {
        PanelContingencia pcont = vista.getPanelContingencia();
        String item = (String) pcont.getComboConsultorios().getSelectedItem();

        if (item == null || item.isEmpty()) {
            return new ArrayList<>();
        }

        if (item.startsWith("[Todos")) {
            return new ArrayList<>(clienteCrud.listarTodos());
        }

        try {
            int nit = Integer.parseInt(item.split(" - ")[0].trim());
            Consultorio cons = consultorioCrud.buscarConsultorio(nit);
            if (cons != null) {
                return new ArrayList<>(cons.getConsultas().values());
            }
        } catch (Exception ignored) {
        }

        return new ArrayList<>();
    }

    private String obtenerNombreOrigenContingencia() {
        PanelContingencia pcont = vista.getPanelContingencia();
        String item = (String) pcont.getComboConsultorios().getSelectedItem();
        if (item == null || item.startsWith("[Todos")) {
            return "Registro General (pacientes.dat)";
        }
        return item;
    }

    private void autoCompletarPacienteEnFormularioCita() {
        PanelConsultas pc = vista.getPanelConsultas();
        String cedStr = pc.getCedulaPacienteCitaF().getText().trim();
        if (cedStr.isEmpty()) {
            return;
        }
        try {
            int ced = Integer.parseInt(cedStr);
            Cliente pac = clienteCrud.buscarPaciente(ced);
            if (pac != null) {
                pc.getComboPrioridadCita().setSelectedItem(pac.getPrioridad());
                String proc = (pac.getTipoDeAtencion() != null && !pac.getTipoDeAtencion().isEmpty())
                        ? pac.getTipoDeAtencion().get(0) : "Diagnostico";
                pc.getComboProcedimientoCita().setSelectedItem(proc);
                if (pac.getFechaCita() != null) {
                    pc.getFechaCitaSpinner().setValue(pac.getFechaCita());
                }
                for (int i = 0; i < pc.getComboPacientesRegistrados().getItemCount(); i++) {
                    String item = pc.getComboPacientesRegistrados().getItemAt(i);
                    if (item.startsWith(pac.getCedula() + " - ")) {
                        pc.getComboPacientesRegistrados().setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void generarPilaContingencia() {
        List<Cliente> clientes = obtenerListaClientesContingencia();
        this.pilaContingenciaActual = planContingencia.generarPilaUrgencias(clientes);

        String origen = obtenerNombreOrigenContingencia();
        String reporte = planContingencia.generarReportePila(pilaContingenciaActual, origen);
        vista.getPanelContingencia().getVistaPilaReporte().setText(reporte);

        JOptionPane.showMessageDialog(vista,
                "Pila de contingencia generada exitosamente.\n"
                        + "Origen: " + origen + "\n"
                        + "Citas urgentes en orden prioritario (LIFO): " + pilaContingenciaActual.size(),
                "Plan de Contingencia",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void llamarUrgenciaPila() {
        if (pilaContingenciaActual == null || pilaContingenciaActual.isEmpty()) {
            JOptionPane.showMessageDialog(vista,
                    "No hay pacientes pendientes en la Pila de Urgencias.\nPresione 'Generar Pila Urgencias' para cargarla.",
                    "Pila Vacía",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Cliente prioritario = pilaContingenciaActual.pop();

        String origen = obtenerNombreOrigenContingencia();
        vista.getPanelContingencia().getVistaPilaReporte()
                .setText(planContingencia.generarReportePila(pilaContingenciaActual, origen));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String fecha = prioritario.getFechaCita() != null ? sdf.format(prioritario.getFechaCita()) : "Sin fecha";

        JOptionPane.showMessageDialog(vista,
                "🚨 LLAMANDO A PACIENTE PRIORITARIO (PILA - POP):\n\n"
                        + "Cédula: " + prioritario.getCedula() + "\n"
                        + "Nombre: " + prioritario.getNombre() + "\n"
                        + "Teléfono: " + prioritario.getTelefono() + "\n"
                        + "Atención: Extracción (Urgente)\n"
                        + "Fecha Cita: " + fecha + "\n\n"
                        + "Urgencias pendientes en pila: " + pilaContingenciaActual.size(),
                "Llamado Prioritario",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void cargarAgendaDiariaCola() {
        List<Cliente> clientes = obtenerListaClientesContingencia();
        Date fechaFiltro = null;
        if (vista.getPanelContingencia().getChkFiltrarPorFecha().isSelected()) {
            fechaFiltro = (Date) vista.getPanelContingencia().getFechaAgendaSpinner().getValue();
        }

        agendaDiaria.cargarAgendaDelDia(clientes, fechaFiltro);

        String origen = obtenerNombreOrigenContingencia();
        String reporte = agendaDiaria.generarReporteCola(origen);
        vista.getPanelContingencia().getVistaColaReporte().setText(reporte);

        JOptionPane.showMessageDialog(vista,
                "Agenda cargada en la Cola de Atención (FIFO).\n"
                        + "Origen: " + origen + "\n"
                        + "Turnos en espera: " + agendaDiaria.totalEnCola(),
                "Agenda de Turnos",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void atenderTurnoCola() {
        if (agendaDiaria.totalEnCola() == 0) {
            JOptionPane.showMessageDialog(vista,
                    "No hay pacientes en espera en la Cola de Atención Diaria.\nPresione 'Cargar Cola Agenda' para sincronizar.",
                    "Cola Vacía",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Cliente atendido = agendaDiaria.atenderSiguiente();

        String origen = obtenerNombreOrigenContingencia();
        vista.getPanelContingencia().getVistaColaReporte().setText(agendaDiaria.generarReporteCola(origen));

        String procedimiento = (atendido.getTipoDeAtencion() != null && !atendido.getTipoDeAtencion().isEmpty())
                ? atendido.getTipoDeAtencion().get(0)
                : "Diagnostico";

        JOptionPane.showMessageDialog(vista,
                "🔔 ATENDIENDO TURNO DE LA AGENDA (COLA - POLL):\n\n"
                        + "Cédula: " + atendido.getCedula() + "\n"
                        + "Nombre: " + atendido.getNombre() + "\n"
                        + "Procedimiento: " + procedimiento + "\n\n"
                        + "Turnos restantes en cola: " + agendaDiaria.totalEnCola(),
                "Turno Atendido",
                JOptionPane.INFORMATION_MESSAGE);
    }
}