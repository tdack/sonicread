/*
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Remco den Breeje, <stacium@gmail.com>
 */

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.Locale;


class SonicRead {
  public static void main(String[] args) throws IOException {
    Scanner s = null;
    int val;
    
    try { 
      int c, i = 0;
      int[] data = new int[8*1024];
      CreateHsr hsr = new CreateHsr();
      SonicLink sonic = new SonicLink();
      CaptureAudio audio = new CaptureAudio();
      audio.Start();
      
      System.out.format("Listening for SonicLink data\n");
      while(audio.ReadSample()) 
      {
	if((val = sonic.decode(audio.GetSample())) == -2)
	{
	  System.out.format("HALTED - Error while decoding data\n");
	  break;
	}

	/* Got byte? */
	if(val >= 0)
	{
	  if(!hsr.AddData(val))
	  {
	    System.out.format("HALTED - Error while checking data\n");
	    break;
	  }
	}

	if(hsr.IsDone())
	{
	  System.out.format("\nDisplaying raw data:\n\n");
	  hsr.WriteHsr();
	  break;
	}
      }
    }
    catch (Exception e) {
      System.exit(0);
    }

  }
}

