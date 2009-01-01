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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.sound.sampled.*;

public class CaptureAudio {
  private AudioFormat audioFormat;
  private TargetDataLine targetDataLine;
  private byte[] buf;
  private int bufwix;
  private int bufrix;

  public CaptureAudio(){
    try{
      buf = new byte[10000];
      bufwix = bufrix = 0;
      audioFormat = getAudioFormat();
      DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
      targetDataLine = (TargetDataLine)AudioSystem.getLine(dataLineInfo);
      targetDataLine.open(audioFormat);

    }catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  private AudioFormat getAudioFormat(){
    float sampleRate = 44100.0F;	//8000,11025,16000,22050,44100
    int sampleSizeInBits = 16;
    int channels = 1;
    boolean signed = true;
    boolean bigEndian = false;
    return new AudioFormat(sampleRate,
                           sampleSizeInBits,
                           channels,
                           signed,
                           bigEndian);
  }

  public void Start() {
    targetDataLine.start();
  }

  public boolean ReadSample() {
    int tmp;
    if((bufwix % 2) == 1)
      System.out.format("oneven\n");
    if(bufwix > 0 && (bufrix < bufwix))
      return true;
    bufwix = targetDataLine.read(buf, 0, buf.length);
    bufrix = 0;
    return(bufwix > 0);
  }

  public short GetSample() {
    short val = 0;
    if(bufrix < bufwix)
    {
      val = (short)((short)buf[bufrix + 1] << 8);
      val += (short)buf[bufrix];
      bufrix += 2;
    }
    return(val);
  }
}
