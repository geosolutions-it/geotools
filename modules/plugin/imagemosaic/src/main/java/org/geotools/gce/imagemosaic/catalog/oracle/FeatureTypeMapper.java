package org.geotools.gce.imagemosaic.catalog.oracle;

import java.util.List;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.transform.Definition;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A simple interface which provides FeatureType mapping information such as
 * the name of the original type name and the mapped one, the wrapped feature type as well as the customized version 
 * 
 * @author Daniele Romagnoli
 *
 */
public interface FeatureTypeMapper {
    public Name getName();

    public String getMappedName();

    public List<Definition> getDefinitions();
    
    public CoordinateReferenceSystem getCoordinateReferenceSystem();

    public SimpleFeatureType getInnerFeatureType();

    public SimpleFeatureType getWrappedFeatureType();

    public SimpleFeatureSource getSimpleFeatureSource();

}
