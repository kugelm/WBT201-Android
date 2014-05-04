package de.thgk.wbt20x.app;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Adapted for Android by thomas on 20.04.14.
 */
/*
 * WBT201.java
 *
 * Created on July 7, 2007, 8:45 PM
 * Copyright: 2007, 2008, Dirkjan Krijnders <dirkjan@krijnders.net>
 *
 * WBT201.java is part of GetWBT
 *
 * GetWBT is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GetWBT is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GetWBT.  If not, see <http://www.gnu.org/licenses/>.


 */

public class WBT201 {
    /**
     *
     * @author J.D. Krijnders
     */


    private static String TAG = "WBT201";
    
    private static final int LINE_DELIMITER = 10;
    private static final int BT201CHUNK = 4096;
    private static final int RECLEN_WBT201 = 16;

    private static final char TRACKMASK = 0x01;

    private String name = null;
    private String serialNumber = null;
    private Double hwVersion = null;
    private Double fmtVersion = null;
    private Double swVersion = null;
    private int trackCount = 0;
    private Vector tracks;
    private Track currentTrack = null;

    private int offset;
    private int lastTrackOffset = 0x0400;
    private byte[] lastDateChunk;
    private byte[] prevLastDateChunk;

    InputStream isr;
    OutputStream osr;
    boolean loggedIn;

    FileWriter fos = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    DateFormat fnamFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    Context context;

    /**
     * Creates a new instance of WBT201
     */
    public WBT201(InputStream aIsr, OutputStream aOsr, Context aContext) {
        isr = aIsr;
        osr = aOsr;
        context = aContext;
        loggedIn = false;
        tracks = new Vector();
        lastDateChunk = new byte[4];
        prevLastDateChunk = new byte[4];
        TimeZone tz = TimeZone.getTimeZone("UTC");
        dateFormat.setTimeZone(tz);
    }

    public boolean login() throws IOException {
        //getStringParam("@AL");
        String s = getStringParam("@AL");
        if (s.isEmpty()) return false;
        loggedIn = true;
        return true;
    }

    public void logout() throws IOException {
        sendCommand("@AL,2,1");
        loggedIn = false;
    }

    public String readLine(char prefix) throws IOException {
        StringBuffer output = new StringBuffer();

        // Read one line and try to parse it.
        int input;
        boolean state = false;
        while ((input = isr.read()) != LINE_DELIMITER) {
            if ((char) input == prefix)
                state = true;
            if (state) {
                output.append((char) input);

            }
            //Log.d(TAG, "isr.read = " + (char)input);
        }
        //Log.d(TAG, "Last read: " + input);

        return output.toString();
    }

    public boolean sendCommand(String cmd) throws IOException {
        if (loggedIn)
            while (isr.available()>0)
                isr.read();

        Log.d(TAG, "Going to send: " + cmd);
        osr.write(cmd.getBytes());
        osr.write((char) LINE_DELIMITER);
        //ƒosr.write(cmd + '\r' + '\n');
        return true;
    }

    public String getStringParam(String cmd) throws IOException {
        sendCommand(cmd);
        return readString(cmd);
    }

    public String readString(String cmd) throws IOException {
        boolean loop = true;
        int i = 0;
        String ret = new String();
        while (loop) {
            if (i++ > 20)
                return ret;
            ret = readLine('@');
            //Log.d(TAG, "readLine returned: " + ret);
            if (ret.startsWith(cmd)) {
                loop = false;
            } else {
                sendCommand(cmd);
            }
        }
        ret = ret.substring(cmd.length());
        if (ret.startsWith(","))
            ret = ret.substring(1);

        Log.d(TAG, "Device returned in readString: " + ret);
        return ret;
    }

    public Integer getIntParam(String cmd) throws IOException {
        String s = getStringParam(cmd);
        Integer ret = new Integer(Integer.parseInt(s));
        return ret;
    }

