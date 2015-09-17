package at.yawk.cricket.template;

import com.github.jknack.handlebars.io.AbstractTemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class TemplateLoaderImpl extends AbstractTemplateLoader {
    private final ResourcePath resourcePath;
    private final Path configPath;

    private volatile boolean initialized = false;
    private volatile Map<String, Path> staticLocationMap;

    public synchronized void invalidate() {
        initialized = false;
    }

    private synchronized void init() throws IOException {
        if (initialized) { return; }
        Map<String, Path> staticLocationMap = new HashMap<>();
        for (ResourcePath.Entry entry : resourcePath.listEntries()) {
            staticLocationMap.put(entry.getName(), entry.getPath());
        }
        this.staticLocationMap = staticLocationMap;

        initialized = true;
    }

    @Override
    public TemplateSource sourceAt(String location) throws IOException {
        init();

        Path path = staticLocationMap.get(location);
        if (path == null) { throw new FileNotFoundException("No template for path " + location + " found"); }

        return new TemplateSourceImpl(path, configPath.resolve(location));
    }

    @RequiredArgsConstructor
    private static class TemplateSourceImpl implements TemplateSource {
        private final Path staticPath;
        private final Path configPath;

        @Override
        public String content() throws IOException {
            Path path = Files.exists(configPath) ? configPath : staticPath;
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }

        @Override
        public String filename() {
            return staticPath.getFileName().toString();
        }

        @Override
        public long lastModified() {
            try {
                return Files.getLastModifiedTime(configPath).toMillis();
            } catch (IOException e) {
                return 0L;
            }
        }
    }
}
