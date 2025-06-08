package antix.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("url")
    private String url;
    
    // Constructeurs
    public Tag() {}
    
    public Tag(String name) {
        this.name = name;
    }
    
    public Tag(String name, String url) {
        this.name = name;
        this.url = url;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
