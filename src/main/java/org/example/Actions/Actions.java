package org.example.Actions;

import org.example.HttpClient.Currency;
import org.example.HttpClient.HttpClientConnect;

import java.util.List;

public class Actions {

    private static List<Currency> currencies;

    public Actions() {

        currencies = HttpClientConnect.getJson();

    }

    public String getCurrencyNameUz(String currency) {

        for (Currency curr : currencies) {
            if (curr.getCurrency().equals(currency)) {
                return curr.getCurrencyNameUz();
            }
        }

        return currency;
    }

    public String getCurrencyNameRu(String currency) {

        for (Currency curr : currencies) {
            if (curr.getCurrency().equals(currency)) {
                return curr.getCurrencyNameRu();
            }
        }

        return currency;
    }

    public String getCurrencyNameUzKrill(String currency) {

        for (Currency curr : currencies) {
            if (curr.getCurrency().equals(currency)) {
                return curr.getCurrencyNameKr();
            }
        }

        return currency;

    }

    public String getCurrencyNameEn(String currency) {

        for (Currency curr : currencies) {
            if (curr.getCurrency().equals(currency)) {
                return curr.getCurrencyNameEn();
            }
        }

        return currency;
    }

    public Double getCurrencyRate(String currency) {

        for (Currency curr : currencies) {
            if (curr.getCurrency().equals(currency)) {
                return curr.getRate();
            }
        }

        return 0.0;
    }

    public Double getCurrencyDifference(String currency) {

        for (Currency curr : currencies) {
            if (curr.getCurrency().equals(currency)) {
                return curr.getDifference();
            }
        }

        return 0.0;
    }

    public String getCurrencyDate() {

        return currencies.getFirst().getDate();
    }

}
