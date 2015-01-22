/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.cricket.template;

/**
 * Convert a given XML string to an object representation T. Can be passed to a TemplateManager.
 *
 * @author yawkat
 */
public interface MarkupConverter<T> {
    T convert(String xml);
}
