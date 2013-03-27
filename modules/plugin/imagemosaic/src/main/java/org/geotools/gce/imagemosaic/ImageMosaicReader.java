/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.gce.imagemosaic;

import java.awt.Rectangle;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.spi.ImageReaderSpi;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataUtilities;
import org.geotools.factory.Hints;
import org.geotools.gce.imagemosaic.catalog.CatalogConfigurationBean;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalog;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalogFactory;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.Utilities;
import org.opengis.coverage.grid.Format;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
/**
 * This reader is responsible for providing access to mosaic of georeferenced
 * images. Citing JAI documentation:
 * 
 * The "Mosaic" operation creates a mosaic of two or more source images. This
 * operation could be used for example to assemble a set of overlapping
 * geospatially rectified images into a contiguous image. It could also be used
 * to create a montage of photographs such as a panorama.
 * 
 * All source images are assumed to have been geometrically mapped into a common
 * coordinate space. The origin (minX, minY) of each image is therefore taken to
 * represent the location of the respective image in the common coordinate
 * system of the source images. This coordinate space will also be that of the
 * destination image.
 * 
 * All source images must have the same data type and sample size for all bands
 * and have the same number of bands as color components. The destination will
 * have the same data type, sample size, and number of bands and color
 * components as the sources.
 * 
 * 
 * @author Simone Giannecchini, GeoSolutions S.A.S
 * @author Stefan Alfons Krueger (alfonx), Wikisquare.de : Support for jar:file:foo.jar/bar.properties URLs
 * @since 2.3
 * 
 *
 *
 * @source $URL$
 */
@SuppressWarnings("rawtypes")
public class ImageMosaicReader extends AbstractGridCoverage2DReader implements GridCoverage2DReader {

    Set<String> names = new HashSet<String>();
    
    String defaultName = null;
    
    public static final String UNSPECIFIED = "_UN$PECIFIED_";

    HashMap<String, RasterManager> rasterManagers = new HashMap<String, RasterManager>();

    public RasterManager getRasterManager(String name) {
          if(rasterManagers.containsKey(name)){
              return rasterManagers.get(name);
          }
          return null;
  }
    
//    public String[] getGridCoverageNames() {
//            return (String[]) names.toArray();
//        }

    /** Logger. */
	private final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(ImageMosaicReader.class);

	/**
	 * The source {@link URL} pointing to the index shapefile for this
	 * {@link ImageMosaicReader}.
	 */
	URL sourceURL;

	boolean expandMe;
	
	PathType pathType;
	
	ExecutorService multiThreadedLoader;

	String locationAttributeName=Utils.DEFAULT_LOCATION_ATTRIBUTE;

//	CoveragesManager coveragesManager;

        int maxAllowedTiles=ImageMosaicFormat.MAX_ALLOWED_TILES.getDefaultValue();

	/** The suggested SPI to avoid SPI lookup*/
	ImageReaderSpi suggestedSPI;
	
	// TODO: Move it out to RasterManagers
	GranuleCatalog granuleCatalog;

	boolean cachingIndex;

	boolean imposedBBox;
	
	boolean heterogeneousGranules;

	String typeName;

