package de.knewcleus.openradar.gui.contacts;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.Timer;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.knewcleus.fgfs.multiplayer.IPlayerListener;
import de.knewcleus.openradar.gui.GuiMasterController;
import de.knewcleus.openradar.gui.chat.auto.AtcMessageDialog;
import de.knewcleus.openradar.gui.chat.auto.TextManager;
import de.knewcleus.openradar.gui.contacts.GuiRadarContact.Alignment;
import de.knewcleus.openradar.gui.contacts.GuiRadarContact.Operation;
import de.knewcleus.openradar.gui.contacts.GuiRadarContact.State;
import de.knewcleus.openradar.gui.radar.GuiRadarBackend;
import de.knewcleus.openradar.radardata.fgmp.TargetStatus;

/**
 * This class manages the radar contacts to be displayed in the front end.
 * Problem here is thread safety: The radar back end updates the contacts
 * frequently and we need to react on users modifications.
 * 
 * @author Wolfram Wagner
 */
public class RadarContactController implements ListModel<GuiRadarContact>, ListSelectionListener, IPlayerListener<TargetStatus> {

    private GuiMasterController master = null;

    private GuiRadarBackend radarBackend = null;
    private TextManager textManager = new TextManager();
    private AtcMessageDialog atcMessageDialog;
    private ContactSettingsDialog contactSettingsDialog;
    
    // private volatile GuiRadarContact.Operation filterOperation = null;

    private volatile GuiRadarContact selectedContact = null;
    private final Object selectedContactLock = new Object();

    private volatile List<GuiRadarContact> activeContactList = new ArrayList<GuiRadarContact>();
    private final List<GuiRadarContact> completeContactList = Collections.synchronizedList(new ArrayList<GuiRadarContact>());

    private final Map<String, GuiRadarContact> mapCallSignContact = Collections.synchronizedMap(new TreeMap<String, GuiRadarContact>());
    private final Map<String, GuiRadarContact> mapExpiredCallSigns = Collections.synchronizedMap(new TreeMap<String, GuiRadarContact>());

    private volatile List<GuiRadarContact> modelList = new ArrayList<GuiRadarContact>();

    private List<ListDataListener> dataListeners = new ArrayList<ListDataListener>();

    private boolean updaterisRunning = true;
    private GuiUpdater guiUpdater = new GuiUpdater();
    private JList<GuiRadarContact> guiList = null;
    private ContactMouseListener contactMouseListener = new ContactMouseListener();
    private ContactFilterMouseListener contactFilterMouseListener = new ContactFilterMouseListener();
    private DetailsFocusListener detailsFocusListener = new DetailsFocusListener();

    private enum OrderMode {
        AUTO, MANUAL
    };

    private enum ClickLocation {
        LEFT, ON_STRIP, RIGHT
    };

    private OrderMode orderMode = OrderMode.AUTO;
    private ArrayList<GuiRadarContact> orderList = new ArrayList<GuiRadarContact>(20);

    private Map<String, String> mapAtcComments = new TreeMap<String, String>();

    public RadarContactController(GuiMasterController master, GuiRadarBackend radarBackend) {
        this.master = master;
        this.radarBackend = radarBackend;
        guiUpdater.setDaemon(true);
        guiUpdater.start();
        loadAtcNotes();
        atcMessageDialog = new AtcMessageDialog(master, textManager);
        contactSettingsDialog = new ContactSettingsDialog(master, this);
    }

    public GuiMasterController getMaster() {
        return master;
    }

    public void setFilterOperation(Operation filterOperation) {
        // this.filterOperation = filterOperation;
        applyFilter();
    }

    private synchronized void applyFilter() {
        if (true) { // filterOperation == null) {
            activeContactList = completeContactList;
        }
        // else {
        // activeContactList = Collections.synchronizedList(new
        // ArrayList<GuiRadarContact>());
        //
        // for (GuiRadarContact c : completeContactList) {
        // if (filterOperation.equals(c.getOperation())) {
        // activeContactList.add(c);
        // }
        // }
        // }
    }

