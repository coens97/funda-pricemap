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
      zoom: 8,
      maxZoom: 16,
      pitch: 20,
      bearing: 0
    };
  }

  render() {
    const { viewport, data, colorScale } = this.props;

    if (!data) {
      return null;
    }

    const layer = new GeoJsonLayer({
      id: 'geojson',
      data,
      opacity: 0.3,
      stroked: false,
      filled: true,
      extruded: true,
      wireframe: true,
      fp64: true,
      getElevation: f => Math.floor(Math.random() * 20) * 20,//Math.sqrt(f.properties.valuePerSqm) * 10,
      getFillColor: f => [255, 0, 255],//colorScale(f.properties.growth),
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