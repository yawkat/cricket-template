package at.yawk.cricket.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yawkat
 */
class DirectoryResourcePath implements ResourcePath {
    private final Path path;

    public DirectoryResourcePath(Path path) {
        this.path = path;
    }

    static List<Entry> list(Path path) throws IOException {
        return Files.walk(path)
                .map(sub -> new Entry(sub, path.relativize(sub).toString()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Entry> listEntries() throws IOException {
        return list(path);
    }

    @Override
    public void close() {
    }
}
