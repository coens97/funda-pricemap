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
      pitch: 20,
      bearing: 0
    };
  }

  render() {
    const { viewport, data, statistics } = this.props;

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
      getElevation: f => {
        const postcode = f.properties.POSTCODE;
        if (postcode in statistics) {
          const listHouses = statistics[postcode];
          if (listHouses.length == 0) {
            return 5;
          }
          const averageHouseprice = listHouses.reduce((p, c) => p.vraagprijs + c.vraagprijs, 0) / listHouses.length;
          const averageSize = listHouses.reduce((p, c) => p.woonoppervlakte + c.woonoppervlakte, 0) / listHouses.length;
          return averageHouseprice / (100 * averageSize);
        }
        else {
          return 10;
        }
      },
      getFillColor: f => [255, 0, 255],//const colorScale = r => [r * 255, 140, 200 * (1 - r)];
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