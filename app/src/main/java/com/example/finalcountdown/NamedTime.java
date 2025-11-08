package com.example.finalcountdown;

public class NamedTime {
    private String name;
    private String time;
    private String id;

    // No-arg constructor for Gson deserialization
    public NamedTime() {
    }

    public NamedTime(String name, String time) {
        this.name = name;
        this.time = time;
        this.id = System.currentTimeMillis() + "_" + name.hashCode();
    }

    public NamedTime(String id, String name, String time) {
        this.id = id;
        this.name = name;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return name + " (" + time + ")";
    }
}


