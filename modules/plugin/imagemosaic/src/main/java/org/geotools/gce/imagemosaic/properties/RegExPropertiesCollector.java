/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.gce.imagemosaic.properties;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.geotools.util.logging.Logging;

/**
 * 
 *
 * @source $URL$
 */
public abstract class RegExPropertiesCollector extends PropertiesCollector {
	
    private final static Logger LOGGER = Logging.getLogger(RegExPropertiesCollector.class);
    
	public RegExPropertiesCollector(
			PropertiesCollectorSPI spi,
			List<String> propertyNames,
			String regex) {
		super(spi, propertyNames);
		pattern = Pattern.compile(regex);
	}

	private Pattern pattern;

	@Override
	public RegExPropertiesCollector collect(File file) {
		super.collect(file);
		
		// get name of the file
		final String name= FilenameUtils.getBaseName(file.getAbsolutePath());
		
		// get matches 
		final Matcher matcher = pattern.matcher(name);
		 while (matcher.find()) {
			 addMatch(matcher.group());
         }
		
		 return this;
	}
	
    @Override
    public void setProperties(Map<String, Object> map) {

        // get all the matches and convert them 
        List<String> matches = getMatches();

        // set the properties, only if we have matches!
        if (matches.size() <= 0) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine("No matches found for this property extractor:");
        }
        int index = 0;
        for (String propertyName : getPropertyNames()) {
            map.put(propertyName, matches.get(index++));
            // do we have more values?
            if (index >= matches.size())
                return;
        }
    }

}
