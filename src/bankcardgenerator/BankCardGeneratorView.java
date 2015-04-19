/*
 * BankCardGeneratorView.java
 */
package bankcardgenerator;

import bankcardgenerator.model.KVModel;
import java.awt.Color;
import javax.swing.event.DocumentEvent;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import util.DataBaseHelper;
import util.Tools;

/**
 * The application's main frame.
 */
public class BankCardGeneratorView extends FrameView {

    class BirthdayDocumentLinstner implements DocumentListener {
        
        private JTextField field;

        private Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

        public BirthdayDocumentLinstner(JTextField field){
            this.field = field;
        }

        public String getText(){
            if(this.field == null){
                return null;
            }
            return this.field.getText();
        }

        public boolean checkTextInvalid(){
            String text = this.getText();
            if(text == null){
                return false;
            }
            Matcher m = pattern.matcher(text);

            if(m.matches()){
                this.field.setBackground(new Color(255, 255, 255));
                return true;
            }
            else{
                this.field.setBackground(new Color(255, 215, 215));
                return false;
            }
        }

        public void insertUpdate(DocumentEvent e) {
            checkTextInvalid();
        }

        public void removeUpdate(DocumentEvent e) {
            checkTextInvalid();
        }

        public void changedUpdate(DocumentEvent e) {
            checkTextInvalid();
        }
    }

    public BankCardGeneratorView(SingleFrameApplication app) {
        super(app);

        initComponents();

        this.styleGroup.add(this.jRadioButton1);
        this.styleGroup.add(this.jRadioButton2);
        this.jRadioButton1.setSelected(true);
        this.styleSelect();
        this.suffix.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                checkSuffixFormat(suffix.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                checkSuffixFormat(suffix.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                checkSuffixFormat(suffix.getText());
            }
        });

        this.bankcard.setEditable(false);

        //init city and birthday box
        initProvinceComboBox();

        //init check birthday
        this.fromBirthday.setText("1960-01-01");
        this.toBirthday.setText("1996-01-01");
        this.fromBirthday.getDocument().addDocumentListener(new BirthdayDocumentLinstner(fromBirthday));
        this.toBirthday.getDocument().addDocumentListener(new BirthdayDocumentLinstner(toBirthday));

        //init gender radio button
        this.genderGroup.add(this.gender);
        this.genderGroup.add(this.genderMan);
        this.genderGroup.add(this.genderWom);
        this.gender.setSelected(true);

