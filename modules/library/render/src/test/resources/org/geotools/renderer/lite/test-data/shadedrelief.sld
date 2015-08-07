<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld"
  xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
  <sld:UserLayer>
    <sld:UserStyle>
      <sld:Name>1</sld:Name>
      <sld:Title>Default Raster</sld:Title>
      <sld:Abstract>A sample style that draws a raster, good for
        displaying imagery</sld:Abstract>
      <sld:FeatureTypeStyle>
        <sld:Name>name</sld:Name>
        <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
        <sld:Rule>
          <sld:Name>rule1</sld:Name>
          <sld:Title>Opaque Raster</sld:Title>
          <sld:Abstract>ShadedRelief style</sld:Abstract>
          <sld:RasterSymbolizer>
            <sld:ShadedRelief>
              <sld:BrightnessOnly>true</sld:BrightnessOnly>
              <sld:ReliefFactor>100</sld:ReliefFactor>
            </sld:ShadedRelief>
          </sld:RasterSymbolizer>
        </sld:Rule>
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:UserLayer>
</sld:StyledLayerDescriptor>