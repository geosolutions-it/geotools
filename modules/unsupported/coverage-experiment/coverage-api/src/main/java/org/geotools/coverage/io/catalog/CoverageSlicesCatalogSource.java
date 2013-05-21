package org.geotools.coverage.io.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.resources.coverage.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * A {@link GranuleSource} implementation wrapping a {@link CoverageSlicesCatalog}.
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class CoverageSlicesCatalogSource implements GranuleSource {

    private CoverageSlicesCatalog innerCatalog;
    
    protected String typeName;
    
    private final static FilterFactory2 FF = FeatureUtilities.DEFAULT_FILTER_FACTORY;
    
//    private String sourceName;
    /** This filter is created to extract the portion of catalog related to the specified name */
    private final Filter filterName;
    
    public CoverageSlicesCatalogSource(CoverageSlicesCatalog innerCatalog, String coverageName, String typeName) {

        this.typeName = typeName;
//        this.sourceName = sourceName;
        List<Filter> filters = new ArrayList<Filter>(); 
        filters.add(FF.equal(FF.property(CoverageSlice.Attributes.COVERAGENAME),
                FF.literal(coverageName), true));
        filterName = FF.and(filters);
    }

    @Override
    public SimpleFeatureCollection getGranules(Query q) throws IOException {
        //TODO: optimize this. It's currently "putting" all the features. No iterator is used. 
        
        // Filtering by coverageName
        if (q == null) {
            q = new Query(typeName);
        } else {
            q.setTypeName(typeName);
        }
        
        Filter filter = q.getFilter();
        if (filter != null) {
            filter = FF.and(filter, filterName);
        } else {
            filter = filterName;
        }
        q.setFilter(filter);
        List<CoverageSlice> granules = innerCatalog.getGranules(q);
        SimpleFeatureCollection collection = new ListFeatureCollection(innerCatalog.getSchema(typeName));
        for (CoverageSlice granule: granules) {
            ((ListFeatureCollection)collection).add(granule.getOriginator());
        }
        return collection;
    }

    @Override
    public int getCount(Query q) throws IOException {
        //TODO: quick implementation. think about something less expensive
//        return getGranules(q).size();
        return innerCatalog.getGranules(q).size();
    }

    @Override
    public ReferencedEnvelope getBounds(Query q) throws IOException {
        //TODO: quick implementation. think about something less expensive
        return getGranules(q).getBounds();
    }

    @Override
    public SimpleFeatureType getSchema() throws IOException {
        return innerCatalog.getSchema(typeName);
    }

    @Override
    public void dispose() throws IOException {
        //TODO: Should we dispose the inner catalog?
//        innerCatalog.dispose();
    }

}
