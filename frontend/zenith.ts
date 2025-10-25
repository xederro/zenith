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
import {html, css, LitElement} from 'lit';
import {customElement, query} from 'lit/decorators.js';
import * as d3 from "d3";

interface Project {
  name: string;
  values: Object;
  children: Project[];
}

interface Value {
  value: string;
  is_inherited: boolean;
}

@customElement('zenith-page')
export class ZenithPage extends LitElement {

  @query('#query') queryInput!: HTMLInputElement;
  data: Project | undefined;

  static override get styles() {
    return css`
      input,
      select,
      textarea {
        background-color: var(--background-color-primary);
        border: 1px solid var(--border-color);
        border-radius: var(--border-radius);
        box-sizing: border-box;
        color: var(--primary-text-color);
        margin: 0;
        padding: var(--spacing-s);
        font: inherit;
      }
      
      nav {
        align-items: center;
        display: flex;
        height: 3rem;
        justify-content: space-between;
        margin: 0 var(--spacing-l);
      }
      
      main {
        background-color: var(--background-color-primary);
      }
    `;
  }

  override render() {
    return html`
      <nav>
        <div>
          <h1>Repository Graph</h1>
        </div>
        <div>
          <label for="query" style="flex: 0 0 60px;">Filter:</label>
          <input type="text" id="query" name="query" placeholder="Filter projects" style="flex: 1;">
        </div>
      </nav>
      <main>
        <div id="d3-container"></div>
      </main>
    `;
  }

  override async firstUpdated() {
    this.queryInput.addEventListener("change", () => {
      let args = []
      if (!!this.queryInput.value) {
        args.push(`query=${this.queryInput.value}`);
      }
      if (this.getQueryVariable("config") != null) {
        args.push(`config=${this.getQueryVariable("config")}`);
      }
      window.history.pushState({}, "", `${args.length > 0 ? `?${args.join("&")}` : ""}`);

      this.getDataAndRender();
    })

    await this.getDataAndRender();
  }

  async getDataAndRender() {
    const plugin = (this as any).plugin;

    let args = []
    if (!!this.getQueryVariable("query")) {
      args.push(`query=${this.getQueryVariable("query")}`);
      this.queryInput.value = this.getQueryVariable("query") ?? "";
    }

    const query = args.length > 0 ? `?${args.join("&")}` : "";
    this.data = await plugin.restApi().send('GET', `/config/server/zenith~tree${query}`);
    if (this.data != undefined) this.renderTree(this.data);
  }

  getQueryVariable(variable: string): string | null {
    const params = new URLSearchParams(window.location.search);
    const val = params.get(variable)
    return val != null ? decodeURI(val) : null;
  }

  renderTree(data: Project) {
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
    const chosenConfig = this.getQueryVariable("config") ?? "parent"
    const container = this.renderRoot.querySelector('#d3-container');
    if (!container) return;
    container.innerHTML = '';

    const width = container.clientWidth;

    const color = d3.scaleOrdinal(d3.quantize(d3.interpolateRainbow, this.countUniqueValues(data)+1));

    const root = d3.hierarchy(data);

    const dx = 30;
    const dy = (width-100) / (root.height + 1);
    const tree = d3.tree<Project>().nodeSize([dx, dy]);
    tree(root);

    let x0 = Infinity;
    let x1 = -Infinity;
    root.each(d => {
      if (d.x != undefined && d.x > x1) x1 = d.x;
      if (d.x != undefined && d.x < x0) x0 = d.x;
    });

    const height = x1 - x0 + dx * 2;

    const svg = d3.create("svg")
        .attr("viewBox", [-dy / 2, x0 - dx, width, height])
        .attr("width", width)
        .attr("height", height)
        .attr("style", "max-width: 100%; height: auto; font: 16px sans-serif;");

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
        .attr("fill", (d: any) => color(this.unwrap(d.data.values[chosenConfig]).value))
        .attr("r", 6);

    node.append("title")
        .text((d: any) => this.unwrap(d.data.values[chosenConfig]).is_inherited ?
            `${d.data.name}: ${this.unwrap(d.data.values[chosenConfig]).value} (INHERIT)` :
            `${d.data.name}: ${this.unwrap(d.data.values[chosenConfig]).value}`);

    node.append("text")
        .attr("dy", "0.32em")
        .attr("x", 10)
        .attr("text-anchor", "start")
        .attr("paint-order", "stroke")
        .attr("fill", "#fff")
        .text((d: any) => this.unwrap(d.data.values[chosenConfig]).is_inherited ?
            `${this.unwrap(d.data.values[chosenConfig]).value} (INHERIT)` :
            `${this.unwrap(d.data.values[chosenConfig]).value}`);

    node.append("text")
        .attr("dy", "0.32em")
        .attr("x", -10)
        .attr("text-anchor", "end")
        .attr("paint-order", "stroke")
        .attr("fill", "#fff")
        .text((d: any) => d.data.name);

    container.appendChild(svg.node() as Node);
  }

  unwrap(val: Value | null) : Value {
    if (val == null) {
      return {value: 'NOT_AVAILABLE', is_inherited: false} as Value;
    }
    return val as Value;
  }

  countUniqueValues(project: Project | null): number {
    const uniqueValues = new Set<string>();
    const chosenConfig = this.getQueryVariable("config") ?? "parent"

    this.traverse(project, chosenConfig, uniqueValues);
    return uniqueValues.size;
  }

  traverse(node: Project | null, chosenConfig: string, uniqueValues: Set<string>): void {
    if (node == null) return;

    // @ts-ignore
    uniqueValues.add(this.unwrap(node.values[chosenConfig]).value);

    for (const child of node.children) {
      this.traverse(child, chosenConfig, uniqueValues);
    }
  }
}
