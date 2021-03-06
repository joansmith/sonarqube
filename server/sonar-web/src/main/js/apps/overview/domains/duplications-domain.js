/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import _ from 'underscore';
import d3 from 'd3';
import React from 'react';

import { getMeasures } from '../../../api/measures';
import { DetailedMeasure } from '../components/detailed-measure';
import { DomainTimeline } from '../components/domain-timeline';
import { DomainTreemap } from '../components/domain-treemap';
import { DomainBubbleChart } from '../components/domain-bubble-chart';
import { getPeriodLabel, getPeriodDate } from './../helpers/periods';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { filterMetrics, filterMetricsForDomains, getMetricName } from '../helpers/metrics';
import { DomainLeakTitle } from '../main/components';
import { CHART_COLORS_RANGE_PERCENT } from '../../../helpers/constants';
import { formatMeasure, formatMeasureVariation } from '../../../helpers/measures';
import { DonutChart } from '../../../components/charts/donut-chart';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { translate } from '../../../helpers/l10n';


export const DuplicationsMain = React.createClass({
  mixins: [TooltipsMixin],

  getInitialState() {
    return {
      ready: false,
      leakPeriodLabel: getPeriodLabel(this.props.component.periods, this.props.leakPeriodIndex),
      leakPeriodDate: getPeriodDate(this.props.component.periods, this.props.leakPeriodIndex)
    };
  },

  componentDidMount() {
    this.requestMeasures().then(r => {
      let measures = this.getMeasuresValues(r);
      let leak = this.getMeasuresValues(r, Number(this.props.leakPeriodIndex));
      this.setState({ ready: true, measures, leak });
    });
  },

  getMeasuresValues (measures, period) {
    let values = {};
    measures.forEach(measure => {
      const container = period ? _.findWhere(measure.periods, { index: period }) : measure;
      if (container) {
        values[measure.metric] = container.value;
      }
    });
    return values;
  },

  getMetricsForDomain() {
    return this.props.metrics
        .filter(metric => ['Duplication'].indexOf(metric.domain) !== -1)
        .map(metric => metric.key);
  },

  getMetricsForTimeline() {
    return filterMetricsForDomains(this.props.metrics, ['Duplication']);
  },

  getAllMetricsForTimeline() {
    return filterMetrics(this.props.metrics);
  },

  requestMeasures () {
    return getMeasures(this.props.component.key, this.getMetricsForDomain());
  },

  renderLoading () {
    return <div className="flex-1 text-center">
      <i className="spinner spinner-margin"/>
    </div>;
  },

  renderEmpty() {
    return <div className="overview-detailed-page">
      <div className="page">
        <p>{translate('overview.no_duplications')}</p>
      </div>
    </div>;
  },

  renderLegend () {
    return <DomainLeakTitle inline={true} label={this.state.leakPeriodLabel} date={this.state.leakPeriodDate}/>;
  },

  renderMeasures() {
    let metrics = filterMetricsForDomains(this.props.metrics, ['Duplication'])
        .filter(metric => metric.key !== 'duplicated_lines_density')
        .map(metric => {
          return <DetailedMeasure key={metric.key}
                                  {...this.props}
                                  {...this.state}
                                  metric={metric.key}
                                  type={metric.type}/>;
        });
    return <div>{metrics}</div>;
  },

  renderDonut() {
    const duplicationsMetric = 'duplicated_lines_density';
    const duplications = this.state.measures[duplicationsMetric];
    const donutData = [
      { value: duplications, fill: '#f3ca8e' },
      { value: Math.max(0, 20 - duplications), fill: '#e6e6e6' }
    ];
    return <DonutChart width="30"
                       height="30"
                       thickness="4"
                       data={donutData}/>;
  },

  renderDuplicationsLeak() {
    if (!this.state.leakPeriodDate) {
      return null;
    }
    const duplicationsMetric = 'duplicated_lines_density';
    const leak = this.state.leak[duplicationsMetric];
    return <div className="overview-detailed-measure-leak">
      <span className="overview-detailed-measure-value">
        {formatMeasureVariation(leak, 'PERCENT')}
      </span>
    </div>;
  },

  render () {
    if (!this.state.ready) {
      return this.renderLoading();
    }

    const duplicationsMetric = 'duplicated_lines_density';
    const duplications = this.state.measures[duplicationsMetric];

    if (duplications == null) {
      return this.renderEmpty();
    }

    let treemapScale = d3.scale.linear()
        .domain([0, 25, 50, 75, 100])
        .range(CHART_COLORS_RANGE_PERCENT);

    return <div className="overview-detailed-page">
      <div className="overview-cards-list">
        <div className="overview-card overview-card-fixed-width">
          <div className="overview-card-header">
            <div className="overview-title">{translate('overview.domain.duplications')}</div>
            {this.renderLegend()}
          </div>
          <div className="overview-detailed-measures-list">
            <div className="overview-detailed-measure" style={{ lineHeight: '30px' }}>
              <div className="overview-detailed-measure-nutshell">
                <span className="overview-detailed-measure-name big">
                  {getMetricName('duplications')}
                </span>
                <span className="overview-detailed-measure-value">
                  <span className="spacer-right">{this.renderDonut()}</span>
                  <DrilldownLink component={this.props.component.key} metric={duplicationsMetric}>
                    {formatMeasure(duplications, 'PERCENT')}
                  </DrilldownLink>
                </span>
              </div>

              {this.renderDuplicationsLeak()}
            </div>

            {this.renderMeasures()}
          </div>
        </div>
        <div className="overview-card">
          <DomainBubbleChart {...this.props}
              xMetric="ncloc"
              yMetric="duplicated_lines"
              sizeMetrics={['duplicated_blocks']}/>
        </div>
      </div>

      <div className="overview-cards-list">
        <div className="overview-card">
          <DomainTimeline {...this.props} {...this.state}
              initialMetric="duplicated_lines_density"
              metrics={this.getMetricsForTimeline()}
              allMetrics={this.getAllMetricsForTimeline()}/>
        </div>
        <div className="overview-card">
          <DomainTreemap {...this.props}
              sizeMetric="ncloc"
              colorMetric="duplicated_lines_density"
              scale={treemapScale}/>
        </div>
      </div>
    </div>;

  }
});
