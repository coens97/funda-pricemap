import React, { Component } from 'react';
import DeckGL, { GeoJsonLayer } from 'deck.gl';

const LIGHT_SETTINGS = {
  lightsPosition: [-125, 50.5, 5000, -122.8, 48.5, 8000],
  ambientRatio: 0.2,
  diffuseRatio: 0.5,
  specularRatio: 0.3,
  lightsStrength: [1.0, 0.0, 2.0, 0.0],
  numberOfLights: 2
};

export default class DeckGLOverlay extends Component {

  static get defaultViewport() {
    return {
      latitude: 52.2125708,
      longitude: 4.9636486,
      zoom: 7,
      maxZoom: 16,
      pitch: 5,
      bearing: 0
    };
  }

  render() {
    const { viewport, data, statistics } = this.props;

    if (!data) {
      return null;
    }

    const calcDepth = (f, defaultValue, scale) => {
      const priceMax = 10000;
      const postcode = f.properties.POSTCODE;
      if (postcode in statistics.postcodes) {
        const data = statistics.postcodes[postcode];
        if (data.r > priceMax) {
          return scale;
        }
        return ((data.r - statistics.minprice) / (priceMax - statistics.minprice)) * scale;
      }
      else {
        return defaultValue;
      }
    };

    const colorScale = r => [r * 255, 140, 200 * (1 - r)];

    const layer = new GeoJsonLayer({
      id: 'geojson',
      data,
      opacity: 0.3,
      stroked: false,
      filled: true,
      extruded: true,
      wireframe: true,
      fp64: true,
      getElevation: f => calcDepth(f, 0, 10000),
      getFillColor: f => colorScale(calcDepth(f, 0, 1)),
      getLineColor: f => [255, 255, 255],
      lightSettings: LIGHT_SETTINGS,
      pickable: Boolean(this.props.onHover),
      onHover: this.props.onHover
    });

    return (
      <DeckGL {...viewport} layers={[layer]} initWebGLParameters />
    );
  }
}