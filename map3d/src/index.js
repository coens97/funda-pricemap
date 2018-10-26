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
import Select from 'react-select';

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
      selectedDate: null,
      selectedOtherStatistics: { value: "", label: "All" }
    };

    Promise.all([fetch(DATA_URL), fetch(OVERVIEW_URL)])
      .then(response =>
        Promise.all(response.map(x => x.json()))
      )
      .then(jsonData => {
        const datesList = jsonData[1]
          .dates
          .reverse()
          .map(x => ({ value: x, label: x }));
        const loadingDate = datesList[0];
        this.setState(
          {
            data: jsonData[0],
            overview: datesList,
            overviewLoaded: true,
            selectedDate: loadingDate
          });

        this.loadStatisticsFile(loadingDate.value);
      }).catch(ex => {
        console.warn(ex);
        toast.error("Could not load the map data");
      });

  }

  loadStatisticsFile(filename) {
    fetch(`generated/${filename}.json`)
      .then(response =>
        response.json()
      )
      .then(jsonData => {
        this.setState(
          {
            statistics: jsonData,
            loading: false,
          });
      }).catch(ex => {
        console.warn(ex);
        toast.error("Could not load the statistics data");
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

  _onDateChanged = (newDate) => {
    this.setState(
      {
        selectedDate: newDate
      });

    this.loadStatisticsFile(newDate.value + this.state.selectedOtherStatistics.value);
  };

  _onOtherChanged = (newStatistics) => {
    this.setState(
      {
        selectedOtherStatistics: newStatistics
      });

    this.loadStatisticsFile(this.state.selectedDate.value + newStatistics.value);
  };

  render() {
    const { viewport, data, statistics } = this.state;
    const extraStatistics = [
      { value: "", label: "All" },
      { value: ".slaap.1", label: "1 bedroom" },
      { value: ".slaap.2", label: "2 bedrooms" },
      { value: ".slaap.3", label: "3 bedrooms" },
      { value: ".slaap.4", label: "4 bedrooms" },
      { value: ".slaap.5", label: "5 bedrooms" },
      { value: ".slaap.6", label: "6 bedrooms" },
      { value: ".year.0", label: " 0 - 5 years old" },
      { value: ".year.5", label: " 5 - 10 years old" },
      { value: ".year.10", label: "10 - 20 years old" },
      { value: ".year.20", label: "20 - 40 years old" },
      { value: ".year.40", label: ">40 years old" }
    ];
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
            <h1 className="header">Price per &#13217;</h1>
            <hr></hr>
            <h3 className="select-header">Date</h3>
            <Select
              className="menu-select"
              value={this.state.selectedDate}
              onChange={this._onDateChanged}
              options={this.state.overview}
            />
            <h3 className="select-header">Other statistics</h3>
            <Select
              className="menu-select"
              value={this.state.selectedOtherStatistics}
              onChange={this._onOtherChanged}
              options={extraStatistics}
            />
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
