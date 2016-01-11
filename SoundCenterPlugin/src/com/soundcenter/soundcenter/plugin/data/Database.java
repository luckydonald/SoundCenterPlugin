package com.soundcenter.soundcenter.plugin.data;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.soundcenter.soundcenter.lib.data.Area;
import com.soundcenter.soundcenter.lib.data.Box;
import com.soundcenter.soundcenter.lib.data.GlobalConstants;
import com.soundcenter.soundcenter.lib.data.Song;
import com.soundcenter.soundcenter.lib.data.Station;
import com.soundcenter.soundcenter.lib.util.FileOperation;
import com.soundcenter.soundcenter.plugin.SoundCenter;

public class Database implements Serializable {

	private static final long serialVersionUID = 4L;

	public ConcurrentHashMap<Short, Station> boxes = new ConcurrentHashMap<Short, Station>();
	public ConcurrentHashMap<Short, Station> areas = new ConcurrentHashMap<Short, Station>();
	public ConcurrentHashMap<Short, Station> biomes = new ConcurrentHashMap<Short, Station>();
	public ConcurrentHashMap<Short, Station> worlds = new ConcurrentHashMap<Short, Station>();
	public ConcurrentHashMap<Short, Station> wgRegions = new ConcurrentHashMap<Short, Station>();
	public ConcurrentHashMap<String, Song> songs = new ConcurrentHashMap<String, Song>();
	private ConcurrentHashMap<String, Integer> boxCounts = new ConcurrentHashMap<String, Integer>();
	private ConcurrentHashMap<String, Integer> areaCounts = new ConcurrentHashMap<String, Integer>();

	public void addStation(byte type, Station station) {
		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			map.put(station.getId(), station);
			incrementCounter(station);
		}
		saveToDisk();
	}

	public Station getStation(byte type, short id) {
		Station station = null;
		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			station = (Station) map.get(id);
		}

		return station;
	}

	public Station getStationByName(byte type, String name) {
		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			for (Entry<Short, Station> entry : map.entrySet()) {
				Station station = entry.getValue();
				if (station.getName().equals(name)) {
					return station;
				}
			}
		}

		return null;
	}

	public int getStationCount(byte type) {
		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			return map.size();
		}

		return 0;
	}

	public int getStationCount(byte type, String player) {
		int count = 0;
		ConcurrentHashMap<Short, Station> map = getMap(type);

		if (map != null) {
			for (Entry<Short, Station> entry : map.entrySet()) {
				Station station = entry.getValue();
				if (station.getOwner().equals(player)) {
					count++;
				}
			}
		}

		return count;
	}

	public Station removeStation(byte type, short id) {
		Station station = null;

		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			station = map.remove(id);
			if (station != null) {
				decrementCounter(station);
			}
		}

		saveToDisk();
		
		return station;
	}

	public void removeStation(Station station) {
		removeStation(station.getType(), station.getId());
	}

	public short getAvailableId(byte type) {
		ConcurrentHashMap<Short, Station> map = getMap(type);
		if (map != null) {
			Random rand = new Random();
			short id = (short) (rand.nextInt(Short.MAX_VALUE + Math.abs(Short.MIN_VALUE)) - Math.abs(Short.MIN_VALUE));
			if (map.get(id) == null && id != 0) {
				return id;
			} else
				return getAvailableId(type);
		} else
			return 0;
	}
	
	public Song getSong(String title) {
		return songs.get(title);
	}

	public void addSong(Song song) {
		songs.put(song.getTitle(), song);
		saveToDisk();
	}
	
	public void removeSong(Song song) {
		
		songs.remove(song.getTitle());
		
		for (Entry<Short, Station> entry : areas.entrySet()) {
			entry.getValue().removeSong(song);
		}
		for (Entry<Short, Station> entry : boxes.entrySet()) {
			entry.getValue().removeSong(song);
		}
		for (Entry<Short, Station> entry : biomes.entrySet()) {
			entry.getValue().removeSong(song);
		}
		for (Entry<Short, Station> entry : worlds.entrySet()) {
			entry.getValue().removeSong(song);
		}
		for (Entry<Short, Station> entry : wgRegions.entrySet()) {
			entry.getValue().removeSong(song);
		}
		
		saveToDisk();
	}

	private void incrementCounter(Station station) {
		ConcurrentHashMap<String, Integer> map = null;

		if (station instanceof Area) {
			map = areaCounts;
		} else if (station instanceof Box) {
			map = boxCounts;
		}

		if (map != null) {
			String owner = station.getOwner();
			int count = 0;
			if (map.containsKey(owner)) {
				count = map.get(owner);
			}
			map.put(owner, ++count);
		}
	}

	private void decrementCounter(Station station) {
		ConcurrentHashMap<String, Integer> map = null;

		if (station instanceof Area) {
			map = areaCounts;
		} else if (station instanceof Box) {
			map = boxCounts;
		}

		if (map != null) {
			String owner = station.getOwner();
			int count = 0;
			if (map.containsKey(owner)) {
				count = map.get(owner);
			}
			if (count > 0) {
				map.put(owner, --count);
			}
		}
	}

	private ConcurrentHashMap<Short, Station> getMap(byte type) {
		switch (type) {
		case GlobalConstants.TYPE_AREA:
			return areas;
		case GlobalConstants.TYPE_BOX:
			return boxes;
		case GlobalConstants.TYPE_BIOME:
			return biomes;
		case GlobalConstants.TYPE_WORLD:
			return worlds;
		case GlobalConstants.TYPE_WGREGION:
			return wgRegions;
		default:
			return null;
		}
	}
	
	private void saveToDisk() {
		try {
			FileOperation.saveObject(SoundCenter.dataFile, this);
		} catch (IOException e) {
			SoundCenter.logger.w("Error while saving data.", e);
		}
	}
	
	/* set empty hashmaps for maps that fail to load */
	public void readObject(ObjectInputStream stream) throws InvalidClassException, ClassNotFoundException, IOException {
		stream.defaultReadObject();
		
		if (areas == null)
			areas = new ConcurrentHashMap<Short, Station>();
		if (boxes == null)
			boxes = new ConcurrentHashMap<Short, Station>();
		if (biomes == null)
			biomes = new ConcurrentHashMap<Short, Station>();
		if (worlds == null)
			worlds = new ConcurrentHashMap<Short, Station>();
		if (wgRegions == null)
			wgRegions = new ConcurrentHashMap<Short, Station>();
		if (songs == null)
			songs = new ConcurrentHashMap<String, Song>();
		if (boxCounts == null)
			boxCounts = new ConcurrentHashMap<String, Integer>();
		if (areaCounts == null)
			areaCounts = new ConcurrentHashMap<String, Integer>();
	}
}
