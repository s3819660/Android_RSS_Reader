package com.example.rss_reader;

public class Category {
    private String title;
    private String url;
    private int imageSrc;

    public Category(String title, String url, int imageSrc) {
        this.title = title;
        this.url = url;
        this.imageSrc = imageSrc;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getImageSrc() {
        return imageSrc;
    }

    public void setImageSrc(int imageSrc) {
        this.imageSrc = imageSrc;
    }
}
