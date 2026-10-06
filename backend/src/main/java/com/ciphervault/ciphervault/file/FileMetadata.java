package com.ciphervault.ciphervault.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "file_metadata", indexes = {
        @Index(name = "idx_meta_make", columnList = "camera_make"),
        @Index(name = "idx_meta_model", columnList = "camera_model"),
        @Index(name = "idx_meta_resolution", columnList = "resolution"),
        @Index(name = "idx_meta_vcodec", columnList = "video_codec"),
        @Index(name = "idx_meta_artist", columnList = "artist"),
        @Index(name = "idx_meta_album", columnList = "album"),
        @Index(name = "idx_meta_genre", columnList = "genre"),
        @Index(name = "idx_meta_author", columnList = "author"),
        @Index(name = "idx_meta_title", columnList = "title")
})
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false, unique = true)
    @JsonIgnore
    private StoredFile file;

    // Image / Camera Attributes
    @Column(name = "camera_make")
    private String cameraMake;

    @Column(name = "camera_model")
    private String cameraModel;

    @Column(name = "lens")
    private String lens;

    @Column(name = "focal_length")
    private String focalLength;

    @Column(name = "iso")
    private String iso;

    @Column(name = "exposure_time")
    private String exposureTime;

    @Column(name = "f_number")
    private String fNumber;

    @Column(name = "date_taken")
    private String dateTaken;

    // Dimensions & Resolution
    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "resolution")
    private String resolution;

    // Video / Audio Attributes
    @Column(name = "duration")
    private String duration;

    @Column(name = "video_codec")
    private String videoCodec;

    @Column(name = "audio_codec")
    private String audioCodec;

    @Column(name = "frame_rate")
    private String frameRate;

    @Column(name = "bitrate")
    private String bitrate;

    // Audio Metadata
    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "artist")
    private String artist;

    @Column(name = "album")
    private String album;

    @Column(name = "genre")
    private String genre;

    @Column(name = "release_year")
    private String releaseYear;

    // Document / PDF Metadata
    @Column(name = "author", length = 500)
    private String author;

    @Column(name = "creator", columnDefinition = "TEXT")
    private String creator;

    @Column(name = "subject", columnDefinition = "TEXT")
    private String subject;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "doc_created_date")
    private String docCreatedDate;

    @Column(name = "doc_modified_date")
    private String docModifiedDate;

    @Lob
    @Column(name = "raw_metadata_json", columnDefinition = "LONGTEXT")
    private String rawMetadataJson;

    public FileMetadata() {
    }

    public Long getId() {
        return id;
    }

    public void setFile(StoredFile file) {
        this.file = file;
    }

    public String getCameraMake() {
        return cameraMake;
    }

    public void setCameraMake(String cameraMake) {
        this.cameraMake = cameraMake;
    }

    public String getCameraModel() {
        return cameraModel;
    }

    public void setCameraModel(String cameraModel) {
        this.cameraModel = cameraModel;
    }

    public String getLens() {
        return lens;
    }

    public void setLens(String lens) {
        this.lens = lens;
    }

    public String getFocalLength() {
        return focalLength;
    }

    public void setFocalLength(String focalLength) {
        this.focalLength = focalLength;
    }

    public String getIso() {
        return iso;
    }

    public void setIso(String iso) {
        this.iso = iso;
    }

    public String getExposureTime() {
        return exposureTime;
    }

    public void setExposureTime(String exposureTime) {
        this.exposureTime = exposureTime;
    }

    public String getFNumber() {
        return fNumber;
    }

    public void setFNumber(String fNumber) {
        this.fNumber = fNumber;
    }

    public String getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(String dateTaken) {
        this.dateTaken = dateTaken;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(String frameRate) {
        this.frameRate = frameRate;
    }

    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(String releaseYear) {
        this.releaseYear = releaseYear;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getDocCreatedDate() {
        return docCreatedDate;
    }

    public void setDocCreatedDate(String docCreatedDate) {
        this.docCreatedDate = docCreatedDate;
    }

    public String getDocModifiedDate() {
        return docModifiedDate;
    }

    public void setDocModifiedDate(String docModifiedDate) {
        this.docModifiedDate = docModifiedDate;
    }

    public String getRawMetadataJson() {
        return rawMetadataJson;
    }

    public void setRawMetadataJson(String rawMetadataJson) {
        this.rawMetadataJson = rawMetadataJson;
    }
}