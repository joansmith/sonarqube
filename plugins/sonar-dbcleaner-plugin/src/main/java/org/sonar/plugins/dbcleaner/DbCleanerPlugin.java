/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.plugins.dbcleaner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.core.computation.dbcleaner.period.DefaultPeriodCleaner;

import java.util.List;

@Deprecated
public final class DbCleanerPlugin extends SonarPlugin {

  static List<PropertyDefinition> propertyDefinitions() {
    return Lists.newArrayList();
  }

  @Override
  public List getExtensions() {
    return ImmutableList.builder().add(DefaultPeriodCleaner.class, DeprecatedDefaultPurgeTask.class)
      .addAll(propertyDefinitions()).build();
  }
}
