/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
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

import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.spi.ImageReaderSpi;
import javax.media.jai.ImageLayout;
import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.factory.Hints;
import org.geotools.gce.image.WorldImageFormat;
import org.geotools.gce.imagemosaic.Utils.Prop;
import org.geotools.gce.imagemosaic.catalog.CatalogConfigurationBean;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalog;
import org.geotools.gce.imagemosaic.catalog.index.Indexer;
import org.geotools.gce.imagemosaic.catalogbuilder.CatalogBuilderConfiguration;
import org.geotools.gce.imagemosaic.catalogbuilder.MosaicBeanBuilder;
import org.geotools.gce.imagemosaic.properties.PropertiesCollector;
import org.geotools.gce.imagemosaic.properties.PropertiesCollectorFinder;
import org.geotools.gce.imagemosaic.properties.PropertiesCollectorSPI;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Utilities;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.sun.media.imageioimpl.common.BogusColorSpace;

/**
 * This class is in responsible for creating the index for a mosaic of images that we want to tie together as a single coverage.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * 
 * @source $URL$
 */
@SuppressWarnings("rawtypes")
public class ImageMosaicWalker implements Runnable {

    List<PropertiesCollector> propertiesCollectors = null;
    
    Map<String, MosaicConfigurationBean> configurations = new HashMap<String, MosaicConfigurationBean>();
    
    /** Default Logger * */
    final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(ImageMosaicWalker.class);

    static abstract public class ProcessingEventListener implements EventListener {
        abstract public void getNotification(final ProcessingEvent event);
        abstract public void exceptionOccurred(final ExceptionEvent event);
    }

    /**
     * @author Simone Giannecchini, GeoSolutions.
     * 
     */
    static public class ProcessingEvent extends EventObject {

        private static final long serialVersionUID = 6930580659705360225L;
        private String message = null;
        private double percentage = 0;

        /**
         * @param source
         */
        public ProcessingEvent(final Object source, final String message, final double percentage) {
            super(source);
            this.message = message;
            this.percentage = percentage;
        }

        public double getPercentage() {
            return percentage;
        }

        public String getMessage() {
            return message;
        }

    }
    
    /**
     * A special ProcessingEvent raised when a file has completed/failed ingestion
     */
    static public class FileProcessingEvent extends ProcessingEvent {
        private File file;
        private boolean ingested;

        /**
         * @param source
         */
        public FileProcessingEvent(final Object source, final File file, final boolean ingested, final String message, final double percentage) {
            super(source, message, percentage);
            this.file = file;
            this.ingested = ingested;
        }

        public File getFile() {
            return file;
        }

        public boolean isIngested() {
            return ingested;
        }

        

    }

    /**
     * Event launched when an exception occurs. Percentage and message may be missing, in this case they will be -1 and the exception message
     * (localized if available, standard otherwise)
     * 
     * @author aaime, TOPP.
     * 
     */
    static public final class ExceptionEvent extends ProcessingEvent {

        private static final long serialVersionUID = 2272452028229922551L;

        private Exception exception;

        public ExceptionEvent(Object source, String message, double percentage, Exception exception) {
            super(source, message, percentage);
            this.exception = exception;
        }

        public ExceptionEvent(Object source, Exception exception) {
            super(source, Utils.getMessageFromException(exception), -1);
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }

    }

    /**
     * Private Class which simply fires the events using a copy of the listeners list in order to avoid problems with listeners that remove themselves
     * or are removed by someone else
     */
    final static class ProgressEventDispatchThreadEventLauncher implements Runnable {

        /**
         * The event we want to fire away.
         */
        private ProcessingEvent event;

        /**
         * The list of listeners.
         */
        private Object[] listeners;

        /**
         * Default constructor.
         */
        ProgressEventDispatchThreadEventLauncher() {
        }

        /**
         * Used to send an event to an array of listeners.
         * 
         * @param evt is the {@link ProcessingEvent} to send.
         * @param listeners is the array of {@link ProcessingEventListener}s to notify.
         */
        synchronized void setEvent(final ProcessingEvent evt, final Object[] listeners) {
            if (listeners == null || evt == null)
                throw new NullPointerException("Input argumentBuilder cannot be null");
            this.listeners = listeners;
            this.event = evt;
        }

        /** 
         * Run the event launcher
         */
        public void run() {
            final int numListeners = listeners.length;
            if (event instanceof ExceptionEvent)
                for (int i = 0; i < numListeners; i++)
                    ((ProcessingEventListener) listeners[i])
                            .exceptionOccurred((ExceptionEvent) this.event);
            else
                for (int i = 0; i < numListeners; i++)
                    ((ProcessingEventListener) listeners[i]).getNotification(this.event);
        }
    }

    /**
     * This class is responsible for walking through the files inside a directory (and its children directories) which respect a specified wildcard.
     * 
     * <p>
     * Its role is basically to simplify the construction of the mosaic by implementing a visitor pattern for the files that we have to use for the
     * index.
     * 
     * <p>
     * It is based on the Commons IO {@link DirectoryWalker} class.
     * 
     * @author Simone Giannecchini, GeoSolutions SAS
     * @author Daniele Romagnoli, GeoSolutions SAS
     * 
     */
    final class MosaicDirectoryWalker extends DirectoryWalker {

        private DefaultTransaction transaction;

        private volatile boolean canceled;

        @Override
        protected void handleCancelled(File startDirectory, Collection results,
                CancelException cancel) throws IOException {
            super.handleCancelled(startDirectory, results, cancel);
            // clean up objects and rollback transaction
            if (LOGGER.isLoggable(Level.INFO))
                LOGGER.info("Stop requested when walking directory " + startDirectory);
            super.handleEnd(results);
        }

        @Override
        protected boolean handleIsCancelled(final File file, final int depth, Collection results)
                throws IOException {

            //
            // Anyone has asked us to stop?
            //
            if (!checkStop()) {
                canceled = true;
                return true;
            }
            return false;
        }

