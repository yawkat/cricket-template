/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.cricket.template;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
class ResourceProvider {
    static final String DEFAULT_TEMPLATE_RESOURCE_DIR = "/";

    private final Path source;
    private final Path config;

    private final Map<String, String> stringCache = new HashMap<>();
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private boolean cacheLoaded = false;

    @SneakyThrows(URISyntaxException.class)
    ResourceProvider(Path templateConfigDir, String templateResourceDirectory, Class<?> contextClass) {
        URL url = ResourceProvider.class.getResource(templateResourceDirectory);

        Path sourceLocal = null;
        if (url != null) {
            try {
                sourceLocal = Paths.get(url.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (FileSystemNotFoundException ignored) {}
        }
        if (sourceLocal == null) {
            if (url != null) {
                String[] components = url.toString().split("!");
                sourceLocal = getZipPath(components[0], components[1]);
            } else {
                URL location = contextClass.getProtectionDomain().getCodeSource().getLocation();
                Path path = Paths.get(location.toURI());
                if (Files.isDirectory(path)) {
                    sourceLocal = path.resolve(templateResourceDirectory);
                } else {
                    String basePath = "jar:" + location;
                    sourceLocal = getZipPath(basePath, templateResourceDirectory);
                }
            }
        }
        this.source = sourceLocal;

        config = templateConfigDir;
    }

    private static Path getZipPath(String zipPath, String filePath) {
        Path sourceLocal;
        try {
            URI uri = URI.create(zipPath);
            FileSystem fs;
            try {
                fs = FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException e) {
                fs = FileSystems.newFileSystem(uri, new HashMap<>());
            }
            sourceLocal = fs.getPath(filePath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sourceLocal;
    }

    public void loadCacheAndStoreDefaults() {
        if (cacheLoaded) { return; }
        cacheLock.writeLock().lock();
        try {
            if (cacheLoaded) { return; }

            storeDefaults();
            loadCache();
            cacheLoaded = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public void storeDefaults() throws IOException {
        Files.walk(source).forEach(child -> {
            try {
                Path inConfig = config.resolve(relativize(source, child, config.getFileSystem()));
                if (Files.isDirectory(child)) {
                    Files.createDirectories(inConfig);
                    return;
                }
                if (child.toString().endsWith(".class")) {
                    return;
                }
                Path orig = inConfig.resolveSibling(inConfig.getFileName().toString() + ".orig");
                if (Files.exists(inConfig)) {
                    boolean unmodified = contentsEqual(child, inConfig);
                    if (!unmodified && Files.exists(orig)) {
                        unmodified = contentsEqual(orig, inConfig);
                    }
                    if (unmodified) {
                        Files.delete(inConfig);
                    }
                }
                Files.copy(child, orig, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static boolean contentsEqual(Path a, Path b) throws IOException {
        if (Files.size(a) != Files.size(b)) {
            return false;
        }
        try (SeekableByteChannel ca = Files.newByteChannel(a); SeekableByteChannel cb = Files.newByteChannel(b)) {
            ByteBuffer bufa = ByteBuffer.allocateDirect(1024);
            ByteBuffer bufb = ByteBuffer.allocateDirect(1024);
            while (ca.size() > ca.position()) {
                while (bufa.position() < bufa.capacity()) {
                    ca.read(bufa);
                }
                while (bufb.position() < bufb.capacity()) {
                    ca.read(bufb);
                }
                bufa.position(0);
                bufb.position(0);
                if (!bufa.equals(bufb)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Path relativize(Path parent, Path child, FileSystem targetFs) {
        if (parent.equals(child)) {
            return targetFs.getPath("");
        }

        String parentString = parent.toString();
        String childString = child.toString();
        if (parentString.endsWith("/") && !parentString.equals("/")) {
            parentString = parentString.substring(0, parentString.length() - 1);
        }
        if (childString.endsWith("/")) {
            childString = childString.substring(0, childString.length() - 1);
        }

        parent = parent.getFileSystem().getPath(parentString);
        child = child.getFileSystem().getPath(childString);

        if (parent.isAbsolute()) { child = child.toAbsolutePath(); }
        if (child.isAbsolute()) { parent = parent.toAbsolutePath(); }

        Path relative = parent.relativize(child);

        String root = relative.getName(0).toString();
        String[] components = new String[relative.getNameCount() - 1];
        for (int i = 0; i < components.length; i++) {
            components[i] = relative.getName(i + 1).toString();
        }
        return targetFs.getPath(root, components);
    }

    public void loadCache() throws IOException {
        cacheLock.writeLock().lock();
        try {
            stringCache.clear();
            Files.walk(source)
                    .filter(Files::isRegularFile)
                    .forEach(child -> {
                        Path relative = relativize(source, child, config.getFileSystem());
                        Path configured = config.resolve(relative);
                        String fileName = relative.getFileName().toString();
                        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
                        boolean useConfigured = Files.exists(configured);
                        byte[] data;
                        try {
                            data = Files.readAllBytes(useConfigured ? configured : child);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        String prefix = relative.getParent() == null ?
                                "" :
                                Joiner.on('/').join(relative.getParent()) + "/";
                        if (extension.equalsIgnoreCase("properties")) {
                            Properties properties = new Properties();
                            try {
                                if (useConfigured) {
                                    // also load from jar in case we added new properties
                                    try (Reader reader = Files.newBufferedReader(child)) {
                                        properties.load(reader);
                                    }
                                }
                                properties.load(new StringReader(new String(data, StandardCharsets.UTF_8)));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                            properties.forEach((k, v) -> stringCache.put(prefix + k, String.valueOf(v)));
                        } else if (extension.equalsIgnoreCase("html") ||
                                   extension.equalsIgnoreCase("hbs")) {
                            String name = prefix + fileName.substring(0, fileName.lastIndexOf('.'));
                            stringCache.put(name, new String(data, StandardCharsets.UTF_8).trim());
                        }
                    });
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public String getString(String key) {
        cacheLock.readLock().lock();
        try {
            String v = stringCache.get(key);
            if (v == null) {
                throw new NoSuchElementException(
                        "Missing template " + key + " (available: " + stringCache.keySet() + ")");
            }
            return v;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
}
