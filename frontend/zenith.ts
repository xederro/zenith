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

interface Project {
  name: string;
  value: string;
  children: Project[];
}

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

  override async firstUpdated() {
    const plugin = (this as any).plugin;
    const data: Project = await plugin.restApi().send('GET', "/config/server/zenith~tree");
    this.renderIcicle(data);
  }

  renderIcicle(data: any) {
    const container = this.renderRoot.querySelector('#d3-container');
    if (!container) return;
    container.innerHTML = '';

    const width = 1000;
    const height = 4000;

    const color = d3.scaleOrdinal(d3.quantize(d3.interpolateRainbow, data.children.length + 1));
    const partition = d3.partition().size([height, width]).padding(1);

    const root = partition(
        d3.hierarchy(data)
            .sum(() => 1)
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
        .text((d: any) => `${d.data.name}\n${d.data.value}`);

    cell.append("rect")
        .attr("width", (d: any) => d.y1 - d.y0)
        .attr("height", (d: any) => d.x1 - d.x0)
        .attr("fill-opacity", 0.6)
        .attr("fill", (d: any) => {
          return color(d.data.value);
        });

    const text = cell.filter((d: any) => (d.x1 - d.x0) > 16).append("text")
        .attr("x", 4)
        .attr("y", 13);

    text.append("tspan")
        .text((d: any) => d.data.name);

    text.append("tspan")
        .attr("fill-opacity", 0.7)
        .text((d: any) => ` ${d.data.value}`);

    container.appendChild(svg.node() as Node);
  }
}
