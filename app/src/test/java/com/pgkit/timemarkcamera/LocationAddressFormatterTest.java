package com.pgkit.timemarkcamera;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class LocationAddressFormatterTest {
    @Test public void vietnameseAddressOmitsCountryLikeReferenceCapture() {
        assertEquals("234 Example Street, Example District, Hồ Chí Minh",
                LocationAddressFormatter.formatLine(
                        "234 Example Street, Example District, Hồ Chí Minh, Việt Nam",
                        "234", "VN", "Việt Nam"));
    }

    @Test public void stripsLeadingPlusCodeWithoutDroppingStreet() {
        assertEquals("Đường A, Hồ Chí Minh", LocationAddressFormatter.formatLine(
                "AB12+CD, Đường A, Hồ Chí Minh, Việt Nam", "AB12+CD", "VN", "Việt Nam"));
    }

    @Test public void preservesChineseLineAndUsesThaiSpaceDelimiter() {
        assertEquals("北京市朝阳区", LocationAddressFormatter.formatLine(
                "中国北京市朝阳区", null, "CN", "中国"));
        assertEquals("123 Bangkok", LocationAddressFormatter.formatLine(
                "123 Bangkok Thailand", null, "TH", "Thailand"));
    }

    @Test public void fallbackRetainsHouseNumberWithoutDuplicatingFeature() {
        assertEquals("234 Đường A, Phường B, Thành phố C, 700000, Việt Nam",
                LocationAddressFormatter.formatParts("234 Đường A", "234", "Đường A",
                        "Phường B", null, "Thành phố C", "700000", "VN", "Việt Nam", "VN"));
        assertEquals("234, Đường A, Thành phố C, Việt Nam",
                LocationAddressFormatter.formatParts("AB12+CD", "234", "Đường A",
                        null, null, "Thành phố C", null, "VN", "Việt Nam", "VN"));
    }

    @Test public void fallbackKeepsReferencePostalOrderAndCountryException() {
        assertEquals("1, Road, Singapore, Singapore, 123456",
                LocationAddressFormatter.formatParts(null, "1", "Road", null,
                        "Singapore", null, "123456", "SG", "Singapore", "SG"));
        assertEquals("Road, Taipei", LocationAddressFormatter.formatParts(
                "Road", null, "Road", null, "Taipei", null, null, "TW", "Taiwan", "TW"));
    }

    @Test public void emptyInputNeverBecomesCoordinatesOrPlaceholder() {
        assertEquals("", LocationAddressFormatter.formatLine(null, null, null, null));
        assertEquals("", LocationAddressFormatter.formatParts(null, null, null,
                null, null, null, null, null, null, "VN"));
    }
}
