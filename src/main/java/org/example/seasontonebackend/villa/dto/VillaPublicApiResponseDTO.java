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

        @JacksonXmlProperty(localName = "보증금액")
        private String deposit;

        @JacksonXmlProperty(localName = "월세금액")
        private String monthlyRent;

        @JacksonXmlProperty(localName = "전용면적")
        private double area;

        @JacksonXmlProperty(localName = "년")
        private int year;

        @JacksonXmlProperty(localName = "월")
        private int month;

        @JacksonXmlProperty(localName = "일")
        private int day;

        @JacksonXmlProperty(localName = "시군구")
        private String district; // 시군구

        @JacksonXmlProperty(localName = "법정동")
        private String neighborhood; // 법정동

        @JacksonXmlProperty(localName = "층")
        private String floor; // 층

        @JacksonXmlProperty(localName = "건축년도")
        private String buildYear; // 건축년도

        @JacksonXmlProperty(localName = "계약구분")
        private String contractType; // 계약구분

        @JacksonXmlProperty(localName = "계약기간")
        private String contractTerm; // 계약기간
    }
}