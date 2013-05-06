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
package org.geotools.gce.imagemosaic.catalogbuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.geotools.factory.Hints;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.gce.imagemosaic.Utils.Prop;
import org.geotools.gce.imagemosaic.catalog.index.Indexer;
import org.geotools.gce.imagemosaic.catalog.index.ParametersType;
import org.geotools.gce.imagemosaic.catalog.index.ParametersType.Parameter;
import org.geotools.gce.imagemosaic.catalog.index.SchemaType;
import org.geotools.gce.imagemosaic.catalog.index.SchemasType;
import org.geotools.util.Utilities;

/**
 * Simple bean that conveys the information needed by the CatalogBuilder to
 * create a catalogue of granules
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 *
 *
 * @source $URL$
 */
public class CatalogBuilderConfiguration {

	private Hints hints;

	private String timeAttribute;
//
//	private String elevationAttribute;

	private String runtimeAttribute;

//	private String additionalDomainAttribute;

	private Indexer indexer

//	private boolean absolute = Utils.DEFAULT_PATH_BEHAVIOR;
//	
//	private boolean caching = Utils.DEFAULT_CONFIGURATION_CACHING;

	;
	
//	/**
//	 * Index file name. Default is index.
//	 */
//	private String indexName = Utils.DEFAULT_INDEX_NAME;

//	private String locationAttribute = Utils.DEFAULT_LOCATION_ATTRIBUTE;

//	private boolean footprintManagement = Utils.DEFAULT_FOOTPRINT_MANAGEMENT;
//
//	@Option(description = "Root directory where to place the index file", mandatory = true, name = "rootDirectory")
//	private String rootMosaicDirectory;
//
//	@Option(description = "Wildcard to use for building the index of this mosaic", mandatory = false, name = "wildcard")
//	private String wildcard = Utils.DEFAULT_WILCARD;

//	/**
//	 * String to pass to the featuretypebuilder for building the schema for the
//	 * index.
//	 */
//	private String schema;
//
//	private String propertyCollectors;
//	
//        private List<String> indexingDirectories;

	
//	/**
//	 * Imposed BBOX
//	 */
//	private String envelope2D;
	
	public String getResolutionLevels() {
		return resolutionLevels;
	}

	public void setResolutionLevels(String resolutionLevels) {
		this.resolutionLevels = resolutionLevels;
	}

	/**
	 * Imposed resolution levels
	 */
	private String resolutionLevels;

	/**
	 * @deprecated parse indexer parameters instead.
	 * @return
	 */
	public String getEnvelope2D() {
	    return getParameter(Prop.ENVELOPE2D);
//		return envelope2D;
	}
//
//	public void setEnvelope2D(String bbox) {
//		this.envelope2D = bbox;
//	}

    public CatalogBuilderConfiguration() {
        initDefaultsParam();
    }

    private void initDefaultsParam() {
        final Indexer defaultIndexer = Utils.OBJECT_FACTORY.createIndexer();
        final ParametersType parameters = Utils.OBJECT_FACTORY.createParametersType();
        final List<Parameter> parameterList = parameters.getParameter();
        defaultIndexer.setParameters(parameters);
        setIndexer(defaultIndexer);
        Utils.setParam(parameterList, Prop.LOCATION_ATTRIBUTE, Utils.DEFAULT_LOCATION_ATTRIBUTE);
        Utils.setParam(parameterList, Prop.WILDCARD, Utils.DEFAULT_WILCARD);
        Utils.setParam(parameterList, Prop.FOOTPRINT_MANAGEMENT, Boolean.toString(Utils.DEFAULT_FOOTPRINT_MANAGEMENT));
        Utils.setParam(parameterList, Prop.ABSOLUTE_PATH, Boolean.toString(Utils.DEFAULT_PATH_BEHAVIOR));
        Utils.setParam(parameterList, Prop.RECURSIVE, Boolean.toString(Utils.DEFAULT_RECURSION_BEHAVIOR));
        Utils.setParam(parameterList, Prop.INDEX_NAME, Utils.DEFAULT_INDEX_NAME);
    }

	public CatalogBuilderConfiguration(final CatalogBuilderConfiguration that) {
		Utilities.ensureNonNull("CatalogBuilderConfiguration", that);
		initDefaultsParam();
		try {
			BeanUtils.copyProperties(this, that);
		} catch (IllegalAccessException e) {
			final IllegalArgumentException iae = new IllegalArgumentException(e);
			throw iae;
		} catch (InvocationTargetException e) {
			final IllegalArgumentException iae = new IllegalArgumentException(e);
			throw iae;
		}

	}

	/**
	 * @return the hints
	 */
	public Hints getHints() {
		return hints;
	}

	/**
	 * @param hints
	 *            the hints to set
	 */
	public void setHints(Hints hints) {
		this.hints = hints;
	}


//	/**
//	 * @deprecated use parameters instead
//	 */
//	public void setIndexingDirectories(List<String> indexingDirectories) {
//		this.indexingDirectories = indexingDirectories;
//	}

//	private boolean recursive = Utils.DEFAULT_RECURSION_BEHAVIOR;

