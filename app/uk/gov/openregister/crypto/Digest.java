/*
 * Copyright 2015 openregister.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.openregister.crypto;

import org.apache.commons.codec.binary.Hex;
import play.Logger;

import java.security.MessageDigest;

public class Digest {

    public static String shasum(String raw) {
        try {
            String head = "blob " + raw.getBytes("UTF-8").length + "\0";

            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update((head + raw).getBytes("UTF-8"));
            return new String(Hex.encodeHex(md.digest()));

        } catch (Exception e) {
            Logger.error("Unable to create hash", e);
            throw new RuntimeException(e);
        }
    }
}
