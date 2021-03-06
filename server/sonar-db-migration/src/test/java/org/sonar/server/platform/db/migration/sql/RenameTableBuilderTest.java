/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.db.migration.sql;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;

public class RenameTableBuilderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void rename_table_on_h2() {
    verifySql(new H2(), "ALTER TABLE foo RENAME TO bar");
  }

  @Test
  public void rename_table_on_mssql() {
    verifySql(new MsSql(), "EXEC sp_rename 'foo', 'bar'");
  }

  @Test
  public void rename_table_on_mysql() {
    verifySql(new MySql(), "ALTER TABLE foo RENAME TO bar");
  }

  @Test
  public void rename_table_on_oracle() {
    verifySql(new Oracle(),
      "DROP TRIGGER foo_idt",
      "RENAME foo TO bar",
      "RENAME foo_seq TO bar_seq",
      "CREATE OR REPLACE TRIGGER bar_idt BEFORE INSERT ON bar FOR EACH ROW BEGIN IF :new.id IS null THEN SELECT bar_seq.nextval INTO :new.id FROM dual; END IF; END;");
  }

  @Test
  public void rename_table_on_postgresql() {
    verifySql(new PostgreSql(), "ALTER TABLE foo RENAME TO bar");
  }

  @Test
  public void throw_IAE_if_name_is_not_valid() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Table name must be lower case and contain only alphanumeric chars or '_', got '(not valid)'");

    new RenameTableBuilder(new H2()).setName("(not valid)").build();
  }

  @Test
  public void throw_IAE_if_new_name_is_not_valid() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Table name must be lower case and contain only alphanumeric chars or '_', got '(not valid)'");

    new RenameTableBuilder(new H2()).setName("foo").setNewName("(not valid)").build();
  }

  private static void verifySql(Dialect dialect, String... expectedSql) {
    List<String> actual = new RenameTableBuilder(dialect)
      .setName("foo")
      .setNewName("bar")
      .build();
    assertThat(actual).containsExactly(expectedSql);
  }
}
