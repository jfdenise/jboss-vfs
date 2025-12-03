/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.vfs.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSigner;
import java.util.ArrayList;
import java.util.List;

import org.jboss.vfs.TempDir;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VirtualFile;
import org.wildfly.graal.runtime.WildFlyGraalSetup;

/**
 * A kind of JavaZipFileSystem that works in a Graal VM context.
 * @author jdenise
 */
public final class JavaZipFileSystemGraal implements FileSystem {

    private static final List<JavaZipFileSystemGraal> ALL = new ArrayList<>();

    private static synchronized void addFile(JavaZipFileSystemGraal file) throws IOException {
        ALL.add(file);
    }

    public static void passivateFiles(TempFileProvider provider) throws IOException {
        for(JavaZipFileSystemGraal zip : ALL) {
            zip.passivate(provider);
        }
    }
    public static void activateFiles(TempFileProvider provider) throws IOException {
        for(JavaZipFileSystemGraal zip : ALL) {
            zip.activate(provider);
        }
    }
    private JavaZipFileSystem delegate;
    private final File archiveFile;
    private TempFileProvider provider;
    private final String name;
    /**
     * Create a new instance.
     *
     * @param name        the name of the source archive
     * @param inputStream an input stream from the source archive
     * @param tempDir     the temp dir into which zip information is stored
     * @throws java.io.IOException if an I/O error occurs
     */
    public JavaZipFileSystemGraal(String name, InputStream inputStream, TempDir tempDir) throws IOException {
        this(tempDir.createFile(name, inputStream), tempDir);
    }

    /**
     * Create a new instance.
     *
     * @param archiveFile the original archive file
     * @param tempDir     the temp dir into which zip information is stored
     * @throws java.io.IOException if an I/O error occurs
     */
    public JavaZipFileSystemGraal(File archiveFile, TempDir tempDir) throws IOException {
        addFile(this);
        delegate = new JavaZipFileSystem(archiveFile, tempDir);
        this.archiveFile = archiveFile;
        name = archiveFile.toPath().relativize(tempDir.getRoot().toPath()).toString();
    }

    void passivate(TempFileProvider provider) throws IOException {
        delegate = null;
        this.provider = provider;
    }

    void activate(TempFileProvider provider) throws IOException {
        delegate = new JavaZipFileSystem(archiveFile, provider.createTempDir(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getFile(VirtualFile mountPoint, VirtualFile target) throws IOException {
        return delegate.getFile(mountPoint, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException {
        return getFileSystem().openInputStream(mountPoint, target);
    }

    private JavaZipFileSystem getFileSystem() {
        JavaZipFileSystem zipFs;
        if (delegate == null) {
            // Can only occur post passivate, Graal compiling and loading classes.
            if (!WildFlyGraalSetup.isBuildTime()) {
                throw new RuntimeException("Invalid state, zip file is null for " + archiveFile);
            }
            try {
                try {
                    return zipFs = new JavaZipFileSystem(archiveFile, provider.createTempDir(name));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            return delegate;
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(VirtualFile mountPoint, VirtualFile target) {
        return getFileSystem().delete(mountPoint, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize(VirtualFile mountPoint, VirtualFile target) {
        return getFileSystem().getSize(mountPoint, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastModified(VirtualFile mountPoint, VirtualFile target) {
        return getFileSystem().getLastModified(mountPoint, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(VirtualFile mountPoint, VirtualFile target) {
        return getFileSystem().exists(mountPoint, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFile(final VirtualFile mountPoint, final VirtualFile target) {
        return getFileSystem().isFile(mountPoint, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory(VirtualFile mountPoint, VirtualFile target) {
        return getFileSystem().isDirectory(mountPoint, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) {
        return getFileSystem().getDirectoryEntries(mountPoint, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CodeSigner[] getCodeSigners(VirtualFile mountPoint, VirtualFile target) {
        return getFileSystem().getCodeSigners(mountPoint, target);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly() {
        return getFileSystem().isReadOnly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getMountSource() {
        return getFileSystem().getMountSource();
    }

    @Override
    public URI getRootURI() throws URISyntaxException {
        return getFileSystem().getRootURI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // DO nothing.
    }
}