    /**
     * @deprecated parse indexer parameters instead.
     * @return
     */
    public boolean isRecursive() {
        return Boolean.parseBoolean(getParameter(Prop.RECURSIVE));
    }

//	public void setRecursive(boolean recursive) {
//		this.recursive = recursive;
//	}

        /**
         * @deprecated parse indexer parameters instead.
         * @return
         */
	public boolean isCaching() {
		return Boolean.parseBoolean(getParameter(Prop.CACHING));
	}

//	public void setCaching(boolean caching) {
//		this.caching = caching;
//	}
//
//	public String getPropertyCollectors() {
//		return propertyCollectors;
//	}
//
//	public void setPropertyCollectors(String propertyCollectors) {
//		this.propertyCollectors = propertyCollectors;
//	}

//	public String getSchema() {
//		return schema;
//	}

    /**
     * Get the schema with the specified name
     * 
     * @param name
     * @return
     */
    public String getSchema(String name) {
        // return schema;
        SchemasType schemas = indexer.getSchemas();
        if (schemas != null) {
            List<SchemaType> schemaList = schemas.getSchema();
            for (SchemaType schema : schemaList) {
                if (schema.getName().equalsIgnoreCase(name)) {
                    return schema.getAttributes();
                }
            }
        }
        return null;
    }

//	public void setSchema(String schema) {
//		this.schema = schema;
//	}

    /**
     * Set the indexer parameter
     * @param parameterName
     * @param parameterValue
     */
    public void setParameter(String parameterName, String parameterValue) {
        List<Parameter> params = indexer.getParameters().getParameter();
        parameterValue = Utils.refineParameterValue(parameterName, parameterValue);
        for (Parameter param : params) {
            if (param.getName().equalsIgnoreCase(parameterName)) {
                param.setValue(parameterValue);
                return;
            }
        }
        Parameter param = Utils.OBJECT_FACTORY.createParametersTypeParameter();
        param.setName(parameterName);
        param.setValue(parameterValue);
        params.add(param);
    }

    public String getParameter(String parameterName) {
        return Utils.getParameter(parameterName, indexer);
    }

    public String getTimeAttribute() {
        return timeAttribute;
    }

    public Indexer getIndexer() {
        return indexer;
    }

    public void setIndexer(Indexer indexer) {
        this.indexer = indexer;
    }

    // public void setTimeAttribute(String timeAttribute) {
    //		this.timeAttribute = timeAttribute;
//	}
//
//    
//	public String getElevationAttribute() {
//		return elevationAttribute;
//	}
//
//	public void setElevationAttribute(String elevationAttribute) {
//		this.elevationAttribute = elevationAttribute;
//	}

	public String getRuntimeAttribute() {
		return runtimeAttribute;
	}

	public void setRuntimeAttribute(String runtimeAttribute) {
		this.runtimeAttribute = runtimeAttribute;
	}

//    public String getAdditionalDomainAttribute() {
//        return additionalDomainAttribute;
//    }
//    
//    public void setAdditionalDomainAttribute(String additionalDomainAttribute) {
//        this.additionalDomainAttribute = additionalDomainAttribute;
//    }

//    /**
////     * @deprecated parse indexer parameters instead
//     * @return
//     */
//	public List<String> getIndexingDirectories() {
//		return indexingDirectories;
//	}

    /**
     * @deprecated parse indexer parameters instead.
     * @return
     */
    public String getIndexName() {
        // return indexName;
        return getParameter(Prop.INDEX_NAME);
    }
    
    /**
     * @deprecated parse indexer parameters instead.
     * @return
     */
    public boolean isFootprintManagement() {
//        return footprintManagement;
        return  Boolean.parseBoolean(getParameter(Prop.FOOTPRINT_MANAGEMENT));
}

//public void setFootprintManagement(boolean footprintManagement) {
//      this.footprintManagement = footprintManagement;
//}
    
    /**
     * @deprecated parse indexer parameters instead.
     * @return
     */
    public String getLocationAttribute() {
        // return locationAttribute;
        return getParameter(Prop.LOCATION_ATTRIBUTE);
    }

    /**
     * @deprecated parse indexer parameters instead.
     * @return
     */
    public String getRootMosaicDirectory() {
        // return rootMosaicDirectory;
        return getParameter(Prop.ROOT_MOSAIC_DIR);
    }

        /**
         * @deprecated parse Indexer parameters instead.
         * @return
         */
        public String getWildcard() {
            return getParameter(Prop.WILDCARD);
        }

//            /**
//            * @deprecated parse Indexer parameters instead.
//            * @return
//            */
//            public void setWildcard(String wildcardString) {
//                this.wildcard = wildcardString;
//            }

