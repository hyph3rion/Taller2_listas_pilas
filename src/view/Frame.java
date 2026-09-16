package view;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class Frame extends JFrame {

    private PanelDatos panelDatos;
    private PanelDoctores panelDoctores;
    private PanelVista panelVista;
    private PanelDoctoresVista panelDocsVista;
    private PanelConsultas panelConsultas;
    private PanelContingencia panelContingencia;
    private PanelOperaciones panelOperaciones;
    private JTabbedPane moduloPestanas;

    public Frame() {
        setTitle("Sistema de agendamiento consultorio odontologico");
        setSize(950, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        panelDatos = new PanelDatos();
        panelDoctores = new PanelDoctores();
        panelVista = new PanelVista();
        panelConsultas = new PanelConsultas();
        panelContingencia = new PanelContingencia();
        panelDocsVista = new PanelDoctoresVista();
        panelOperaciones = new PanelOperaciones();

        moduloPestanas = new JTabbedPane();

        JPanel tabPacientes = new JPanel(new BorderLayout());
        tabPacientes.add(panelDatos, BorderLayout.WEST);
        tabPacientes.add(panelVista, BorderLayout.CENTER);

        JPanel tabDoctores = new JPanel(new BorderLayout());
        tabDoctores.add(panelDoctores, BorderLayout.NORTH);
        tabDoctores.add(panelDocsVista, BorderLayout.CENTER);

        JPanel tabConsultorios = new JPanel(new BorderLayout());
        tabConsultorios.add(panelConsultas, BorderLayout.CENTER);

        moduloPestanas.addTab("Gestion de Consultorios", tabConsultorios);
        moduloPestanas.addTab("Gestion de Pacientes", tabPacientes);
        moduloPestanas.addTab("Gestion de Doctores", tabDoctores);
        moduloPestanas.addTab("Plan Contingencia (Pilas/Colas)", panelContingencia);

        add(moduloPestanas, BorderLayout.CENTER);
        add(panelOperaciones, BorderLayout.SOUTH);

        setVisible(true);
    }

    public PanelDatos getPanelDatos() {
        return panelDatos;
    }

    public void setPanelDatos(PanelDatos panelDatos) {
        this.panelDatos = panelDatos;
    }

    public PanelVista getPanelVista() {
        return panelVista;
    }

    public void setPanelVista(PanelVista panelVista) {
        this.panelVista = panelVista;
    }

    public PanelOperaciones getPanelOperaciones() {
        return panelOperaciones;
    }

    public void setPanelOperaciones(PanelOperaciones panelOperaciones) {
        this.panelOperaciones = panelOperaciones;
    }

    public PanelDoctores getPanelDoctores() {
        return panelDoctores;
    }

    public void setPanelDoctores(PanelDoctores panelDoctores) {
        this.panelDoctores = panelDoctores;
    }

    public PanelConsultas getPanelConsultas() {
        return panelConsultas;
    }

    public void setPanelConsultas(PanelConsultas panelConsultas) {
        this.panelConsultas = panelConsultas;
    }

    public PanelContingencia getPanelContingencia() {
        return panelContingencia;
    }

    public void setPanelContingencia(PanelContingencia panelContingencia) {
        this.panelContingencia = panelContingencia;
    }

    public PanelDoctoresVista getPanelDocsVista() {
        return panelDocsVista;
    }

    public void setPanelDocsVista(PanelDoctoresVista panelDocsVista) {
        this.panelDocsVista = panelDocsVista;
    }

    public JTabbedPane getModuloPestanas() {
        return moduloPestanas;
    }

    public void setModuloPestanas(JTabbedPane moduloPestanas) {
        this.moduloPestanas = moduloPestanas;
    }
}
