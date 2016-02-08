/*
 * Copyright (C) 2015 Alexey Ragozin
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
package org.gridkit.vicluster.telecontrol.jvm;


class Any {

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object ref) {
        return (T)ref;
    }
    
    public static RuntimeException throwUnchecked(Throwable e) {
        throw AnyThrow.<RuntimeException>throwAny(e);
    }
    
    private static class AnyThrow {
       
        @SuppressWarnings("unchecked")
        private static <E extends Throwable> RuntimeException throwAny(Throwable e) throws E {
            throw (E)e;
        }
    }
}
