package com.assetstree.jira.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ClinicCircleDto {
    private int id;
    private String name = "";
    private List<UserProfileDto> members = new ArrayList<UserProfileDto>();
    private List<Place> places = new ArrayList<Place>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public List<UserProfileDto> getMembers() {
        return members;
    }

    public void setMembers(List<UserProfileDto> members) {
        this.members = members == null ? new ArrayList<UserProfileDto>() : members;
    }

    public List<Place> getPlaces() {
        return places;
    }

    public void setPlaces(List<Place> places) {
        this.places = places == null ? new ArrayList<Place>() : places;
    }

    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class Place {
        private int id;
        private String name = "";
        private String objectKey = "";

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name == null ? "" : name;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public void setObjectKey(String objectKey) {
            this.objectKey = objectKey == null ? "" : objectKey;
        }
    }
}
