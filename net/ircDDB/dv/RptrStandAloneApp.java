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

import java.util.Properties;
import java.util.regex.Pattern;
import java.util.Date;
import net.ircDDB.IRCApplication;
import net.ircDDB.IRCMessage;
import net.ircDDB.IRCMessageQueue;
import net.ircDDB.IRCDDBExtApp;

import net.ircDDB.Dbg;



public class RptrStandAloneApp extends RptrApp
{

  int udpPort;
	
  public void setParams (Properties p, Pattern k, Pattern v, net.ircDDB.IRCDDBEntryValidator validator)
  {
    super.setParams(p, k, v, validator);

    insertUsers = p.getProperty("rptr_insert_users", "no").equals("yes");

    if ( !p.getProperty("version", "multihomed").equals("standalone") )
    {
      Dbg.println(Dbg.ERR, "'version' property not correct - will not insert users");
      insertUsers = false;
    }

    udpPort = Integer.parseInt(p.getProperty("mheard_udp_port", "12346"));
  }	  

  public RptrStandAloneApp()
  {
    super();

    udpPort = 12456;
  }	


  void mheardCall(String targetCS, char repeaterModule)
  {
    String areaCS = repeaterCall + repeaterModule;

    if ( !targetCS.substring(0,7).equals(areaCS.substring(0,7)) &&
       isValidCallSign(targetCS, targetPattern, 3, 7) &&
       isValidCallSign(areaCS, areaPattern, 4, 6) &&
	 (currentServerNick != null))
    {
      IRCMessage m = new IRCMessage();
      m.command = "PRIVMSG";
      m.numParams = 2;
      m.params[0] = currentServerNick;
      m.params[1] = "UPDATE " + dbDateFormat.format(new Date())
	 + " " + targetCS.replace(' ', '_') + " " +  areaCS.replace(' ', '_');

      IRCMessageQueue q = getSendQ();

      if (q != null)
      {
        q.putMessage(m);
      }
    }

  }

  public void run()
  {
    RptrUDPReceiver u = new RptrUDPReceiver(this, udpPort);

    Thread t = new Thread(u);

    t.start();

    super.run();
  }

}

