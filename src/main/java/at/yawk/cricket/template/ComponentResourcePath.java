package at.yawk.cricket.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class ComponentResourcePath implements ResourcePath {
    private final List<ResourcePath> components;

    @Override
    public List<Entry> listEntries() throws IOException {
        List<Entry> entries = new ArrayList<>();
        for (ResourcePath component : components) {
            entries.addAll(component.listEntries());
        }
        return entries;
    }

    @Override
    public void close() throws IOException {
        for (ResourcePath component : components) {
            component.close();
        }
    }
}
