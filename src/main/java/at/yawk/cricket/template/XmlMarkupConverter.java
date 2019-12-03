/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.cricket.template;

import lombok.Getter;

/**
 * Identity markup converter (xml -&gt; xml). Used when the template manager should just fill in the templated values but
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
