package com.iiiP.billboardtube;

/**
 * @author zhanghao
 * 
 */
public class MyRecord {
	private int id;
	private String label;
	private String artist;
	private String imageUrl;

	public MyRecord(int id, String label, String artist, String imageUrl) {
		super();
		this.id = id;
		this.label = label;
		this.artist = artist;
		this.imageUrl = imageUrl;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

}
