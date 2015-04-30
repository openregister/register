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

package functional;

import helper.PostgresqlStoreForTesting;

import java.util.HashMap;
import java.util.Map;

public class TestSettings {

    public static Map<String, String> forRegister(String name) {
        HashMap<String, String> map = new HashMap<>();
        map.put("store.uri", PostgresqlStoreForTesting.POSTGRESQL_URI);
        map.put("register.name", name);
        map.put("registers.service.url", "http://localhost:8888");
        return map;
    }

}
