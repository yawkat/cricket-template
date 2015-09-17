/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.cricket.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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

    @Getter private final ObjectMapper objectMapper = new ObjectMapper();

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

    public void registerHelper(String name, Helper<?> helper) {
        handlebars.registerHelper(name, helper);
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

    public <T> T format(String templateName, MarkupConverter<T> converter, Object... args) {
        Template template = getTemplate(templateName);
        String xml;
        try {
            Map<Object, Object> mappedArgs = new HashMap<>();
            if (args != null) {
                for (Object arg : args) {
                    try (TokenBuffer buffer = new TokenBuffer(getObjectMapper(), false)) {
                        objectMapper.writeValue(buffer, arg);
                        mappedArgs.putAll(objectMapper.readValue(buffer.asParser(), Map.class));
                    }
                }
            }
            xml = template.apply(mappedArgs);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return converter.convert(xml);
    }

    public String formatXml(String templateName, Object... args) {
        return format(templateName, XmlMarkupConverter.getInstance(), args);
    }

}