        /**
	 * Constructor.
	 * 
	 * @param source
	 *            The source object.
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * 
	 */
	public ImageMosaicReader(Object source, Hints uHints) throws IOException {
	    super(source,uHints);
	    
	    //
	    // try to extract a multithreaded loader if available
	    //
	    if (this.hints.containsKey(Hints.EXECUTOR_SERVICE)) {
	      final Object executor = uHints.get(Hints.EXECUTOR_SERVICE);
	      if (executor != null && executor instanceof ExecutorService){
	          multiThreadedLoader = (ExecutorService) executor;
	          if (LOGGER.isLoggable(Level.FINE)){
	              if (multiThreadedLoader instanceof ThreadPoolExecutor){
	                  final ThreadPoolExecutor tpe = (ThreadPoolExecutor) multiThreadedLoader;
	                  LOGGER.fine("Using ThreadPoolExecutor with the following settings: " +
	                              "core pool size = " + tpe.getCorePoolSize() + 
	                              "\nmax pool size = " + tpe.getMaximumPoolSize() + 
	                              "\nkeep alive time " + tpe.getKeepAliveTime(TimeUnit.MILLISECONDS));    
	              }
	          }
	      }
	    }
	    
	    // max allowed tiles for a single request
            if (this.hints.containsKey(Hints.MAX_ALLOWED_TILES))
                this.maxAllowedTiles = ((Integer) this.hints.get(Hints.MAX_ALLOWED_TILES));
    
            //
            // Check source
            //
            if (source instanceof ImageMosaicDescriptor) {
                initReaderFromDescriptor((ImageMosaicDescriptor) source, uHints);
            } else {
                try {
                    initReaderFromURL(source, uHints);
                } catch (Exception e) {
                    throw new DataSourceException(e);
                }
            }
	}
	
	/**
	 * Init this {@link ImageMosaicReader} using the provided {@link ImageMosaicDescriptor} as source. 
	 * @param source
	 * @param uHints
	 * @throws DataSourceException
	 */
    private void initReaderFromDescriptor(final ImageMosaicDescriptor source, final Hints uHints) throws IOException {
        Utilities.ensureNonNull("source", source);
        final MosaicConfigurationBean configuration = source.getConfiguration();
        if (configuration == null) {
            throw new DataSourceException("Unable to create reader for this mosaic since we could not parse the configuration.");
        }
        extractProperties(configuration);
        GranuleCatalog catalog = source.getCatalog();
        if (catalog == null) {
            throw new DataSourceException("Unable to create reader for this mosaic since the inner catalog is null.");
        }

        final SimpleFeatureType schema = catalog.getType(configuration.getCatalogConfigurationBean().getTypeName());
        if (schema == null) {
            throw new DataSourceException("Unable to create reader for this mosaic since the inner catalog schema is null.");
        }
       

        // grid geometry
        setGridGeometry(typeName);
        
        // raster manager
//        coveragesManager = new CoveragesManager(this);
        String name = configuration.getName();
        RasterManager rasterManager = new RasterManager(this, configuration);
        addRasterManager(name, rasterManager);
        
        //TODO: CHECK THAT : Can we have different coverages with different types of data?
//        mosaicManager.defaultSM = configuration.getSampleModel();
    }

    /**
     * Init this {@link ImageMosaicReader} using the provided object as a source referring to an {@link URL}. 
     * 
     * @param source
     * @param uHints
     * @throws DataSourceException
     */
    private void initReaderFromURL(final Object source, final Hints hints) throws Exception {
		this.sourceURL=Utils.checkSource(source,hints);
		if(this.sourceURL==null)
			throw new DataSourceException("This plugin accepts File, URL or String. The string may describe a File or an URL");
		
		//
		// Load properties file with information about levels and envelope
		//
		// TODO: Need to define different mosaicConfigurations when dealing with multiple coverages.
		// Edit coveragesManager accordingly
		MosaicConfigurationBean configuration = Utils.loadMosaicProperties(sourceURL,this.locationAttributeName);
		if(configuration==null){
			//
			// do we have a datastore properties file? It will preempt on the shapefile
			//
        	final File parent=DataUtilities.urlToFile(sourceURL).getParentFile();
			
			// this can be used to look for properties files that do NOT define a datastore
			final File[] properties = parent.listFiles(
					(FilenameFilter)
					FileFilterUtils.and(
							FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter("indexer.properties")),
						FileFilterUtils.and(
								FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter("datastore.properties")),
								FileFilterUtils.makeFileOnly(FileFilterUtils.suffixFileFilter(".properties")
						)
					)
			));
			
