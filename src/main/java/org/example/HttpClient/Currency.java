package org.example.HttpClient;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Currency {

    @SerializedName("id")
    private Integer id;

    @SerializedName("Code")
    private String code;

    @SerializedName("Ccy")
    private String currency;

    @SerializedName("CcyNm_RU")
    private String currencyNameRu;

    @SerializedName("CcyNm_UZ")
    private String currencyNameUz;

    @SerializedName("CcyNm_UZC")
    private String currencyNameKr;

    @SerializedName("CcyNm_EN")
    private String currencyNameEn;

    @SerializedName("Nominal")
    private Integer nominal;

    @SerializedName("Rate")
    private Double rate;

    @SerializedName("Diff")
    private Double difference;

    @SerializedName("Date")
    private String date;

}
