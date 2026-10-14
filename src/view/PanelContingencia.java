package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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

    private JButton btnGenerarPila;
    private JButton btnLlamarUrgencia;
    private JButton btnGuardarInforme;
    private JButton btnCargarAgendaCola;
    private JButton btnAtenderTurnoCola;

    private JTextArea vistaPilaReporte;
    private JTextArea vistaColaReporte;

    public PanelContingencia() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel panelAcciones = new JPanel(new BorderLayout(5, 5));
        panelAcciones.setBorder(BorderFactory.createTitledBorder("Plan de Contingencia y Agenda Diaria"));

        JPanel filaConfig = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        filaConfig.add(new JLabel("Consultorio:"));
        comboConsultorios = new JComboBox<>();
        comboConsultorios.setPreferredSize(new Dimension(240, 24));
        filaConfig.add(comboConsultorios);

        btnRefrescarConsultorios = new JButton("Refrescar");
        filaConfig.add(btnRefrescarConsultorios);

        filaConfig.add(new JLabel("Fecha de la agenda:"));
        SpinnerDateModel modeloFecha = new SpinnerDateModel();
        fechaAgendaSpinner = new JSpinner(modeloFecha);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(fechaAgendaSpinner, "dd/MM/yyyy");
        fechaAgendaSpinner.setEditor(editor);
        filaConfig.add(fechaAgendaSpinner);

        JPanel filaBotones = new JPanel(new GridLayout(1, 5, 8, 0));
        btnGenerarPila = new JButton("Generar Pila");
        btnLlamarUrgencia = new JButton("Llamar Cliente (Desapilar)");
        btnGuardarInforme = new JButton("Guardar Informe Pila");
        btnCargarAgendaCola = new JButton("Generar Agenda (Cola)");
        btnAtenderTurnoCola = new JButton("Atender Cliente (Desencolar)");

        filaBotones.add(btnGenerarPila);
        filaBotones.add(btnLlamarUrgencia);
        filaBotones.add(btnGuardarInforme);
        filaBotones.add(btnCargarAgendaCola);
        filaBotones.add(btnAtenderTurnoCola);

        panelAcciones.add(filaConfig, BorderLayout.NORTH);
        panelAcciones.add(filaBotones, BorderLayout.SOUTH);

        JPanel panelReportes = new JPanel(new GridLayout(2, 1, 0, 10));

        vistaPilaReporte = new JTextArea();
        vistaPilaReporte.setEditable(false);
        vistaPilaReporte.setLineWrap(false);
        vistaPilaReporte.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPila = new JScrollPane(vistaPilaReporte);
        scrollPila.setBorder(BorderFactory.createTitledBorder("Pila de Contingencia - Extracciones Urgentes"));

        vistaColaReporte = new JTextArea();
        vistaColaReporte.setEditable(false);
        vistaColaReporte.setLineWrap(false);
        vistaColaReporte.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollCola = new JScrollPane(vistaColaReporte);
        scrollCola.setBorder(BorderFactory.createTitledBorder("Cola de Atencion - Agenda Diaria"));

        panelReportes.add(scrollPila);
        panelReportes.add(scrollCola);

        add(panelAcciones, BorderLayout.NORTH);
        add(panelReportes, BorderLayout.CENTER);
    }

    public JComboBox<String> getComboConsultorios() { return comboConsultorios; }
    public JButton getBtnRefrescarConsultorios() { return btnRefrescarConsultorios; }
    public JSpinner getFechaAgendaSpinner() { return fechaAgendaSpinner; }
    public JButton getBtnGenerarPila() { return btnGenerarPila; }
    public JButton getBtnLlamarUrgencia() { return btnLlamarUrgencia; }
    public JButton getBtnGuardarInforme() { return btnGuardarInforme; }
    public JButton getBtnCargarAgendaCola() { return btnCargarAgendaCola; }
    public JButton getBtnAtenderTurnoCola() { return btnAtenderTurnoCola; }
    public JTextArea getVistaPilaReporte() { return vistaPilaReporte; }
    public JTextArea getVistaColaReporte() { return vistaColaReporte; }
}