package org.geotools.coverage.processing;

import java.util.Collection;

import javax.media.jai.RegistryElementDescriptor;

import org.geotools.metadata.iso.citation.Citations;
import org.geotools.parameter.ImagingParameterDescriptors;
import org.geotools.referencing.NamedIdentifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.ReferenceIdentifier;

public class ExtendedImagingParameterDescriptors extends
		ImagingParameterDescriptors {

	private ReferenceIdentifier operationName;

	public ExtendedImagingParameterDescriptors(String operationName,
			RegistryElementDescriptor operation) {
		this(operationName, operation, null);
	}
	
	ExtendedImagingParameterDescriptors(String operationName,
			RegistryElementDescriptor operation, Collection<ParameterDescriptor> extension){
		
		super(operation, extension);
		this.operationName = new NamedIdentifier(Citations.JAI, operationName);
	}

	@Override
	public ReferenceIdentifier getName() {
		return operationName;
	}

}