    public Double getDoubleParam(String cmd) throws IOException {
        Double d;
        try {
            d = Double.valueOf(getStringParam(cmd));
        } catch (NumberFormatException e) {
            d = new Double(0.0);
            e.printStackTrace();
        }
        return d;
    }

    private byte readWithTimeout(int timeout) throws IOException {
        byte ret = 0;
        if (isr.available()>0) {
            ret = (byte) isr.read();
        } else {
            try {
                int i;
                for (i = 0; i < 10; i++) {
                    Thread.sleep(timeout / 10);
                    if (isr.available()>0) {
                        ret = (byte) isr.read();
                        break;
                    }
                }
                if (i == 10)
                    throw new IOException();
            } catch (InterruptedException e) {

            }
        }
        return ret;
    }

    public byte[] getData(String cmd, int length) throws IOException {
        byte ret[] = new byte[length]; //length]; // = new char(length);
        sendCommand(cmd);
        try {
            int j;
            for (j = 0; j < 25; j++) {
                if (!(isr.available() > 0)) {
                    Thread.sleep(100);
                }
            }
            if (!(isr.available()>0)) {
                throw new IOException();
            }
            /**
            int readBytes = isr.read(ret, 0, length);

            if (readBytes!=length) {
                Log.d(TAG, "getData: unexpected num of bytes read");
                throw new IOException();
            }
             **/
            int readBytes = 0;
            while (readBytes < length) {
                byte b  = readWithTimeout(500);
                ret[readBytes] = b;
                readBytes += 1;
            }
            } catch (InterruptedException e) {
        }
        return ret;
    }

    public String calcChecksum(byte[] data, int len) {
        int checksum = 0;
        int i = 0;
        for (i = 0; i < len; i++) {
            checksum ^= (int) data[i];
            checksum &= 0xFF;
        }
        String CS = new String(Integer.toHexString(checksum));
        if (CS.length() == 1) // Fix zero padding
            CS = "0" + CS;
        return CS.toUpperCase(); // WBT201 returns uppercase checksums;
    }

