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
package org.geotools.imageio.unidata;

import java.io.IOException;
import java.util.Map;

import org.geotools.gce.imagemosaic.catalog.GranuleCatalog;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * A property collector used to populate an existing catalog (through a specified catalogSchema)
 *   
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public abstract class CatalogPropertiesCollector {
    
    public class CatalogSchema {
        
        String name;
        
        String schemaTemplate;
        
        SimpleFeatureType schema;
    }
    
    public class CatalogIndexer {
        
        Map<String, CatalogSchema> schemaMapping;
        
        String timeAttribute;
        
        String elevationAttribute;
        
        String additionalDomainsAttribute;
    }

    protected CatalogIndexer indexer;
    
    /** 
     * An instance of collectingRules which provide hints on how to check
     * supported variable names, how to deal with time attributes, how to deal
     * with runtime/modifiytime. 
     */
    protected CollectingRules collectingRules;

    public CollectingRules getCollectingRules() {
        return collectingRules;
    }

    public void setCollectingRules(CollectingRules collectingRules) {
        this.collectingRules = collectingRules;
    }

    public abstract void setCatalog(GranuleCatalog catalog) throws IOException;
    
//    public SimpleFeatureType getCatalogSchema() {
//        return catalogSchema;
//    }
//
//    public void setCatalogSchema(SimpleFeatureType catalogSchema) {
//        this.catalogSchema = catalogSchema;
//    }
}
