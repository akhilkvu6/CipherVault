package com.ciphervault.app;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class FileMetadataDTO implements Serializable {

    @SerializedName("cameraMake")
    private String cameraMake;

    @SerializedName("cameraModel")
    private String cameraModel;

    @SerializedName("lens")
    private String lens;

    @SerializedName("focalLength")
    private String focalLength;

    @SerializedName("iso")
    private String iso;

    @SerializedName("exposureTime")
    private String exposureTime;

    @SerializedName("fNumber")
    private String fNumber;

    @SerializedName("dateTaken")
    private String dateTaken;

    @SerializedName("width")
    private Integer width;

    @SerializedName("height")
    private Integer height;

    @SerializedName("resolution")
    private String resolution;

    @SerializedName("duration")
    private String duration;

    @SerializedName("videoCodec")
    private String videoCodec;

    @SerializedName("audioCodec")
    private String audioCodec;

    @SerializedName("frameRate")
    private String frameRate;

    @SerializedName("bitrate")
    private String bitrate;

    @SerializedName("title")
    private String title;

    @SerializedName("artist")
    private String artist;

    @SerializedName("album")
    private String album;

    @SerializedName("genre")
    private String genre;

    @SerializedName("releaseYear")
    private String releaseYear;

    @SerializedName("author")
    private String author;

    @SerializedName("creator")
    private String creator;

    @SerializedName("subject")
    private String subject;

    @SerializedName("keywords")
    private String keywords;

    @SerializedName("docCreatedDate")
    private String docCreatedDate;

    @SerializedName("docModifiedDate")
    private String docModifiedDate;

    public FileMetadataDTO() {}

    public String getCameraMake() { return cameraMake; }
    public String getCameraModel() { return cameraModel; }
    public String getLens() { return lens; }
    public String getFocalLength() { return focalLength; }
    public String getIso() { return iso; }
    public String getExposureTime() { return exposureTime; }
    public String getFNumber() { return fNumber; }
    public String getDateTaken() { return dateTaken; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public String getResolution() { return resolution; }
    public String getDuration() { return duration; }
    public String getVideoCodec() { return videoCodec; }
    public String getAudioCodec() { return audioCodec; }
    public String getFrameRate() { return frameRate; }
    public String getBitrate() { return bitrate; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getGenre() { return genre; }
    public String getReleaseYear() { return releaseYear; }
    public String getAuthor() { return author; }
    public String getCreator() { return creator; }
    public String getSubject() { return subject; }
    public String getKeywords() { return keywords; }
    public String getDocCreatedDate() { return docCreatedDate; }
    public String getDocModifiedDate() { return docModifiedDate; }
}
