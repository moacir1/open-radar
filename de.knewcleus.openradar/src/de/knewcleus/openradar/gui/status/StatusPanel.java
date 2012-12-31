package de.knewcleus.openradar.gui.status;

import java.awt.Color;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JTextField;

import de.knewcleus.openradar.gui.GuiMasterController;
import de.knewcleus.openradar.gui.Palette;
import de.knewcleus.openradar.gui.setup.AirportData.FgComMode;
import de.knewcleus.openradar.gui.status.radio.RadioPanel;
import de.knewcleus.openradar.gui.status.runways.RunwayPanel;
import de.knewcleus.openradar.weather.IMetarListener;
import de.knewcleus.openradar.weather.MetarData;

/**
 * The panel containing the status information
 * 
 * @author Wolfram Wagner
 */
public class StatusPanel extends javax.swing.JPanel implements IMetarListener {

    private static final long serialVersionUID = 1L;
    private GuiMasterController master;
    private RadioPanel radioPanel;
    private RunwayPanel runwayPanel;
    
    private javax.swing.JPanel headerPanel;
    private javax.swing.JLabel lbCurrentCallSign;
    private JTextField tfCurrentCallSign;
    private javax.swing.JLabel lbTime;
    private javax.swing.JLabel lbAirport;
    private javax.swing.JLabel lbPressure;
    private javax.swing.JLabel lbVisibility;
    private javax.swing.JLabel lbPtS;
    private javax.swing.JLabel lbSelection;
    private javax.swing.JLabel lbWind;
    private javax.swing.JSeparator sep1;
    private javax.swing.JSeparator sep2;
//    private javax.swing.JSeparator sep3;

    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    

    /**
     * Creates new form WeatherRadioRunwayPanel
     */
    public StatusPanel(GuiMasterController guiInteractionManager) {
        this.master=guiInteractionManager;
        this.master.getStatusManager().setStatusPanel(this);
        this.master.getMetarReader().addMetarListener(this);
        
        radioPanel = new RadioPanel(guiInteractionManager,guiInteractionManager.getRadioManager());
        
        runwayPanel = new RunwayPanel(guiInteractionManager);
    
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        initComponents();
    }

    private void initComponents() {
        setBackground(Palette.DESKTOP);
        setForeground(Palette.DESKTOP_TEXT);
        
        java.awt.GridBagConstraints gridBagConstraints;
// todo nice up!
        headerPanel = new javax.swing.JPanel();
        lbCurrentCallSign = new javax.swing.JLabel();
        tfCurrentCallSign = new JTextField();
        lbTime = new javax.swing.JLabel();
        lbAirport = new javax.swing.JLabel();
        lbSelection = new javax.swing.JLabel();
        lbPressure = new javax.swing.JLabel();
        lbVisibility = new javax.swing.JLabel();
        lbWind = new javax.swing.JLabel();
        lbPtS = new javax.swing.JLabel();
        sep1 = new javax.swing.JSeparator();
        sep2 = new javax.swing.JSeparator();
      //  sep3 = new javax.swing.JSeparator();

        setLayout(new java.awt.GridBagLayout());

        headerPanel.setLayout(new java.awt.GridBagLayout());
        headerPanel.setOpaque(false);
        
        lbCurrentCallSign.setForeground(Color.white);
        lbCurrentCallSign.setText("Call Sign:");
        lbCurrentCallSign.setToolTipText("Current ATC call sign");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth=1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 0, 4);
        headerPanel.add(lbCurrentCallSign, gridBagConstraints);

        tfCurrentCallSign.setOpaque(false);
        tfCurrentCallSign.setText(master.getDataRegistry().getInitialATCCallSign());
        tfCurrentCallSign.setToolTipText("Your current airport");
        tfCurrentCallSign.setPreferredSize(new Dimension(80,(int)tfCurrentCallSign.getPreferredSize().getHeight()));
        tfCurrentCallSign.addActionListener(master.getStatusManager().getCallSignActionListener());
        tfCurrentCallSign.addKeyListener(master.getStatusManager().getCallSignKeyListener());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 0, 4);
        headerPanel.add(tfCurrentCallSign, gridBagConstraints);
        
        lbTime.setForeground(Color.white);
        lbTime.setText("");
        lbTime.setToolTipText("time in UTC");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth=1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        headerPanel.add(lbTime, gridBagConstraints);
        
        lbAirport.setForeground(Color.white);
        lbAirport.setText(master.getDataRegistry().getAirportCode() + " " + master.getDataRegistry().getAirportName());
        lbAirport.setToolTipText("Current call sign");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth=2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 10, 0, 4);
        headerPanel.add(lbAirport, gridBagConstraints);

        lbSelection.setForeground(Color.white);
        lbSelection.setText("");
        lbSelection.setToolTipText("Callsign of selected contact");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        headerPanel.add(lbSelection, gridBagConstraints);

        lbPtS.setForeground(Color.white);
        lbPtS.setText("StP: (Please select a contact)");
        lbPtS.setToolTipText("magn., wind compens. direction Selection=>Pointer, back, distance, time needed");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