        @Override
        protected void handleFile(final File fileBeingProcessed, final int depth,
                final Collection results) throws IOException {

            // increment counter
            fileIndex++;

            //
            // Check that this file is actually good to go
            //
            if (!checkFile(fileBeingProcessed))
                return;

            // replacing chars on input path
            String validFileName;
            SimpleFeatureType indexSchema = null;
            try {
                validFileName = fileBeingProcessed.getCanonicalPath();
                validFileName = FilenameUtils.normalize(validFileName);
            } catch (IOException e) {
                fireFileEvent(Level.FINER, fileBeingProcessed, false, 
                        "Exception occurred while processing file " + fileBeingProcessed + ": " + e.getMessage(), ((fileIndex * 100.0) / numFiles));
                fireException(e);
                return;
            }
            validFileName = FilenameUtils.getName(validFileName);
            fireEvent(Level.INFO, "Now indexing file " + validFileName,
                    ((fileIndex * 100.0) / numFiles));
            GridCoverage2DReader coverageReader = null;
            try {
                // STEP 1
                // Getting a coverage reader for this coverage.
                //
                final AbstractGridFormat format;
                final Hints configurationHints = runConfiguration.getHints();
//                if (runConfiguration.getIndexer() != null) {
//                    configurationHints.add(new RenderingHints(Utils.AUXILIARY_FILES_PATH, indexerFile.getAbsolutePath()));
//                }
                
                if (cachedFormat == null) {
                    // When looking for formats which may parse this file, make sure to exclude the ImageMosaicFormat as return
                    format = (AbstractGridFormat) GridFormatFinder.findFormat(fileBeingProcessed, excludeMosaicHints);
                } else {
                    if (cachedFormat.accepts(fileBeingProcessed)) {
                        format = cachedFormat;
                    } else {
                        format = new UnknownFormat();
                    }
                }
                if ((format instanceof UnknownFormat) || format == null) {
                    fireFileEvent(
                            Level.INFO,
                            fileBeingProcessed, false,
                            "Skipped file " + fileBeingProcessed + ": File format is not supported.",
                            ((fileIndex * 99.0) / numFiles));
                    return;
                }
                cachedFormat = format;
                coverageReader = (GridCoverage2DReader) format.getReader(fileBeingProcessed, configurationHints);
                
                // Getting available coverageNames from the reader
                String [] coverageNames = coverageReader.getGridCoverageNames();
                CatalogBuilderConfiguration catalogConfig;
                for (String cvName : coverageNames) {
                    
                    final String inputCoverageName = cvName;
                    String coverageName = coverageReader instanceof StructuredGridCoverage2DReader ? inputCoverageName
                            : runConfiguration.getIndexName();

                    // checking whether the coverage already exists 
                    final boolean coverageExists = coverageExists(coverageName);
                    MosaicConfigurationBean mosaicConfiguration = null;
                    MosaicConfigurationBean currentConfigurationBean = null;
                    RasterManager rasterManager = null;
                    if (coverageExists) {
                        
                        // Get the manager for this coverage so it can be updated
                        rasterManager = parentReader.getRasterManager(coverageName);
                        mosaicConfiguration = rasterManager.getConfiguration();
                    }
                    
                    // STEP 2
                    // Collecting all Coverage properties to setup a MosaicConfigurationBean through
                    // the builder
                    final MosaicBeanBuilder configBuilder = new MosaicBeanBuilder();
                    
                    final GeneralEnvelope envelope = (GeneralEnvelope) coverageReader.getOriginalEnvelope(cvName);
                    final CoordinateReferenceSystem actualCRS = coverageReader.getCoordinateReferenceSystem(cvName);

                    SampleModel sm = null;
                    ColorModel cm = null;
                    int numberOfLevels = 1;
                    double[][] resolutionLevels = null;

                    if (mosaicConfiguration == null) {
                        catalogConfig = runConfiguration;
                        // We don't have a configuration for this configuration 
                        
                        // Get the type specifier for this image and the check that the
                        // image has the correct sample model and color model.
                        // If this is the first cycle of the loop we initialize everything.
                        //
                        ImageLayout layout = coverageReader.getImageLayout(inputCoverageName);
                        cm = layout.getColorModel(null);
                        sm = layout.getSampleModel(null);
                        numberOfLevels = coverageReader.getNumOverviews(inputCoverageName) + 1;
                        resolutionLevels = coverageReader.getResolutionLevels(inputCoverageName);

                        // at the first step we initialize everything that we will
                        // reuse afterwards starting with color models, sample
                        // models, crs, etc....

                        configBuilder.setSampleModel(sm);
                        configBuilder.setColorModel(cm);
                        ColorModel defaultCM = cm;

                        // Checking palette
                        if (defaultCM instanceof IndexColorModel) {
                            IndexColorModel icm = (IndexColorModel) defaultCM;
                            int numBands = defaultCM.getNumColorComponents();
                            byte[][] defaultPalette = new byte[3][icm.getMapSize()];
                            icm.getReds(defaultPalette[0]);
                            icm.getGreens(defaultPalette[0]);
                            icm.getBlues(defaultPalette[0]);
                            if (numBands == 4) {
                                icm.getAlphas(defaultPalette[0]);
                            }
                            configBuilder.setPalette(defaultPalette);
                        }

                        // STEP 2.A
                        // Preparing configuration 
                        configBuilder.setCrs(actualCRS);
                        configBuilder.setLevels(resolutionLevels);
                        configBuilder.setLevelsNum(numberOfLevels);
                        configBuilder.setName(coverageName);
                        configBuilder.setTimeAttribute(runConfiguration.getTimeAttribute());
                        configBuilder.setElevationAttribute(runConfiguration.getElevationAttribute());
                        configBuilder.setAdditionalDomainAttributes(runConfiguration.getAdditionalDomainAttribute());

                        final CatalogConfigurationBean catalogConfigurationBean = new CatalogConfigurationBean();
                        catalogConfigurationBean.setCaching(runConfiguration.isCaching());
                        catalogConfigurationBean.setAbsolutePath(runConfiguration.isAbsolute());
                        catalogConfigurationBean.setLocationAttribute(runConfiguration.getLocationAttribute());
                        configBuilder.setCatalogConfigurationBean(catalogConfigurationBean);

                        currentConfigurationBean = configBuilder.getMosaicConfigurationBean();

                        // creating the schema
                        indexSchema = CatalogManager.createSchema(runConfiguration, currentConfigurationBean.getName(), actualCRS);

                        // Creating a rasterManager which will be initialized after populating the catalog
                        rasterManager = parentReader.addRasterManager(currentConfigurationBean, false);

                        // Creating a granuleStore
                        parentReader.createCoverage(coverageName, indexSchema);
                        configurations.put(currentConfigurationBean.getName(), currentConfigurationBean);

                    } else {
                        catalogConfig = new CatalogBuilderConfiguration();
                        CatalogConfigurationBean bean = mosaicConfiguration.getCatalogConfigurationBean();
                        catalogConfig.setLocationAttribute(bean.getLocationAttribute());
                        catalogConfig.setAbsolute(bean.isAbsolutePath());
                        catalogConfig.setRootMosaicDirectory(runConfiguration.getRootMosaicDirectory());
                        // We already have a Configuration for this coverage.
                        // Check its properties are compatible with the existing coverage.
                        
                        CatalogConfigurationBean catalogConfigurationBean = bean;
                        if (!catalogConfigurationBean.isHeterogeneous()) {

                            // There is no need to check resolutions if the mosaic
                            // has been already marked as heterogeneous

                            numberOfLevels = coverageReader.getNumOverviews(inputCoverageName) + 1;
                            boolean needUpdate = false;

                            // 
                            // Heterogeneousity check
                            //
                            if (numberOfLevels != mosaicConfiguration.getLevelsNum()) {
                                catalogConfigurationBean.setHeterogeneous(true);
                                if (numberOfLevels > mosaicConfiguration.getLevelsNum()) {
                                    resolutionLevels = coverageReader.getResolutionLevels(inputCoverageName);
                                    mosaicConfiguration.setLevels(resolutionLevels);
                                    mosaicConfiguration.setLevelsNum(numberOfLevels);
                                    needUpdate = true;
                                }
                            } else {
                                final double[][] mosaicLevels = mosaicConfiguration.getLevels();
                                resolutionLevels = coverageReader.getResolutionLevels(inputCoverageName);
                                final boolean homogeneousLevels = Utils.homogeneousCheck(numberOfLevels, resolutionLevels, mosaicLevels);
                                if (!homogeneousLevels) {
                                    catalogConfigurationBean.setHeterogeneous(true);
                                    needUpdate = true;
                                }
                            }
                            // configuration need to be updated
                            if (needUpdate) {
                                configurations.put(mosaicConfiguration.getName(), mosaicConfiguration);
                            }
                        }
                        ImageLayout layout = coverageReader.getImageLayout(inputCoverageName);
                        cm = layout.getColorModel(null);
                        sm = layout.getSampleModel(null);

                        // comparing ColorModel
                        // comparing SampeModel
                        // comparing CRSs
                        ColorModel actualCM = cm;
                        CoordinateReferenceSystem expectedCRS; 
                        if(mosaicConfiguration.getCrs() != null) {
                            expectedCRS = mosaicConfiguration.getCrs();
                        } else {
                            expectedCRS = rasterManager.spatialDomainManager.coverageCRS;
                        }
                        if (!(CRS.equalsIgnoreMetadata(expectedCRS, actualCRS))) {
                            // if ((fileIndex > 0 ? !(CRS.equalsIgnoreMetadata(defaultCRS, actualCRS)) : false)) {
                            fireFileEvent(Level.INFO, fileBeingProcessed, false,
                                    "Skipping image " + fileBeingProcessed + " because CRSs do not match.",
                                    (((fileIndex + 1) * 99.0) / numFiles));
                            return;
                        }

                        byte[][] palette = mosaicConfiguration.getPalette();
                        ColorModel colorModel = mosaicConfiguration.getColorModel();
                        if(colorModel == null) {
                            palette = rasterManager.getConfiguration().getPalette();
                            colorModel = rasterManager.defaultCM;
                        }
                        if (checkColorModels(colorModel, palette, mosaicConfiguration, actualCM)) {
                            // if (checkColorModels(defaultCM, defaultPalette, actualCM)) {
                            fireFileEvent(Level.INFO, fileBeingProcessed, false,
                                    "Skipping image " + fileBeingProcessed + " because color models do not match.",
                                    (((fileIndex + 1) * 99.0) / numFiles));
                            return;
                        }
                    }

                    // STEP 3
                    // create and store features
                    CatalogManager.updateCatalog(coverageName, fileBeingProcessed, coverageReader,
                            parentReader, catalogConfig, envelope, transaction,
                            propertiesCollectors);
                    // fire event
                    fireFileEvent(Level.FINE, fileBeingProcessed, true, "Done with file " + fileBeingProcessed,
                            (((fileIndex + 1) * 99.0) / numFiles));

                }
            } catch (IOException e) {
                fireException(e);
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                fireException(e);
                return;
            } finally {
                // ////////////////////////////////////////////////////////
                //
                // STEP 5
                //
                // release resources
                //
                // ////////////////////////////////////////////////////////
                try {
                    if (coverageReader != null)
                        // release resources
                        coverageReader.dispose();
                } catch (Throwable e) {
                    // ignore exception
                    if (LOGGER.isLoggable(Level.FINEST))
                        LOGGER.log(Level.FINEST, e.getLocalizedMessage(), e);
                }
            }

            super.handleFile(fileBeingProcessed, depth, results);
        }

