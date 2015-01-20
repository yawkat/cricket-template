package at.yawk.cricket.template;

/**
 * Convert a given XML string to an object representation T. Can be passed to a TemplateManager.
 *
 * @author yawkat
 */
public interface MarkupConverter<T> {
    T convert(String xml);
}
