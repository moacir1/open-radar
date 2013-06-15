/**
 * Copyright (C) 2012 Wolfram Wagner
 *
 * This file is part of OpenRadar.
 *
 * OpenRadar is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenRadar is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenRadar. If not, see <http://www.gnu.org/licenses/>.
 *
 * Diese Datei ist Teil von OpenRadar.
 *
 * OpenRadar ist Freie Software: Sie können es unter den Bedingungen der GNU
 * General Public License, wie von der Free Software Foundation, Version 3 der
 * Lizenz oder (nach Ihrer Option) jeder späteren veröffentlichten Version,
 * weiterverbreiten und/oder modifizieren.
 *
 * OpenRadar wird in der Hoffnung, dass es nützlich sein wird, aber OHNE JEDE
 * GEWÄHELEISTUNG, bereitgestellt; sogar ohne die implizite Gewährleistung der
 * MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK. Siehe die GNU General
 * Public License für weitere Details.
 *
 * Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
 * Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 */
package de.knewcleus.openradar.weather;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.sun.istack.internal.logging.Logger;

import de.knewcleus.fgfs.location.GeoUtil;
import de.knewcleus.fgfs.navdata.impl.Aerodrome;
import de.knewcleus.openradar.gui.GuiMasterController;
import de.knewcleus.openradar.gui.SoundManager;
import de.knewcleus.openradar.gui.setup.AirportData;

/**
 * This class downloads the METAR information...
 *
 * http://weather.noaa.gov/pub/data/observations/metar/stations/KSFO.TXT
 *
 * @author Wolfram Wagner
 *
 */
public class MetarReader implements Runnable {

    private Thread thread = new Thread(this, "OpenRadar - Metar Reader");
    private String baseUrl = null;
    private List<String> activeWeatherStationList = new ArrayList<String>();
    private List<Aerodrome> weatherStationList = new ArrayList<Aerodrome>();
    private AirportData data = null;
    private volatile boolean isRunning = true;
    private volatile boolean reloadMetars = false;

    private Map<String,MetarData> metars = Collections.synchronizedMap(new HashMap<String,MetarData>());
    private int sleeptime = 5 * 60 * 1000;

    private final GuiMasterController master;

    private final Object metarsLock = new Object();

    private ZipFile zif=null;

    public MetarReader(GuiMasterController master) {
        this.master=master;

        this.data = master.getDataRegistry();

        this.baseUrl = data.getMetarUrl();
        thread.setDaemon(true);
    }

    public synchronized void changeMetarSources(String ownMetarSource, String addMetarSources) {
        // remove old
        synchronized(metarsLock) {
            metars.remove(master.getDataRegistry().getMetarSource()); // remove old
        }
        // add new
        if(ownMetarSource!=null) {
            master.getDataRegistry().setMetarSource(ownMetarSource);
            addWeatherStation(ownMetarSource);
        }
        if(addMetarSources!=null && !addMetarSources.trim().isEmpty()) {
            // remove old
            String oldMetarSources = master.getDataRegistry().getAddMetarSources();
            if(oldMetarSources!=null) {
                StringTokenizer st = new StringTokenizer(oldMetarSources,",");
                synchronized(metarsLock) {
                    while(st.hasMoreTokens()) {
                        metars.remove(st.nextToken());
                    }
                }
            }
            // add new
            addMetarSources.trim();
            master.getDataRegistry().setAddMetarSources(addMetarSources);
            StringTokenizer st = new StringTokenizer(addMetarSources,",");
            while(st.hasMoreTokens()) {
                addWeatherStation(st.nextToken());
            }
        }
        thread.interrupt();
    }

    public synchronized void addWeatherStation(String code) {
        activeWeatherStationList.add(code);
    }

    public synchronized void removeWeatherStation(String code) {
        activeWeatherStationList.remove(code);
    }


    private List<IMetarListener> listener = new ArrayList<IMetarListener>();

    public synchronized void addMetarListener(IMetarListener l) {
        listener.add(l);
    }

    public synchronized void removeMetarListener(IMetarListener l) {
        listener.remove(l);
    }

