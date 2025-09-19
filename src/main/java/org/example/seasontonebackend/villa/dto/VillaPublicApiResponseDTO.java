package org.example.seasontonebackend.villa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JacksonXmlRootElement(localName = "response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class VillaPublicApiResponseDTO {

    private Body body;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Items items;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<Item> itemList;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        // 🔥 빌라 API는 연립다세대 필드명 사용 (추정)
        @JacksonXmlProperty(localName = "연립다세대")
        private String buildingName;

        @JacksonXmlProperty(localName = "deposit")
        private String deposit;

        @JacksonXmlProperty(localName = "monthlyRent")
        private String monthlyRent;

        @JacksonXmlProperty(localName = "totalFloorAr")
        private double area;

        @JacksonXmlProperty(localName = "dealYear")
        private int year;

        @JacksonXmlProperty(localName = "dealMonth")
        private int month;

        @JacksonXmlProperty(localName = "dealDay")
        private int day;

        @JacksonXmlProperty(localName = "sggCd")
        private String district; // 시군구

        @JacksonXmlProperty(localName = "umdNm")
        private String neighborhood; // 법정동

        @JacksonXmlProperty(localName = "buildYear")
        private String buildYear; // 건축년도

        @JacksonXmlProperty(localName = "contractType")
        private String contractType; // 계약구분

        @JacksonXmlProperty(localName = "contractTerm")
        private String contractTerm; // 계약기간

        @JacksonXmlProperty(localName = "houseType")
        private String houseType; // 주택유형
    }
}