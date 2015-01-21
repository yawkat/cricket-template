package at.yawk.cricket.template;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.AbstractTemplateLoader;
import com.github.jknack.handlebars.io.StringTemplateSource;
import com.github.jknack.handlebars.io.TemplateSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class TemplateManager {
    private final Handlebars handlebars;

    private final Map<String, Template> resources = new ConcurrentHashMap<>();

    @Getter private final Serializer serializer = new Serializer();

    public TemplateManager(Path templateConfigDir) {
        this(templateConfigDir, ResourceProvider.DEFAULT_TEMPLATE_RESOURCE_DIR);
    }

    public TemplateManager(Path templateConfigDir, String templateResourceDirectory) {
        ResourceProvider resourceProvider = new ResourceProvider(templateConfigDir, templateResourceDirectory);
        handlebars = new Handlebars(new AbstractTemplateLoader() {
            @Override
            public TemplateSource sourceAt(String location) throws IOException {
                resourceProvider.loadCacheAndStoreDefaults();
                try {
                    String string = resourceProvider.getString(location);
                    return new StringTemplateSource(location, string);
                } catch (NoSuchElementException e) {
                    // rethrow as IO so handlebars can decide what to do
                    throw new IOException(e);
                }
            }
        });
        handlebars.registerHelper("readableIndex", ReadableIndexHelper.getInstance());
        handlebars.registerHelpers(StringHelpers.class);
    }

    /**
     * @param from template location
     */
    private Template createTemplate(String from) {
        try {
            return handlebars.compile(from);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @param from template location
     */
    private Template getTemplate(String from) {
        return resources.computeIfAbsent(from, this::createTemplate);
    }

    public <T> T format(String templateName, MarkupConverter<T> converter, Object args) {
        Template template = getTemplate(templateName);
        String xml;
        try {
            Map<String, Object> mappedArgs = mapArgs(args);
            mappedArgs.put("page", templateName);
            xml = template.apply(mappedArgs);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return converter.convert(xml);
    }

    public String formatXml(String templateName, Object args) {
        return format(templateName, XmlMarkupConverter.getInstance(), args);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapArgs(Object args) {
        Object s = serializer.serialize(args);
        log.debug("Serialized {} -> {}", args, s);
        // let's hope for the best!
        return s == null ? new HashMap<>() : (Map<String, Object>) s;
    }
}