        /**
         * Check whether the specified coverage already exist in the reader.
         * This allows to get the rasterManager associated with that coverage instead
         * of creating a new one.
         * 
         * @param coverageName the name of the coverage to be searched
         * @return {@code true} in case that coverage already exists
         * @throws IOException
         */
        private boolean coverageExists(String coverageName) throws IOException {
            String[] coverages = parentReader.getGridCoverageNames();
            for (String coverage: coverages) {
                if (coverage.equals(coverageName)) {
                    return true;
                }
            }
            return false;
        }

        private boolean checkStop() {
            if (getStop()) {
                fireEvent(Level.INFO, "Stopping requested at file  " + fileIndex + " of "
                        + numFiles + " files", ((fileIndex * 100.0) / numFiles));
                return false;
            }
            return true;
        }

        private boolean checkFile(final File fileBeingProcessed) {
            if (!fileBeingProcessed.exists() || !fileBeingProcessed.canRead()
                    || !fileBeingProcessed.isFile()) {
                // send a message
                fireEvent(Level.INFO, "Skipped file " + fileBeingProcessed
                        + " snce it seems invalid", ((fileIndex * 99.0) / numFiles));
                return false;
            }
            return true;
        }

        public MosaicDirectoryWalker(final List<String> indexingDirectories,
                final FileFilter filter) throws IOException {
            super(filter, Integer.MAX_VALUE);// runConfiguration.isRecursive()?Integer.MAX_VALUE:0);

            this.transaction = new DefaultTransaction("MosaicCreationTransaction"
                    + System.nanoTime());
            indexingPreamble();

            try {
                // start walking directories
                for (String indexingDirectory : indexingDirectories) {
                    walk(new File(indexingDirectory), null);

                    // did we cancel?
                    if (canceled)
                        break;
                }
                // did we cancel?
                if (canceled)
                    transaction.rollback();
                else
                    transaction.commit();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failure occurred while collecting the granules", e);
                transaction.rollback();
            } finally {
                try {
                    transaction.close();
                } catch (Exception e) {
                    final String message = "Unable to close indexing" + e.getLocalizedMessage();
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, message, e);
                    }
                    // notify listeners
                    fireException(e);
                }

