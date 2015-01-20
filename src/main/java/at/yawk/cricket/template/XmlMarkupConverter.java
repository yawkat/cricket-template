package at.yawk.cricket.template;

import lombok.Getter;

/**
 * Identity markup converter (xml -> xml). Used when the template manager should just fill in the templated values but
 * not convert the document in any way, for example for further use.
 *
 * @author yawkat
 */
public class XmlMarkupConverter implements MarkupConverter<String> {
    @Getter private static final MarkupConverter<String> instance = new XmlMarkupConverter();

    private XmlMarkupConverter() {}

    @Override
    public String convert(String xml) {
        return xml;
    }
}
