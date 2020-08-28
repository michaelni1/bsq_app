package com.bsq.bsquared;

import java.util.Date;

public class Event {

    String desc, loc, ageGroup, event_id, zipcode;
    Integer num_ppl;
    Date timestamp;
    Boolean deleted;

    public Event(String desc, String loc, String ageGroup, String event_id, String zipcode, Date timestamp) {
        this.desc = desc;
        this.loc = loc;
        this.ageGroup = ageGroup;
        this.event_id = event_id;
        this.timestamp = timestamp;
        this.num_ppl = 0;
        this.deleted = false;
        this.zipcode = zipcode;
    }

    public Event(){}

    public void setDesc(String desc) {
        this.desc = desc;
    }
    public void setLoc(String loc) { this.loc = loc; }
    public void setageGroup(String ageGroup) {
        this.ageGroup = ageGroup;
    }
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    public void setEvent_id(String event_id) { this.event_id = event_id; }
    public void setNum_ppl(Integer num_ppl) { this.num_ppl = num_ppl; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    public void setZipcode(String zipcode) { this.zipcode = zipcode; }

    public String getDesc() {
        return desc;
    }

    public String getLoc() { return loc; }

    public String getageGroup() { return ageGroup; }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getEvent_id() { return event_id; }

    public Integer getNum_ppl() { return num_ppl; }

    public Boolean getDeleted() { return deleted; }

    public String getZipcode() { return zipcode; }
}