    public synchronized GuiRadarContact getContactFor(String callSign) {
        GuiRadarContact rc = mapCallSignContact.get(callSign);
        if (rc == null)
            rc = mapExpiredCallSigns.get(callSign);
        if (rc == null) {
            // System.out.println("Callsign " + callSign + " not found!");
        }
        return rc;
    }

    public boolean isCallSignInRange(String callSign) {
        return callSign.equals(master.getCurrentATCCallSign()) || radarBackend.isContactInRange(getContactFor(callSign));
    }

    public boolean isCallSignVisible(String callSign) {
        return getContactFor(callSign)!=null &&  getContactFor(callSign).isActive() && radarBackend.isContactVisible(getContactFor(callSign));
    }

    private class GuiUpdater extends Thread {

        public void run() {
            setName("OpenRadar - Gui Updater");

            while (updaterisRunning == true) {
                publishData();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void publishData() {
        boolean resetSelectedContact = false;
        synchronized (selectedContactLock) {
            if (null != selectedContact && !selectedContact.isActive()) {
                selectedContact = null;
                resetSelectedContact = true; // to avoid lock because of incapsulated synchronizes
            }
        }
        synchronized(this) {
            if (orderMode == OrderMode.AUTO)
                orderContacts();
            int formerSize = modelList.size();
            modelList = new ArrayList<GuiRadarContact>(activeContactList);
            notifyListenersListChange(formerSize);
    
            checkExpired();
        }
        if (resetSelectedContact) {
            master.getMpChatManager().setSelectedCallSign(null, false); // lock
        }
    }

    private void checkExpired() {
        for (GuiRadarContact ec : new ArrayList<GuiRadarContact>(mapExpiredCallSigns.values())) {
            if (ec.isActive()) {
                // revive them
                mapExpiredCallSigns.remove(ec.getCallSign());

                completeContactList.add(ec);
                mapCallSignContact.put(ec.getCallSign(), ec);
            }
        }
        for (GuiRadarContact c : new ArrayList<GuiRadarContact>(completeContactList)) {
            if (c.isExpired()) {
                // hide them
                completeContactList.remove(c);
                mapCallSignContact.remove(c.getCallSign());

                mapExpiredCallSigns.put(c.getCallSign(), c);
            }
        }
    }

    private void notifyListenersListChange(int formerListSize) {

        for (ListDataListener dl : dataListeners) {
            dl.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, formerListSize));
            if (!activeContactList.isEmpty()) {
                dl.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 0, modelList.size()));
            }
        }
    }

    // List model

    @Override
    public synchronized int getSize() {
        return modelList.size();
    }

    @Override
    public synchronized GuiRadarContact getElementAt(int index) {
        return modelList.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        dataListeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        dataListeners.remove(l);
    }

    public boolean isSelected(GuiRadarContact guiRadarContact) {
        synchronized (selectedContactLock) {
            return guiRadarContact == selectedContact;
        }
    }

    public GuiRadarContact getSelectedContact() {
        synchronized (selectedContactLock) {
            return selectedContact;
        }
    }

    public void select(GuiRadarContact guiRadarContact, boolean force, boolean exlcusive) {
        synchronized (selectedContactLock) {
            if (selectedContact != null) {
                selectedContact.setAtcComment(master.getDetails());
            }
            if (selectedContact == guiRadarContact && !force) {
                // deselect
                selectedContact = null;
                master.getMpChatManager().setSelectedCallSign(null, false);
            } else {
                selectedContact = guiRadarContact;
                master.getMpChatManager().setSelectedCallSign(guiRadarContact.getCallSign(), exlcusive);
                master.getStatusManager().setSelectedCallSign(guiRadarContact.getCallSign());
                master.setDetails(guiRadarContact.getAtcComment());
            }
        }
        master.getMpChatManager().requestGuiUpdate();
    }

    // ListSelectionListener

    @Override
    public synchronized void valueChanged(ListSelectionEvent e) {
        int selectedIndex = e.getFirstIndex();
        select(activeContactList.get(selectedIndex), true, false);
    }

    // Player registry listener

    @Override
    public synchronized void playerAdded(TargetStatus player) {
        if (!mapCallSignContact.containsKey(player.getCallsign()) && !mapExpiredCallSigns.containsKey(player.getCallsign())) {
            // add new player
            GuiRadarContact c = new GuiRadarContact(master, this, player, mapAtcComments.get(player.getCallsign()));
            // c.setOperation(GuiRadarContact.Operation.UNKNOWN);

            completeContactList.add(c);
            mapCallSignContact.put(c.getCallSign(), c);

        } else if (mapExpiredCallSigns.containsKey(player.getCallsign())) {
            // re-activate expired player
            GuiRadarContact c = mapExpiredCallSigns.remove(player.getCallsign());
            c.setTargetStatus(player); // link to new player
            completeContactList.add(c);
            mapCallSignContact.put(c.getCallSign(), c);
        }

        applyFilter();
    }

    @Override
    public synchronized void playerRemoved(TargetStatus player) {
        String callSign = player.getCallsign();
        if (mapCallSignContact.containsKey(player.getCallsign())) {
            GuiRadarContact c = mapCallSignContact.remove(callSign);
            completeContactList.remove(c);
            activeContactList.remove(c);
            mapCallSignContact.remove(c.getCallSign());
            mapExpiredCallSigns.remove(c.getCallSign());
        }
    }

    @Override
    public synchronized void playerListEmptied(TargetStatus player) {
        mapCallSignContact.clear();
        activeContactList.clear();
        completeContactList.clear();
    }

    public synchronized void setJList(JList<GuiRadarContact> liRadarContacts) {
        this.guiList = liRadarContacts;
    }

    public synchronized void dragAndDrop(int selectedIndex, int insertAtIndex, Alignment alignment) {

        GuiRadarContact c = activeContactList.get(selectedIndex);
        c.setAlignment(alignment);

        if (selectedIndex == insertAtIndex)
            return;

        activeContactList.remove(selectedIndex);

        if (insertAtIndex < activeContactList.size()) {
            activeContactList.add(insertAtIndex, c);
        } else {
            activeContactList.add(c);
        }
        publishData();
    }

    // mouse listener

    public ContactMouseListener getContactMouseListener() {
        return contactMouseListener;
    }

    private class ContactMouseListener extends MouseAdapter {

        private ClickLocation clickLocation = ClickLocation.ON_STRIP;
        private volatile GuiRadarContact activeDoubleClickContact = null;
        private javax.swing.Timer doubleClickTimer = null;

        @Override
        public void mouseClicked(MouseEvent e) {
            synchronized (RadarContactController.this) {
                int index = guiList.locationToIndex(e.getPoint());

                if (index > -1) {
                    GuiRadarContact c = activeContactList.get(index);
                    analyseClickPoint(c, guiList, e);

                    if (clickLocation == ClickLocation.ON_STRIP) {
                        if (e.isControlDown() && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                            // toggle neglect
                            c.setNeglect(!c.isNeglect()); 
                        } else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                            // select
                            select(c, true, false);
                            if(c.isSelected()) {
                                master.getMpChatManager().requestFocusForInput();
                            }
                        } else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                            // select and move map to it
                            select(c, true, false);
                            master.getRadarBackend().showRadarContact(c);
                            atcMessageDialog.setVisible(false);
                            // } else if (e.getButton() == MouseEvent.BUTTON1 &&
                            // e.getClickCount() == 2) {
                            // select(c, true, false); // normal select
                            // master.getMpChatManager().requestFocusForInput();
                            //
                        } else if (e.getButton() == MouseEvent.BUTTON2 && e.getClickCount() == 1) {
                            // show contact settings dialog
                            selectNShowContactDialog(c, e);
                        } else if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 1) {
                         // show ATC message dialog
                            selectNShowAtcMsgDialog(c, e);
                        }
                        publishData();
                    } else {
                        handleSideClick(c, c.getAlignment(), e.getClickCount());
                    }
                }
            }
        }

        private void handleSideClick(GuiRadarContact c, Alignment alignment, int clickCount) {
            if(clickCount==1) {
                activeDoubleClickContact = c;
                Integer timerinterval = (Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
                doubleClickTimer = new Timer(timerinterval.intValue(), new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        if(activeDoubleClickContact!=null) {
                            executeClick(activeDoubleClickContact, activeDoubleClickContact.getAlignment(), 1);
                        }
                        
                    }
                });
                doubleClickTimer.setRepeats(false);
                doubleClickTimer.start();                
            } else {
                activeDoubleClickContact=null;
                executeClick(c, alignment, clickCount);
            }
            
        }

        private void executeClick(GuiRadarContact c, Alignment alignment, int clickCount) {

            switch (clickLocation) {
            case LEFT:
                if (alignment == Alignment.CENTER || (alignment == Alignment.RIGHT && clickCount == 2)) {
                    c.setAlignment(Alignment.LEFT);
                } else if (alignment == Alignment.RIGHT && clickCount == 1) {
                    c.setAlignment(Alignment.CENTER);
                }
                break;
            case RIGHT:
                if (alignment == Alignment.CENTER || alignment == Alignment.LEFT && clickCount == 2) {
                    c.setAlignment(Alignment.RIGHT);
                } else if (alignment == Alignment.LEFT && clickCount == 1) {
                    c.setAlignment(Alignment.CENTER);
                }
                break;
            default:
                break;
            }
            publishData();
        }

        private void analyseClickPoint(GuiRadarContact c, JList<?> list, MouseEvent e) {
            double totalSize = list.getVisibleRect().getWidth();
            double stripWidth = RadarContactListCellRenderer.STRIP_WITDH;

            double sideGap = c.getAlignment() == Alignment.CENTER ? (totalSize - stripWidth) / 2 : (totalSize - stripWidth);
            double clickX = e.getPoint().getX();

            switch (c.getAlignment()) {
            case LEFT:
                if (clickX > stripWidth) {
                    clickLocation = ClickLocation.RIGHT;
                } else {
                    clickLocation = ClickLocation.ON_STRIP;
                }
                break;
            case CENTER:
                if (clickX < sideGap) {
                    clickLocation = ClickLocation.LEFT;
                } else if (clickX > sideGap + stripWidth) {
                    clickLocation = ClickLocation.RIGHT;
                } else {
                    clickLocation = ClickLocation.ON_STRIP;
                }
                break;
            case RIGHT:
                if (clickX < sideGap) {
                    clickLocation = ClickLocation.LEFT;
                } else {
                    clickLocation = ClickLocation.ON_STRIP;
                }
                break;
            }
        }

        @Override
        public synchronized void mouseDragged(MouseEvent e) {
            Rectangle listBounds = guiList.getBounds();
            Rectangle rect = guiList.getVisibleRect();
            Point p = e.getPoint();
            if (p.getY() < rect.getY()) {
                if (rect.getY() > 10) {
                    rect.setRect(rect.getX(), rect.getY() - 10, rect.getWidth(), rect.getHeight());
                    guiList.scrollRectToVisible(rect);
                }
            }
            if (p.getY() > listBounds.getHeight()) {
                Dimension rectReal = guiList.getPreferredSize();
                if (rect.getY() + rect.getHeight() > rectReal.getHeight() - 10) {
                    rect.setRect(rect.getX(), rect.getY() + 10, rect.getWidth(), rect.getHeight());
                    guiList.scrollRectToVisible(rect);
                }
            }
        }
    }

    public synchronized void selectNShowAtcMsgDialog(GuiRadarContact c, MouseEvent e) {
        select(c, true, false); // normal select
        master.getMpChatManager().requestFocusForInput();
        if(c.isActive()) atcMessageDialog.setLocation(c,e);
    }
    
    public synchronized void selectNShowContactDialog(GuiRadarContact c, MouseEvent e) {
        // show details dialog
        contactSettingsDialog.show(c, e);
    }
    
    public ContactFilterMouseListener getContactFilterMouseListener() {
        return contactFilterMouseListener;
    }

    public class ContactFilterMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            JLabel lSource = (JLabel) e.getSource();
            if (lSource.getName().equals("MODE")) {
                if (lSource.getText().equals("AUTO")) {
                    orderMode = OrderMode.MANUAL;
                    lSource.setText("MANUAL");
                } else {
                    orderMode = OrderMode.AUTO;
                    lSource.setText("AUTO");
                    orderContacts();
                }
            }
            // if (lSource.getName().equals("ALL")) {
            // setFilterOperation(null);
            // } else if (lSource.getName().equals("GROUND")) {
            // setFilterOperation(Operation.GROUND);
            // } else if (lSource.getName().equals("LANDING")) {
            // setFilterOperation(Operation.LANDING);
            // } else if (lSource.getName().equals("STARTING")) {
            // setFilterOperation(Operation.STARTING);
            // } else if (lSource.getName().equals("TRAVEL")) {
            // setFilterOperation(Operation.TRAVEL);
            // } else if (lSource.getName().equals("EMERGENCY")) {
            // setFilterOperation(Operation.EMERGENCY);
            // } else if (lSource.getName().equals("UNKNOWN")) {
            // setFilterOperation(Operation.UNKNOWN);
            // }
            ContactsPanel parent = (ContactsPanel) lSource.getParent();
            parent.resetFilters();
            parent.selectFilter(lSource);
        }
    }

    private synchronized void orderContacts() {
        for (GuiRadarContact c : activeContactList) {
            if (c.getState() == State.IMPORTANT) {
                orderList.add(c);
            }
        }
        for (GuiRadarContact c : activeContactList) {
            if (c.getState() == State.CONTROLLED) {
                orderList.add(c);
            }
        }
        for (GuiRadarContact c : activeContactList) {
            if (c.getState() == State.UNCONTROLLED) {
                orderList.add(c);
            }
        }
        activeContactList = new ArrayList<GuiRadarContact>(orderList);
        orderList.clear();
    }

    public DetailsFocusListener getDetailsFocusListener() {
        return detailsFocusListener;
    }

    public class DetailsFocusListener extends FocusAdapter {
        @Override
        public void focusLost(FocusEvent e) {
            setAtcComment(master.getDetails());
        }
    }

    public void setAtcComment(String comment) {
        synchronized (selectedContactLock) {

            if (selectedContact != null) {
                selectedContact.setAtcComment(comment);
            }

        }
    }

    public synchronized void loadAtcNotes() {
        File atcCommentFile = new File("settings" + File.separator + "atcComments.xml");
        if (atcCommentFile.exists()) {
            FileInputStream fis = null;
            ;
            try {
                fis = new FileInputStream(atcCommentFile);
                Properties props = new Properties();
                props.loadFromXML(fis);
                for (Object op : props.keySet()) {
                    String key = (String) op;
                    String value = props.getProperty(key);
                    mapAtcComments.put(key, value);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    /**
     * Save the ATC comments of all active and expired contacts to a file, to be
     * able to load them at restart
     */
    public synchronized void saveAtcNotes() {
        for (GuiRadarContact c : mapCallSignContact.values()) {
            if (c.getAtcComment() != null && !c.getAtcComment().isEmpty()) {
                mapAtcComments.put(c.getCallSign(), c.getAtcComment());
            }
        }
        for (GuiRadarContact c : mapExpiredCallSigns.values()) {
            if (c.getAtcComment() != null && !c.getAtcComment().isEmpty()) {
                mapAtcComments.put(c.getCallSign(), c.getAtcComment());
            }
        }

        File atcCommentFile = new File("settings" + File.separator + "atcComments.xml");
        Properties p = new Properties();
        for (String key : mapAtcComments.keySet()) {
            if (mapAtcComments.get(key) != null) {
                p.put(key, mapAtcComments.get(key));
            }
        }
        FileOutputStream fos = null;
        try {
            if (atcCommentFile.exists()) {
                File backup = new File("settings" + File.separator + "atcComments.bak");
                if (backup.exists())
                    backup.delete();
                atcCommentFile.renameTo(backup);
            }
            fos = new FileOutputStream(atcCommentFile);
            p.storeToXML(fos, "OpenRadar ATC notes", "UTF-8");
            // System.out.println("atcnotes saved.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }

    }

    public void hideDialogs() {
        atcMessageDialog.setVisible(false);
        contactSettingsDialog.closeDialog();
    }

    public String[] getAutoAtcLanguages() {
        List<String> result = textManager.getLanguages();
        if(result==null)  result = new ArrayList<String>();
        // todo change codes to texts
        return result.toArray(new String[result.size()]);
    }

    public String getAutoAtcLanguages(int index) {
        return textManager.getLanguages().get(index);
    }
}
