package at.yawk.cricket.template;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yawkat
 */
class ClasspathResourcePath extends ComponentResourcePath {
    public ClasspathResourcePath(URLClassLoader loader, String basePath) throws URISyntaxException, IOException {
        super(findComponents(loader, basePath));
    }

    private static List<ResourcePath> findComponents(URLClassLoader loader, String basePath)
            throws URISyntaxException, IOException {
        List<ResourcePath> components = new ArrayList<>();
        for (URL url : loader.getURLs()) {
            URI uri = url.toURI();
            try {
                Path path = Paths.get(uri);
                if (Files.isDirectory(path)) {
                    components.add(new DirectoryResourcePath(path.resolve(basePath)));
                    continue;
                }
            } catch (FileSystemNotFoundException e) {
                // cannot handle this as fs path
            }

            @SuppressWarnings("resource")
            FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
            components.add(new FileSystemResourcePath(fs, basePath));
        }
        return components;
    }
}
