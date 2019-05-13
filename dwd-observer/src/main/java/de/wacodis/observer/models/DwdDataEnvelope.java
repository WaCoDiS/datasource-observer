package de.wacodis.observer.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Objects;
import javax.validation.constraints.*;

/** DwdDataEnvelope */
@javax.annotation.Generated(
        value = "org.openapitools.codegen.languages.SpringCodegen",
        date = "2019-05-13T08:43:27.051+02:00[Europe/Berlin]")
public class DwdDataEnvelope extends AbstractDataEnvelope implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("serviceUrl")
    private String serviceUrl;

    @JsonProperty("layerName")
    private String layerName;

    @JsonProperty("parameter")
    private String parameter;

    public DwdDataEnvelope serviceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        return this;
    }

    /**
     * Get serviceUrl
     *
     * @return serviceUrl
     */
    @ApiModelProperty(required = true, value = "")
    @NotNull
    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public DwdDataEnvelope layerName(String layerName) {
        this.layerName = layerName;
        return this;
    }

    /**
     * the name of the queried layer
     *
     * @return layerName
     */
    @ApiModelProperty(required = true, value = "the name of the queried layer ")
    @NotNull
    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public DwdDataEnvelope parameter(String parameter) {
        this.parameter = parameter;
        return this;
    }

    /**
     * designation of the layer as a clear name
     *
     * @return parameter
     */
    @ApiModelProperty(required = true, value = "designation of the layer as a clear name ")
    @NotNull
    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DwdDataEnvelope dwdDataEnvelope = (DwdDataEnvelope) o;
        return Objects.equals(this.serviceUrl, dwdDataEnvelope.serviceUrl)
                && Objects.equals(this.layerName, dwdDataEnvelope.layerName)
                && Objects.equals(this.parameter, dwdDataEnvelope.parameter)
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceUrl, layerName, parameter, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DwdDataEnvelope {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    serviceUrl: ").append(toIndentedString(serviceUrl)).append("\n");
        sb.append("    layerName: ").append(toIndentedString(layerName)).append("\n");
        sb.append("    parameter: ").append(toIndentedString(parameter)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first
     * line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
