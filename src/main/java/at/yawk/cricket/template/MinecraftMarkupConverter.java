/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.cricket.template;

import at.yawk.mcomponent.*;
import at.yawk.mcomponent.action.BaseAction;
import at.yawk.mcomponent.action.BaseEvent;
import at.yawk.mcomponent.action.Event;
import at.yawk.mcomponent.style.Color;
import at.yawk.mcomponent.style.FlagKey;
import at.yawk.mcomponent.style.FlagValue;
import at.yawk.mcomponent.style.Style;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.ToString;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * MarkupConverter implementation that converts to a list of minecraft components (one each line).
 *
 * @author yawkat
 */
public class MinecraftMarkupConverter implements MarkupConverter<List<Component>> {
    private static final Map<String, Color> COLOR_BY_NAME = new HashMap<String, Color>() {{
        for (Color color : Color.values()) {
            color.getName().ifPresent(name -> put(name, color));
        }
    }};

    @Getter private static final MinecraftMarkupConverter instance = new MinecraftMarkupConverter(false);
    @Getter private static final MinecraftMarkupConverter instanceWithLinefeeds = new MinecraftMarkupConverter(true);

    /**
     * If this is true, &lt;lf&gt; is translated to \n (as used in kick packets), otherwise it will cause a new
     * component to be started.
     */
    private final boolean keepLinefeeds;

    private MinecraftMarkupConverter(boolean keepLinefeeds) {
        this.keepLinefeeds = keepLinefeeds;
    }

    @Override
    public List<Component> convert(String xml) {
        return convertStream(xml).collect(Collectors.toList());
    }

    protected Stream<Component> convertStream(String xml) {
        Parser parser = new Parser();
        ComponentConverter converter = new ComponentConverter();
        parser.setContentHandler(converter);
        try {
            parser.parse(new InputSource(new StringReader(xml)));
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
        if (!converter.text) { converter.lines.remove(converter.lines.size() - 1); }
        return converter.lines.stream().map(n -> n.build(true));
    }

    private class ComponentConverter extends DefaultHandler {
        private ComponentNode root = new ComponentNode();
        private List<ComponentNode> lines = new ArrayList<>(Collections.singleton(root));
        private ComponentNode current = root;
        private boolean whitespace = true;
        private boolean text = false;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (qName.equals("lf")) {
                if (keepLinefeeds) {
                    characters0(new char[]{ '\n' }, 0, 1, true);
                    whitespace = true;
                    return;
                } else {
                    if (!text) {
                        lines.remove(lines.size() - 1);
                    }
                    current = root = current.copyStyle();
                    whitespace = true;
                    text = false;
                    lines.add(root);
                    return;
                }
            }

            ComponentNode newNode = new ComponentNode();
            current.members.add(newNode);
            newNode.parent = current;
            current = newNode;

            if (qName.equals("hover")) {
                current.events.add(new BaseEvent(BaseEvent.Type.HOVER, parseAction(attributes, true)));
            } else if (qName.equals("click")) {
                if (attributes.getValue("shift") != null) {
                    current.events.add(new BaseEvent(BaseEvent.Type.SHIFT_CLICK, parseAction(attributes, false)));
                } else {
                    current.events.add(new BaseEvent(BaseEvent.Type.CLICK, parseAction(attributes, false)));
                }
            }

            Color color = COLOR_BY_NAME.get(attributes.getValue("color"));
            if (color != null) {
                current.style = current.style.withColor(color);
            }

            for (FlagKey key : FlagKey.values()) {
                if (attributes.getValue(key.getKey()) != null) {
                    current.style = current.style.withFlag(key, FlagValue.TRUE);
                }
            }
        }

        private BaseAction parseAction(Attributes attributes, boolean component) {
            BaseAction.Type type = BaseAction.Type.valueOf(attributes.getValue("action").toUpperCase());
            String valueString = attributes.getValue("value");
            Component value = component ? convert(valueString).get(0) : new StringComponent(valueString);
            return new BaseAction(type, value);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("lf")) {
                return;
            }

            while (current.fromText) {
                current = current.parent;
            }
            current = current.parent;
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            characters0(ch, start, length, false);
        }

        private void characters0(char[] ch, int start, int length, boolean exact) {
            if (!current.members.isEmpty()) {
                ComponentNode newNode = new ComponentNode();
                current.members.add(newNode);
                newNode.parent = current;
                newNode.fromText = true;
                current = newNode;
            }

            current.text.ensureCapacity(current.text.length() + length);
            for (int i = 0; i < length; i++) {
                char c = ch[i + start];
                if (!exact && (c == '\n' || c == '\r')) {
                    c = ' ';
                }
                if (whitespace) {
                    if (!Character.isWhitespace(c)) {
                        current.text.append(c);
                        whitespace = false;
                    } else if (exact) {
                        current.text.append(c);
                    }
                } else {
                    current.text.append(c);
                    whitespace = Character.isWhitespace(c);
                }
            }
            text |= current.text.length() > 0;
        }
    }

    @ToString(exclude = "members")
    private static class ComponentNode {
        ComponentNode parent = null;
        StringBuilder text = new StringBuilder();
        Style style = Style.INHERIT;
        List<ComponentNode> members = new ArrayList<>();
        Set<Event> events = new HashSet<>();
        boolean fromText;

        private boolean empty() {
            return text.length() <= 0 && members.isEmpty() && events.isEmpty();
        }

        Component build(boolean root) {
            if (!root) {
                if (empty()) {
                    return null;
                }
                if (members.isEmpty() && events.isEmpty() && style.equals(Style.INHERIT)) {
                    return new StringComponent(text.toString());
                }
            }
            BaseComponent component = new BaseComponent(
                    new StringComponentValue(text.toString()),
                    members.stream()
                            .map(m -> m.build(false))
                            .filter(m -> m != null)
                            .collect(Collectors.toList()),
                    style,
                    events
            );
            return ComponentMinimizer.minimizeOne(component);
        }

        ComponentNode copyStyle() {
            ComponentNode p = parent == null ? null : parent.copyStyle();
            if (fromText && events.isEmpty() && style.equals(Style.INHERIT)) {
                return p;
            }
            ComponentNode node = new ComponentNode();
            node.style = this.style;
            node.events.addAll(this.events);
            node.parent = p;
            return node;
        }
    }
}
