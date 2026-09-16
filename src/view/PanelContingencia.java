package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerDateModel;

public class PanelContingencia extends JPanel {

    private JComboBox<String> comboConsultorios;
    private JButton btnRefrescarConsultorios;
    private JSpinner fechaAgendaSpinner;
    private JCheckBox chkFiltrarPorFecha;

    private JButton btnGenerarPila;
    private JButton btnLlamarUrgencia;
    private JButton btnCargarAgendaCola;
    private JButton btnAtenderTurnoCola;

    private JTextArea vistaPilaReporte;
    private JTextArea vistaColaReporte;

    public PanelContingencia() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel panelAcciones = new JPanel(new BorderLayout(5, 5));
        panelAcciones.setBorder(BorderFactory.createTitledBorder("Control de Estructuras (Taller 2)"));

        JPanel filaConfig = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        filaConfig.add(new JLabel("Consultorio:"));
        comboConsultorios = new JComboBox<>();
        comboConsultorios.setPreferredSize(new Dimension(240, 24));
        filaConfig.add(comboConsultorios);

        btnRefrescarConsultorios = new JButton("Refrescar");
        filaConfig.add(btnRefrescarConsultorios);

        filaConfig.add(new JLabel("Fecha Agenda:"));
        SpinnerDateModel modeloFecha = new SpinnerDateModel();
        fechaAgendaSpinner = new JSpinner(modeloFecha);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(fechaAgendaSpinner, "dd/MM/yyyy");
        fechaAgendaSpinner.setEditor(editor);
        filaConfig.add(fechaAgendaSpinner);

        chkFiltrarPorFecha = new JCheckBox("Filtrar solo esta fecha", false);
        filaConfig.add(chkFiltrarPorFecha);

        JPanel filaBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        btnGenerarPila = new JButton("Generar Pila Urgencias");
        btnLlamarUrgencia = new JButton("Llamar Urgencia (Pop)");
        btnCargarAgendaCola = new JButton("Cargar Cola Agenda");
        btnAtenderTurnoCola = new JButton("Atender Turno (Poll)");

        filaBotones.add(btnGenerarPila);
        filaBotones.add(btnLlamarUrgencia);
        filaBotones.add(btnCargarAgendaCola);
        filaBotones.add(btnAtenderTurnoCola);

        panelAcciones.add(filaConfig, BorderLayout.NORTH);
        panelAcciones.add(filaBotones, BorderLayout.SOUTH);

        JPanel panelReportes = new JPanel(new GridLayout(1, 2, 10, 0));

        vistaPilaReporte = new JTextArea();
        vistaPilaReporte.setEditable(false);
        vistaPilaReporte.setLineWrap(false);
        vistaPilaReporte.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPila = new JScrollPane(vistaPilaReporte);
        scrollPila.setBorder(BorderFactory.createTitledBorder("Pila Contingencia - Extracciones Urgentes (LIFO)"));

        vistaColaReporte = new JTextArea();
        vistaColaReporte.setEditable(false);
        vistaColaReporte.setLineWrap(false);
        vistaColaReporte.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollCola = new JScrollPane(vistaColaReporte);
        scrollCola.setBorder(BorderFactory.createTitledBorder("Cola Atención Diaria - Agenda de Turnos (FIFO)"));

        panelReportes.add(scrollPila);
        panelReportes.add(scrollCola);

        add(panelAcciones, BorderLayout.NORTH);
        add(panelReportes, BorderLayout.CENTER);
    }

    public JComboBox<String> getComboConsultorios() { return comboConsultorios; }
    public JButton getBtnRefrescarConsultorios() { return btnRefrescarConsultorios; }
    public JSpinner getFechaAgendaSpinner() { return fechaAgendaSpinner; }
    public JCheckBox getChkFiltrarPorFecha() { return chkFiltrarPorFecha; }
    public JButton getBtnGenerarPila() { return btnGenerarPila; }
    public JButton getBtnLlamarUrgencia() { return btnLlamarUrgencia; }
    public JButton getBtnCargarAgendaCola() { return btnCargarAgendaCola; }
    public JButton getBtnAtenderTurnoCola() { return btnAtenderTurnoCola; }
    public JTextArea getVistaPilaReporte() { return vistaPilaReporte; }
    public JTextArea getVistaColaReporte() { return vistaColaReporte; }
}