/*
 * WorldEdit
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.tools.brushes;

import com.sk89q.util.MessageColor;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class to provide documentation for brushes.
 */
public class BrushDocumentation {
    private final Map<Character, FlagUsage> flags = new HashMap<Character, FlagUsage>();
    private String[] names;
    private String longDescription;

    public BrushDocumentation(String... names) {
        if (names.length < 1) {
            throw new IllegalArgumentException("names length must be > 0");
        }
        
        this.names = names;
    }
    
    public BrushDocumentation flag(char flag, Class<?> type, String usage) {
        flags.put(flag, new FlagUsage(type, usage));
        return this;
    }
    
    public BrushDocumentation desc(String longDescription) {
        this.longDescription = longDescription;
        return this;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Name: ").append(names[0]).append('\n');
        builder.append("Aliases: ").append(MessageColor.WHITE);
        for (int i = 1; i < names.length; i++) {
            builder.append(names[1]);
            if (i < names.length - 1) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }
    
    public static class FlagUsage {
        private final Class<?> type;
        private final String usage;
        
        public FlagUsage(Class<?> type, String usage) {
            this.type = type == null ? boolean.class : type;
            this.usage = usage;
        }

        @Override
        public String toString() {
            return MessageColor.PURPLE + "Type: " + MessageColor.PINK + type.getSimpleName()
                    + MessageColor.PURPLE + " Usage: " + MessageColor.PINK + usage;
        }
    }
}
