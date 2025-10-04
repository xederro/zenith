/**
 * @license
 * Copyright (C) 2025 Dawid Jabłoński
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import '@gerritcodereview/typescript-api/gerrit';
import {html, LitElement} from 'lit';
import {customElement} from 'lit/decorators.js';
import * as d3 from "d3";

@customElement('zenith-page')
export class ZenithPage extends LitElement {
  override render() {
    return html`
      <div>
        <h1>Icicle Graph</h1>
        <div id="d3-container"></div>
      </div>
    `;
  }

  override firstUpdated() {
    this.renderIcicle({
          "name": "flare",
          "children": [
            {
              "name": "analytics",
              "children": [
                {
                  "name": "cluster",
                  "children": [
                    {"name": "AgglomerativeCluster", "value": 1},
                    {"name": "CommunityStructure", "value": 1},
                    {"name": "HierarchicalCluster", "value": 1},
                    {"name": "MergeEdge", "value": 1}
                  ]
                },
                {
                  "name": "graph",
                  "children": [
                    {"name": "BetweennessCentrality", "value": 1},
                    {"name": "LinkDistance", "value": 1},
                    {"name": "MaxFlowMinCut", "value": 1},
                    {"name": "ShortestPaths", "value": 1},
                    {"name": "SpanningTree", "value": 1}
                  ]
                },
                {
                  "name": "optimization",
                  "children": [
                    {"name": "AspectRatioBanker", "value": 1}
                  ]
                }
              ]
            },
            {
              "name": "animate",
              "children": [
                {"name": "Easing", "value": 1},
                {"name": "FunctionSequence", "value": 1},
                {
                  "name": "interpolate",
                  "children": [
                    {"name": "ArrayInterpolator", "value": 1},
                    {"name": "ColorInterpolator", "value": 1},
                    {"name": "DateInterpolator", "value": 1},
                    {"name": "Interpolator", "value": 1},
                    {"name": "MatrixInterpolator", "value": 1},
                    {"name": "NumberInterpolator", "value": 1},
                    {"name": "ObjectInterpolator", "value": 1},
                    {"name": "PointInterpolator", "value": 1},
                    {"name": "RectangleInterpolator", "value": 1}
                  ]
                },
                {"name": "ISchedulable", "value": 1},
                {"name": "Parallel", "value": 1},
                {"name": "Pause", "value": 1},
                {"name": "Scheduler", "value": 1},
                {"name": "Sequence", "value": 1},
                {"name": "Transition", "value": 1},
                {"name": "Transitioner", "value": 1},
                {"name": "TransitionEvent", "value": 1},
                {"name": "Tween", "value": 1}
              ]
            },
            {
              "name": "data",
              "children": [
                {
                  "name": "converters",
                  "children": [
                    {"name": "Converters", "value": 1},
                    {"name": "DelimitedTextConverter", "value": 1},
                    {"name": "GraphMLConverter", "value": 1},
                    {"name": "IDataConverter", "value": 1},
                    {"name": "JSONConverter", "value": 1}
                  ]
                },
                {"name": "DataField", "value": 1},
                {"name": "DataSchema", "value": 1},
                {"name": "DataSet", "value": 1},
                {"name": "DataSource", "value": 1},
                {"name": "DataTable", "value": 1},
                {"name": "DataUtil", "value": 1}
              ]
            },
            {
              "name": "display",
              "children": [
                {"name": "DirtySprite", "value": 1},
                {"name": "LineSprite", "value": 1},
                {"name": "RectSprite", "value": 1},
                {"name": "TextSprite", "value": 1}
              ]
            },
            {
              "name": "flex",
              "children": [
                {"name": "FlareVis", "value": 1}
              ]
            },
            {
              "name": "physics",
              "children": [
                {"name": "DragForce", "value": 1},
                {"name": "GravityForce", "value": 1},
                {"name": "IForce", "value": 1},
                {"name": "NBodyForce", "value": 1},
                {"name": "Particle", "value": 1},
                {"name": "Simulation", "value": 1},
                {"name": "Spring", "value": 1},
                {"name": "SpringForce", "value": 1}
              ]
            },
            {
              "name": "query",
              "children": [
                {"name": "AggregateExpression", "value": 1},
                {"name": "And", "value": 1},
                {"name": "Arithmetic", "value": 1},
                {"name": "Average", "value": 1},
                {"name": "BinaryExpression", "value": 1},
                {"name": "Comparison", "value": 1},
                {"name": "CompositeExpression", "value": 1},
                {"name": "Count", "value": 1},
                {"name": "DateUtil", "value": 1},
                {"name": "Distinct", "value": 1},
                {"name": "Expression", "value": 1},
                {"name": "ExpressionIterator", "value": 1},
                {"name": "Fn", "value": 1},
                {"name": "If", "value": 1},
                {"name": "IsA", "value": 1},
                {"name": "Literal", "value": 1},
                {"name": "Match", "value": 1},
                {"name": "Maximum", "value": 1},
                {
                  "name": "methods",
                  "children": [
                    {"name": "add", "value": 1},
                    {"name": "and", "value": 1},
                    {"name": "average", "value": 1},
                    {"name": "count", "value": 1},
                    {"name": "distinct", "value": 1},
                    {"name": "div", "value": 1},
                    {"name": "eq", "value": 1},
                    {"name": "fn", "value": 1},
                    {"name": "gt", "value": 1},
                    {"name": "gte", "value": 1},
                    {"name": "iff", "value": 1},
                    {"name": "isa", "value": 1},
                    {"name": "lt", "value": 1},
                    {"name": "lte", "value": 1},
                    {"name": "max", "value": 1},
                    {"name": "min", "value": 1},
                    {"name": "mod", "value": 1},
                    {"name": "mul", "value": 1},
                    {"name": "neq", "value": 1},
                    {"name": "not", "value": 1},
                    {"name": "or", "value": 1},
                    {"name": "orderby", "value": 1},
                    {"name": "range", "value": 1},
                    {"name": "select", "value": 1},
                    {"name": "stddev", "value": 1},
                    {"name": "sub", "value": 1},
                    {"name": "sum", "value": 1},
                    {"name": "update", "value": 1},
                    {"name": "variance", "value": 1},
                    {"name": "where", "value": 1},
                    {"name": "xor", "value": 1},
                    {"name": "_", "value": 1}
                  ]
                },
                {"name": "Minimum", "value": 1},
                {"name": "Not", "value": 1},
                {"name": "Or", "value": 1},
                {"name": "Query", "value": 1},
                {"name": "Range", "value": 1},
                {"name": "StringUtil", "value": 1},
                {"name": "Sum", "value": 1},
                {"name": "Variable", "value": 1},
                {"name": "Variance", "value": 1},
                {"name": "Xor", "value": 1}
              ]
            },
            {
              "name": "scale",
              "children": [
                {"name": "IScaleMap", "value": 1},
                {"name": "LinearScale", "value": 1},
                {"name": "LogScale", "value": 1},
                {"name": "OrdinalScale", "value": 1},
                {"name": "QuantileScale", "value": 1},
                {"name": "QuantitativeScale", "value": 1},
                {"name": "RootScale", "value": 1},
                {"name": "Scale", "value": 1},
                {"name": "ScaleType", "value": 1},
                {"name": "TimeScale", "value": 1}
              ]
            },
            {
              "name": "util",
              "children": [
                {"name": "Arrays", "value": 1},
                {"name": "Colors", "value": 1},
                {"name": "Dates", "value": 1},
                {"name": "Displays", "value": 1},
                {"name": "Filter", "value": 1},
                {"name": "Geometry", "value": 1},
                {
                  "name": "heap",
                  "children": [
                    {"name": "FibonacciHeap", "value": 1},
                    {"name": "HeapNode", "value": 1}
                  ]
                },
                {"name": "IEvaluable", "value": 1},
                {"name": "IPredicate", "value": 1},
                {"name": "IValueProxy", "value": 1},
                {
                  "name": "math",
                  "children": [
                    {"name": "DenseMatrix", "value": 1},
                    {"name": "IMatrix", "value": 1},
                    {"name": "SparseMatrix", "value": 1}
                  ]
                },
                {"name": "Maths", "value": 1},
                {"name": "Orientation", "value": 1},
                {
                  "name": "palette",
                  "children": [
                    {"name": "ColorPalette", "value": 1},
                    {"name": "Palette", "value": 1},
                    {"name": "ShapePalette", "value": 1},
                    {"name": "SizePalette", "value": 1}
                  ]
                },
                {"name": "Property", "value": 1},
                {"name": "Shapes", "value": 1},
                {"name": "Sort", "value": 1},
                {"name": "Stats", "value": 1},
                {"name": "Strings", "value": 1}
              ]
            },
            {
              "name": "vis",
              "children": [
                {
                  "name": "axis",
                  "children": [
                    {"name": "Axes", "value": 1},
                    {"name": "Axis", "value": 1},
                    {"name": "AxisGridLine", "value": 1},
                    {"name": "AxisLabel", "value": 1},
                    {"name": "CartesianAxes", "value": 1}
                  ]
                },
                {
                  "name": "controls",
                  "children": [
                    {"name": "AnchorControl", "value": 1},
                    {"name": "ClickControl", "value": 1},
                    {"name": "Control", "value": 1},
                    {"name": "ControlList", "value": 1},
                    {"name": "DragControl", "value": 1},
                    {"name": "ExpandControl", "value": 1},
                    {"name": "HoverControl", "value": 1},
                    {"name": "IControl", "value": 1},
                    {"name": "PanZoomControl", "value": 1},
                    {"name": "SelectionControl", "value": 1},
                    {"name": "TooltipControl", "value": 1}
                  ]
                },
                {
                  "name": "data",
                  "children": [
                    {"name": "Data", "value": 1},
                    {"name": "DataList", "value": 1},
                    {"name": "DataSprite", "value": 1},
                    {"name": "EdgeSprite", "value": 1},
                    {"name": "NodeSprite", "value": 1},
                    {
                      "name": "render",
                      "children": [
                        {"name": "ArrowType", "value": 1},
                        {"name": "EdgeRenderer", "value": 1},
                        {"name": "IRenderer", "value": 1},
                        {"name": "ShapeRenderer", "value": 1}
                      ]
                    },
                    {"name": "ScaleBinding", "value": 1},
                    {"name": "Tree", "value": 1},
                    {"name": "TreeBuilder", "value": 1}
                  ]
                },
                {
                  "name": "events",
                  "children": [
                    {"name": "DataEvent", "value": 1},
                    {"name": "SelectionEvent", "value": 1},
                    {"name": "TooltipEvent", "value": 1},
                    {"name": "VisualizationEvent", "value": 1}
                  ]
                },
                {
                  "name": "legend",
                  "children": [
                    {"name": "Legend", "value": 1},
                    {"name": "LegendItem", "value": 1},
                    {"name": "LegendRange", "value": 1}
                  ]
                },
                {
                  "name": "operator",
                  "children": [
                    {
                      "name": "distortion",
                      "children": [
                        {"name": "BifocalDistortion", "value": 1},
                        {"name": "Distortion", "value": 1},
                        {"name": "FisheyeDistortion", "value": 1}
                      ]
                    },
                    {
                      "name": "encoder",
                      "children": [
                        {"name": "ColorEncoder", "value": 1},
                        {"name": "Encoder", "value": 1},
                        {"name": "PropertyEncoder", "value": 1},
                        {"name": "ShapeEncoder", "value": 1},
                        {"name": "SizeEncoder", "value": 1}
                      ]
                    },
                    {
                      "name": "filter",
                      "children": [
                        {"name": "FisheyeTreeFilter", "value": 1},
                        {"name": "GraphDistanceFilter", "value": 1},
                        {"name": "VisibilityFilter", "value": 1}
                      ]
                    },
                    {"name": "IOperator", "value": 1},
                    {
                      "name": "label",
                      "children": [
                        {"name": "Labeler", "value": 1},
                        {"name": "RadialLabeler", "value": 1},
                        {"name": "StackedAreaLabeler", "value": 1}
                      ]
                    },
                    {
                      "name": "layout",
                      "children": [
                        {"name": "AxisLayout", "value": 1},
                        {"name": "BundledEdgeRouter", "value": 1},
                        {"name": "CircleLayout", "value": 1},
                        {"name": "CirclePackingLayout", "value": 1},
                        {"name": "DendrogramLayout", "value": 1},
                        {"name": "ForceDirectedLayout", "value": 1},
                        {"name": "IcicleTreeLayout", "value": 1},
                        {"name": "IndentedTreeLayout", "value": 1},
                        {"name": "Layout", "value": 1},
                        {"name": "NodeLinkTreeLayout", "value": 1},
                        {"name": "PieLayout", "value": 1},
                        {"name": "RadialTreeLayout", "value": 1},
                        {"name": "RandomLayout", "value": 1},
                        {"name": "StackedAreaLayout", "value": 1},
                        {"name": "TreeMapLayout", "value": 1}
                      ]
                    },
                    {"name": "Operator", "value": 1},
                    {"name": "OperatorList", "value": 1},
                    {"name": "OperatorSequence", "value": 1},
                    {"name": "OperatorSwitch", "value": 1},
                    {"name": "SortOperator", "value": 1}
                  ]
                },
                {"name": "Visualization", "value": 1}
              ]
            }
          ]
        }
    );
  }

  renderIcicle(data: any) {
    const container = this.renderRoot.querySelector('#d3-container');
    if (!container) return;
    container.innerHTML = '';

    const width = 1000;
    const height = 4000;

    const format = d3.format(",d");
    const color = d3.scaleOrdinal(d3.quantize(d3.interpolateRainbow, data.children.length + 1));
    const partition = d3.partition().size([height, width]).padding(1);

    const root = partition(
        d3.hierarchy(data)
            .sum((d: any) => d.value)
            .sort((a, b) => b.height - a.height)
    );

    const svg = d3.create("svg")
        .attr("width", width)
        .attr("height", height)
        .attr("viewBox", [0, 0, width, height])
        .attr("style", "max-width: 100%; height: auto; font: 10px sans-serif");

    const cell = svg.selectAll()
        .data(root.descendants())
        .join("g")
        .attr("transform", (d: any) => `translate(${d.y0},${d.x0})`);

    cell.append("title")
        .text((d: any) => `${d.ancestors().map((d: any) => d.data.name).reverse().join("/")}\n${format(d.value)}`);

    cell.append("rect")
        .attr("width", (d: any) => d.y1 - d.y0)
        .attr("height", (d: any) => d.x1 - d.x0)
        .attr("fill-opacity", 0.6)
        .attr("fill", (d: any) => {
          if (!d.depth) return "#ccc";
          while (d.depth > 1) d = d.parent;
          return color(d.data.name);
        });

    const text = cell.filter((d: any) => (d.x1 - d.x0) > 16).append("text")
        .attr("x", 4)
        .attr("y", 13);

    text.append("tspan")
        .text((d: any) => d.data.name);

    text.append("tspan")
        .attr("fill-opacity", 0.7)
        .text((d: any) => ` ${format(d.value)}`);

    container.appendChild(svg.node() as Node);
  }
}
