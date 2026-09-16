package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import model.Cliente;
import model.Consultorio;

public class PanelConsultas extends JPanel {

    // Formulario de Información del Consultorio
    private JLabel nitL;
    private JTextField nitF;
    private JLabel nombreConsultorioL;
    private JTextField nombreConsultorioF;
    private JLabel direccionL;
    private JTextField direccionF;
    private JLabel telefonoDoctorL;
    private JTextField telefonoDoctorF;
    private JButton btnGuardarConsultorio;
    private JButton btnActualizarConsultorio;
    private JButton btnEliminarConsultorio;
    private JButton btnLimpiarConsultorio;

    // Formulario de Gestión de Citas y Turnos
    private JLabel comboCitasL;
    private JComboBox<String> comboCitasExistentes;
    private JLabel codigoCitaL;
    private JTextField codigoCitaF;
    private JButton btnCargarCita;
    private JLabel comboPacientesL;
    private JComboBox<String> comboPacientesRegistrados;
    private JLabel cedulaPacienteCitaL;
    private JTextField cedulaPacienteCitaF;
    private JLabel doctorCitaL;
    private JComboBox<String> comboDoctores;
    private JLabel prioridadCitaL;
    private JComboBox<String> comboPrioridadCita;
    private JLabel procedimientoCitaL;
    private JComboBox<String> comboProcedimientoCita;
    private JLabel fechaCitaL;
    private JSpinner fechaCitaSpinner;

    private JButton btnAsignarConsulta;
    private JButton btnActualizarConsulta;
    private JButton btnCancelarConsulta;
    private JButton btnAtenderSiguiente;
    private JButton btnLimpiarCita;

    // Panel superior de filtro y selección de consultorios
    private JLabel lblSeleccionarConsultorio;
    private JComboBox<String> comboSeleccionarConsultorio;
    private JLabel lblBuscarNit;
    private JTextField txtBuscarNit;
    private JButton btnBuscarConsultorio;
    private JButton btnRefrescarDoctores;

    // Vistas y sub-pestañas
    private JTextArea vistaDatosConsultorio;
    private JTextArea vistaCitasAgendadas;
    private JTextArea vistaColaCitas;
    private JTextArea vistaLogCancelaciones;
    private JScrollPane scrollConsultorio;
    private JScrollPane scrollCitas;
    private JScrollPane scrollCola;
    private JScrollPane scrollLog;
    private JTabbedPane subModuloConsultas;

