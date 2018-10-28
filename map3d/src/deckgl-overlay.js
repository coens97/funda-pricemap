import React, { Component } from 'react';
import DeckGL, { GeoJsonLayer } from 'deck.gl';

const {
  // main component
  Chart,
  // graphs
  Bars, Cloud, Dots, Labels, Lines, Pies, RadialLines, Ticks, Title,
  // wrappers
  Layer, Animate, Transform, Handlers,
  // helpers
  DropShadow, Gradient, helpers
} = require('rumble-charts');

const LIGHT_SETTINGS = {
  lightsPosition: [-125, 50.5, 5000, -122.8, 48.5, 8000],
  ambientRatio: 0.2,
  diffuseRatio: 0.5,
  specularRatio: 0.3,
  lightsStrength: [1.0, 0.0, 2.0, 0.0],
  numberOfLights: 2
};

export default class DeckGLOverlay extends Component {

  constructor(props) {
    super(props);
    this.state = {
      showStats: false,
      stats: null
    };
  }

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
    const { viewport, postmap, statistics } = this.props;

    if (!postmap || !statistics || !(statistics.postcodes)) {
      return null;
    }

    const filteredPostmap = {
      ...postmap,
      features: postmap.features.filter(f => {
        const postcode = f.properties.POSTCODE;
        return postcode in statistics.postcodes
      })
    }

    const calcDepth = (f, defaultValue, scale) => {
      const priceMax = 3750;
      const postcode = f.properties.POSTCODE;
      if (postcode in statistics.postcodes) {
        const data = statistics.postcodes[postcode];

        return ((data.r - statistics.minprice) / (priceMax - statistics.minprice)) * scale;
      }
      else {
        return defaultValue;
      }
    };

    const colorScale = r => [r * 255, 140, 200 * (1 - r)];

    const layer = new GeoJsonLayer({
      id: 'geojson',
      data: filteredPostmap,
      opacity: 0.3,
      stroked: false,
      filled: true,
      extruded: true,
      wireframe: true,
      fp64: true,
      getElevation: f => calcDepth(f, 0, 5000),
      getFillColor: f => colorScale(calcDepth(f, 0, 1)),
      getLineColor: f => [255, 255, 255],
      lightSettings: LIGHT_SETTINGS,
      pickable: true,
      onClick: f => {
        console.warn(f);
        const postcode = f.object.properties.POSTCODE;
        const statistics = postcode in this.props.statistics.postcodes ? this.props.statistics.postcodes[postcode] : null;
        this.setState({
          showStats: true,
          stats: {
            properties: f.object.properties,
            statistics: statistics
          }
        })
      }
    });

    return (
      <div>
        <DeckGL {...viewport} layers={[layer]} initWebGLParameters />
        {this.state.showStats &&
          <div className="postalDetails">
            <h2>{this.state.stats.properties.POSTCODE}, {this.state.stats.properties.GM_NAAM}</h2>
            <span>Men/Woman</span>
            <Chart width={240} height={20} series={([{ data: [this.state.stats.properties.AANT_MAN] }, { data: [this.state.stats.properties.AANT_VROUW] }])}>
              <Transform method={['stack', 'rotate']}>
                <Bars combined={true} innerPadding='2%' />
              </Transform>
            </Chart>
            <span>0-14, 15-24, 25-44, 45-64, 65+</span>
            <Chart width={240} height={20} series={([
              { data: [this.state.stats.properties.P_00_14_JR] },
              { data: [this.state.stats.properties.P_15_24_JR] },
              { data: [this.state.stats.properties.P_25_44_JR] },
              { data: [this.state.stats.properties.P_45_64_JR] },
              { data: [this.state.stats.properties.P_65_EO_JR] }])}>
              <Transform method={['stack', 'rotate']}>
                <Bars combined={true} innerPadding='2%' />
              </Transform>
            </Chart>
          </div>}
      </div>
    );
  }
}
