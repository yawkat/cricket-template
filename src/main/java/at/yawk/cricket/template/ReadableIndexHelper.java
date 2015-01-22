/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
