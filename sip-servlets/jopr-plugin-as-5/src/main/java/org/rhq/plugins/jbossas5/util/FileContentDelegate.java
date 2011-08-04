/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.rhq.plugins.jbossas5.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.content.PackageDetails;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;

/**
* Delegate class used for manipulating artifacts in a JON plugin.
*
* @author Greg Hinkle
* @author Jason Dobies
*/
public class FileContentDelegate {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(FileContentDelegate.class);

    protected File directory;

    private final String fileEnding;
    private final String packageTypeName;

    // Constructors  --------------------------------------------

    public FileContentDelegate(File directory, String fileEnding, String packageTypeName) {
        this.directory = directory;
        this.fileEnding = fileEnding;
        this.packageTypeName = packageTypeName;
    }

    // Public  --------------------------------------------

    public String getFileEnding() {
        return fileEnding;
    }

    public String getPackageTypeName() {
        return packageTypeName;
    }

    public File getDirectory() {
        return directory;
    }

    /**
     * Creates a new package described by the specified details. The destination of the content in the provided input
     * stream will be determined by the package name.
     *
     * @param  details  describes the package being created
     * @param  content  content to be written for the package. NOTE this Stream will be closed by this method.
     * @param  unzip    if <code>true</code>, the content stream will be treated like a ZIP file and be unzipped as
    *                  it is written, using the package name as the base directory; if <code>false</code> the
     * @param createBackup If <code>true</code>, the original file will be backed up to file.bak
     */
    public void createContent(PackageDetails details, InputStream content, boolean unzip, boolean createBackup) {
        File contentFile = getPath(details);
        try {
            if (createBackup) {
                moveToBackup(contentFile, ".bak");
            }
            if (unzip) {
                ZipUtil.unzipFile(content, contentFile);
            } else {
                FileUtil.writeFile(content, contentFile);
            }
            details.setFileName(contentFile.getPath());
        } catch (IOException e) {
            throw new RuntimeException("Error creating artifact from details: " + contentFile, e);
        }
    }

    /**
     * Try to move the passed contentFile to a backup named contentFile + suffix
     * @param contentFile File object pointing to the original file
     * @param suffix the suffix to tack on
     */
    private void moveToBackup(File contentFile, String suffix) {

        File backupFile = new File(contentFile.getAbsolutePath() + suffix);
        if (backupFile.exists()) {
            // remove
            if (!backupFile.delete())
                log.warn("Removing of old backup file " + backupFile + " failed ");
        }
        if (!contentFile.renameTo(backupFile)) {
            log.warn("Moving " + contentFile + " to backup " + backupFile + " failed");
        }
    }

    public File getPath(PackageDetails details) {
        /* JBNADM-2022 - It still needs to be determined if it is the responsibility of the plugin container or the
         *               plugin to be concerned with path information in the package name. For now, it's the plugin's
         *               responsibility. We strip out the path information to keep control of where the JARs are
         *               deployed to. Note: when we add support for more package types, we'll need to refactor this
         *               out on a package type basis.
         *
         * jdobies, Sep 20, 2007
         */
        PackageDetailsKey key = details.getKey();
        String fileName = key.getName();
        int lastPathStart = fileName.lastIndexOf(File.separatorChar);
        if (lastPathStart > -1) {
            fileName = fileName.substring(lastPathStart + 1);
        }

        if (!fileName.endsWith(fileEnding)) {
            fileName = fileName + fileEnding;
        }
        return new File(this.directory, fileName);
    }

    /**
     * Returns a stream from which the content of the specified package can be read.
     *
     * @param details package being loaded
     *
     * @return buffered input stream containing the contents of the package; will not be <code>null</code>, an
     *         exception is thrown if the content cannot be loaded
     */
    public InputStream getContent(PackageDetails details) {
        File contentFile = getPath(details);
        try {
            return new BufferedInputStream(new FileInputStream(contentFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Package content not found for package " + contentFile, e);
        }
    }

    /**
     * Deletes the underlying file for the specified package.
     *
     * @param details package to delete
     */
    public void deleteContent(PackageDetails details) {
        File contentFile = getPath(details);
        if (!contentFile.exists())
            return;

        try {
            FileUtils.purge(contentFile, true);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete underlying file [" + contentFile + "] for " + details + ".", e);
        }
    }

    public Set<ResourcePackageDetails> discoverDeployedPackages() {
        /*
         * This is a stub implementation, you need to implement a
         * discovery for artifacts of your particular content type
         */
        return null;
    }
}