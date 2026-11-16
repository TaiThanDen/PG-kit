package com.pgkit.timemarkcamera;

import android.location.Address;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Formats system geocoder results using Timemark's observed system-address route. */
final class LocationAddressFormatter {
    private LocationAddressFormatter() {}

    static String format(Address address) {
        if (address.getMaxAddressLineIndex() >= 0) {
            return formatLine(address.getAddressLine(0), address.getFeatureName(),
                    address.getCountryCode(), address.getCountryName());
        }
        return formatParts(address.getFeatureName(), address.getSubThoroughfare(),
                address.getThoroughfare(), address.getSubLocality(), address.getLocality(),
                address.getAdminArea(), address.getPostalCode(), address.getCountryCode(),
                address.getCountryName(), Locale.getDefault().getCountry());
    }

    static String formatLine(String line, String feature, String countryCode, String countryName) {
        String result = value(line);
        String name = value(feature);
        // A geocoder Plus Code is a locator, not a street/POI name.
        if (!name.isEmpty() && name.contains("+") && result.startsWith(name)) {
            result = result.substring(name.length()).replaceFirst("^[, ]+", "");
        }
        // The original keeps the full Chinese line; Thai lines can use spaces instead of commas.
        if (!"CN".equals(countryCode)) {
            char delimiter = "TH".equals(countryCode) && result.indexOf(',') < 0 ? ' ' : ',';
            int last = result.lastIndexOf(delimiter);
            if (last >= 0) result = result.substring(0, last);
        }
        String country = value(countryName);
        if (!country.isEmpty()) {
            result = result.replace(country + ", ", "")
                    .replace(" " + country + ",", "").replace(country, "");
        }
        return result.trim();
    }

    static String formatParts(String feature, String houseNumber, String street, String ward,
                              String city, String province, String postalCode, String countryCode,
                              String countryName, String localeCountry) {
        List<String> pieces = new ArrayList<>();
        String name = value(feature);
        if (!name.contains("+")) add(pieces, name);
        if (!name.contains(value(houseNumber))) add(pieces, houseNumber);
        if (!name.contains(value(street))) add(pieces, street);
        add(pieces, ward);
        add(pieces, city);
        add(pieces, province);
        if (!"SG".equals(localeCountry)) add(pieces, postalCode);
        // formatLargePosition suppresses the TW country name in the original system fallback.
        if (!"TW".equals(countryCode)) add(pieces, countryName);
        if ("SG".equals(localeCountry)) add(pieces, postalCode);
        return String.join(", ", pieces);
    }

    private static void add(List<String> pieces, String text) {
        String part = value(text);
        if (!part.isEmpty()) pieces.add(part);
    }

    private static String value(String text) {
        return text == null ? "" : text.trim();
    }
}
