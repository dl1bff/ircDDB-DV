/*

ircDDB DV Plugins

Copyright (C) 2010   Michael Dirska, DL1BFF (dl1bff@mdx.de)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package net.ircDDB.dv;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;
import java.util.ArrayList;

import net.ircDDB.Dbg;



public class RptrAppDBCheck
{
  String jdbcClass;
  String jdbcURL;
  String jdbcUsername;
  String jdbcPassword;


  public RptrAppDBCheck(
	String jdbcClass,
	String jdbcURL,
	String jdbcUsername,
	String jdbcPassword )
  {
    this.jdbcClass = jdbcClass;
    this.jdbcURL = jdbcURL;
    this.jdbcUsername = jdbcUsername;
    this.jdbcPassword = jdbcPassword;
  }


  boolean checkTables( Connection db, List<String> tables ) throws SQLException
  {
    int numTables = tables.size();

    boolean[] tableExists = new boolean[ numTables ];

    int i;

    for (i=0; i < numTables; i++)
    {
      tableExists[i] = false;
    }

    DatabaseMetaData dbmd;
    dbmd = db.getMetaData();

    final String[] types = { "TABLE" };

    ResultSet rs = dbmd.getTables(null, "public", "%", types );

    while (rs.next())
    {
      String tableName = rs.getString(3);

      int pos = tables.indexOf(tableName);
      
      Dbg.println(Dbg.DBG1, "Table: " + tableName + "  " + pos);

      if (pos >= 0)
      {
	tableExists[pos] = true;
      }
    }

    boolean allTablesExist = true;

    for (i=0; i < numTables; i++)
    {
      if (tableExists[i] == false)
      {
	Dbg.println(Dbg.INFO, "DBCheck: '" + tables.get(i) +"' is missing");
	allTablesExist = false;
      }
    }

    rs.close();

    return allTablesExist;
  }

  boolean fixit(String[] neededTables)
  {
    try
    {
      Class.forName(jdbcClass);
    }
    catch (ClassNotFoundException e)
    {
      Dbg.println(Dbg.ERR, "DBCheck/ClassLoader: " + e);
      return false;
    }

    Connection db;

    try
    {
      db = DriverManager.getConnection(jdbcURL,
	    jdbcUsername, jdbcPassword);
    }
    catch (SQLException e)
    {
      Dbg.println(Dbg.ERR, "DBCheck/getConnection: " + e);
      return false;
    }


    try
    {
      int result;

      List<String> t = new ArrayList<String>();

      t.add("sync_mng");
      t.add("sync_gip");

      if (neededTables != null)
      {
	int i;
	for (i=0; i < neededTables.length; i++)
	{
	  t.add(neededTables[i]);
	}
      }

      if (!checkTables(db, t))
      {
	Dbg.println(Dbg.ERR, "DBCheck: tables missing");
	return false;
      }


      List<String> t2 = new ArrayList<String>();

      t2.add("ircddb_zonerp");

      if (!checkTables(db, t2))
      {
	Dbg.println(Dbg.INFO, "trying to create 'ircddb_zonerp'");

	Statement sql = db.createStatement();

	sql.executeUpdate(
	   "CREATE TABLE ircddb_zonerp ( " +
	   " arearp_cs character(8) NOT NULL, " +
	   " last_mod_time timestamp without time zone NOT NULL, " +
	   " zonerp_cs character(8) NOT NULL )" );

	sql.executeUpdate(
	   "ALTER TABLE ONLY ircddb_zonerp " +
	   " ADD CONSTRAINT pk_ircddb_zonerp PRIMARY KEY (arearp_cs)" );

      }

      t.addAll(t2);

      t2 = new ArrayList<String>();

      t2.add("sync_mng_external");

      if (!checkTables(db, t2))
      {
	Dbg.println(Dbg.INFO, "trying to create 'sync_mng_external'");

	Statement sql = db.createStatement();

	sql.executeUpdate(
	   "CREATE TABLE sync_mng_external ( " +
	   " target_cs character(8) NOT NULL, " +
	   " last_mod_time timestamp without time zone NOT NULL, " +
	   " arearp_cs character(8) NOT NULL, " +
	   " zonerp_cs character(8) NOT NULL )" );

	sql.executeUpdate(
	   "ALTER TABLE ONLY sync_mng_external " +
	   " ADD CONSTRAINT pk_sync_mng_external PRIMARY KEY (target_cs)" );

      }

      t.addAll(t2);

      if (!checkTables(db, t))
      {
	Dbg.println(Dbg.ERR, "DBCheck2: tables missing");
	return false;
      }
    }
    catch (SQLException e)
    {
      Dbg.println(Dbg.ERR, "DBCheck: " + e);
      return false;
    }

    try
    {
      db.close();
    }
    catch (SQLException e)
    {
      Dbg.println(Dbg.ERR, "DBClient/close: " + e);
      return false;
    }

    return true;
  }
}