			// do we have a valid datastore + mosaic properties pair?
			for(File propFile:properties)
				if(Utils.checkFileReadable(propFile)&&
						Utils.loadMosaicProperties(DataUtilities.fileToURL(propFile), "")!=null){
					configuration = Utils.loadMosaicProperties(DataUtilities.fileToURL(propFile),this.locationAttributeName);
				}               	
			
		}
		if(configuration==null)
			throw new DataSourceException("Unable to create reader for this mosaic since we could not parse the configuration.");
		
		CatalogConfigurationBean catalogConfigurationBean = configuration.getCatalogConfigurationBean();
		// now load the configuration and extract properties from there
		extractProperties(configuration);
		
		//location attribute override
		if(this.hints.containsKey(Hints.MOSAIC_LOCATION_ATTRIBUTE)){
		    this.locationAttributeName=((String)this.hints.get(Hints.MOSAIC_LOCATION_ATTRIBUTE));	
		}
		
		//
		// Load tiles informations, especially the bounds, which will be
		// reused
		//
		try {
			// create the index
		    
		    //TODO: Need to add more schema if any
			granuleCatalog = GranuleCatalogFactory.createGranuleCatalog(sourceURL, catalogConfigurationBean);
			// error
			String typeName = catalogConfigurationBean.getTypeName();
			if(granuleCatalog==null){
			    throw new DataSourceException("Unable to create index for this URL "+sourceURL);
			}
			if (typeName == null) {
			    String[] typeNames = granuleCatalog.getTypeNames();
			    if (typeNames != null && typeNames.length > 0) {
			        typeName = typeNames[0];
			    }
			}
                        final SimpleFeatureType type= granuleCatalog.getType(typeName);
                        if (type==null){
                            throw new IllegalArgumentException("Problems when opening the index, no typenames for the schema are defined");
                        }
                        
		
			// everything is fine
			if (LOGGER.isLoggable(Level.FINE))
				LOGGER.fine("Connected mosaic reader to its index "
						+ sourceURL.toString());

			setGridGeometry(configuration.getEnvelope(), granuleCatalog, typeName);

            //
            // get the crs if able to
            //
            final Object tempCRS = this.hints.get(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM);
            if (tempCRS != null) {
                this.crs = (CoordinateReferenceSystem) tempCRS;
                LOGGER.log(Level.WARNING, "Using forced coordinate reference system ");
            } else {
                final CoordinateReferenceSystem tempcrs = type.getGeometryDescriptor().getCoordinateReferenceSystem();
                if (tempcrs == null) {
                    // use the default crs
                    crs = AbstractGridFormat.getDefaultCRS();
                    LOGGER.log(Level.WARNING, "Unable to find a CRS for this coverage, using a default one" );
                } else
                    crs = tempcrs;
            }
						
			//
			// perform checks on location attribute name
			//
			if(this.locationAttributeName==null) {
			    throw new DataSourceException("The provided name for the location attribute is invalid.");
			} else {
			    if(type.getDescriptor(this.locationAttributeName)==null){
			        // ORACLE fix
			        this.locationAttributeName=this.locationAttributeName.toUpperCase();
			        
			        // try again with uppercase
			        if(type.getDescriptor(this.locationAttributeName)==null){
			            throw new DataSourceException("The provided name for the location attribute is invalid.");
			        }
			    }
			}
			
			// creating the raster manager
//			coveragesManager = new CoveragesManager(this);
			String name = configuration.getName();
			RasterManager rasterManager = new RasterManager(this, configuration);
		        addRasterManager(name, rasterManager);
		}
		catch (Throwable e) {
			try {
				if(granuleCatalog!=null){
				    granuleCatalog.dispose();
				}
			} catch (Throwable e1) {
				if (LOGGER.isLoggable(Level.FINEST)){
				    LOGGER.log(Level.FINEST, e1.getLocalizedMessage(), e1);
				}
			}
			finally{
			    granuleCatalog=null;
			}
			
			// dispose raster manager as well
//			try {
//				if (coveragesManager != null) {
//					coveragesManager.dispose();
//                }
//			} catch (Throwable e1) {
//				if (LOGGER.isLoggable(Level.FINEST))
//					LOGGER.log(Level.FINEST, e1.getLocalizedMessage(), e1);
//			}
//			finally{
//				coveragesManager = null;
//			}
						
			// rethrow
			throw new  DataSourceException(e);
		}

		
	}

	private void setGridGeometry(final ReferencedEnvelope envelope, final GranuleCatalog catalog, String typeName) {
		Utilities.ensureNonNull("index", catalog);
	    //
        // save the bbox and prepare other info
        //
        final BoundingBox bounds = catalog.getBounds(typeName);
        if(bounds.isEmpty()) {
                throw new IllegalArgumentException("Cannot create a mosaic out of an empty index");
        }
        
        // we might have an imposed bbox
        this.crs=bounds.getCoordinateReferenceSystem();
        if(envelope==null)
        	this.originalEnvelope=new GeneralEnvelope(bounds);
        else{
        	this.originalEnvelope=new GeneralEnvelope(envelope);
        	this.originalEnvelope.setCoordinateReferenceSystem(crs);
        }
        
        // original gridrange (estimated). I am using the floor here in order to make sure
        // we always stays inside the real area that we have for the granule
        originalGridRange = new GridEnvelope2D(
                        new Rectangle(
                                        (int) (originalEnvelope.getSpan(0)/ highestRes[0]), 
                                        (int) (originalEnvelope.getSpan(1)/ highestRes[1])
                                        )
                        );
        raster2Model= new AffineTransform2D(
                highestRes[0], 
                0, 
                0, 
                -highestRes[1], 
                originalEnvelope.getLowerCorner().getOrdinate(0)+0.5*highestRes[0], 
                originalEnvelope.getUpperCorner().getOrdinate(1)-0.5*highestRes[1]);   
		
	}

	private void setGridGeometry (final String typeName) {
	    setGridGeometry(null, granuleCatalog, typeName); 
    }
	private void extractProperties(final MosaicConfigurationBean configuration) throws IOException {

		// resolutions levels
		numOverviews = configuration.getLevelsNum() - 1;
		final double[][] resolutions = configuration.getLevels();
		overViewResolutions = numOverviews >= 1 ? new double[numOverviews][2]: null;
		highestRes = new double[2];
		highestRes[0] = resolutions[0][0];
		highestRes[1] = resolutions[0][1];

		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.fine(new StringBuilder("Highest res ").append(highestRes[0])
					.append(" ").append(highestRes[1]).toString());

		if (numOverviews > 0){
	   		for (int i = 0; i < numOverviews; i++) {     			
				overViewResolutions[i][0] = resolutions[i+1][0];
				overViewResolutions[i][1] = resolutions[i+1][1];
	   		}	
		}
	
		// name
		coverageName = configuration.getName();

		// need a color expansion?
		// this is a newly added property we have to be ready to the case where
		// we do not find it.
		expandMe = configuration.isExpandToRGB();
		
		CatalogConfigurationBean catalogConfigurationBean = configuration.getCatalogConfigurationBean();
		
		// do we have heterogenous granules
		heterogeneousGranules = catalogConfigurationBean.isHeterogeneous();

		// absolute or relative path
		pathType = catalogConfigurationBean.isAbsolutePath()?PathType.ABSOLUTE:PathType.RELATIVE;
		
		//
		// location attribute
		//
		locationAttributeName = catalogConfigurationBean.getLocationAttribute();
		
		// suggested SPI
		final String suggestedSPIClass = catalogConfigurationBean.getSuggestedSPI();
		if (suggestedSPIClass != null){
			try {
				final Class<?> clazz=Class.forName(suggestedSPIClass);
				if(clazz.newInstance() instanceof ImageReaderSpi)
					suggestedSPI=(ImageReaderSpi)clazz.newInstance();
				else
					suggestedSPI=null;
			} catch (Exception e) {
				if(LOGGER.isLoggable(Level.FINE))
					LOGGER.log(Level.FINE,e.getLocalizedMessage(),e);
				suggestedSPI=null;
			} 
		}

		// caching for the index
		cachingIndex = catalogConfigurationBean.isCaching();
		
		// imposed BBOX
                if(configuration.getEnvelope()!=null){
            		this.imposedBBox=true;
            		// we set the BBOX later to retain also the CRS
                } else {
                	this.imposedBBox=false;
                }
		
		// typeName to be used for reading the mosaic
		this.typeName = catalogConfigurationBean.getTypeName();

	}

	/**
	 * Constructor.
	 * 
	 * @param source
	 *            The source object.
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * 
	 */
	public ImageMosaicReader(Object source) throws IOException {
		this(source, null);

	}

	/**
	 * 
	 * @see org.opengis.coverage.grid.GridCoverageReader#getFormat()
	 */
	public Format getFormat() {
		return new ImageMosaicFormat();
	}

    public GridCoverage2D read(GeneralParameterValue[] params) throws IOException {
	     return read (UNSPECIFIED, params); 
	}

	/**
	 * 
	 * @see org.opengis.coverage.grid.GridCoverageReader#read(org.opengis.parameter.GeneralParameterValue[])
	 * @Override
	 */
	public GridCoverage2D read(String coverageName, GeneralParameterValue[] params) throws IOException {
	   
	    // check if we were disposed already
	    if(rasterManagers == null){
	        throw new IOException("Looks like this reader has been already disposed or it has not been properly initialized.");
	    }
		if (LOGGER.isLoggable(Level.FINE)) {
    		    if (sourceURL != null) {
    			LOGGER.fine("Reading mosaic from " + sourceURL.toString());
    		    } else {
    		        LOGGER.fine("Reading mosaic");
    		    }
		    LOGGER.fine("Highest res "+highestRes[0]+" "+highestRes[1]);
		}
		//
		// add max allowed tiles if missing
		//
		if(this.maxAllowedTiles!=ImageMosaicFormat.MAX_ALLOWED_TILES.getDefaultValue()){
			if(params!=null){
				// first thing let's see if we have it already, in which case we do nothing since a read parameter override a Hint
				boolean found=false;
				for(GeneralParameterValue pv:params)
				{
					if(pv.getDescriptor().getName().equals(ImageMosaicFormat.MAX_ALLOWED_TILES.getName()))
					{
						found=true;
						break;
					}
				}
				
				//ok, we did not find it, let's add it back 
				if(!found)
				{
					final GeneralParameterValue[] temp = new GeneralParameterValue[params.length+1];
					System.arraycopy(params, 0, temp, 0, params.length);
					ParameterValue<Integer> tempVal = ImageMosaicFormat.MAX_ALLOWED_TILES.createValue();
					tempVal.setValue(this.maxAllowedTiles);
					temp[params.length]=tempVal;
				}
			}
			else
			{
				// we do not have nay read params, we have to create the array for them
				ParameterValue<Integer> tempVal = ImageMosaicFormat.MAX_ALLOWED_TILES.createValue();
				tempVal.setValue(this.maxAllowedTiles);
				params= new GeneralParameterValue[]{tempVal};
			}
				
		}
		
		//
		// Loading tiles trying to optimize as much as possible
		//
		final Collection<GridCoverage2D> response = read(params, coverageName);
		if (response.isEmpty()) {
		    if (LOGGER.isLoggable(Level.FINE)){
		        LOGGER.fine("The response is empty. ==> returning a null GridCoverage");
		    }
		    return null;
		} else {
			return response.iterator().next();
		}
	}
	
	  /**
	     * Look for the parameter containing the coverage name and check its validity.
	     * Then delegate the proper RasterManager to do the read operation.
	     * @param params
	     * @return
	     * @throws IOException
	     */
	    private Collection<GridCoverage2D> read(GeneralParameterValue[] params, String coverageName) throws IOException {
	        /*String coverageName = UNSPECIFIED;
	        if (params != null) {
	            for (GeneralParameterValue gParam : params) {
	                if(gParam instanceof ParameterValue<?>){                
	                    final ParameterValue<?> param = (ParameterValue<?>) gParam;
	                    final ReferenceIdentifier name = param.getDescriptor().getName();
	                    if (name.equals(ImageMosaicFormat.COVERAGE_NAME.getName())) {
	                        coverageName = (String) param.getValue();
	                    }
	                }
	            }
	        }*/
	        coverageName = checkUnspecifiedCoverage(coverageName);
	        return getRasterManager(coverageName).read(params);
	    }

	/**
	 * Package private accessor for {@link Hints}.
	 * 
	 * @return this {@link Hints} used by this reader.
	 */
	Hints getHints(){
		return super.hints;
	}
	
	/**
	 * Package private accessor for the highest resolution values.
	 * 
	 * @return the highest resolution values.
	 */
	double[] getHighestRes(){
		return super.highestRes;
	}
	
	/**
	 * 
	 * @return
	 */
	double[][] getOverviewsResolution(){
		return super.overViewResolutions;
	}
	
	int getNumberOfOvervies(){
		return super.numOverviews;
	}
	

    /** Package scope grid to world transformation accessor */
    MathTransform getRaster2Model() {
        return raster2Model;
    }
    
    /**
     * Let us retrieve the {@link GridCoverageFactory} that we want to use.
     * 
     * @return
     * 			retrieves the {@link GridCoverageFactory} that we want to use.
     */
    GridCoverageFactory getGridCoverageFactory(){
    	return coverageFactory;
    }

