/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * This code is based on the work of Tomás Oliveira e Silva
 * http://www.ieeta.pt/~tos/software/polar_s410.html
 * 
 * Remco den Breeje, <stacium@gmail.com>
 */

package sonicread;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 *
 * @author Remco den Breeje
 */
public class CreateHsr {
  
  public enum Series { S, F };
    
  public int ix;        /* byte index */
  public int is;        /* Section index */
  public int iss;       /* Section size index */
  public int section_start;
  public int section_number;
  public int totalByteCnt;
  public int[] hsrData;
  private int crc;
  private String monitorType;
  private Series monitorSerie;

  public CreateHsr()
  {
    hsrData = new int[8*1024];
    crc = ix = is = iss = 0;
    section_start = 8;
    section_number = 1;
    totalByteCnt = 0;
    monitorType = "";
    monitorSerie = Series.S;
  }

  public int[] getHsrData() {
      if(totalByteCnt == 0)
          return null;
      int[] rval = new int[totalByteCnt];
      System.arraycopy(hsrData, 0, rval, 0, totalByteCnt);
      return rval;
  }
  
  public boolean IsDone()
  {
    return(totalByteCnt > 0 && (ix == totalByteCnt));
  }
  
  public boolean IsStarted()
  {
    return(totalByteCnt > 0);
  }
  
  public int GetProgress()
  {
      int rval = 0;
      if(totalByteCnt > 0)
      {
          rval = 100 * ix / totalByteCnt;
      }
      return rval;
  }
  
  public int GetNumberOfBytes()
  {
      return totalByteCnt;
  }
  
  public String GetMonitorType()
  {
      return monitorType;
  }
  
  public void setMonitorSerie(Series monitorSerie) {
      this.monitorSerie = monitorSerie;
  }

  public Series getMonitorSerie() {
      return monitorSerie;
  }
  public boolean isFSerie() { return getMonitorSerie() == Series.F; }
  public boolean isSSerie() { return getMonitorSerie() == Series.S; }
  
  public boolean AddData(int b) throws Exception
  {
    if(b >= 0)
    {
      hsrData[ix++] = b;
      crc16(hsrData[ix - 1]);
      if(ix == 1 && hsrData[0] != 85)
      {
        throw new Exception(String.format("Bad first section header (byte 1 (%d))", hsrData[0]));
      }
      if(ix == 2)
      {
        switch(hsrData[1]) {
            case 10:
                this.monitorType = "S410";
                setMonitorSerie(Series.S);
                break;
            case 81:
                this.monitorType = "S510";
                setMonitorSerie(Series.S);
                break;
            case 1:
                this.monitorType = "F6";
                setMonitorSerie(Series.F);
            default:
                this.monitorType = String.format("Unkown (0x%X)", hsrData[1]);
                break;
        }
      }
      if(ix == 3 && hsrData[2] != (isSSerie() ? 1 : 2))
      {
        throw new Exception(String.format("Bad first section header (byte 3 (%d))", hsrData[2]));
      }
      /* looking for end of section header */
      if(ix == 8)
      {
        if(crc > 0)
        {
          throw new Exception("First section CRC error");
        }

        /* calculate total number of bytes to be received (8=section header) */
        totalByteCnt = 8 + 5 * hsrData[3] + hsrData[4] + 256 * hsrData[5] + 1;
        if(1==1)
            throw new Exception(String.format("end %d", totalByteCnt));
      }
      if(ix <= 8) /* got all info we need for the first 8 bytes */
        return true;
      if(ix == section_start + 3) /* new section (+3=header size) */
      {
        if(hsrData[section_start] != 85 ||
            hsrData[section_start + 1 ] != section_number ||
            hsrData[section_start + 2 ] < 1 ||
            hsrData[section_start + 2 ] > (isSSerie() ? 60 : 99))
        {
          throw new Exception(String.format("Bad section header, section_number: %d\n", section_number));
        }
        if(section_number == 1) { /* tell user, we started listening */
          System.out.format("Section %d started at %d\n", section_number, ix);
        }
      }
      if(crc == 0) 
          System.out.format("CRC ok at %d\n", ix);
      /* looking for end of section */
      if(ix >= section_start + 3 && ix == section_start + hsrData[section_start + 2] + 3 + 2)
      {
        System.out.format("Section %d ended at %d\n", section_number, ix);
        if(crc > 0) 
        {
          throw new Exception("Section CRC error");
        }
        /* update section start with new index */
        section_start = ix;
        section_number++;
      }
      /* looking for final byte */
      if(ix == totalByteCnt)
      {
        if(hsrData[ix - 1] != 7)
        {
          throw new Exception("Bad trailer byte");
        }
        if(section_start != ix - 1 || section_number != hsrData[3] + 1)
        {
          throw new Exception("Bad section structure");
        }
      }
    }
    return true;
  }
  
  @Override
  public String toString()
  {
      StringBuilder sBuilder = new StringBuilder ();

      sBuilder.append (CreateHsr.class.getName () + ":\n");
      sBuilder.append (" [totalByteCnt=" + this.totalByteCnt + "\n");
      sBuilder.append ("  monitorType=" + this.monitorType + "]\n");

      return sBuilder.toString ();
  }
  
  public void WriteHsr(File file) throws Exception
  {
    int i;
    try {
      // todo, make filename dynamic dynamic
      DataOutputStream os = new DataOutputStream(new FileOutputStream(file));
      os.writeByte((byte)(totalByteCnt + 2));
      os.writeByte((byte)((totalByteCnt + 2) / 255));
      for(i = 0;i < totalByteCnt;i++)
      {
        os.writeByte(hsrData[i]);
      }
      os.close();
    }
    catch (Exception e) {
      throw new Exception ("Error writing hsr");
    }
  }

  public int crc16(int data)
  {
    int i;
    for(i=7; i >= 0; i--)
    {
      crc <<= 1;
      if((data & (1 << i)) > 0)
        crc ^= 1;
      if((crc & 0x10000) > 0)
        crc ^= 0x18005;
    }
    return crc;
  }
}
