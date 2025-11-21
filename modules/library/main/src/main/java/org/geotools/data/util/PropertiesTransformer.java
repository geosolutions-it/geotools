/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.data.util;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Utility class for converting between {@link Properties} objects and alternative representations such as
 * {@link java.util.Map} or flat key–value strings.
 */
public class PropertiesTransformer {

    private static final String NEW_LINE = "\n";

    /** Converts a Properties object into a Map representation. */
    public static Map<String, Object> propertiesToMap(Properties props) {
        return props.stringPropertyNames().stream().collect(Collectors.toMap(k -> k, props::get));
    }

    /** Serializes a Properties object into a semicolon-delimited key=value string. */
    public static String propertiesToString(Properties props) {
        return props.stringPropertyNames().stream()
                .map(k -> k + "=" + props.getProperty(k))
                .collect(Collectors.joining(";"));
    }

    /** Parses an encoded parameter string into a Properties object. */
    public static Properties paramsStringToProperties(String paramsText) throws IOException {
        Properties props = new Properties();
        if (paramsText == null || paramsText.isEmpty()) {
            return props;
        }

        String decoded = URLDecoder.decode(paramsText, StandardCharsets.UTF_8);
        loadProperties(decoded, props);
        return props;
    }

    /** Loads key–value pairs from raw text into a Properties object after normalization. */
    public static void loadProperties(String params, Properties properties) throws IOException {
        try (StringReader reader = new StringReader(normalizePropertiesString(params))) {
            properties.load(reader);
        }
    }

    /** Normalizes raw property text into a clean, loadable format. */
    public static String normalizePropertiesString(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String s = raw.trim();
        s = s.replace("\\n", NEW_LINE);
        if (!s.contains(NEW_LINE) && s.contains(";")) {
            s = s.replace(";", NEW_LINE);
        }

        // 3. Normalize Windows CRLF -> LF
        s = s.replace("\r\n", NEW_LINE).replace("\r", NEW_LINE);

        return Arrays.stream(s.split(NEW_LINE))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining(NEW_LINE));
    }
}
