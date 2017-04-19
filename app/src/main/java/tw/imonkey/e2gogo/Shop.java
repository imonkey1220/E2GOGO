package tw.imonkey.e2gogo;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Shop {
    private String companyId;
    private String description;
    private String device;
    private String deviceType;
    private String masterEmail;
    private String topics_id; //=deviceId
    private Long timeStamp;
    private Boolean connection;
    private Map<String, Object> alert = new HashMap<>();
    public Shop() {
    }

    public Shop(String topics_id,String companyId, String device, String deviceType, String description, String masterEmail,Long timeStamp,boolean connection,Map<String,Object> alert) {

         this.companyId=companyId;
         this.description=description;
         this.device=device;
         this.deviceType=deviceType;
         this.masterEmail=masterEmail;
         this.topics_id=topics_id;
         this.timeStamp=timeStamp;
         this.connection=connection;
         this.alert=alert;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getDescription() {
        return description;
    }

    public String getDevice() {
        return device;
    }
    public String getDeviceType() {return deviceType;}

    public String getMasterEmail() {
        return masterEmail;
    }

    public String getTopics_id() {
        return topics_id;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public Boolean getConnection() {
        return connection;
    }

    public Map<String,Object> getAlert() {
        return alert;
    }
}
