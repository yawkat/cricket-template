/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.cricket.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
class ResourceProvider {
    static final String DEFAULT_TEMPLATE_RESOURCE_DIR = "/";

    public static void copyDefaults(ResourcePath resourcePath, Path target) throws IOException {
        for (ResourcePath.Entry entry : resourcePath.listEntries()) {
            Path targetOrig = target.resolve(entry.getName() + ".orig");
            Path targetReal = target.resolve(entry.getName());

            if (Files.exists(targetReal)) {
                if (
                    // same as provided resource
                        contentsEqual(targetReal, entry.getPath()) ||
                        // same as old .orig
                        (Files.exists(targetOrig) && contentsEqual(targetOrig, targetReal))) {

                    Files.delete(targetReal);
                }
            }

            Files.copy(entry.getPath(), targetOrig, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean contentsEqual(Path a, Path b) throws IOException {
        return Files.size(a) == Files.size(b) &&
               Arrays.equals(Files.readAllBytes(a), Files.readAllBytes(b));
    }
}