    public int countTracks(byte[] data, int len) {
        int i, count = 0;

        for (i = 0; i < len; i = i + 16) {
            boolean track = (data[i] & TRACKMASK) != 0x00;
            if (track) {
                // If no current track, don't save it
                if (currentTrack != null) {
                    // Update track time and store track in vector
                    currentTrack.setStopDate(chunkDate(swapBytesToInt(prevLastDateChunk, 0)));
                    tracks.addElement(currentTrack);

                    if (fos != null) {
                        try {
                            fos.write("  </trkseg>\n");
                            fos.write(" </trk>\n");
                            fos.write("</gpx>\n");
                            fos.flush();
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                currentTrack = new Track(trackCount + count);
                currentTrack.setStartDate(chunkDate(swapBytesToInt(data, i + 2)));
                currentTrack.setNumberOfPoints(0);
                currentTrack.setOffset(offset + i + 1024);
                currentTrack.setDistance(0);
                currentTrack.setTime(0);
                count++;

                String sfnam = "track_" + fnamFormat.format(currentTrack.getStartDate()) + ".gpx";
                File fout = new File(ConnectionService.downloadDir.getPath(), sfnam);
                try {
                    fos = new FileWriter(fout, false);

                    fos.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n");
                    fos.write("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"WBT201.app\"\n");
                    fos.write("   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
                    fos.write("   xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");
                    fos.write(" <metadata></metadata>\n");
                    fos.write(" <trk>\n");
                    fos.write("  <trkseg>\n");

                } catch (IOException e) {
                    Log.d(TAG, "Can't write to sdcard");
                    fos = null;
                }

            }

            int latitude = swapBytesToInt(data, i + 6);
            int longitude = swapBytesToInt(data, i + 10);
            Date dateTime = chunkDate(swapBytesToInt(data, i + 2));
            double lat = (double) latitude / 10000000d;
            double lon = (double) longitude / 10000000d;
            int alt = swapBytesToShort(data, i + 14);
            currentTrack.addPoint(lat, lon);
            prevLastDateChunk = lastDateChunk;
            lastDateChunk[0] = data[i + 2];
            lastDateChunk[1] = data[i + 3];
            lastDateChunk[2] = data[i + 4];
            lastDateChunk[3] = data[i + 5];
            try {
                if (fos != null) {
                    fos.write("   <trkpt lat=\"" + lat + "\" lon=\"" + lon + "\">");
                    fos.write("<time>"+ dateFormat.format(dateTime) +"</time>");
                    fos.write("<ele>"+alt+"</ele>");
                    fos.write("</trkpt>\n");
                }
            } catch (IOException e) {
                Log.d(TAG, "Can't write to sdcard");
                fos = null;
            }
        }
        return count;
    }

    private int swapBytesToInt(byte[] data, int i) {
        int accum = 0;
        int shiftBy = 0;
        for (shiftBy = 0; shiftBy < 32; shiftBy += 8) {
            accum |= ((int)data[i] & 0xff) << shiftBy;
            i++;
        }
        return accum;
    }

    private short swapBytesToShort(byte[] data, int i) {
        short accum = 0;
        int shiftBy = 0;
        for (shiftBy = 0; shiftBy < 16; shiftBy += 8) {
            accum |= ((short)data[i] & 0xff) << shiftBy;
            i++;
        }
        return accum;
    }

    public Date chunkDate(int data) {
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        date.setTime(new Date());
        date.set(Calendar.SECOND, ((data) >> 0) & 0x3F);
        date.set(Calendar.MINUTE, ((data) >> 6) & 0x3F);
        date.set(Calendar.HOUR_OF_DAY, ((data) >> 12) & 0x1F);
        date.set(Calendar.YEAR, 2000 + (((data) >> 26) & 0x3F));
        date.set(Calendar.MONTH, (((data) >> 22) & 0x0F) - 1);
        date.set(Calendar.DATE, ((data) >> 17) & 0x1F);
        return date.getTime();

    }

    public String getName() throws IOException {
        if (name == null) {
            name = getStringParam("@AL,7,1");
        }
        return name;
    }

    public Double getHwVersion() throws IOException {
        if (hwVersion == null)
            hwVersion = getDoubleParam("@AL,8,1");
        return hwVersion;
    }

    public Double getSwVersion() throws IOException {
        if (swVersion == null)
            swVersion = getDoubleParam("@AL,8,2");
        Log.d(TAG, "Got software version: " + swVersion);
        return swVersion;
    }

    public Double getFmtVersion() throws IOException {
        if (fmtVersion == null)
            fmtVersion = getDoubleParam("@AL,8,3");
        return fmtVersion;
    }

    public String getSerialNumber() throws IOException {
        if (serialNumber == null)
            serialNumber = getStringParam("@AL,7,3");
        return serialNumber;
    }

    public int bytesToBeRead() throws IOException {
        Integer logStart = getIntParam("@AL,5,1");
        Integer logStop = getIntParam("@AL,5,2");
        return logStop.intValue() - logStart.intValue();
    }

    public int getCapacity() throws IOException {
        Integer addrStart = getIntParam("@AL,5,9");
        Integer addrStop = getIntParam("@AL,5,10");
        return addrStop.intValue() - addrStart.intValue();
    }

    public int getTrackCount() {
        if (trackCount == 0)
            Log.d(TAG, "Asked for track count before reading data");

        return trackCount;
    }

    public int getPointCount() throws IOException {
        return bytesToBeRead() / 16;
    }

    private void progress(float p) {
        //Log.d(TAG, "feedback = "+txt);
        Intent bc = new Intent(MainActivity.ACTION_PROGRESS);
        bc.putExtra(MainActivity.EXTRA_PERCENTAGE, p);
        //lbm.sendBroadcast(bc);
        context.sendBroadcast(bc);
    }


    public void readAndWrite() throws IOException {
        Integer logStart = getIntParam("@AL,05,01");
        Integer logStop = getIntParam("@AL,05,02");
        long startms = new Date().getTime();
        long diffms;
        trackCount = 0;
        Log.d(TAG, "Reading " + logStart + " to " + logStop);
        byte[] data = null;
        int want = logStop.intValue() - logStart.intValue();
        int totalWant = want;
        loggedIn = true;

        int numReads = want / BT201CHUNK;
        Log.d(TAG, "Going to read " + Integer.toString(numReads) + " blocks");
        offset = logStart.intValue();
        String CScalc;
        String CSphone;
        int retry = 0;
        while (want > BT201CHUNK) {
            data = getData("@AL,05,03," + Integer.toString(offset), BT201CHUNK);
            if (data.length == BT201CHUNK) {

                CScalc = calcChecksum(data, BT201CHUNK);
                //String tmp = readString("@AL,CS");
                //CSphone = tmp.substring(0, 2);
                String retLine = readLine('@');
                Log.d(TAG, "r/w: line after data chunk:" + retLine);
                String[] tmp = retLine.split(",");
                CSphone = tmp[2];
                Log.d(TAG, CScalc + "<=>" + CSphone);
                if (CSphone.equalsIgnoreCase(CScalc)) { //  || CSphone.equalsIgnoreCase("0" + CScalc)
                    // TODO: fos.write(new String(data).getBytes());
                    trackCount += countTracks(data, BT201CHUNK);
                    int percentage = (int) (1000f * offset / totalWant);
                    diffms = new Date().getTime() - startms;
                    long speed = (long) 10000f * offset / (1024 * diffms);


                    Log.d(TAG, "Read " + offset + "/" + totalWant + " bytes\n" +
                            percentage / 10f + "% @ " + (speed / 10f) + " kb/s");
                    progress(percentage / 10f);
                    want = want - BT201CHUNK;
                    offset = offset + BT201CHUNK;
                    retry = 0;
                }
            } else {
                retry = retry + 1;
                Log.d(TAG, "Retry # " + retry);
                if (retry > 11) {
                    Log.d(TAG, "Giving up");
                    break;
                }
            }
        }
        Log.d(TAG, "Reading remainder");
        data = getData("@AL,05,03," + Integer.toString(offset), want);
        CScalc = calcChecksum(data, want);
        String tmp = readString("@AL,CS");
        CSphone = tmp.substring(0, 2);
        Log.d(TAG, CScalc + "<=>" + CSphone);
        if (CSphone.equalsIgnoreCase(CScalc)) {
            Log.d(TAG, "Checksums matched");
            Log.d(TAG, "Copyping from " + 0 + " to offset, " + offset + ", length " + want);
            // TODO:
            // fos.write(new String(data).getBytes());
            trackCount += countTracks(data, want);
        }
        tracks.addElement(currentTrack);
        tracks.addElement(new Track(trackCount, offset + want + 1024, 1));
        if (fos != null) {
            try {
                fos.write("  </trkseg>\n");
                fos.write(" </trk>\n");
                fos.write("</gpx>\n");
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //trackCount++;

        int k;
        for (k = 0; k < trackCount; k++) {
            Track tr = (Track) tracks.elementAt(k);
            /*
            try {
                // TODO: tr.writeTrack(fos);
            } catch (IOException e) {
                Log.d(TAG, "Caught during writing track: " + e.getMessage());
            }
            */
        }
        Log.d(TAG, "Found " + trackCount + " tracks");
    }

    public void erase() throws IOException {
        sendCommand("@AL,5,6");
    }

    public int getConfigDynamicPlatform() throws IOException {
        return 1; //this.getIntParam("@AL");
    }

    public Vector getTracks() {
        if (trackCount == 0)
            Log.d(TAG, "Asked for track count before reading data");

        return tracks;
    }

}
