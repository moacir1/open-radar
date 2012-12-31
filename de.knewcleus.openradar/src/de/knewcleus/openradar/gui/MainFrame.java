package de.knewcleus.openradar.gui;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.knewcleus.openradar.gui.radar.RadarMapPanel;

/**
 * This is the application main window. 
 *
 * @author Wolfram Wagner
 */
public class MainFrame extends javax.swing.JFrame {
    private static final long serialVersionUID = 2623104404247180992L;

    private GuiMasterController guiInteractionManager;

    private JPanel jPnlContentPane = new JPanel();

    private de.knewcleus.openradar.gui.contacts.ContactsPanel contactsPanel;
    private de.knewcleus.openradar.gui.status.DetailPanel detailPanel;
    private javax.swing.JSplitPane hspMain;
    private de.knewcleus.openradar.gui.chat.MpChatPanel mpChatPanel;
    private javax.swing.JPanel pnlRightTop;
    private de.knewcleus.openradar.gui.radar.RadarPanel radarPanel;
    private de.knewcleus.openradar.gui.status.StatusPanel radioRunwayPanel;
    private javax.swing.JSplitPane vspLeft;
    private javax.swing.JSplitPane vspRight;
    
    /**
     * Creates new form MainFrame
     */
    public MainFrame(GuiMasterController guiInteractionManager) {
        this.guiInteractionManager=guiInteractionManager;
        initComponents();
    }
    
    public RadarMapPanel getRadarScreen() {
        return radarPanel.getRadarMapPanel();
    }
    
    private void initComponents() {

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("OpenRadar - "+guiInteractionManager.getDataRegistry().getAirportCode()+" "+guiInteractionManager.getDataRegistry().getAirportName());
        
        // maximize it 
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle maxBounds = env.getMaximumWindowBounds();
        this.setLocation(0, 0);
        this.setSize(maxBounds.width,maxBounds.height);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        // add main view
        this.setContentPane(jPnlContentPane);
        
        jPnlContentPane.setMinimumSize(new java.awt.Dimension(800, 600));
        jPnlContentPane.setPreferredSize(new java.awt.Dimension(1280, 1050));
        jPnlContentPane.setLayout(new java.awt.GridBagLayout());
        jPnlContentPane.setBackground(Palette.DESKTOP);

        hspMain = new javax.swing.JSplitPane();

        java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPnlContentPane.add(hspMain, gridBagConstraints);
        
        
        vspLeft = new javax.swing.JSplitPane();
        radarPanel = new de.knewcleus.openradar.gui.radar.RadarPanel(guiInteractionManager);
        mpChatPanel = new de.knewcleus.openradar.gui.chat.MpChatPanel(guiInteractionManager);
        vspRight = new javax.swing.JSplitPane();
        pnlRightTop = new javax.swing.JPanel();
        radioRunwayPanel = new de.knewcleus.openradar.gui.status.StatusPanel(guiInteractionManager);
        contactsPanel = new de.knewcleus.openradar.gui.contacts.ContactsPanel(guiInteractionManager);
        // detailPanel = new de.knewcleus.openradar.gui.status.DetailPanel(guiInteractionManager);

        // Left MAIN split pane
        
        vspLeft.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        vspLeft.setResizeWeight(0.0);
        vspLeft.setForeground(Palette.DESKTOP);
        hspMain.setLeftComponent(vspLeft);

        // Left top: Radar panel
        
        radarPanel.setMinimumSize(new java.awt.Dimension(400, 400));
        vspLeft.setTopComponent(radarPanel);

        // Left botton: MP Chat
        
        mpChatPanel.setMinimumSize(new java.awt.Dimension(0, 0));
        vspLeft.setBottomComponent(mpChatPanel);

        
        // Right MAIN split pane
        
        vspRight.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        vspRight.setResizeWeight(1.0);
        vspRight.setMinimumSize(new java.awt.Dimension(400,0));
        vspRight.setPreferredSize(new java.awt.Dimension(400,0));
        hspMain.setRightComponent(vspRight);

        // Right top: Radios, Details of wind and runways
        
        pnlRightTop.setLayout(new java.awt.GridBagLayout());
        pnlRightTop.setBackground(Palette.DESKTOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        pnlRightTop.add(radioRunwayPanel, gridBagConstraints);

//        detailPanel.setMinimumSize(new java.awt.Dimension(0, 40));
//        detailPanel.setPreferredSize(new java.awt.Dimension(0, 40));
//        gridBagConstraints = new java.awt.GridBagConstraints();
//        gridBagConstraints.gridx = 0;
//        gridBagConstraints.gridy = 1;
//        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
//        gridBagConstraints.weightx = 1.0;
//        gridBagConstraints.weighty = 1.0;
//        pnlRightTop.add(detailPanel, gridBagConstraints);

        vspRight.setTopComponent(pnlRightTop);
        
        // Right bottom
        
        vspRight.setBottomComponent(contactsPanel);
    }
    
    public void setDividerPosition() {
        Dimension windowSize = getSize();
        hspMain.setDividerLocation((int)Math.round(windowSize.getWidth()*0.8));
        Dimension dim = pnlRightTop.getPreferredSize();
        vspRight.setDividerLocation((int)Math.round(dim.getHeight()));
        vspLeft.setDividerLocation((int)Math.round(windowSize.getHeight()*0.8));
    }
}