package org.wavescale.sourcesync.ui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.wavescale.sourcesync.api.ConnectionConfiguration;
import org.wavescale.sourcesync.api.ConnectionConstants;
import org.wavescale.sourcesync.config.FTPConfiguration;
import org.wavescale.sourcesync.config.FTPSConfiguration;
import org.wavescale.sourcesync.config.SCPConfiguration;
import org.wavescale.sourcesync.config.SFTPConfiguration;
import org.wavescale.sourcesync.factory.ConfigConnectionFactory;
import org.wavescale.sourcesync.factory.ConfigPanelFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

/**
 * ****************************************************************************
 * Copyright (c) 2005-2014 Faur Ioan-Aurel.                                     *
 * All rights reserved. This program and the accompanying materials             *
 * are made available under the terms of the MIT License                        *
 * which accompanies this distribution, and is available at                     *
 * http://opensource.org/licenses/MIT                                           *
 * *
 * For any issues or questions send an email at: fioan89@gmail.com              *
 * *****************************************************************************
 */
public class SourceSyncConfig {
    private JPanel configPanel;
    private JList<String> lstTargets;
    private JButton btnAdd;
    private JButton btnRemove;
    private JPanel pnConfig;
    private JLabel lbTarget;
    private JButton okButton;
    private JButton applyButton;

    private ConnectionConfigPanel connectionPanel;
    private JFrame frame;

