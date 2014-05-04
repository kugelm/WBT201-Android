package de.thgk.wbt20x.app;

import android.util.Log;

import java.util.Date;

/**
 * Adapted for Android by thomas on 20.04.14.
 */

/*
 * Track.java
 *
 * Created on January 3, 2008, 2:26 PM
 * Copyright: 2008, Dirkjan Krijnders <dirkjan@krijnders.net>
 *
 * track.java is part of GetWBT
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

public class Track {

    private static String TAG = "TRACK";

    public int trackNumber = 0;
    public int offset = 0;
    private int numberOfPoints = 0;
    public int time = 0;
    public double distance = 0;
    private double lastLatitude = 0;
    private double lastLongitude = 0;
    private boolean firstPoint;
    private Date startDate;
    private Date stopDate;

    /** Creates a new instance of track */
    public Track() {
        firstPoint = true;
    }

    public Track(int number) {
        firstPoint = true;
        trackNumber = number;
    }

    public Track(int number, int offset, int numberOfPoints) {
        firstPoint = true;
        trackNumber = number;
        this.offset = offset;
        this.numberOfPoints = numberOfPoints;
    }

    public int numberOfPoints() {
        return this.numberOfPoints;
    }

    public void setNumberOfPoints(int np) {
        this.numberOfPoints = np;
        Log.d(TAG, "Number of points for track " + this.trackNumber + " is " + this.numberOfPoints);
    }

    public void setOffset(int offset) {
        this.offset = offset;
        Log.d(TAG, "Offset for track " + this.trackNumber + " is " + this.offset);
    }

    public double getDistance() {
        return distance;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setStartDate(Date startDate) {
        Log.d(TAG, "StartDate: " + startDate);
        this.startDate = startDate;
        setStopDate(startDate); // Make sure a stopdate is set...
    }

    public void setStopDate(Date stopDate) {
        Log.d(TAG, "Stopdate: " + stopDate);
        this.time = (int) ((stopDate.getTime()/1000) - (startDate.getTime()/1000));
        Log.d(TAG, "Diff in seconds is " + this.time);
        this.stopDate = stopDate;
    }

    public void addPoint(double latitude, double longitude) {
        latitude = Math.toRadians(latitude); // / 180 * 3.1415;
        longitude = Math.toRadians(longitude); // / 180 * 3.1415;
        if (!firstPoint) {
            distance += calcDistance(latitude, longitude, lastLatitude, lastLongitude);
        } else {
            firstPoint = false;
//            logger.message(4, "First point: " + latitude + "," + longitude );
        }
        numberOfPoints++;
        lastLatitude = latitude;
        lastLongitude = longitude;
    }

    private double calcDistance(double latitude, double longitude, double lastLatitude, double lastLongitude) {
        double R = 6371d;
        double dLat = lastLatitude - latitude;
        double dLon = lastLongitude - longitude;

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(latitude) * Math.cos(lastLatitude) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

}