                try {
                    indexingPostamble(!canceled);
                } catch (Exception e) {
                    final String message = "Unable to close indexing" + e.getLocalizedMessage();
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, message, e);
                    }
                    // notify listeners
                    fireException(e);
                }
            }
        }

        /**
         * This method checks the {@link ColorModel} of the current image with the one of the first image in order to check if they are compatible or
         * not in order to perform a mosaic operation.
         * 
         * <p>
         * It is worth to point out that we also check if, in case we have two index color model image, we also try to suggest whether or not we
         * should do a color expansion.
         * 
         * @param defaultCM
         * @param defaultPalette
         * @param actualCM
         * @return a boolean asking to skip this feature.
         */
        private boolean checkColorModels(ColorModel defaultCM, byte[][] defaultPalette, MosaicConfigurationBean configuration, ColorModel actualCM) {
            //
            //
            // ComponentColorModel
            //
            //
            
            if (defaultCM instanceof ComponentColorModel && actualCM instanceof ComponentColorModel) {
                final ComponentColorModel defCCM = (ComponentColorModel) defaultCM, actualCCM = (ComponentColorModel) actualCM;
                final ColorSpace defCS = defCCM.getColorSpace();
                final ColorSpace actualCS = actualCCM.getColorSpace();
                final boolean isBogusDef = defCS instanceof BogusColorSpace;
                final boolean isBogusActual = actualCS instanceof BogusColorSpace;
                final boolean colorSpaceIsOk;
                if (isBogusDef && isBogusActual) {
                    final BogusColorSpace def = (BogusColorSpace) defCS;
                    final BogusColorSpace act = (BogusColorSpace) actualCS;
                    colorSpaceIsOk = def.getNumComponents() == act.getNumComponents()
                            && def.isCS_sRGB() == act.isCS_sRGB() && def.getType() == act.getType();
                } else
                    colorSpaceIsOk = defCS.equals(actualCS);

                return !(defCCM.getNumColorComponents() == actualCCM.getNumColorComponents()
                        && defCCM.hasAlpha() == actualCCM.hasAlpha() && colorSpaceIsOk
                        && defCCM.getTransparency() == actualCCM.getTransparency() && defCCM
                            .getTransferType() == actualCCM.getTransferType());
            }

            //
            //
            // IndexColorModel
            //
            //

            if (defaultCM instanceof IndexColorModel && actualCM instanceof IndexColorModel) {
                final IndexColorModel defICM = (IndexColorModel) defaultCM, actualICM = (IndexColorModel) actualCM;
                if (defICM.getNumColorComponents() != actualICM.getNumColorComponents()
                        || defICM.hasAlpha() != actualICM.hasAlpha()
                        || !defICM.getColorSpace().equals(actualICM.getColorSpace())
                        || defICM.getTransferType() != actualICM.getTransferType())
                    return true;

                //
                // Suggesting expansion in the simplest case
                //
                if (defICM.getMapSize() != actualICM.getMapSize()
                        || defICM.getTransparency() != actualICM.getTransparency()
                        || defICM.getTransferType() != actualICM.getTransferType()
                        || defICM.getTransparentPixel() != actualICM.getTransparentPixel()) {
                    configuration.setExpandToRGB(true);
                    return false;
                }

                //
                // Now checking palettes to see if we need to do a color convert
                //
                // get the palette for this color model
                int numBands = actualICM.getNumColorComponents();
                byte[][] actualPalette = new byte[3][actualICM.getMapSize()];
                actualICM.getReds(actualPalette[0]);
                actualICM.getGreens(actualPalette[0]);
                actualICM.getBlues(actualPalette[0]);
                if (numBands == 4)
                    actualICM.getAlphas(defaultPalette[0]);
                // compare them
                for (int i = 0; i < defICM.getMapSize(); i++)
                    for (int j = 0; j < numBands; j++)
                        if (actualPalette[j][i] != defaultPalette[j][i]) {
                            configuration.setExpandToRGB(true);
                            break;
                        }
                return false;

            }

            //
            // if we get here this means that the two color models where completely
            // different, hence skip this feature.
            //
            return true;
        }

    }

    /** Number of files to process. */
    private int numFiles;

    /**
     * List containing all the objects that want to be notified during processing.
     */
    private List<ProcessingEventListener> notificationListeners = new CopyOnWriteArrayList<ProcessingEventListener>();

    /**
     * Set this to false for command line UIs where the delayed event sending may prevent some messages to be seen before the tool exits, to true for
     * real GUI where you don't want the processing to be blocked too long, or when you have slow listeners in general.
     */
    private boolean sendDelayedMessages = false;

    /**
     * Proper way to stop a thread is not by calling Thread.stop() but by using a shared variable that can be checked in order to notify a terminating
     * condition.
     */
    private volatile boolean stop = false;

    private GranuleCatalog catalog;

    /**
     * This field will tell the plugin if it must do a conversion of color from the original index color model to an RGB color model. This happens f
     * the original images uses different color maps between each other making for us impossible to reuse it for the mosaic.
     */
    private int fileIndex = 0;

    private CatalogBuilderConfiguration runConfiguration;

    private ImageReaderSpi cachedReaderSPI;

    private ReferencedEnvelope imposedBBox;

    private AbstractGridFormat cachedFormat;

    private ImageMosaicReader parentReader;

    private Hints excludeMosaicHints = new Hints(Utils.EXCLUDE_MOSAIC, true);

    private File indexerFile;

    private File parent;

    private IOFileFilter fileFilter;

    /**
     * run the directory walker
     */
    public void run() {

        try {

            //
            // creating the file filters for scanning for files to check and index
            //
            final IOFileFilter finalFilter = createGranuleFilterRules();

            // TODO we might want to remove this in the future for performance
            numFiles = 0;
            for (String indexingDirectory : runConfiguration.getIndexingDirectories()) {
                final File directoryToScan = new File(indexingDirectory);
                final Collection files = FileUtils.listFiles(directoryToScan, finalFilter,
                        runConfiguration.isRecursive() ? TrueFileFilter.INSTANCE
                                : FalseFileFilter.INSTANCE);
                numFiles += files.size();
            }
            //
            // walk over the files that have filtered out
            //
            if (numFiles > 0) {
                final List<String> indexingDirectories = runConfiguration.getIndexingDirectories();
                @SuppressWarnings("unused")
                final MosaicDirectoryWalker walker = new MosaicDirectoryWalker(
                        indexingDirectories, finalFilter);

            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

    }

    /**
     * @return
     */
    private IOFileFilter createGranuleFilterRules() {
        final IOFileFilter specialWildCardFileFilter = new WildcardFileFilter(
                runConfiguration.getWildcard(), IOCase.INSENSITIVE);
        IOFileFilter dirFilter = FileFilterUtils.and(
                FileFilterUtils.directoryFileFilter(), HiddenFileFilter.VISIBLE);
        IOFileFilter filesFilter = Utils.excludeFilters(FileFilterUtils.makeSVNAware(FileFilterUtils
                .makeFileOnly(FileFilterUtils.and(specialWildCardFileFilter,
                        HiddenFileFilter.VISIBLE))), FileFilterUtils.suffixFileFilter("shp"),
                FileFilterUtils.suffixFileFilter("dbf"), FileFilterUtils.suffixFileFilter("shx"),
                FileFilterUtils.suffixFileFilter("qix"), FileFilterUtils.suffixFileFilter("lyr"),
                FileFilterUtils.suffixFileFilter("prj"), FileFilterUtils
                        .nameFileFilter("error.txt"), FileFilterUtils
                        .nameFileFilter("error.txt.lck"), FileFilterUtils
                        .suffixFileFilter("properties"), FileFilterUtils
                        .suffixFileFilter("svn-base"));

        // exclude common extensions
        Set<String> extensions = WorldImageFormat.getWorldExtension("png");
        for (String ext : extensions) {
            filesFilter = FileFilterUtils.and(filesFilter, FileFilterUtils
                    .notFileFilter(FileFilterUtils.suffixFileFilter(ext.substring(1))));
        }
        extensions = WorldImageFormat.getWorldExtension("gif");
        for (String ext : extensions) {
            filesFilter = FileFilterUtils.and(filesFilter, FileFilterUtils
                    .notFileFilter(FileFilterUtils.suffixFileFilter(ext.substring(1))));
        }
        extensions = WorldImageFormat.getWorldExtension("jpg");
        for (String ext : extensions) {
            filesFilter = FileFilterUtils.and(filesFilter, FileFilterUtils
                    .notFileFilter(FileFilterUtils.suffixFileFilter(ext.substring(1))));
        }
        extensions = WorldImageFormat.getWorldExtension("tiff");
        for (String ext : extensions) {
            filesFilter = FileFilterUtils.and(filesFilter, FileFilterUtils
                    .notFileFilter(FileFilterUtils.suffixFileFilter(ext.substring(1))));
        }
        extensions = WorldImageFormat.getWorldExtension("bmp");
        for (String ext : extensions) {
            filesFilter = FileFilterUtils.and(filesFilter, FileFilterUtils
                    .notFileFilter(FileFilterUtils.suffixFileFilter(ext.substring(1))));
        }

        // sdw
        filesFilter = FileFilterUtils.and(filesFilter,
                FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter("sdw")) //);
        // aux
//        fileFilter = FileFilterUtils.andFileFilter(fileFilter,
                ,FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter("aux"))//);
        // wld
//        fileFilter = FileFilterUtils.andFileFilter(fileFilter,
                ,FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter("wld"))//);
        // svn
//        fileFilter = FileFilterUtils.andFileFilter(fileFilter,
                ,FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter("svn")));
        
        if(this.fileFilter != null) {
            filesFilter = FileFilterUtils.and(this.fileFilter, filesFilter);
        }

        final IOFileFilter finalFilter = FileFilterUtils.or(dirFilter, filesFilter);
        return finalFilter;
    }

    /**
     * Default constructor
     * 
     * @throws
     * @throws IllegalArgumentException
     */
    public ImageMosaicWalker(final CatalogBuilderConfiguration configuration) {
        Utilities.ensureNonNull("runConfiguration", configuration);
        Utilities.ensureNonNull("root location", configuration.getRootMosaicDirectory());

        // look for and indexer.properties file
        parent = new File(configuration.getRootMosaicDirectory());
        indexerFile = new File(parent, Utils.INDEXER_XML);
        Hints hints = configuration.getHints();
        if (Utils.checkFileReadable(indexerFile)) {
            try {
                Indexer indexer = (Indexer) Utils.UNMARSHALLER.unmarshal(indexerFile);
                configuration.setIndexer(indexer);
            } catch (JAXBException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        } else {
            // Backward compatible with old indexing
            indexerFile = new File(parent, Utils.INDEXER_PROPERTIES);
            if (Utils.checkFileReadable(indexerFile)) {
                // load it and parse it
                final Properties props = Utils.loadPropertiesFromURL(DataUtilities
                        .fileToURL(indexerFile));
    
                // name
                if (props.containsKey(Prop.NAME))
                    configuration.setIndexName(props.getProperty(Prop.NAME));
    
                // absolute
                if (props.containsKey(Prop.ABSOLUTE_PATH))
                    configuration.setAbsolute(Boolean.valueOf(props.getProperty(Prop.ABSOLUTE_PATH)));
    
                // recursive
                if (props.containsKey(Prop.RECURSIVE))
                    configuration.setRecursive(Boolean.valueOf(props.getProperty(Prop.RECURSIVE)));
    
                // wildcard
                if (props.containsKey(Prop.WILDCARD))
                    configuration.setWildcard(props.getProperty(Prop.WILDCARD));
    
                // schema
                if (props.containsKey(Prop.SCHEMA))
                    configuration.setSchema(props.getProperty(Prop.SCHEMA));
    
                // time attr
                if (props.containsKey(Prop.TIME_ATTRIBUTE))
                    configuration.setTimeAttribute(props.getProperty(Prop.TIME_ATTRIBUTE));
    
                // elevation attr
                if (props.containsKey(Prop.ELEVATION_ATTRIBUTE))
                    configuration.setElevationAttribute(props.getProperty(Prop.ELEVATION_ATTRIBUTE));
    
                // Additional domain attr
                if (props.containsKey(Prop.ADDITIONAL_DOMAIN_ATTRIBUTES))
                    configuration.setAdditionalDomainAttribute(props
                            .getProperty(Prop.ADDITIONAL_DOMAIN_ATTRIBUTES));
    
                // imposed BBOX
                if (props.containsKey(Prop.ENVELOPE2D))
                    configuration.setEnvelope2D(props.getProperty(Prop.ENVELOPE2D));
    
                // imposed Pyramid Layout
                if (props.containsKey(Prop.RESOLUTION_LEVELS))
                    configuration.setResolutionLevels(props.getProperty(Prop.RESOLUTION_LEVELS));
    
                // collectors
                if (props.containsKey(Prop.PROPERTY_COLLECTORS))
                    configuration.setPropertyCollectors(props.getProperty(Prop.PROPERTY_COLLECTORS));
    
                if (props.containsKey(Prop.CACHING))
                    configuration.setCaching(Boolean.valueOf(props.getProperty(Prop.CACHING)));
                
                if (props.containsKey(Prop.ROOT_MOSAIC_DIR)) {
                    // Overriding root mosaic directory
                    configuration.setRootMosaicDirectory(props.getProperty(Prop.ROOT_MOSAIC_DIR));
                }
                
                if (props.containsKey(Prop.AUXILIARY_FILE)) {
                    String ancillaryFile = configuration.getRootMosaicDirectory() + File.separatorChar + 
                            props.getProperty(Prop.AUXILIARY_FILE);
                    if (hints != null) {
                        hints.put(Utils.AUXILIARY_FILES_PATH, ancillaryFile);
                    } else {
                        hints = new Hints(Utils.AUXILIARY_FILES_PATH, ancillaryFile);
                        configuration.setHints(hints);
                    }
                }
            }
        }

        if (hints != null && hints.containsKey(Utils.MOSAIC_READER)) {
            Object reader = hints.get(Utils.MOSAIC_READER);
            if (reader instanceof ImageMosaicReader) {
                parentReader = (ImageMosaicReader) reader;
                Hints readerHints = parentReader.getHints();
                readerHints.add(hints);
            }
        }
        

        // check config
        configuration.check();

        this.runConfiguration = new CatalogBuilderConfiguration(configuration);
    }

    /**
     * Adding a listener to the {@link ProcessingEventListener}s' list.
     * 
     * @param listener to add to the list of listeners.
     */
    public final void addProcessingEventListener(final ProcessingEventListener listener) {
        notificationListeners.add(listener);
    }

    /**
     * Perform proper clean up.
     * 
     * <p>
     * Make sure to call this method when you are not running the {@link ImageMosaicWalker} or bad things can happen. If it is running, please stop it
     * first.
     */
    public void reset() {
        removeAllProcessingEventListeners();
        // clear stop
        stop = false;
        closeIndexObjects();

        fileIndex = 0;
        runConfiguration = null;

    }

    /**
     * Firing an event to listeners in order to inform them about what we are doing and about the percentage of work already carried out.
     * 
     * @param level
     * 
     * @param message The message to show.
     * @param percentage The percentage for the process.
     */
    private void fireEvent(Level level, final String inMessage, final double percentage) {
        if (LOGGER.isLoggable(level)) {
            LOGGER.log(level, inMessage);
        }
        synchronized (notificationListeners) {
            final String newLine = System.getProperty("line.separator");
            final StringBuilder message = new StringBuilder("Thread Name ");
            message.append(Thread.currentThread().getName()).append(newLine);
            message.append(this.getClass().toString()).append(newLine).append(inMessage);
            final ProcessingEvent evt = new ProcessingEvent(this, message.toString(), percentage);
            ProgressEventDispatchThreadEventLauncher eventLauncher = new ProgressEventDispatchThreadEventLauncher();
            eventLauncher.setEvent(evt, this.notificationListeners.toArray());
            sendEvent(eventLauncher);
        }
    }
    
    /**
     * Firing an event to listeners in order to inform them about what we are doing and about the percentage of work already carried out.
     * 
     * @param level
     * 
     * @param message The message to show.
     * @param percentage The percentage for the process.
     */
    private void fireFileEvent(Level level, final File file, final boolean ingested, final String inMessage, final double percentage) {
        if (LOGGER.isLoggable(level)) {
            LOGGER.log(level, inMessage);
        }
        synchronized (notificationListeners) {
            final String newLine = System.getProperty("line.separator");
            final StringBuilder message = new StringBuilder("Thread Name ");
            message.append(Thread.currentThread().getName()).append(newLine);
            message.append(this.getClass().toString()).append(newLine).append(inMessage);
            final FileProcessingEvent evt = new FileProcessingEvent(this, file, ingested, message.toString(), percentage);
            ProgressEventDispatchThreadEventLauncher eventLauncher = new ProgressEventDispatchThreadEventLauncher();
            eventLauncher.setEvent(evt, this.notificationListeners.toArray());
            sendEvent(eventLauncher);
        }
    }

    /**
     * Firing an exception event to listeners in order to inform them that processing broke and we can no longer proceed. This is a convenience
     * method, it will call {@link #fireException(String, double, Exception)} with the exception message and -1 as percentage.
     * 
     * @param ex the actual exception occurred
     */
    private void fireException(Exception ex) {
        synchronized (notificationListeners) {
            fireException(Utils.getMessageFromException(ex), -1, ex);
        }
    }

    /**
     * Firing an exception event to listeners in order to inform them that processing broke and we can no longer proceed
     * 
     * @param string The message to show.
     * @param percentage The percentage for the process.
     * @param ex the actual exception occurred
     */
    private void fireException(final String string, final double percentage, Exception ex) {
        synchronized (notificationListeners) {
            final String newLine = System.getProperty("line.separator");
            final StringBuilder message = new StringBuilder("Thread Name ");
            message.append(Thread.currentThread().getName()).append(newLine);
            message.append(this.getClass().toString()).append(newLine).append(string);
            final ExceptionEvent evt = new ExceptionEvent(this, string, percentage, ex);
            ProgressEventDispatchThreadEventLauncher eventLauncher = new ProgressEventDispatchThreadEventLauncher();
            eventLauncher.setEvent(evt, this.notificationListeners.toArray());
            sendEvent(eventLauncher);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.gce.imagemosaic.JMXIndexBuilderMBean#getStop()
     */
    public boolean getStop() {
        return stop;
    }

    public boolean isSendDelayedMessages() {
        return sendDelayedMessages;
    }

    public void setSendDelayedMessages(boolean sendDelayedMessages) {
        this.sendDelayedMessages = sendDelayedMessages;
    }

    /**
     * Removing all the listeners.
     * 
     */
    public void removeAllProcessingEventListeners() {
        synchronized (notificationListeners) {
            notificationListeners.clear();
        }

    }

    /**
     * Removing a {@link ProcessingEventListener} from the listeners' list.
     * 
     * @param listener {@link ProcessingEventListener} to remove from the list of listeners.
     */
    public void removeProcessingEventListener(final ProcessingEventListener listener) {
        notificationListeners.remove(listener);

    }

    private void sendEvent(ProgressEventDispatchThreadEventLauncher eventLauncher) {
        if (sendDelayedMessages)
            SwingUtilities.invokeLater(eventLauncher);
        else
            eventLauncher.run();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.geotools.gce.imagemosaic.JMXIndexBuilderMBean#stop()
     */
    public void stop() {
        stop = true;
    }

    private void indexingPreamble() throws IOException {

         //
         // create the index
         //
        catalog = CatalogManager.createCatalog(runConfiguration);
        parentReader.granuleCatalog = catalog;

        
        //
        // IMPOSED ENVELOPE
        //
        String bbox = runConfiguration.getEnvelope2D();
        try {
            this.imposedBBox = Utils.parseEnvelope(bbox);
        } catch (Exception e) {
            this.imposedBBox = null;
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Unable to parse imposed bbox", e);
        }
        
        //
        // load property collectors
        //
        loadPropertyCollectors();
    }

    /**
     * Load properties collectors from the configuration
     */
    private void loadPropertyCollectors() {
        // load property collectors
        String pcConfig = runConfiguration.getPropertyCollectors();
        if (pcConfig != null && pcConfig.length() > 0) {
            pcConfig = pcConfig.trim();
            // load the SPI set
            final Set<PropertiesCollectorSPI> pcSPIs = PropertiesCollectorFinder
                    .getPropertiesCollectorSPI();

            // parse the string
            final List<PropertiesCollector> pcs = new ArrayList<PropertiesCollector>();
            final String[] pcsDefs = pcConfig.trim().split(",");
            for (String pcDef : pcsDefs) {
                // parse this def as NAME[CONFIG_FILE](PROPERTY;PROPERTY;....;PROPERTY)
                final int squareLPos = pcDef.indexOf("[");
                final int squareRPos = pcDef.indexOf("]");
                final int squareRPosLast = pcDef.lastIndexOf("]");
                final int roundLPos = pcDef.indexOf("(");
                final int roundRPos = pcDef.indexOf(")");
                final int roundRPosLast = pcDef.lastIndexOf(")");
                if (squareRPos != squareRPosLast) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Skipping unparseable PropertyCollector definition: " + pcDef);
                    }
                    continue;
                }
                if (squareLPos == -1 || squareRPos == -1) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Skipping unparseable PropertyCollector definition: " + pcDef);
                    }
                    continue;
                }
                if (squareLPos == 0) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Skipping unparseable PropertyCollector definition: " + pcDef);
                    }
                    continue;
                }

                if (roundRPos != roundRPosLast) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Skipping unparseable PropertyCollector definition: " + pcDef);
                    }
                    continue;
                }
                if (roundLPos == -1 || roundRPos == -1) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Skipping unparseable PropertyCollector definition: " + pcDef);
                    }
                    continue;
                }
                if (roundLPos == 0) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Skipping unparseable PropertyCollector definition: " + pcDef);
                    }
                    continue;
                }
                if (roundLPos != squareRPos + 1) {// ]( or exit
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Skipping unparseable PropertyCollector definition: " + pcDef);
                    }
                    continue;
                }
                if (roundRPos != (pcDef.length() - 1)) {// end with )
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Skipping unparseable PropertyCollector definition: " + pcDef);
                    }
                    continue;
                }

                // name
                final String name = pcDef.substring(0, squareLPos);
                PropertiesCollectorSPI selectedSPI = null;
                for (PropertiesCollectorSPI spi : pcSPIs) {
                    if (spi.isAvailable() && spi.getName().equalsIgnoreCase(name)) {
                        selectedSPI = spi;
                        break;
                    }
                }
                if (selectedSPI == null) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Unable to find a PropertyCollector for this definition: "
                                + pcDef);
                    }
                    continue;
                }

                // config
                final String config = squareLPos < squareRPos ? pcDef.substring(squareLPos + 1,
                        squareRPos) : "";
                final File configFile = new File(runConfiguration.getRootMosaicDirectory(), config
                        + ".properties");
                if (!Utils.checkFileReadable(configFile)) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Unable to access the file for this PropertyCollector: "
                                + configFile.getAbsolutePath());
                    }
                    continue;
                }

                // property names
                final String propertyNames[] = pcDef.substring(roundLPos + 1, roundRPos).split(",");

                // create the PropertiesCollector
                final PropertiesCollector pc = selectedSPI.create(configFile,
                        Arrays.asList(propertyNames));
                if (pc != null) {
                    pcs.add(pc);
                } else {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Unable to create PropertyCollector " + pcDef
                                + " from config file:" + configFile);
                    }
                }
            }
            this.propertiesCollectors = pcs;
        }
    }

    private void indexingPostamble(final boolean success) throws IOException {
        // close shapefile elements
        if (success) {

            // complete initialization of mosaic configuration
            if (configurations != null && !configurations.isEmpty()) {
                
                // We did found some MosaicConfigurations 
                Set<String> keys = configurations.keySet();
                for (String key : keys) {
                    MosaicConfigurationBean mosaicConfiguration = configurations.get(key);
                    RasterManager manager = parentReader.getRasterManager(key);
                    manager.initialize();

                    // create sample image if the needed elements are available
                    // TODO: DR: Create SampleImage
                    createSampleImage(mosaicConfiguration.getSampleModel(), mosaicConfiguration.getColorModel());
                    fireEvent(Level.INFO, "Creating final properties file ", 99.9);
                    createPropertiesFiles(mosaicConfiguration);
                }
                final String base = FilenameUtils.getName(parent.getAbsolutePath());
                // we create a root properties file if we have more than one coverage, or if the
                // one coverage does not have the default name
                if (keys.size() > 1 || (keys.size() > 0 && !base.equals(keys.iterator().next()))) {
                    
                    // TODO: DR: Remove that when dealing with Indexer.xml which should allow to know if configurations
                    // are available
                    final String source = 
                            runConfiguration.getRootMosaicDirectory() + File.separatorChar + configurations.get(keys.iterator().next()).getName() +  ".properties";
                    
                    final File mosaicFile = new File(indexerFile.getAbsolutePath().replace(Utils.INDEXER_PROPERTIES, (base + ".properties")));
                    FileUtils.copyFile(new File(source) , mosaicFile);
                }

                // processing information
                fireEvent(Level.FINE, "Done!!!", 100);
                
            } else {
                // processing information
                fireEvent(Level.FINE, "Nothing to process!!!", 100);
            }
        } else {
            // processing information
            fireEvent(Level.FINE, "Canceled!!!", 100);
        }
        closeIndexObjects();
    }

    /**
     * Store a sample image frmo which we can derive the default SM and CM
     */
    private void createSampleImage(final SampleModel sm, final ColorModel cm) {
        // create a sample image to store SM and CM
        if (cm != null && sm != null) {

            // sample image file
            //TODO: Consider revisit this using different name/folder
            final File sampleImageFile = new File(runConfiguration.getRootMosaicDirectory()
                    + "/sample_image");
            try {
                Utils.storeSampleImage(sampleImageFile, sm, cm);
            } catch (IOException e) {
                fireEvent(Level.SEVERE, e.getLocalizedMessage(), 0);
            }
        }
    }

    private void closeIndexObjects() {
        
        // TODO: We may consider avoid disposing the catalog to allow the reader to use the already available catalog
        try {
            if (catalog != null) {
                catalog.dispose();
            }
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }

        catalog = null;
        parentReader.granuleCatalog = null;
    }

    /**
     * Creates the final properties file.
     */
    private void createPropertiesFiles(MosaicConfigurationBean mosaicConfiguration) {

        //
        // FINAL STEP
        //
        // CREATING GENERAL INFO FILE
        //

        CatalogConfigurationBean catalogConfigurationBean = mosaicConfiguration.getCatalogConfigurationBean();

        //envelope
        final Properties properties = new Properties();
        properties.setProperty(Utils.Prop.ABSOLUTE_PATH, Boolean.toString(catalogConfigurationBean.isAbsolutePath()));
        properties.setProperty(Utils.Prop.LOCATION_ATTRIBUTE, catalogConfigurationBean.getLocationAttribute());

        // Time
        final String timeAttribute = mosaicConfiguration.getTimeAttribute();
        if (timeAttribute != null) {
            properties.setProperty(Utils.Prop.TIME_ATTRIBUTE, mosaicConfiguration.getTimeAttribute());
        }

        // Elevation
        final String elevationAttribute = mosaicConfiguration.getElevationAttribute();
        if (elevationAttribute != null) {
            properties.setProperty(Utils.Prop.ELEVATION_ATTRIBUTE, mosaicConfiguration.getElevationAttribute());
        }

        // Additional domains
        final String additionalDomainAttribute = mosaicConfiguration.getAdditionalDomainAttributes();
        if (additionalDomainAttribute != null) {
            properties.setProperty(Utils.Prop.ADDITIONAL_DOMAIN_ATTRIBUTES, mosaicConfiguration.getAdditionalDomainAttributes());
        }

        final int numberOfLevels = mosaicConfiguration.getLevelsNum();
        final double[][] resolutionLevels = mosaicConfiguration.getLevels();
        properties.setProperty(Utils.Prop.LEVELS_NUM, Integer.toString(numberOfLevels));
        final StringBuilder levels = new StringBuilder();
        for (int k = 0; k < numberOfLevels; k++) {
            levels.append(Double.toString(resolutionLevels[k][0])).append(",")
                    .append(Double.toString(resolutionLevels[k][1]));
            if (k < numberOfLevels - 1) {
                levels.append(" ");
            }
        }
        properties.setProperty(Utils.Prop.LEVELS, levels.toString());
        properties.setProperty(Utils.Prop.NAME, mosaicConfiguration.getName());
        properties.setProperty(Utils.Prop.TYPENAME, mosaicConfiguration.getName());
        properties.setProperty(Utils.Prop.EXP_RGB, Boolean.toString(mosaicConfiguration.isExpandToRGB()));
        properties.setProperty(Utils.Prop.HETEROGENEOUS, Boolean.toString(catalogConfigurationBean.isHeterogeneous()));

        if (cachedReaderSPI != null) {
            // suggested spi
            properties.setProperty(Utils.Prop.SUGGESTED_SPI, cachedReaderSPI.getClass().getName());
        }

        // write down imposed bbox
        if (imposedBBox != null) {
            properties.setProperty(
                    Utils.Prop.ENVELOPE2D,
                    imposedBBox.getMinX() + "," + imposedBBox.getMinY() + " "
                            + imposedBBox.getMaxX() + "," + imposedBBox.getMaxY());
        }
        properties.setProperty(Utils.Prop.CACHING, Boolean.toString(catalogConfigurationBean.isCaching()));
        OutputStream outStream = null;
        try {
            outStream = new BufferedOutputStream(new FileOutputStream(
                    runConfiguration.getRootMosaicDirectory() + "/"
//                            + runConfiguration.getIndexName() + ".properties"));
                    + mosaicConfiguration.getName() + ".properties"));
            properties.store(outStream, "-Automagically created from GeoTools-");
        } catch (FileNotFoundException e) {
            fireEvent(Level.SEVERE, e.getLocalizedMessage(), 0);
        } catch (IOException e) {
            fireEvent(Level.SEVERE, e.getLocalizedMessage(), 0);
        } finally {
            if (outStream != null) {
                IOUtils.closeQuietly(outStream);
            }
        }
    }

    public void dispose() {
        reset();
    }
    
    public Map<String, MosaicConfigurationBean> getConfigurations() {
        return configurations;
    }

    public IOFileFilter getFileFilter() {
        return fileFilter;
    }

    /**
     * Sets a filter that can reduce the file the mosaic walker will take into consideration
     * (in a more flexible way than the wildcards)
     * @param fileFilter
     */
    public void setFileFilter(IOFileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

}