    public SourceSyncConfig() {
        frame = new JFrame("sourcesync connection configuration");
        frame.setLocationRelativeTo(null);
        frame.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("/sourcesync.png")).getImage());
        lstTargets.setModel(new DefaultListModel<String>());

        FormLayout layout = new FormLayout("fill:300:grow(1)", "fill:300:grow(1)");
        CellConstraints cc = new CellConstraints();
        pnConfig.setLayout(layout);
        connectionPanel = ConfigPanelFactory.getInstance().getConnectionConfigPanel();
        pnConfig.add(connectionPanel.getConfigPanel(), cc.xy(1, 1));
        pnConfig.setVisible(false);

        frame.setContentPane(configPanel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        lstTargets.getSelectionModel().addListSelectionListener(new TargetListListener());

        btnAdd.addActionListener(new ActionButtonListener());
        btnRemove.addActionListener(new ActionButtonListener());
        okButton.addActionListener(new ActionButtonListener());
        applyButton.addActionListener(new ActionButtonListener());

        //frame.setSize(600, 400);
        frame.pack();
        frame.setVisible(true);

        ConfigConnectionFactory connectionFactory = ConfigConnectionFactory.getInstance();
        loadConnections(connectionFactory);
    }

    /**
     * Loads connection stored in the connection factory and add them in the viewer.
     *
     * @param connectionFactory a {@link ConfigConnectionFactory} instance that contains
     *                          connections stored in the persistence layer.
     */
    @SuppressWarnings("unchecked")
    private void loadConnections(ConfigConnectionFactory connectionFactory) {
        Set<String> connectionNames = connectionFactory.getConnectionNames();
        for (String connectionName : connectionNames) {
            ((DefaultListModel) lstTargets.getModel()).addElement(connectionName);
        }
        // select the firs index and trigger an action event.
        lstTargets.setSelectedIndex(0);
    }

    /**
     * Creates a new connection with the given name and type. The newly created connection is automatically
     * registered to the connection factory.
     *
     * @param connectionName a name for the connection.
     * @param connectionType a constant value from the {@link ConnectionConstants}
     */
    private void createConnection(String connectionName, String connectionType) {
        ConfigConnectionFactory connectionFactory = ConfigConnectionFactory.getInstance();
        ConnectionConfiguration connectionConfiguration = connectionFactory.getConnectionConfiguration(connectionName);
        if (connectionConfiguration == null) {
            if (ConnectionConstants.CONN_TYPE_FTP.equals(connectionType)) {
                connectionConfiguration = new FTPConfiguration(connectionName);
            } else if (ConnectionConstants.CONN_TYPE_FTPS.equals(connectionType)) {
                connectionConfiguration = new FTPSConfiguration(connectionName);
            } else if (ConnectionConstants.CONN_TYPE_SFTP.equals(connectionType)) {
                connectionConfiguration = new SFTPConfiguration(connectionName);
            } else {
                connectionConfiguration = new SCPConfiguration(connectionName);
            }
            uploadConfigurationFromPersistance(connectionConfiguration);
            connectionFactory.addConnectionConfiguration(connectionName, connectionConfiguration);
        }
    }

    /**
     * Gets option stored in the configuration panel and stores them in the specified connection configuration instance.
     *
     * @param connectionConfiguration the actual implementation of the <code>ConnectionConfiguration</code>.
     */
    private void downloadConfigurationToPersistence(ConnectionConfiguration connectionConfiguration) {
        if (connectionConfiguration != null) {
            connectionConfiguration.setHost(connectionPanel.getHost());
            connectionConfiguration.setRootPath(connectionPanel.getRootPath());
            connectionConfiguration.setPort(connectionPanel.getPort());
            connectionConfiguration.setUserName(connectionPanel.getUserName());
            connectionConfiguration.setUserPassword(connectionPanel.getUserPassword());
            connectionConfiguration.setExcludedFiles(connectionPanel.getExludedFiles());
            connectionConfiguration.setPreserveTime(connectionPanel.isTimestampPreserved());
            connectionConfiguration.setSimultaneousJobs(connectionConfiguration.getSimultaneousJobs());
            if (ConnectionConstants.CONN_TYPE_FTPS.equals(connectionConfiguration.getConnectionType())) {
                boolean value = connectionPanel.isImplicit();
                ((FTPSConfiguration) connectionConfiguration).setRequireImplicitTLS(value);
                ((FTPSConfiguration) connectionConfiguration).setRequireExplicitTLS(!value);
                connectionConfiguration.setPreserveTime(false);
            } else if (ConnectionConstants.CONN_TYPE_FTP.equals(connectionConfiguration.getConnectionType())) {
                connectionConfiguration.setPreserveTime(false);
            }
        }
    }

    /**
     * Stores option in the configuration panel from the specified connection configuration instance.
     *
     * @param connectionConfiguration the actual implementation if the <code>ConnectionConfiguration</code>.
     */
    private void uploadConfigurationFromPersistance(ConnectionConfiguration connectionConfiguration) {
        if (connectionConfiguration != null) {
            connectionPanel.setConnectionType(connectionConfiguration.getConnectionType());
            connectionPanel.setHost(connectionConfiguration.getHost());
            connectionPanel.setRootPath(connectionConfiguration.getRootPath());
            connectionPanel.setPort(connectionConfiguration.getPort());
            connectionPanel.setUserName(connectionConfiguration.getUserName());
            connectionPanel.setUserPassword(connectionConfiguration.getUserPassword());
            connectionPanel.setExcludedFiles(connectionConfiguration.getExcludedFiles());
            connectionPanel.setSimultaneousJobs(connectionConfiguration.getSimultaneousJobs());
            connectionPanel.setConnectionMethodVisible(false);
            connectionPanel.setPreserveTimestampVisible(true);
            if (ConnectionConstants.CONN_TYPE_FTPS.equals(connectionConfiguration.getConnectionType())) {
                boolean value = ((FTPSConfiguration) connectionConfiguration).isRequireImplicitTLS();
                connectionPanel.setImplicit(value);
                connectionPanel.setExplicit(!value);

                connectionPanel.setConnectionMethodVisible(true);
                connectionPanel.setPreserveTimestampVisible(false);
            } else if (ConnectionConstants.CONN_TYPE_FTP.equals(connectionConfiguration.getConnectionType())) {
                connectionPanel.setPreserveTimestampVisible(false);
            }
            pnConfig.setVisible(true);
        }
    }

    class TargetListListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent listSelectionEvent) {
            DefaultListSelectionModel selectionModel = (DefaultListSelectionModel) listSelectionEvent.getSource();
            int index = selectionModel.getMinSelectionIndex();
            if (lstTargets.getModel() != null && index >= 0) {
                String target = lstTargets.getModel().getElementAt(index);
                lbTarget.setText(target);
                ConnectionConfiguration connectionConfiguration = ConfigConnectionFactory.getInstance().getConnectionConfiguration(target);
                uploadConfigurationFromPersistance(connectionConfiguration);
            }
        }
    }

    class ActionButtonListener implements ActionListener {

        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            JButton actionButton = (JButton) actionEvent.getSource();
            if (okButton.equals(actionButton)) {
                frame.dispose();
            } else if (applyButton.equals(actionButton)) {
                String target = lstTargets.getSelectedValue();
                ConfigConnectionFactory connectionFactory = ConfigConnectionFactory.getInstance();
                ConnectionConfiguration connectionConfiguration = connectionFactory.getConnectionConfiguration(target);
                downloadConfigurationToPersistence(connectionConfiguration);
                connectionFactory.saveConnections();
            } else if (btnAdd.equals(actionButton)) {
                TargetLocation targetConfig = new TargetLocation();
                targetConfig.setModal(true);
                String name = targetConfig.getTargetName();
                String type = targetConfig.getTargetType();
                ((DefaultListModel) lstTargets.getModel()).addElement(name);
                lstTargets.setSelectedIndex(((DefaultListModel) lstTargets.getModel()).lastIndexOf(name));
                createConnection(name, type);
                pnConfig.setVisible(true);
            } else if (btnRemove.equals(actionButton)) {
                if (lstTargets.getModel().getSize() > 0) {
                    String target = lstTargets.getSelectedValue();
                    DefaultListModel listModel = ((DefaultListModel) lstTargets.getModel());
                    int index = listModel.indexOf(target);
                    if (index >= 0) {
                        listModel.removeElement(target);
                        // remove from config
                        ConfigConnectionFactory configConnectionFactory = ConfigConnectionFactory.getInstance();
                        configConnectionFactory.removeConnectionConfiguration(target);
                    }
                }
            }
        }
    }
}