    public PanelConsultas() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel panelIzquierdo = new JPanel(new BorderLayout(0, 10));
        panelIzquierdo.setPreferredSize(new Dimension(380, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Subpanel: Información del Consultorio
        JPanel panelFormConsultorio = new JPanel(new GridBagLayout());
        panelFormConsultorio.setBorder(BorderFactory.createTitledBorder("Información del Consultorio"));

        nitL = new JLabel("NIT Consultorio:");
        nitF = new JTextField(12);
        nombreConsultorioL = new JLabel("Nombre:");
        nombreConsultorioF = new JTextField(12);
        direccionL = new JLabel("Dirección:");
        direccionF = new JTextField(12);
        telefonoDoctorL = new JLabel("Tel. Contacto:");
        telefonoDoctorF = new JTextField(12);

        btnGuardarConsultorio = new JButton("Guardar");
        btnActualizarConsultorio = new JButton("Actualizar");
        btnEliminarConsultorio = new JButton("Eliminar");
        btnLimpiarConsultorio = new JButton("Limpiar");

        JPanel panelBotonesConsultorio = new JPanel(new GridLayout(1, 4, 4, 4));
        panelBotonesConsultorio.add(btnGuardarConsultorio);
        panelBotonesConsultorio.add(btnActualizarConsultorio);
        panelBotonesConsultorio.add(btnEliminarConsultorio);
        panelBotonesConsultorio.add(btnLimpiarConsultorio);

        agregarFilaGBC(panelFormConsultorio, nitL, nitF, gbc, 0);
        agregarFilaGBC(panelFormConsultorio, nombreConsultorioL, nombreConsultorioF, gbc, 1);
        agregarFilaGBC(panelFormConsultorio, direccionL, direccionF, gbc, 2);
        agregarFilaGBC(panelFormConsultorio, telefonoDoctorL, telefonoDoctorF, gbc, 3);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(6, 2, 4, 2);
        panelFormConsultorio.add(panelBotonesConsultorio, gbc);

        // 2. Subpanel: Gestión de Citas y Turnos
        JPanel panelFormCitas = new JPanel(new GridBagLayout());
        panelFormCitas.setBorder(BorderFactory.createTitledBorder("Gestión de Citas y Turnos"));

        comboCitasL = new JLabel("Citas en Consultorio:");
        comboCitasExistentes = new JComboBox<>();
        comboCitasExistentes.addItem("[Seleccionar cita existente...]");

        codigoCitaL = new JLabel("Código Cita:");
        codigoCitaF = new JTextField(10);
        btnCargarCita = new JButton("Cargar");

        JPanel panelCodigoConBoton = new JPanel(new BorderLayout(4, 0));
        panelCodigoConBoton.add(codigoCitaF, BorderLayout.CENTER);
        panelCodigoConBoton.add(btnCargarCita, BorderLayout.EAST);

        comboPacientesL = new JLabel("Paciente Registrado:");
        comboPacientesRegistrados = new JComboBox<>();
        comboPacientesRegistrados.addItem("[Seleccionar paciente registrado...]");

        cedulaPacienteCitaL = new JLabel("Cédula Paciente:");
        cedulaPacienteCitaF = new JTextField(12);

        doctorCitaL = new JLabel("Doctor Asignado:");
        comboDoctores = new JComboBox<>();
        comboDoctores.addItem("[Auto-asignar disponible]");

        prioridadCitaL = new JLabel("Prioridad:");
        comboPrioridadCita = new JComboBox<>(new String[]{"Normal", "Urgente"});

        procedimientoCitaL = new JLabel("Procedimiento:");
        comboProcedimientoCita = new JComboBox<>(new String[]{"Extraccion", "Calzas", "Limpieza", "Diagnostico"});

        fechaCitaL = new JLabel("Fecha / Hora Cita:");
        SpinnerDateModel modeloFecha = new SpinnerDateModel();
        fechaCitaSpinner = new JSpinner(modeloFecha);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(fechaCitaSpinner, "dd/MM/yyyy HH:mm");
        fechaCitaSpinner.setEditor(editor);

        btnAsignarConsulta = new JButton("Agendar Cita");
        btnActualizarConsulta = new JButton("Modificar Cita");
        btnCancelarConsulta = new JButton("Cancelar Cita");
        btnAtenderSiguiente = new JButton("Atender Siguiente");
        btnLimpiarCita = new JButton("Limpiar Campos");

        JPanel panelBotonesCitas = new JPanel(new GridLayout(3, 2, 4, 4));
        panelBotonesCitas.add(btnAsignarConsulta);
        panelBotonesCitas.add(btnActualizarConsulta);
        panelBotonesCitas.add(btnCancelarConsulta);
        panelBotonesCitas.add(btnAtenderSiguiente);
        panelBotonesCitas.add(btnLimpiarCita);
        panelBotonesCitas.add(btnCargarCita);

        gbc.insets = new Insets(3, 4, 3, 4);
        agregarFilaGBC(panelFormCitas, comboCitasL, comboCitasExistentes, gbc, 0);
        agregarFilaGBC(panelFormCitas, codigoCitaL, panelCodigoConBoton, gbc, 1);
        agregarFilaGBC(panelFormCitas, comboPacientesL, comboPacientesRegistrados, gbc, 2);
        agregarFilaGBC(panelFormCitas, cedulaPacienteCitaL, cedulaPacienteCitaF, gbc, 3);
        agregarFilaGBC(panelFormCitas, doctorCitaL, comboDoctores, gbc, 4);
        agregarFilaGBC(panelFormCitas, prioridadCitaL, comboPrioridadCita, gbc, 5);
        agregarFilaGBC(panelFormCitas, procedimientoCitaL, comboProcedimientoCita, gbc, 6);
        agregarFilaGBC(panelFormCitas, fechaCitaL, fechaCitaSpinner, gbc, 7);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(6, 2, 4, 2);
        panelFormCitas.add(panelBotonesCitas, gbc);

        panelIzquierdo.add(panelFormConsultorio, BorderLayout.NORTH);
        panelIzquierdo.add(panelFormCitas, BorderLayout.CENTER);

        JScrollPane scrollPanelIzquierdo = new JScrollPane(panelIzquierdo);
        scrollPanelIzquierdo.setBorder(null);
        scrollPanelIzquierdo.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Panel Derecho: Filtro y Vistas Tabuladas
        JPanel panelDerecho = new JPanel(new BorderLayout(5, 5));

        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panelBusqueda.setBorder(BorderFactory.createTitledBorder("Filtro y Selección de Consultorio"));

        lblSeleccionarConsultorio = new JLabel("Consultorio Activo:");
        comboSeleccionarConsultorio = new JComboBox<>();
        comboSeleccionarConsultorio.setPreferredSize(new Dimension(190, 24));

        lblBuscarNit = new JLabel("NIT:");
        txtBuscarNit = new JTextField(7);
        btnBuscarConsultorio = new JButton("Buscar");
        btnRefrescarDoctores = new JButton("Actualizar");

        panelBusqueda.add(lblSeleccionarConsultorio);
        panelBusqueda.add(comboSeleccionarConsultorio);
        panelBusqueda.add(lblBuscarNit);
        panelBusqueda.add(txtBuscarNit);
        panelBusqueda.add(btnBuscarConsultorio);
        panelBusqueda.add(btnRefrescarDoctores);

        subModuloConsultas = new JTabbedPane();

        JPanel panelVistasDivididas = new JPanel(new GridLayout(2, 1, 0, 6));

        vistaDatosConsultorio = new JTextArea();
        vistaDatosConsultorio.setEditable(false);
        scrollConsultorio = new JScrollPane(vistaDatosConsultorio);
        scrollConsultorio.setBorder(BorderFactory.createTitledBorder("Detalles del Consultorio Seleccionado"));

        vistaCitasAgendadas = new JTextArea();
        vistaCitasAgendadas.setEditable(false);
        scrollCitas = new JScrollPane(vistaCitasAgendadas);
        scrollCitas.setBorder(BorderFactory.createTitledBorder("Citas Registradas en este Consultorio"));

        panelVistasDivididas.add(scrollConsultorio);
        panelVistasDivididas.add(scrollCitas);

        vistaColaCitas = new JTextArea();
        vistaColaCitas.setEditable(false);
        scrollCola = new JScrollPane(vistaColaCitas);
        scrollCola.setBorder(BorderFactory.createTitledBorder("Cola de Espera de Pacientes (Turnos y Prioridades)"));

        vistaLogCancelaciones = new JTextArea();
        vistaLogCancelaciones.setEditable(false);
        scrollLog = new JScrollPane(vistaLogCancelaciones);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Registro de Cancelaciones (Historial y Justificaciones)"));

        subModuloConsultas.addTab("Citas Agendadas", panelVistasDivididas);
        subModuloConsultas.addTab("Cola de Espera (Turnos)", scrollCola);
        subModuloConsultas.addTab("Log Cancelaciones", scrollLog);

        panelDerecho.add(panelBusqueda, BorderLayout.NORTH);
        panelDerecho.add(subModuloConsultas, BorderLayout.CENTER);

        add(scrollPanelIzquierdo, BorderLayout.WEST);
        add(panelDerecho, BorderLayout.CENTER);
    }

    private void agregarFilaGBC(JPanel panel, JLabel label, Object campo, GridBagConstraints gbc, int fila) {
        gbc.gridwidth = 1;
        gbc.gridy = fila;

        gbc.gridx = 0;
        gbc.weightx = 0.3;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add((java.awt.Component) campo, gbc);
    }

    public void actualizarComboConsultorios(List<Consultorio> consultorios) {
        Object prevSelection = comboSeleccionarConsultorio.getSelectedItem();
        comboSeleccionarConsultorio.removeAllItems();
        if (consultorios != null) {
            for (Consultorio c : consultorios) {
                comboSeleccionarConsultorio.addItem(c.getNit() + " - " + c.getNombreConsultorio());
            }
        }
        if (prevSelection != null) {
            comboSeleccionarConsultorio.setSelectedItem(prevSelection);
        } else if (comboSeleccionarConsultorio.getItemCount() > 0) {
            comboSeleccionarConsultorio.setSelectedIndex(0);
        }
    }

    public void actualizarComboCitas(Consultorio cons) {
        comboCitasExistentes.removeAllItems();
        comboCitasExistentes.addItem("[Seleccionar cita existente...]");
        if (cons != null && cons.getConsultas() != null) {
            cons.getConsultas().forEach((codigo, cli) -> {
                String nom = cli != null ? cli.getNombre() : "N/A";
                String atenc = (cli != null && !cli.getTipoDeAtencion().isEmpty()) ? cli.getTipoDeAtencion().get(0) : "Diag";
                String prio = (cli != null && cli.getPrioridad() != null) ? cli.getPrioridad() : "Normal";
                comboCitasExistentes.addItem(codigo + " - " + nom + " (" + atenc + " - " + prio + ")");
            });
        }
    }

    public void actualizarComboPacientes(List<Cliente> pacientes) {
        Object prev = comboPacientesRegistrados.getSelectedItem();
        comboPacientesRegistrados.removeAllItems();
        comboPacientesRegistrados.addItem("[Seleccionar paciente registrado...]");
        if (pacientes != null) {
            for (Cliente c : pacientes) {
                String atenc = !c.getTipoDeAtencion().isEmpty() ? c.getTipoDeAtencion().get(0) : "Diagnostico";
                comboPacientesRegistrados.addItem(c.getCedula() + " - " + c.getNombre() + " (" + atenc + " - " + c.getPrioridad() + ")");
            }
        }
        if (prev != null) {
            comboPacientesRegistrados.setSelectedItem(prev);
        }
    }

    public void limpiarFormularioCita() {
        codigoCitaF.setText("");
        cedulaPacienteCitaF.setText("");
        comboPrioridadCita.setSelectedIndex(0);
        comboProcedimientoCita.setSelectedIndex(0);
        fechaCitaSpinner.setValue(new Date());
        if (comboCitasExistentes.getItemCount() > 0) {
            comboCitasExistentes.setSelectedIndex(0);
        }
        if (comboPacientesRegistrados.getItemCount() > 0) {
            comboPacientesRegistrados.setSelectedIndex(0);
        }
    }

    // Getters para formulario de Consultorio
    public JTextField getNitF() { return nitF; }
    public JTextField getNombreConsultorioF() { return nombreConsultorioF; }
    public JTextField getDireccionF() { return direccionF; }
    public JTextField getTelefonoDoctorF() { return telefonoDoctorF; }
    public JButton getBtnGuardarConsultorio() { return btnGuardarConsultorio; }
    public JButton getBtnActualizarConsultorio() { return btnActualizarConsultorio; }
    public JButton getBtnEliminarConsultorio() { return btnEliminarConsultorio; }
    public JButton getBtnLimpiarConsultorio() { return btnLimpiarConsultorio; }

    // Getters para formulario de Citas
    public JComboBox<String> getComboCitasExistentes() { return comboCitasExistentes; }
    public JTextField getCodigoCitaF() { return codigoCitaF; }
    public JButton getBtnCargarCita() { return btnCargarCita; }
    public JComboBox<String> getComboPacientesRegistrados() { return comboPacientesRegistrados; }
    public JTextField getCedulaPacienteCitaF() { return cedulaPacienteCitaF; }
    public JComboBox<String> getComboDoctores() { return comboDoctores; }
    public JComboBox<String> getComboPrioridadCita() { return comboPrioridadCita; }
    public JComboBox<String> getComboProcedimientoCita() { return comboProcedimientoCita; }
    public JSpinner getFechaCitaSpinner() { return fechaCitaSpinner; }
    public JButton getBtnAsignarConsulta() { return btnAsignarConsulta; }
    public JButton getBtnActualizarConsulta() { return btnActualizarConsulta; }
    public JButton getBtnCancelarConsulta() { return btnCancelarConsulta; }
    public JButton getBtnAtenderSiguiente() { return btnAtenderSiguiente; }
    public JButton getBtnLimpiarCita() { return btnLimpiarCita; }

    // Getters de búsqueda y vistas
    public JComboBox<String> getComboSeleccionarConsultorio() { return comboSeleccionarConsultorio; }
    public JTextField getTxtBuscarNit() { return txtBuscarNit; }
    public JButton getBtnBuscarConsultorio() { return btnBuscarConsultorio; }
    public JButton getBtnRefrescarDoctores() { return btnRefrescarDoctores; }
    public JTextArea getVistaDatosConsultorio() { return vistaDatosConsultorio; }
    public JTextArea getVistaCitasAgendadas() { return vistaCitasAgendadas; }
    public JTextArea getVistaColaCitas() { return vistaColaCitas; }
    public JTextArea getVistaLogCancelaciones() { return vistaLogCancelaciones; }
    public JTabbedPane getSubModuloConsultas() { return subModuloConsultas; }
}