    /**
     * Starts the metar loader after the first metar was loaded. This should prevent problems with
     * arriving metar in initial screen setup.
     * So this method returns after Metar is loaded.
     */
    public synchronized void start() {
        try {
            synchronized(this) {
                for(String code : activeWeatherStationList) {
                    // if there was no check, if the weather station exist, an "_" is set as prefix.
                    // the search for closest weather station cannot be done here, because the airports are not read yet
                    // so we try to load it, if it fails, it will come with the start of the application
                    if(code.startsWith("_")) {
                        code = code.substring(1);
                        master.getDataRegistry().setMetarSource(code);
                    }

                    loadMetar(code,false); // not error logging at this point...
                    if(!metars.get(code).exists()) {
                        // we need to search for another weather station
                        reloadMetars = true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        thread.start();
    }

    public synchronized void stop() {
        isRunning = false;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                if(reloadMetars) {
                    reloadMetars=false;
                } else {
                    Thread.sleep(sleeptime);
                }
            } catch (InterruptedException e) {
            }
            synchronized(this) {
                metars.clear();
                for(String code : new ArrayList<String>(activeWeatherStationList)) {
                    if(code.startsWith("_")) {
                        activeWeatherStationList.remove(code);
                        code = getClosestWeatherStation(master.getDataRegistry().getAirportPosition()).getIdentification();
                        master.getDataRegistry().setMetarSource(code);
                        if(code==null) {
                            code = master.getDataRegistry().getAirportCode();
                        }
                        activeWeatherStationList.add(0,code);
                    }
                    try {
                        loadMetar(code, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private void loadMetar(String code, boolean logError) throws IOException, MalformedURLException {
        StringBuilder result = new StringBuilder();
        String line = null;
        URL url = new URL(baseUrl + code.toUpperCase() + ".TXT");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            line = reader.readLine();
            while ( line != null) {
                result.append(line).append("\n");
                line = reader.readLine();
            }
            reader.close();

            MetarData metar = new MetarData(data, result.toString());
            MetarData lastMetar = metars.get(code);

            if(lastMetar==null || !lastMetar.equals(metar)) {
                metars.put(code, metar);
                for (IMetarListener l : listener) {
                    l.registerNewMetar(metar);
                }
                System.out.println("Metar received: " + metar.getMetarBaseData());

                SoundManager.playWeather();
            }
        } else {
            if(logError) {
                System.out.println("WARNING: No Metar for "+code+"(got response code " + responseCode + " from " + url.toString()+")...");
                System.out.println("Set alternative weather station via dialog!");
            }
            MetarData metar = null;
            metar = MetarData.createNotFoundMetar(code);
            metars.put(code, metar);

            for (IMetarListener l : listener) {
                l.registerNewMetar(metar);
            }
        }
    }

    public MetarData getMetar(String code) {
        MetarData metar = null;
        synchronized(metarsLock) {
            metar = metars.get(code);
            if(metar==null) {
                metar = MetarData.createNotFoundMetar(code);
                metars.put(code, metar);
            }
        }
        return metar;
    }
    /**
     * This method loads metar.txt and stores all airports with a metar record at the server in the weather station list.
     *
     * @param aerodromes
     */
    public void retrieveWeatherStations(List<Aerodrome> aerodromes) {
        HashMap<String,Aerodrome> airportsInRange = new HashMap<String,Aerodrome>();
        for(Aerodrome a : aerodromes) {
            airportsInRange.put(a.getIdentification(), a);
        }

        try {
            // read file
            BufferedReader r = new BufferedReader(openMetarDat());

            String line = r.readLine();
            while(line!=null && !line.trim().isEmpty()) {
                if(line.contains("#")) {
                    line = line.substring(0,line.indexOf("#"));
                }
                line.trim();
                if(line.isEmpty()) {
                    line = r.readLine();
                    continue; // empty or comment line
                }

                String key = line.trim();
                if(airportsInRange.containsKey(key)) {
                    weatherStationList.add(airportsInRange.get(key));
                }
                line = r.readLine();
            }
        } catch(Exception e) {
            Logger.getLogger(MetarReader.class).severe("Problem to read metar.dat in metar.dat..zip",e);
        } finally {
            closeFiles();
        }

    }

    public Aerodrome getClosestWeatherStation(Point2D point) {
        Aerodrome result = null;
        double distance = Double.MAX_VALUE;
        for(Aerodrome a : weatherStationList) {
            Point2D aPos = a.getGeographicPosition();
            double currentDistance = GeoUtil.getDistance(aPos.getX(), aPos.getY(),point.getX(), point.getY()).length;
            if(currentDistance<distance) {
                result = a;
                distance = currentDistance;
            }
        }
        return result;
    }

    protected InputStreamReader openMetarDat() throws IOException {
        final File inputFile = new File("data/metar.dat.zip");
        zif = new ZipFile(inputFile);
        Enumeration<? extends ZipEntry> entries = zif.entries();
        while(entries.hasMoreElements()) {
            ZipEntry zipentry = entries.nextElement();
            if(zipentry.getName().equals("metar.dat")) {
                return new InputStreamReader(zif.getInputStream(zipentry));
            }
        }
        throw new IllegalStateException("apt.dat not found in sectors/AtpNav.zip!");
    }
    private void closeFiles() {
        if (zif!=null)
            try {
                zif.close();
            } catch (IOException e) {}
    }

}
