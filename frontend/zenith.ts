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
        <h1>Repository Graph</h1>
        <div id="d3-container"></div>
      </div>
    `;
  }

  override async firstUpdated() {
    const plugin = (this as any).plugin;

    let args = []
    if (this.getQueryVariable("query") != "") {
      args.push(`query=${this.getQueryVariable("query")}`);
    }
    if (this.getQueryVariable("config") != "") {
      args.push(`config=${this.getQueryVariable("config")}`);
    }

    const query = args.length > 0 ? `?${args.join("&")}` : "";
    const data: Project = await plugin.restApi().send('GET', `/config/server/zenith~tree${query}`);

    // window.history.pushState({}, "", `${args.length > 0 ? `?${args.join("&")}` : ""}`);
    this.renderTree(data);
  }

  getQueryVariable(variable: string): string {
    const query = window.location.search.substring(1);
    const vars = query.split("&");
    for (let i = 0; i < vars.length; i++) {
      const pair = vars[i].split("=");
      if (pair[0] == variable) {
        return pair[1];
      }
    }
    return "";
  }

  renderTree(data: any) {
    /*
     * Copyright 2017–2023 Observable, Inc.
     *
     * Permission to use, copy, modify, and/or distribute this software for any
     * purpose with or without fee is hereby granted, provided that the above
     * copyright notice and this permission notice appear in all copies.
     *
     * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
     * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
     * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
     * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
     * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
     * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
     * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
     */
    const container = this.renderRoot.querySelector('#d3-container');
    if (!container) return;
    container.innerHTML = '';

    const width = container.clientWidth;

    const color = d3.scaleOrdinal(d3.quantize(d3.interpolateRainbow, this.countUniqueValues(data)));

    const root = d3.hierarchy(data);

    const dx = 30;
    const dy = width / (root.height + 1);
    const tree = d3.tree<Project>().nodeSize([dx, dy]);
    tree(root);

    let x0 = Infinity;
    let x1 = -x0;
    root.each(d => {
      if (d.x != undefined && d.x > x1) x1 = d.x;
      if (d.x != undefined && d.x < x0) x0 = d.x;
    });

    const height = x1 - x0 + dx * 2;

    const svg = d3.create("svg")
        .attr("viewBox", [-dy / 2, x0 - dx, width, height])
        .attr("width", width)
        .attr("height", height)
        .attr("style", "max-width: 100%; height: auto; font: 16px sans-serif");

    svg.append("g")
        .attr("fill", "none")
        .attr("stroke", "#555")
        .attr("stroke-opacity", 0.4)
        .attr("stroke-width", 3)
        .selectAll("path")
        .data(root.links())
        .join("path")
        .attr("d", (d) => {
          const linkGenerator = d3
              .linkHorizontal<d3.HierarchyPointLink<Project>, [number, number]>()
              .source((link) => [link.source.y, link.source.x])
              .target((link) => [link.target.y, link.target.x]);
          return linkGenerator(d as d3.HierarchyPointLink<Project>);
        });

    const node = svg.append("g")
        .selectAll("a")
        .data(root.descendants())
        .join("a")
        .attr("xlink:href", (d: any) => `/admin/repos/${d.data.name},access`)
        .attr("target", "_blank")
        .attr("transform", (d: any) => `translate(${d.y},${d.x})`);

    node.append("circle")
        .attr("fill", (d: any) => color(d.data.value))
        .attr("r", 6);

    node.append("title")
        .text((d: any) => `${d.data.name}: ${d.data.value}`);

    node.append("text")
        .attr("dy", "0.32em")
        .attr("x", 10)
        .attr("text-anchor", "start")
        .attr("paint-order", "stroke")
        .attr("stroke", "#fff")
        .attr("stroke-width", 3)
        .text((d: any) => `${d.data.name}: ${d.data.value}`);

    container.appendChild(svg.node() as Node);
  }

  countUniqueValues(project: Project | null): number {
    const uniqueValues = new Set<string>();

    function traverse(node: Project | null): void {
      if (!node) return;

      uniqueValues.add(node.value);

      for (const child of node.children) {
        traverse(child);
      }
    }

    traverse(project);
    return uniqueValues.size;
  }
}
