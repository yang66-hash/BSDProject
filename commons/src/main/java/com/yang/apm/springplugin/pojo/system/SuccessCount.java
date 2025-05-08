package com.yang.apm.springplugin.pojo.system;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuccessCount {
    /**
     * the successful transaction(request) number
     */
    @JsonProperty("sum")
    private Integer sum;
    /**
     * total transaction(request) number
     */
    @JsonProperty("value_count")
    private Integer valueCount;
}
