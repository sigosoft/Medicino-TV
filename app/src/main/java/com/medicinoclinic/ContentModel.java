package com.medicinoclinic;

public class ContentModel {

    String title;
    String body;
    String id;
    String type;

    public ContentModel(String title, String body,String id,String type) {
        this.title = title;
        this.body = body;
        this.id = id;
        this.type = type;
    }




    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }


}
