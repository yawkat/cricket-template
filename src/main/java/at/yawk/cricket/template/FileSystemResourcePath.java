package at.yawk.cricket.template;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yawkat
 */
class FileSystemResourcePath implements ResourcePath {
    private final FileSystem fileSystem;
    private final String path;

    public FileSystemResourcePath(FileSystem fileSystem, String path) {
        this.fileSystem = fileSystem;
        this.path = path;
    }

    @Override
    public List<Entry> listEntries() throws IOException {
        List<Entry> entries = new ArrayList<>();
        for (Path root : fileSystem.getRootDirectories()) {
            entries.addAll(DirectoryResourcePath.list(root.resolve(path)));
        }
        return entries;
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }
}
