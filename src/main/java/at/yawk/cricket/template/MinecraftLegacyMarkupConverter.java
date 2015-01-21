package at.yawk.cricket.template;

import at.yawk.mcomponent.LegacyConverter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * @author yawkat
 */
public class MinecraftLegacyMarkupConverter implements MarkupConverter<List<String>> {
    @Getter private static final MarkupConverter<List<String>> instance = new MinecraftLegacyMarkupConverter();

    @Override
    public List<String> convert(String xml) {
        return MinecraftMarkupConverter.getInstance()
                .convertStream(xml)
                .map(LegacyConverter::toLegacyString)
                .collect(Collectors.toList());
    }
}
