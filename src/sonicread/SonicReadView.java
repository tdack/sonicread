/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * This code is based on the work of Tomás Oliveira e Silva
 * http://www.ieeta.pt/~tos/software/polar_s410.html
 * 
 * Remco den Breeje, <stacium@gmail.com>
 */

package sonicread;

import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import org.jdesktop.application.Task;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;


/**
 * The application's main frame.
 */
public class SonicReadView extends FrameView {
    
    private CreateHsr hsr = null;
    private Task listenTask = null;

    private enum fileDialogType { SAVEHSR, OPENWAV }
    private SROptions options;

    public SonicReadView(SingleFrameApplication app, SROptions opt) {
        super(app);
        options = opt;
        initComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        stopIcon = resourceMap.getIcon("StatusBar.stopIcon");
        statusAnimationLabel.setIcon(idleIcon);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                //System.out.format("Task Property name: %s\n", propertyName);
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    startListenButton.setEnabled(true);
                    stopListenButton.setEnabled(false);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    clippedLabel.setText("");
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    statusMessageLabel.setIcon(null);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    //evt.getNewValue()
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
        
        // disable stop icon
        stopListenButton.setEnabled(false);
        saveAsButton.setEnabled(false);
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = SonicReadApp.getApplication().getMainFrame();
            aboutBox = new SonicReadAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        SonicReadApp.getApplication().show(aboutBox);
    }
    
    @Action
    public Task startListen() {
        startListenButton.setEnabled(false);
        stopListenButton.setEnabled(true);
        saveAsButton.setEnabled(false);
        listenTask = new ListenTask(null);
        return listenTask;
    }
    
    @Action
    public Task startImport() {
        startListenButton.setEnabled(false);
        stopListenButton.setEnabled(false);
        saveAsButton.setEnabled(false);
        File file = selectFile(fileDialogType.OPENWAV);
        if(file != null) {
            listenTask = new ListenTask(file);
        } else {
            listenTask = null;
        }
        return listenTask;
    }
    
    @Action
    public void stopListen() {
        startListenButton.setEnabled(true);
        stopListenButton.setEnabled(false);
        listenTask.cancel(true);
    }
    
    @Action
    public Task saveAs() {
        Task task = null;
        File file = selectFile(fileDialogType.SAVEHSR);
        if (file != null) {
            task = new SaveFileTask(file);
        }
        return task;
    }
    
    public void SetHsr(CreateHsr hsr) {
        this.hsr = hsr;
    }
    
    private class ListenTask extends SonicReadApp.SonicListenTask {
        ListenTask(File file) {
            super(SonicReadView.this.getApplication(), file);
        }      
      
        @Override protected void succeeded(CreateHsr rval) {

            if(rval != null) {
                SetHsr(rval);
                statusMessageLabel.setText("Done");
                saveAsButton.setEnabled(true);
            }
            else { // only import returns succeeded w/o hsr
                statusMessageLabel.setText("Import failed");
                saveAsButton.setEnabled(false);
            }
        }

        @Override protected void failed(Throwable e) {
            statusMessageLabel.setIcon(stopIcon);
            statusMessageLabel.setText(e.getMessage());
        }   
    }
    
    
    private class SaveFileTask extends SonicReadApp.SonicSaveFileTask {
        SaveFileTask(File file) {
            super(SonicReadView.this.getApplication(), file, hsr);
        }
        
        @Override protected void succeeded(Void foobar) {
            statusMessageLabel.setText(String.format("Done. Saved data to '%s'", 
                                                    this.getFile().getName()));
        }

        @Override protected void failed(Throwable e) {
            e.printStackTrace();
            statusMessageLabel.setIcon(stopIcon);
            statusMessageLabel.setText(e.getMessage());
        }   
    }
    
    public void setDbLevel(int val)
    {
        DbLevelBar.setValue(val);
    }
    
    public void setClipped(boolean val)
    {
        if(val) {
            clippedLabel.setText("Signal clipped!");
        } 
    }

    private File selectFile(fileDialogType type) {

        JFileChooser fc = new JFileChooser();
        String name, fDescription, fExtensions;
        String initialDir;

        switch (type) {
            case SAVEHSR:
                name = "saveAsFileChooser";
                fDescription = "Hsr files";
                fExtensions = "hsr";
                initialDir = options.getPreviousExerciseDirectory();
                break;
            default: //OPENWAV
                name = "loadWavFileChooser";
                fDescription = "Wav files";
                fExtensions = "wav";
                initialDir = options.getPreviousImportDirectory();
        }

        if(initialDir != null) {
            fc.setCurrentDirectory(new File(initialDir));
        }
        fc.setDialogTitle(getResourceMap().getString(name + ".dialogTitle"));
        fc.setFileFilter(new FileNameExtensionFilter(fDescription, fExtensions));

        int option = fc.showOpenDialog(getFrame());
        if (option == JFileChooser.APPROVE_OPTION) {
            // Store folder
            String previousDirectory = fc.getSelectedFile().getParentFile().getAbsolutePath();
            switch (type) {
                case SAVEHSR:
                    options.setPreviousExerciseDirectory(previousDirectory);
                    break;
                default: // LOADWAV
                    options.setPreviousImportDirectory(previousDirectory);
            }

            return fc.getSelectedFile();
        }
        return null;
    }
   
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        startListenButton = new javax.swing.JButton();
        stopListenButton = new javax.swing.JButton();
        saveAsButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        DbLevelBar = new javax.swing.JProgressBar();
        progressBar = new javax.swing.JProgressBar();
        jLabel2 = new javax.swing.JLabel();
        clippedLabel = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();

        mainPanel.setName("mainPanel"); // NOI18N

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);
        jToolBar1.setName("jToolBar1"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(sonicread.SonicReadApp.class).getContext().getActionMap(SonicReadView.class, this);
        startListenButton.setAction(actionMap.get("startListen")); // NOI18N
        startListenButton.setFocusable(false);
        startListenButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        startListenButton.setName("startListenButton"); // NOI18N
        startListenButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(startListenButton);

        stopListenButton.setAction(actionMap.get("stopListen")); // NOI18N
        stopListenButton.setFocusable(false);
        stopListenButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        stopListenButton.setName("stopListenButton"); // NOI18N
        stopListenButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(stopListenButton);

        saveAsButton.setAction(actionMap.get("saveAs")); // NOI18N
        saveAsButton.setFocusable(false);
        saveAsButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveAsButton.setName("saveAsButton"); // NOI18N
        saveAsButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(saveAsButton);

        jSeparator1.setName("jSeparator1"); // NOI18N
        jToolBar1.add(jSeparator1);

        jButton1.setAction(actionMap.get("startImport")); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setName("jButton1"); // NOI18N
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton1);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(sonicread.SonicReadApp.class).getContext().getResourceMap(SonicReadView.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        DbLevelBar.setName("DbLevelBar"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        clippedLabel.setText(resourceMap.getString("clippedLabel.text")); // NOI18N
        clippedLabel.setName("clippedLabel"); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(progressBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(DbLevelBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(2, 2, 2)
                .addComponent(clippedLabel)
                .addContainerGap(142, Short.MAX_VALUE))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1)
                    .addComponent(DbLevelBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clippedLabel))
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addComponent(jLabel2)))
                .addContainerGap(23, Short.MAX_VALUE))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 480, Short.MAX_VALUE)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JProgressBar DbLevelBar;
    private javax.swing.JLabel clippedLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton saveAsButton;
    private javax.swing.JButton startListenButton;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JButton stopListenButton;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon stopIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
}
