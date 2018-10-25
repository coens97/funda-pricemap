/* global window,document */
import React, { Component } from 'react';
import { render } from 'react-dom';
import MapGL from 'react-map-gl';
import DeckGLOverlay from './deckgl-overlay.js';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import 'mapbox-gl/dist/mapbox-gl.css'
import { BounceLoader } from 'react-spinners';

// Set your mapbox token here
const MAPBOX_TOKEN = 'pk.eyJ1IjoiY29lbnM5NyIsImEiOiJjam5objg4YWQwNWVlM3B0ZHd0NGV2aDdpIn0.0Jg6jNjAROAafiP9QB_i6w'; // eslint-disable-line

// Source data GeoJSON
const DATA_URL = 'map/postcodemap.wsg.json'; // eslint-disable-line

class Root extends Component {

  constructor(props) {
    super(props);
    this.state = {
      viewport: {
        ...DeckGLOverlay.defaultViewport,
        width: 500,
        height: 500,
      },
      data: null,
      statistics: null,
      loading: true
    };

    Promise.all([fetch(DATA_URL), fetch('generated/2018-10-25.slaap.4.json')])
      .then(response =>
        Promise.all(response.map(x => x.json()))
      )
      .then(jsonData => {
        this.setState(
          {
            data: jsonData[0],
            statistics: jsonData[1],
            loading: false
          });
      }).catch(ex => {
        console.warn(ex);
        toast.error("Could not load the map data");
      });

  }

  componentDidMount() {
    window.addEventListener('resize', this._resize.bind(this));
    this._resize();
  }

  _resize() {
    this._onViewportChange({
      width: window.innerWidth,
      height: window.innerHeight
    });
  }

  _onViewportChange(viewport) {
    this.setState({
      viewport: { ...this.state.viewport, ...viewport }
    });
  }

  render() {
    const { viewport, data, statistics } = this.state;
    // Css style of loader
    return (
      <div>
        <ToastContainer />
        { // Render loading screen if applicable
          this.state.loading &&
          <div className="loaderBox">
            <div className="loaderContainer">
              <BounceLoader
                sizeUnit={"px"}
                size={150}
                margin={"0 auto"}
                color={'#43c92c'}
                loading={true}
              />
            </div>
            <h3>Loading rescources...</h3>
          </div>
        }
        <MapGL
          {...viewport}
          mapStyle='mapbox://styles/mapbox/streets-v10'
          onViewportChange={this._onViewportChange.bind(this)}
          mapboxApiAccessToken={MAPBOX_TOKEN}>
          <DeckGLOverlay viewport={viewport}
            postmap={data}
            statistics={statistics} />
        </MapGL>
      </div>
    );
  }
}

render(<Root />, document.getElementById('root'));