//        gridBagConstraints.ipadx = 17;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 10, 2, 0);
        headerPanel.add(lbPtS, gridBagConstraints);

        lbWind.setForeground(Palette.LIGHTBLUE);
        lbWind.setText("W:");
        lbWind.setToolTipText("Wind: knods@Direction");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 4);
        headerPanel.add(lbWind, gridBagConstraints);

        lbPressure.setForeground(Palette.LIGHTBLUE);
        lbPressure.setText("");
        lbPressure.setToolTipText("Pressure");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 4);
        headerPanel.add(lbPressure, gridBagConstraints);

        lbVisibility.setForeground(Palette.LIGHTBLUE);
        lbVisibility.setText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 4);
        headerPanel.add(lbVisibility, gridBagConstraints);


        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        add(headerPanel, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);        
        add(radioPanel, gridBagConstraints);

        if(!master.getDataRegistry().getRadios().isEmpty() && master.getDataRegistry().getFgComMode()!=FgComMode.Off) { 
            // in the other case radioPanel is empty and two separator would be displayed...
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
            add(sep1, gridBagConstraints);
        }
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        add(runwayPanel, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        add(sep2, gridBagConstraints);

//        gridBagConstraints = new java.awt.GridBagConstraints();
//        gridBagConstraints.gridx = 0;
//        gridBagConstraints.gridy = 5;
//        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
//        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
//        gridBagConstraints.weightx = 1.0;
//        gridBagConstraints.weighty = 1.0;
//        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
//        add(sep3, gridBagConstraints);
        
        updateTime();
        doLayout();
      
    }
    
    public void updateTime() {
        lbTime.setText("UTC: "+sdf.format(new Date()));
    }

    public void setAirport(String airport) {
        lbAirport.setText(airport);
    }
    
    public String getCurrentCallSign() {
        return tfCurrentCallSign.getText();
    }

    public void setSelectedCallSign(String callsign) {
        lbSelection.setText(callsign);
        lbSelection.invalidate();
    }

    @Override
    public void registerNewMetar(MetarData metar) {
        StringBuilder sb = new StringBuilder();
        sb.append("W: ");
        if(metar.getWindDirection()==-1) {
            sb.append("VRB");
        } else {
            sb.append(metar.getWindDirection());
        }
        if(metar.getWindDirectionMin()>-1) {
            sb.append("(");
            sb.append(metar.getWindDirectionMin());
            sb.append("-");
            sb.append(metar.getWindDirectionMax());
            sb.append(")");
        }
        sb.append("@");
        sb.append(metar.getWindSpeed());
        if(metar.getWindSpeedGusts()>-1) {
            sb.append("(");
            sb.append(metar.getWindSpeedGusts());
            sb.append(")");
        }
        
        lbWind.setText(sb.toString());
        lbPressure.setText(String.format("P: %2.2f / %4.1f", metar.getPressureInHG(),metar.getPressureHPa()));
        lbWind.setToolTipText(metar.getMetarBaseData());
        lbPressure.setToolTipText(metar.getMetarBaseData());
        lbVisibility.setText("V: "+metar.getVisibility()+" "+metar.getVisibilityUnit());
        lbVisibility.setToolTipText(metar.getMetarBaseData());

        runwayPanel.refreshRunways(metar);
        doLayout();
        revalidate();
    }

    public void setSelectionToPointer(Long degreesToPointer, Long degreesToSelection, Double distanceMiles, Long timeMinutes) {
        String dTP = degreesToPointer==null ? "n/a" : String.format("%03d",degreesToPointer); 
        String dTS = degreesToSelection==null ? "n/a" : String.format("%03d",degreesToSelection); 
        String dist = distanceMiles==null ? "n/a" : String.format("%.1f", distanceMiles);
        String min = timeMinutes==null ? "n/a" : Long.toString(timeMinutes);
        
        lbPtS.setText("StP: "+dTP+" / "+dTS+"     "+dist+" miles     "+min+" min.");
    }

    public String getActiveRunways() {
        return runwayPanel.getActiveRunways();
    }

}