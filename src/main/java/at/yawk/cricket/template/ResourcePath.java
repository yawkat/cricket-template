package at.yawk.cricket.template;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.Value;

/**
 * @author yawkat
 */
interface ResourcePath extends Closeable {
    List<Entry> listEntries() throws IOException;

    @Value
    class Entry {
        private final Path path;
        private final String name;
    }
}