//	String getName() {
//		return mosaicManager.getName();
//	}

    /**
     * Number of coverages for this reader is 1
     * 
     * @return the number of coverages for this reader.
     */
    @Override
    public int getGridCoverageCount() {
        return names.size();
    }

    /**
     * Releases resources held by this reader.
     * 
     */
    @Override
    public synchronized void dispose() {
        super.dispose();
        synchronized (this) {
            try {
                if (granuleCatalog != null)
                    this.granuleCatalog.dispose();

                if (rasterManagers != null) {
                    Set<String> keys = rasterManagers.keySet();
                    for (String key: keys) {
                        rasterManagers.get(key).dispose();
                    }
                    rasterManagers.clear();
                    rasterManagers = null;
                }
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            }
        }
    }

    @Override
    public String[] getMetadataNames() {
        return getMetadataNames(UNSPECIFIED);
    }

    /**
     * Populate the metadata names array for the specified coverageName
     * @param coverageName
     * @return
     */
    @Override
    public String[] getMetadataNames(String coverageName) {
        String name = checkUnspecifiedCoverage(coverageName);
        final List<String> metadataNames = new ArrayList<String>();
        metadataNames.add(GridCoverage2DReader.TIME_DOMAIN);
        metadataNames.add(GridCoverage2DReader.HAS_TIME_DOMAIN);
        metadataNames.add(GridCoverage2DReader.TIME_DOMAIN_MINIMUM);
        metadataNames.add(GridCoverage2DReader.TIME_DOMAIN_MAXIMUM);
        metadataNames.add(GridCoverage2DReader.TIME_DOMAIN_RESOLUTION);
        metadataNames.add(GridCoverage2DReader.ELEVATION_DOMAIN);
        metadataNames.add(GridCoverage2DReader.ELEVATION_DOMAIN_MINIMUM);
        metadataNames.add(GridCoverage2DReader.ELEVATION_DOMAIN_MAXIMUM);
        metadataNames.add(GridCoverage2DReader.HAS_ELEVATION_DOMAIN);
        metadataNames.add(GridCoverage2DReader.ELEVATION_DOMAIN_RESOLUTION);
        RasterManager manager = getRasterManager(name);
        if (manager.domainsManager != null) {
            metadataNames.addAll(manager.domainsManager.getMetadataNames());
        }
        return metadataNames.toArray(new String[metadataNames.size()]);
    }

    @Override
    public String getMetadataValue(final String name) {
        return getMetadataValue(UNSPECIFIED, name);
    }

    @Override
    public String getMetadataValue(String coverageName, final String name) {
        coverageName = checkUnspecifiedCoverage(coverageName);
        String value = null;
        RasterManager manager = getRasterManager(coverageName);
        final boolean hasTimeDomain = manager.timeDomainManager != null;
        final boolean hasElevationDomain = manager.elevationDomainManager != null;

        if (name.equalsIgnoreCase(GridCoverage2DReader.HAS_ELEVATION_DOMAIN))
            return String.valueOf(hasElevationDomain);

        if (name.equalsIgnoreCase(GridCoverage2DReader.HAS_TIME_DOMAIN)) {
            return String.valueOf(hasTimeDomain);
        }

        // NOT supported
        if (name.equalsIgnoreCase(GridCoverage2DReader.TIME_DOMAIN_RESOLUTION)) {
            return null;
        }
        // NOT supported
        if (name.equalsIgnoreCase(GridCoverage2DReader.ELEVATION_DOMAIN_RESOLUTION)) {
            return null;
        }

        if (hasTimeDomain) {
            if (name.equalsIgnoreCase("time_domain")) {
                return manager.timeDomainManager.getMetadataValue(name);
            }
            if ((name.equalsIgnoreCase("time_domain_minimum") || name
                    .equalsIgnoreCase("time_domain_maximum"))) {
                return manager.timeDomainManager.getMetadataValue(name);
            }
        }

        if (hasElevationDomain) {
            if (name.equalsIgnoreCase("elevation_domain")) {
                return manager.elevationDomainManager.getMetadataValue(name);
            }

            if (name.equalsIgnoreCase("elevation_domain_minimum")
                    || name.equalsIgnoreCase("elevation_domain_maximum")) {
                return manager.elevationDomainManager.getMetadataValue(name);
            }
        }

        // check additional domains
        if (manager.domainsManager != null) {
            return manager.domainsManager.getMetadataValue(name);
        }

        //
        return value;
    }

    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters() {
        return getDynamicParameters(UNSPECIFIED);
    }
    
    @Override
    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName) {
        coverageName = checkUnspecifiedCoverage(coverageName);
        RasterManager manager =  getRasterManager(coverageName);
        return (Set<ParameterDescriptor<List>>) (manager.domainsManager != null ? manager.domainsManager
                .getDynamicParameters() :  Collections.emptySet());
    }

    public boolean isParameterSupported(Identifier name) {
        return isParameterSupported(UNSPECIFIED, name);
    }

        /**
         * Check whether the specified parameter is supported for the specified coverage.
         * @param coverageName
         * @param parameterName
         * @return
         */
        public boolean isParameterSupported(String coverageName, Identifier parameterName) {
            coverageName = checkUnspecifiedCoverage(coverageName);
            RasterManager manager = getRasterManager(coverageName);
            return manager.domainsManager != null ? manager.domainsManager.isParameterSupported(parameterName) : false;
    }

    /**
     * Checker whether the specified coverageName is supported. In case the name is Unspecified and the manager only has 1 coverage, then it returns
     * the only available coverage name (using default to speed up the response without need to access the set through an iterator). In case of
     * multiple coverages, throws an Exceptions if the coverage name is unspecified.
     * 
     * @param coverageName
     */
    private String checkUnspecifiedCoverage(String coverageName) {
        if (coverageName.equalsIgnoreCase(UNSPECIFIED)) {
            if (getGridCoverageCount() > 1) {
                throw new IllegalArgumentException(
                        "Need to specify the coverageName for a reader related to multiple coverages");
            } else {
                return defaultName;
            }
        } else {
            if (!names.contains(coverageName)) {
                throw new IllegalArgumentException("The specified coverageName is unavailable");
            } else {
                return coverageName;
            }
        }
    }

    private void addRasterManager(String name, RasterManager manager) {
        rasterManagers.put(name, manager);
        names.add(name);
        if (defaultName == null) {
            defaultName = name;
        }
    }

}
