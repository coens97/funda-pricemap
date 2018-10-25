/* global window,document */
import React, { Component } from 'react';
import { render } from 'react-dom';
import MapGL from 'react-map-gl';
import DeckGLOverlay from './deckgl-overlay.js';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import 'mapbox-gl/dist/mapbox-gl.css'
import { BounceLoader } from 'react-spinners';
import { slide as Menu } from 'react-burger-menu'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fab } from '@fortawesome/free-brands-svg-icons'
import { library } from '@fortawesome/fontawesome-svg-core'

library.add(fab);

// Set your mapbox token here
const MAPBOX_TOKEN = 'pk.eyJ1IjoiY29lbnM5NyIsImEiOiJjam5objg4YWQwNWVlM3B0ZHd0NGV2aDdpIn0.0Jg6jNjAROAafiP9QB_i6w'; // eslint-disable-line

// Source data GeoJSON
const DATA_URL = 'map/postcodemap.wsg.json'; // eslint-disable-line
const OVERVIEW_URL = 'generated/overview.json'

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
      overview: null,
      loading: true,
      overviewLoaded: false,
    };

    Promise.all([fetch(DATA_URL), fetch('generated/2018-10-25.slaap.4.json'), fetch(OVERVIEW_URL)])
      .then(response =>
        Promise.all(response.map(x => x.json()))
      )
      .then(jsonData => {
        this.setState(
          {
            data: jsonData[0],
            statistics: jsonData[1],
            overview: jsonData[2],
            loading: false,
            overviewLoaded: true
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
        {
          // Render menu-bar if overview is loaded
          this.state.overviewLoaded &&
          <Menu>


            <div className="sociallinks">
              <a className="icon-link" target="_blank" href="https://github.com/coens97/funda-pricemap">
                <FontAwesomeIcon icon={['fab', 'github']} size="3x" />
              </a>
              <a className="icon-link" target="_blank" href="https://www.linkedin.com/in/coen-stange/">
                <FontAwesomeIcon className="font-icon" icon={['fab', 'linkedin']} size="3x" />
              </a>
            </div>
          </Menu>
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