                /**
                 * @deprecated parse Indexer parameters instead.
                 * @return
                 */
                public boolean isAbsolute() {
                    return Boolean.parseBoolean(getParameter(Prop.ABSOLUTE_PATH));
                }
             
//                /**
//                 * @deprecated parse Indexer parameters instead.
//                 * @return
//                 */
//                public void setAbsolute(boolean absolute) {
//                    this.absolute = absolute;
//                }
//
//                /**
//                 * @deprecated parse Indexer parameters instead
//                 * @param indexName
//                 */
//	public void setIndexName(String indexName) {
//		this.indexName = indexName;
//	}

	@Override
	public CatalogBuilderConfiguration clone()
			throws CloneNotSupportedException {
		return new CatalogBuilderConfiguration(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof CatalogBuilderConfiguration))
			return false;
		final CatalogBuilderConfiguration that = (CatalogBuilderConfiguration) obj;
		if (!equalsParameter(this, that, Prop.ABSOLUTE_PATH)) 
		    return false;
		
		if (!equalsParameter(this, that, Prop.CACHING)) 
                    return false;
		if (!equalsParameter(this, that, Prop.RECURSIVE)) 
                    return false;
		if (!equalsParameter(this, that, Prop.FOOTPRINT_MANAGEMENT)) 
                    return false;
                if (!equalsParameter(this, that, Prop.INDEX_NAME))
                    return false;
                if (!equalsParameter(this, that, Prop.LOCATION_ATTRIBUTE)) 
                    return false;
                if (!equalsParameter(this, that, Prop.ROOT_MOSAIC_DIR)) 
                    return false;
//		if (!Utilities.deepEquals(this.indexingDirectories,
//				that.indexingDirectories))
//			return false;

		return true;
	}

    private static boolean equalsParameter(CatalogBuilderConfiguration thisConfig,
            CatalogBuilderConfiguration thatConfig, String parameterName) {
        String thisValue = thisConfig.getParameter(parameterName);
        String thatValue = thatConfig.getParameter(parameterName);
        if (!(thisValue == null && thatValue == null) && !thisValue.equals(thatValue)) {
            return false;
        }
        return true;
    }

    @Override
	public int hashCode() {
		int seed = 37;
		seed = Utilities.hash(isAbsolute(), seed);
		seed = Utilities.hash(isRecursive(), seed);
		seed = Utilities.hash(isCaching(), seed);
		seed = Utilities.hash(isFootprintManagement(), seed);
		seed = Utilities.hash(getLocationAttribute(), seed);
		seed = Utilities.hash(getIndexName(), seed);
		seed = Utilities.hash(getWildcard(), seed);
		seed = Utilities.hash(getRootMosaicDirectory(), seed);
//		seed = Utilities.hash(indexingDirectories, seed);
		return seed;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CatalogBuilderConfiguration").append("\n");
		builder.append("wildcardString:\t\t\t").append(getWildcard()).append("\n");
		builder.append("indexName:\t\t\t").append(getIndexName()).append("\n");
		builder.append("absolute:\t\t\t").append(isAbsolute()).append("\n");
		builder.append("caching:\t\t\t").append(isCaching()).append("\n");
		builder.append("recursive:\t\t\t").append(isRecursive()).append("\n");
		builder.append("footprintManagement:\t\t\t").append(isFootprintManagement()).append("\n");
		builder.append("locationAttribute:\t\t\t").append(getLocationAttribute()).append("\n");
		builder.append("rootMosaicDirectory:\t\t\t").append(getRootMosaicDirectory()).append("\n");
//		builder.append("indexingDirectories:\t\t\t").append(
//				Utilities.deepToString(indexingDirectories)).append("\n");
		return builder.toString();
	}

    public void check() {
        // check parameters

        // Check the indexing directories
        String indexingDirs = getParameter(Prop.INDEXING_DIRECTORIES);
        if (indexingDirs == null) {
            throw new IllegalStateException("Indexing directories are empty");
        } else {
            String[] indexingDirectoriesString = indexingDirs.split("\\s*,\\s*");
            if (indexingDirectoriesString == null || indexingDirectoriesString.length <= 0)
                throw new IllegalStateException("Indexing directories are empty");
            // final List<String> directories = new ArrayList<String>();
            // for (String dir : indexingDirectoriesString)
            // directories.add(Utils.checkDirectory(dir,false));
            // indexingDirectories = directories;
        }
        String indexName = getParameter(Prop.INDEX_NAME);
        if (indexName == null || indexName.length() == 0)
            throw new IllegalStateException("Index name cannot be empty");

        // Check the root mosaic directory
        String rootMosaicDirectory = getParameter(Prop.ROOT_MOSAIC_DIR);
        if (rootMosaicDirectory == null || rootMosaicDirectory.length() == 0)
            throw new IllegalStateException("RootMosaicDirectory name cannot be empty");

        rootMosaicDirectory = Utils.checkDirectory(rootMosaicDirectory, true);
        String wildcard = getParameter(Prop.WILDCARD);
        if (wildcard == null || wildcard.length() == 0)
            throw new IllegalStateException("WildcardString name cannot be empty");

    }

}