        //init id card
        this.identifyId.setEditable(false);

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
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
    }

    public boolean checkSuffixFormat(String s) {
        if (s == null) {
            s = "";
        }

        if (!Tools.isAllNumber(s)) {
            this.suffix.setBackground(new Color(255, 215, 215));
            this.suffixLength.setText("0");
            return false;
        } else {
            this.suffix.setBackground(new Color(255, 255, 255));
            this.suffixLength.setText(String.valueOf(s.length()));
            return true;
        }
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = BankCardGeneratorApp.getApplication().getMainFrame();
            aboutBox = new BankCardGeneratorAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        BankCardGeneratorApp.getApplication().show(aboutBox);
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
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jRadioButton1 = new javax.swing.JRadioButton();
        lenthNum = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jRadioButton2 = new javax.swing.JRadioButton();
        suffix = new javax.swing.JFormattedTextField();
        suffixLength = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        bankcard = new javax.swing.JTextField();
        message = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        bankCardNum = new javax.swing.JSpinner();
        jButton2 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        provinceComboBox = new javax.swing.JComboBox();
        cityComboBox = new javax.swing.JComboBox();
        areaComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        isWithX = new javax.swing.JCheckBox();
        genderMan = new javax.swing.JRadioButton();
        genderWom = new javax.swing.JRadioButton();
        gender = new javax.swing.JRadioButton();
        jButton3 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        idNumbers = new javax.swing.JSpinner();
        jButton4 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        fromBirthday = new javax.swing.JTextField();
        toBirthday = new javax.swing.JTextField();
        identifyId = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jButton5 = new javax.swing.JButton();
        busCode = new javax.swing.JTextField();
        orgCode = new javax.swing.JTextField();
        jButton6 = new javax.swing.JButton();
        orgCodeNum = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        plainText = new javax.swing.JTextField();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        digestValue = new javax.swing.JTextField();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        styleGroup = new javax.swing.ButtonGroup();
        genderGroup = new javax.swing.ButtonGroup();
        dateChooserCombo1 = new datechooser.beans.DateChooserCombo();
        dateChooserDialog1 = new datechooser.beans.DateChooserDialog();

        mainPanel.setName("mainPanel"); // NOI18N

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(bankcardgenerator.BankCardGeneratorApp.class).getContext().getResourceMap(BankCardGeneratorView.class);
        jRadioButton1.setText(resourceMap.getString("jRadioButton1.text")); // NOI18N
        jRadioButton1.setName("jRadioButton1"); // NOI18N
        jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });

        lenthNum.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "19", "16", "18", "15", "17" }));
        lenthNum.setName("lenthNum"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jRadioButton2.setText(resourceMap.getString("jRadioButton2.text")); // NOI18N
        jRadioButton2.setName("jRadioButton2"); // NOI18N
        jRadioButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton2ActionPerformed(evt);
            }
        });

        suffix.setText(resourceMap.getString("suffix.text")); // NOI18N
        suffix.setName("suffix"); // NOI18N

        suffixLength.setText(resourceMap.getString("suffixLength.text")); // NOI18N
        suffixLength.setName("suffixLength"); // NOI18N

        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        bankcard.setText(resourceMap.getString("bankcard.text")); // NOI18N
        bankcard.setName("bankcard"); // NOI18N
        bankcard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                bankcardMouseClicked(evt);
            }
        });

        message.setText(resourceMap.getString("message.text")); // NOI18N
        message.setName("message"); // NOI18N

        jSeparator1.setName("jSeparator1"); // NOI18N

        bankCardNum.setName("bankCardNum"); // NOI18N
        bankCardNum.setValue(1000);

        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jRadioButton1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lenthNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jRadioButton2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(suffix, javax.swing.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(suffixLength, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(80, 80, 80)
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(message, javax.swing.GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(bankcard, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(bankCardNum, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton2)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton1)
                    .addComponent(lenthNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton2)
                    .addComponent(suffixLength)
                    .addComponent(suffix, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(message, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bankcard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bankCardNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2))
                .addContainerGap(65, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N

        provinceComboBox.setName("provinceComboBox"); // NOI18N
        provinceComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                provinceComboBoxActionPerformed(evt);
            }
        });

        cityComboBox.setName("cityComboBox"); // NOI18N
        cityComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cityComboBoxActionPerformed(evt);
            }
        });

        areaComboBox.setName("areaComboBox"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        isWithX.setText(resourceMap.getString("isWithX.text")); // NOI18N
        isWithX.setName("isWithX"); // NOI18N

        genderMan.setText(resourceMap.getString("genderMan.text")); // NOI18N
        genderMan.setActionCommand(resourceMap.getString("genderMan.actionCommand")); // NOI18N
        genderMan.setName("genderMan"); // NOI18N

        genderWom.setText(resourceMap.getString("genderWom.text")); // NOI18N
        genderWom.setActionCommand(resourceMap.getString("genderWom.actionCommand")); // NOI18N
        genderWom.setName("genderWom"); // NOI18N

        gender.setText(resourceMap.getString("gender.text")); // NOI18N
        gender.setActionCommand(resourceMap.getString("gender.actionCommand")); // NOI18N
        gender.setName("gender"); // NOI18N

        jButton3.setText(resourceMap.getString("jButton3.text")); // NOI18N
        jButton3.setName("jButton3"); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        jSeparator2.setName("jSeparator2"); // NOI18N

        idNumbers.setModel(new javax.swing.SpinnerNumberModel(1000, 1, 1000, 1));
        idNumbers.setName("idNumbers"); // NOI18N
        idNumbers.setRequestFocusEnabled(false);
        idNumbers.setValue(1000);

        jButton4.setText(resourceMap.getString("jButton4.text")); // NOI18N
        jButton4.setName("jButton4"); // NOI18N
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        fromBirthday.setText(resourceMap.getString("fromBirthday.text")); // NOI18N
        fromBirthday.setName("fromBirthday"); // NOI18N

        toBirthday.setText(resourceMap.getString("toBirthday.text")); // NOI18N
        toBirthday.setName("toBirthday"); // NOI18N

        identifyId.setText(resourceMap.getString("identifyId.text")); // NOI18N
        identifyId.setName("identifyId"); // NOI18N
        identifyId.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                identifyIdMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(identifyId, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addComponent(idNumbers, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE))
                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addComponent(genderMan)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(genderWom)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(gender))
                    .addComponent(isWithX, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 309, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fromBirthday))
                            .addComponent(provinceComboBox, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(cityComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 6, Short.MAX_VALUE)
                                .addComponent(areaComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(toBirthday, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(114, 114, 114))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(provinceComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cityComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(areaComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(fromBirthday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(toBirthday, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(isWithX)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(genderMan)
                    .addComponent(gender)
                    .addComponent(genderWom))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(identifyId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(9, 9, 9)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(idNumbers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton4)
                    .addComponent(jLabel5))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel2.TabConstraints.tabTitle"), jPanel2); // NOI18N

        jPanel3.setName("jPanel3"); // NOI18N

        jButton5.setText(resourceMap.getString("jButton5.text")); // NOI18N
        jButton5.setName("jButton5"); // NOI18N
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        busCode.setEditable(false);
        busCode.setText(resourceMap.getString("busCode.text")); // NOI18N
        busCode.setName("busCode"); // NOI18N
        busCode.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                busCodeMouseClicked(evt);
            }
        });

        orgCode.setEditable(false);
        orgCode.setText(resourceMap.getString("orgCode.text")); // NOI18N
        orgCode.setName("orgCode"); // NOI18N
        orgCode.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                orgCodeMouseClicked(evt);
            }
        });

        jButton6.setText(resourceMap.getString("jButton6.text")); // NOI18N
        jButton6.setName("jButton6"); // NOI18N
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        orgCodeNum.setText(resourceMap.getString("orgCodeNum.text")); // NOI18N
        orgCodeNum.setName("orgCodeNum"); // NOI18N

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(busCode, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(orgCode, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton5, 0, 0, Short.MAX_VALUE)
                            .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(orgCodeNum, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel6))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(busCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(orgCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton6)
                    .addComponent(orgCodeNum))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel6)
                .addContainerGap(141, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel3.TabConstraints.tabTitle"), jPanel3); // NOI18N

        jPanel5.setName("jPanel5"); // NOI18N

        plainText.setText(resourceMap.getString("plainText.text")); // NOI18N
        plainText.setName("plainText"); // NOI18N

        jButton7.setText(resourceMap.getString("jButton7.text")); // NOI18N
        jButton7.setName("jButton7"); // NOI18N
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jButton8.setText(resourceMap.getString("jButton8.text")); // NOI18N
        jButton8.setName("jButton8"); // NOI18N
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jButton9.setText(resourceMap.getString("jButton9.text")); // NOI18N
        jButton9.setName("jButton9"); // NOI18N
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        digestValue.setEditable(false);
        digestValue.setText(resourceMap.getString("digestValue.text")); // NOI18N
        digestValue.setName("digestValue"); // NOI18N
        digestValue.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                digestValueMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(plainText, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton9))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jButton7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton8))
                    .addComponent(digestValue, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(plainText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton7)
                    .addComponent(jButton8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(digestValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(61, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel5.TabConstraints.tabTitle"), jPanel5); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(bankcardgenerator.BankCardGeneratorApp.class).getContext().getActionMap(BankCardGeneratorView.class, this);
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

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 233, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        // TODO add your handling code here:
        styleSelect();
}//GEN-LAST:event_jRadioButton1ActionPerformed

    private void jRadioButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton2ActionPerformed
        // TODO add your handling code here:
        styleSelect();
}//GEN-LAST:event_jRadioButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:

        if (!this.checkSuffixFormat(this.suffix.getText())) {
            JOptionPane.showMessageDialog(mainPanel, "invalid prefix");
            return;
        }

        String prefix = null;

        if (this.jRadioButton1.isSelected()) {
            prefix = Tools.getFixLengthNumString("62", Integer.valueOf(this.lenthNum.getSelectedItem().toString()) - 1);
        } else {
            prefix = this.suffix.getText();
        }

        this.bankcard.setText(Tools.generateBankCardNum(prefix));

    }//GEN-LAST:event_jButton1ActionPerformed

    private void bankcardMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bankcardMouseClicked
        // TODO add your handling code here:
        String value = this.bankcard.getText();
        if (!"".equals(value)) {
            this.bankcard.setSelectionStart(0);
            this.bankcard.setSelectionEnd(value.length());
            Tools.saveToClipBoard(this.bankcard.getText());
        }
    }//GEN-LAST:event_bankcardMouseClicked

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:

        if (!this.jRadioButton1.isSelected()) {
            JOptionPane.showMessageDialog(mainPanel, "please select fix length method");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.showSaveDialog(mainPanel);

        final File file = chooser.getSelectedFile();

        try {
            file.createNewFile();
        } catch (IOException iOException) {
            JOptionPane.showMessageDialog(mainPanel, "cannot create file");
            return;
        }

        if (!file.canWrite()) {
            JOptionPane.showMessageDialog(mainPanel, "cannot write to file");
            return;
        }

        Runnable task = new Runnable() {

            public void run() {
                int num = (Integer) bankCardNum.getValue();

                Set<String> prefixNumSet = new HashSet<String>();

                while (prefixNumSet.size() < num) {
                    String prefix = Tools.getFixLengthNumString("62", Integer.valueOf(lenthNum.getSelectedItem().toString()) - 1);
                    String bankCard = Tools.generateBankCardNum(prefix);
                    prefixNumSet.add(bankCard);
                }

                BufferedWriter writer = null;

                try {
                    writer = new BufferedWriter(new FileWriter(file));
                    for (String bankCard : prefixNumSet) {
                        writer.append(bankCard);
                        writer.append(System.lineSeparator());
                    }

                    writer.flush();
                } catch (IOException ex) {
                    if (mainPanel != null) {
                        JOptionPane.showMessageDialog(mainPanel, "write to file fail");
                        return;
                    }
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException ex) {
                        }
                    }
                }

                if (mainPanel != null) {
                    JOptionPane.showMessageDialog(mainPanel, "生成成功！文件在" + file.getAbsolutePath());
                    return;
                }
            }
        };
        SwingUtilities.invokeLater(task);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void initProvinceComboBox() {
        Runnable task = new Runnable() {

            public void run() {
                DataBaseHelper dh = new DataBaseHelper();
                List<Map<String, String>> result = dh.getDataList("select prov_code, prov_name from t_province_code");
                if (result != null) {
                    KVModel m = null;
                    for (Map<String, String> item : result) {
                        m = new KVModel();
                        m.key = item.get("PROV_CODE");
                        m.value = item.get("PROV_NAME");
                        provinceComboBox.addItem(m);
                    }
                }
            }
        };

        SwingUtilities.invokeLater(task);
    }

    private void provinceComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_provinceComboBoxActionPerformed
        KVModel model = (KVModel) provinceComboBox.getSelectedItem();
        final String key = model.key;
        Runnable task = new Runnable() {

            public void run() {
                DataBaseHelper dh = new DataBaseHelper();
                List<String> bindName = new ArrayList<String>();
                bindName.add(key);
                List<Map<String, String>> result = dh.getDataList("select city_code, city_name from t_city_code where prov_code=?", bindName);
                if (result != null) {
                    KVModel m = null;
                    cityComboBox.removeAllItems();
                    areaComboBox.removeAllItems();
                    for (Map<String, String> item : result) {
                        m = new KVModel();
                        m.key = item.get("CITY_CODE");
                        m.value = item.get("CITY_NAME");
                        cityComboBox.addItem(m);
                    }
                }
            }
        };

        SwingUtilities.invokeLater(task);
    }//GEN-LAST:event_provinceComboBoxActionPerformed

    private void cityComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cityComboBoxActionPerformed
        // TODO add your handling code here:
        KVModel model = (KVModel) cityComboBox.getSelectedItem();
        if (model == null) {
            return;
        }
        final String key = model.key;
        Runnable task = new Runnable() {

            public void run() {
                DataBaseHelper dh = new DataBaseHelper();
                List<String> bindName = new ArrayList<String>();
                bindName.add(key);
                List<Map<String, String>> result = dh.getDataList("select area_code, area_name from t_area_code where area_city_code=?", bindName);
                if (result != null) {
                    KVModel m = null;
                    areaComboBox.removeAllItems();
                    for (Map<String, String> item : result) {
                        m = new KVModel();
                        m.key = item.get("AREA_CODE");
                        m.value = item.get("AREA_NAME");
                        areaComboBox.addItem(m);
                    }
                }
            }
        };

        SwingUtilities.invokeLater(task);
    }//GEN-LAST:event_cityComboBoxActionPerformed

    private void identifyIdMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_identifyIdMouseClicked
        // TODO add your handling code here:
        String value = this.identifyId.getText();
        if (!"".equals(value)) {
            this.identifyId.setSelectionStart(0);
            this.identifyId.setSelectionEnd(value.length());
            Tools.saveToClipBoard(this.identifyId.getText());
        }
    }//GEN-LAST:event_identifyIdMouseClicked

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:

        String genderValue = "-1";

        if(this.genderMan.isSelected()){
            genderValue = "1";
        }

        if (this.genderWom.isSelected()){
            genderValue = "0";
        }

        boolean bX = false;

        if(this.isWithX.isSelected()){
            bX = true;
        }

        String identifyID = generateIDNumber(genderValue, bX);
        this.identifyId.setText(identifyID);
    }//GEN-LAST:event_jButton3ActionPerformed

    private String generateIDNumber(String genderValue, boolean bX){
        String areaCode = ((KVModel)this.areaComboBox.getSelectedItem()).key;
        String begin = this.fromBirthday.getText();
        String end = this.toBirthday.getText();
        String birthday = Tools.getRandomDate(begin, end);

        if(!Tools.matchString(begin, "\\d{4}-\\d{2}-\\d{2}") || !Tools.matchString(end, "\\d{4}-\\d{2}-\\d{2}")){
            JOptionPane.showMessageDialog(jPanel2, "无效的日期");
            return "";
        }

        if(birthday == null){
            JOptionPane.showMessageDialog(jPanel2, "无效的日期");
        }

        String identifyID = Tools.generateID(areaCode, birthday, genderValue, bX);
        return identifyID;
    }

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser();
        chooser.showSaveDialog(mainPanel);

        final File file = chooser.getSelectedFile();

        try {
            file.createNewFile();
        } catch (IOException iOException) {
            JOptionPane.showMessageDialog(mainPanel, "cannot create file");
            return;
        }

        if (!file.canWrite()) {
            JOptionPane.showMessageDialog(mainPanel, "cannot write to file");
            return;
        }

        Runnable task = new Runnable() {

            public void run() {
                int num = (Integer) idNumbers.getValue();
                Set<String> prefixNumSet = new HashSet<String>();

                while (prefixNumSet.size() < num) {
                    String id = generateIDNumber("-1", false);
                    prefixNumSet.add(id);
                }

                BufferedWriter writer = null;

                try {
                    writer = new BufferedWriter(new FileWriter(file));
                    for (String bankCard : prefixNumSet) {
                        writer.append(bankCard);
                        writer.append(System.lineSeparator());
                    }

                    writer.flush();
                } catch (IOException ex) {
                    if (mainPanel != null) {
                        JOptionPane.showMessageDialog(mainPanel, "write to file fail");
                        return;
                    }
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException ex) {
                        }
                    }
                }

                if (mainPanel != null) {
                    JOptionPane.showMessageDialog(mainPanel, "生成成功！文件在" + file.getAbsolutePath());
                    return;
                }
            }
        };
        SwingUtilities.invokeLater(task);
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
        Runnable task = new Runnable(){
            public void run() {
                String value = Tools.getBusCode();
                busCode.setText(value);
            }
        };

        SwingUtilities.invokeLater(task);
    }//GEN-LAST:event_jButton5ActionPerformed

    private void busCodeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_busCodeMouseClicked
        // TODO add your handling code here:
        String value = this.busCode.getText();
        if (!"".equals(value)) {
            this.busCode.setSelectionStart(0);
            this.busCode.setSelectionEnd(value.length());
            Tools.saveToClipBoard(this.busCode.getText());
        }
    }//GEN-LAST:event_busCodeMouseClicked

    private void orgCodeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_orgCodeMouseClicked
        // TODO add your handling code here:
        String value = this.orgCode.getText();
        if (!"".equals(value)) {
            this.orgCode.setSelectionStart(0);
            this.orgCode.setSelectionEnd(value.length());
            Tools.saveToClipBoard(this.orgCode.getText());
        }
    }//GEN-LAST:event_orgCodeMouseClicked

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        // TODO add your handling code here:

        final boolean isNum = this.orgCodeNum.isSelected();

        Runnable task = new Runnable(){
            public void run() {
                String value = Tools.getOrgCode(isNum);
                orgCode.setText(value);
            }
        };

        SwingUtilities.invokeLater(task);
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser();
        chooser.showOpenDialog(mainPanel);
        final File file = chooser.getSelectedFile();
        this.plainText.setText(file.getAbsolutePath());
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        // TODO add your handling code here:
        String plainTextValue = this.plainText.getText();
        File file = new File(plainTextValue);
        if(file.canRead()){
            this.digestValue.setText(Tools.getMD5(file, "MD5"));
        }
        else{
            this.digestValue.setText(Tools.getMD5(plainTextValue, "MD5"));
        }
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        // TODO add your handling code here:
        String plainTextValue = this.plainText.getText();
        File file = new File(plainTextValue);
        if(file.canRead()){
            this.digestValue.setText(Tools.getMD5(file, "SHA1"));
        }
        else{
            this.digestValue.setText(Tools.getMD5(plainTextValue, "SHA1"));
        }
    }//GEN-LAST:event_jButton8ActionPerformed

    private void digestValueMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_digestValueMouseClicked
        // TODO add your handling code here:
        String value = this.digestValue.getText();
        if (!"".equals(value)) {
            this.digestValue.setSelectionStart(0);
            this.digestValue.setSelectionEnd(value.length());
            Tools.saveToClipBoard(this.digestValue.getText());
        }
    }//GEN-LAST:event_digestValueMouseClicked

    private void styleSelect() {
        if (this.jRadioButton1.isSelected()) {
            this.lenthNum.setEnabled(true);
            this.suffix.setEditable(false);
        } else {
            this.lenthNum.setEnabled(false);
            this.suffix.setEditable(true);
            this.suffix.setText("");
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox areaComboBox;
    private javax.swing.JSpinner bankCardNum;
    private javax.swing.JTextField bankcard;
    private javax.swing.JTextField busCode;
    private javax.swing.JComboBox cityComboBox;
    private datechooser.beans.DateChooserCombo dateChooserCombo1;
    private datechooser.beans.DateChooserDialog dateChooserDialog1;
    private javax.swing.JTextField digestValue;
    private javax.swing.JTextField fromBirthday;
    private javax.swing.JRadioButton gender;
    private javax.swing.ButtonGroup genderGroup;
    private javax.swing.JRadioButton genderMan;
    private javax.swing.JRadioButton genderWom;
    private javax.swing.JSpinner idNumbers;
    private javax.swing.JTextField identifyId;
    private javax.swing.JCheckBox isWithX;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private JDatePanelImpl fromDatePanel;
    private JDatePanelImpl toDatePanel;
    private JDatePickerImpl fromDatePicker;
    private JDatePickerImpl toDatePicker;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JComboBox lenthNum;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JLabel message;
    private javax.swing.JTextField orgCode;
    private javax.swing.JCheckBox orgCodeNum;
    private javax.swing.JTextField plainText;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JComboBox provinceComboBox;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.ButtonGroup styleGroup;
    private javax.swing.JFormattedTextField suffix;
    private javax.swing.JLabel suffixLength;
    private javax.swing.JTextField toBirthday;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;
}
