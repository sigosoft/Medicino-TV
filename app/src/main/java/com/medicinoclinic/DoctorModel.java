package com.medicinoclinic;

public class DoctorModel {

    String name;
    String room;
    String token;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }



    public DoctorModel(String name, String room, String token) {
        this.name = name;
        this.room = room;
        this.token = token;
    }


}
