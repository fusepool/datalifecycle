/*
 * Copyright 2014 Reto.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.datalifecycle;

import java.security.Permission;

/**
 *
 * @author Reto
 */
class DlcPermission extends Permission {

    public DlcPermission() {
        super("A DataLifecycle permission");
    }
    
    public DlcPermission(String action, String target) {
        this();
    }

    @Override
    public boolean implies(Permission prmsn) {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass().equals(this.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String getActions() {
        return "";
    }
    
}
