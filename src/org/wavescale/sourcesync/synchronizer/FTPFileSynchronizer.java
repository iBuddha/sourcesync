package org.wavescale.sourcesync.synchronizer;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.Util;
import org.jetbrains.annotations.NotNull;
import org.wavescale.sourcesync.api.FileSynchronizer;
import org.wavescale.sourcesync.config.FTPConfiguration;
import org.wavescale.sourcesync.logger.EventDataLogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ****************************************************************************
 * Copyright (c) 2014-2107 Faur Ioan-Aurel.                                     *
 * All rights reserved. This program and the accompanying materials             *
 * are made available under the terms of the MIT License                        *
 * which accompanies this distribution, and is available at                     *
 * http://opensource.org/licenses/MIT                                           *
 * *
 * For any issues or questions send an email at: fioan89@gmail.com              *
 * *****************************************************************************
 */
public class FTPFileSynchronizer extends FileSynchronizer {
    private final FTPClient ftp;

    public FTPFileSynchronizer(@NotNull FTPConfiguration connectionInfo, @NotNull Project project, @NotNull ProgressIndicator indicator) {
        super(connectionInfo, project, indicator);
        this.ftp = new FTPClient();
        this.getIndicator().setIndeterminate(true);
    }

    @Override
    public boolean connect() {
        if (!isConnected()) {
            try {
                this.ftp.connect(this.getConnectionInfo().getHost(), this.getConnectionInfo().getPort());
                this.ftp.login(this.getConnectionInfo().getUserName(), this.getConnectionInfo().getUserPassword());
                // use passive mode to bypass firewall conflicts
                this.ftp.enterLocalPassiveMode();
            } catch (IOException e) {
                EventDataLogger.logWarning(e.toString(), this.getProject());
                return false;
            }
            // check if successful connection
            if (!FTPReply.isPositiveCompletion(this.ftp.getReplyCode())) {
                EventDataLogger.logWarning("Connection to <b>" + this.getConnectionInfo().getHost() + "</b> failed!", this.getProject());
                return false;
            }
            this.setConnected(true);
        }
        return true;
    }

    @Override
    public void disconnect() {
        if (this.ftp != null) {
            try {
                ftp.completePendingCommand();
                ftp.disconnect();
                this.setConnected(false);
            } catch (IOException e) {
                EventDataLogger.logWarning(e.toString(), this.getProject());
            }
        }
    }

    @Override
    public void syncFile(String sourceLocation, Path uploadLocation) {
        // preserve timestamp for now
        boolean preserveTimestamp = true;
        Path sourcePathLocation = Paths.get(sourceLocation);
        String sourceFileName = sourcePathLocation.getFileName().toString();
        String rootPath = this.getConnectionInfo().getRootPath();
        if (rootPath == null || rootPath.isEmpty()) {
            rootPath = "/";
            EventDataLogger.logWarning("root path of FTP is not specified, use '/'.", getProject());
        }
        Path remotePath = Paths.get(rootPath).resolve(uploadLocation);
        // first try to create the path where this must be uploaded
        try {
            this.ftp.changeWorkingDirectory(remotePath.getRoot().toString());
        } catch (IOException e) {
            EventDataLogger.logError("On remote we could not change directory into root: " + remotePath.getRoot(), this.getProject());
        }
        for (Path current : remotePath) {
            String location = current.toString();
            try {
                this.ftp.makeDirectory(location);
            } catch (IOException e) {
                // this dir probably exist so just ignore now it will fail later
                // if there are other reasons this could not be executed.
            }
            try {
                this.ftp.changeWorkingDirectory(location);
            } catch (IOException e) {
                // probably it doesn't exist or maybe no permission
                EventDataLogger.logError("Remote dir <b>" + remotePath +
                        "</b> might not exist or you don't have permission on this path!", this.getProject());
                return;
            }
        }

        // upload
        try {
            EventDataLogger.logInfo("Uploading " + sourceFileName, getProject());
            this.ftp.setFileType(FTP.BINARY_FILE_TYPE);
            FileInputStream input = new FileInputStream(sourceLocation);
            OutputStream output = this.ftp.storeFileStream(sourceFileName);
            this.getIndicator().setIndeterminate(false);
            this.getIndicator().setText("Uploading...[" + sourceFileName + "]");
//            byte[] buffer = new byte[1024];
//            int len;
//            double totalSize = sourcePathLocation.toFile().length() + 0.0;
//            long totalUploaded = 0;
//            while (true) {
//                len = in.read(buffer, 0, buffer.length);
//                if (len <= 0) {
//                    break;
//                }
//                outputStream.write(buffer, 0, len);
//                totalUploaded += len;
//                this.getIndicator().setFraction(totalUploaded / totalSize);
//            }
//            if (preserveTimestamp) {
//                // TODO - implement preserve timestamp mechanism
//            }
            Util.copyStream(input, output);
            input.close();
            output.close();
            this.getIndicator().setText("Uploaded [" + sourceFileName + "]");
            this.getIndicator().setFraction(100.0);
            ftp.completePendingCommand();
            EventDataLogger.logInfo("Uploaded " + sourceFileName, getProject());
        } catch (FileNotFoundException e) {
            EventDataLogger.logWarning(e.toString(), getProject());
        } catch (IOException e) {
            EventDataLogger.logError(e.toString(), getProject());
        }

    }
}
