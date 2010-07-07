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

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

import net.ircDDB.Dbg;


public class RptrUDPReceiver implements Runnable
{

  RptrStandAloneApp app;

  public RptrUDPReceiver ( RptrStandAloneApp n )
  {
    app = n;
  }

  public void run()
  {
    byte localhostAddr[] = { 127, 0, 0, 1 };
    byte data[] = new byte[20];

    while(true)
    {


      try
      {
	DatagramSocket s = new DatagramSocket(12346, InetAddress.getByAddress(localhostAddr));
	s.setReuseAddress(true);

	for (int count = 0; count < 10000; count++)
	{
	  DatagramPacket p = new DatagramPacket(data, data.length);

	  s.receive(p);

	  // System.out.println(p.getLength() + " " + p.getOffset());

	  if ((p.getLength() == 11) && (p.getOffset() == 0) &&
	    (data[8] == 0) && (data[10] == 0) && ("ABC".indexOf(data[9]) >= 0))
	  {
	    String cs = new String(data, 0, 8);
	    // System.out.println("(" + cs + ")");

	    if (app != null)
	    {
	      app.mheardCall(cs, (char) data[9]);
	    }
	  }

	}


	s.close();

      }
      catch ( Exception e )
      {
	Dbg.println(Dbg.WARN, "RptrUDPReceiver: " +e);
      }
      
      
      try
      {
	Thread.sleep(1000);
      }
      catch ( InterruptedException e )
      {
	Dbg.println(Dbg.WARN, "RptrUDPReceiver/sleep: " +e);
      }

    }
  }

}

