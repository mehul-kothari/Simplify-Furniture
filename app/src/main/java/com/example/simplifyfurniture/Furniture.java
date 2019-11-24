package com.example.simplifyfurniture;

import android.net.Uri;

import java.net.URI;
import java.net.URL;

public class Furniture {
    String description;
    String id;
    String name;
    String link_sfb_file;
    FurnitureType type;
    String url;

    public Furniture(String description, String id, String name, String link_sfb_file, FurnitureType type, String url) {
        this.description = description;
        this.id = id;
        this.name = name;
        this.link_sfb_file = link_sfb_file;
        this.type = type;
        this.url = url;
    }

    public Furniture() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLink_sfb_file() {
        return link_sfb_file;
    }

    public void setLink_sfb_file(String link_sfb_file) {
        this.link_sfb_file = link_sfb_file;
    }

    public FurnitureType getType() {
        return type;
    }

    public void setType(FurnitureType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
