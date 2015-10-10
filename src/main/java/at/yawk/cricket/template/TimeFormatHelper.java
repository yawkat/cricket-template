package at.yawk.cricket.template;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Converts a string timestamp to the given format.
 *
 * @author yawkat
 */
class TimeFormatHelper implements Helper<String> {
    private static final TimeFormatHelper INSTANCE = new TimeFormatHelper();

    /**
     * Support all formats produced by jackson.
     *
     * @see com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer
     */
    private static final List<Function<String, ZonedDateTime>> INPUT_FORMATTERS = Arrays.asList(
            s -> Instant.from(DateTimeFormatter.ISO_INSTANT.parse(s)).atZone(TimeZoneHolder.getZone()),
            s -> ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(s)),
            s -> OffsetDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(s)).toZonedDateTime()
    );

    private static final DateTimeFormatter DEFAULT_OUTPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TimeFormatHelper() {
    }

    static TimeFormatHelper getInstance() {
        return INSTANCE;
    }

    @Override
    public CharSequence apply(String context, Options options) throws IOException {
        if (context == null) {
            return null;
        }

        ZonedDateTime dateTime = null;
        for (Function<String, ZonedDateTime> inputFormatter : INPUT_FORMATTERS) {
            try {
                dateTime = inputFormatter.apply(context);
                break;
            } catch (DateTimeParseException ignored) {
                // try other formatters
            }
        }
        if (dateTime == null) {
            throw new DateTimeParseException("Could not parse input string " + context, context, -1);
        }

        DateTimeFormatter formatter;
        if (options.params.length == 0) {
            formatter = DEFAULT_OUTPUT_FORMAT;
        } else if (options.params.length == 1) {
            formatter = DateTimeFormatter.ofPattern(options.param(0));
        } else {
            throw new IllegalArgumentException("Too many arguments");
        }
        return formatter.format(dateTime);
    }
}
