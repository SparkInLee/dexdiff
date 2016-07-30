/*
 * dex2jar - Tools to work with android .dex and java .class files
 * Copyright (c) 2009-2013 Panxiaobo
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
package com.googlecode.d2j.dex.writer.item;

import com.googlecode.d2j.dex.writer.ann.Alignment;
import com.googlecode.d2j.dex.writer.ann.Idx;
import com.googlecode.d2j.dex.writer.io.DataOut;

@Alignment(4)
public class TypeIdItem extends BaseItem implements Comparable<TypeIdItem> {
    public TypeIdItem(StringIdItem stringIdItem) {
        super();
        this.descriptor = stringIdItem;
    }

    @Idx
    public final StringIdItem descriptor;

    @Override
    public int place(int offset) {
        return offset + 0x04;
    }

    @Override
    public String toString() {
        return "TypeIdItem [descriptor=" + descriptor + "]";
    }

    @Override
    public void write(DataOut out) {
        out.uint("descriptor_idx", this.descriptor.index);
    }

    @Override
    public int compareTo(TypeIdItem o) {
        return descriptor.compareTo(o.descriptor);
    }
    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypeIdItem item = (TypeIdItem) obj;
		if (null != descriptor) {
			if (null != item.descriptor) {
				return descriptor.equals(item.descriptor);
			} else {
				return false;
			}
		} else {
			if (null != descriptor) {
				return false;
			} else {
				return true;
			}
		}
	}
}
