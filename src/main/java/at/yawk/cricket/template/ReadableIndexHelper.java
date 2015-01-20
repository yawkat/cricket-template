package at.yawk.cricket.template;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import java.io.IOException;
import lombok.Getter;

/**
 * @author yawkat
 */
class ReadableIndexHelper implements Helper<Object> {
    @Getter private static final Helper<?> instance = new ReadableIndexHelper();

    private ReadableIndexHelper() {}

    @SuppressWarnings("unchecked")
    @Override
    public CharSequence apply(Object context, Options options) throws IOException {
        CharSequence fn = options.fn();
        return String.valueOf(Integer.parseInt(fn.toString()) + 1);
    }
}
