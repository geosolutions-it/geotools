package org.geotools.data.mongodb.complex;

import org.geotools.xml.AbstractComplexBinding;

import javax.xml.namespace.QName;

public class SituationBinding extends AbstractComplexBinding {

    @Override
    public QName getTarget() {
        return new QName("http://datex2.eu/schema/2/2_0", "Situation");
    }

    @Override
    public Class getType() {
        return null;
    }
}
