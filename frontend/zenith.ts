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
import { Project, Value } from './types';

@customElement('zenith-page')
export class ZenithPage extends LitElement {

  @query('#selectMenu') selectMenu!: HTMLDialogElement;
  @query('#query') queryInput!: HTMLInputElement;
  data: Project | undefined;

  static override get styles() {
    return css`
      dialog {
        min-width: 25vw;
        max-height: 80vh;
        padding: 0;
        border: 1px solid var(--border-color);
        border-radius: var(--border-radius);
        background: var(--dialog-background-color);
        box-shadow: var(--elevation-level-5);
        font-family: var(--font-family, ''), 'Roboto', Arial, sans-serif;
        font-size: var(--font-size-normal, 1rem);
        line-height: var(--line-height-normal, 1.4);
        color: var(--primary-text-color, black);
      }

      dialog::backdrop {
        background-color: black;
        opacity: var(--modal-opacity, 0.6);
      }

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

      a {
        color: var(--link-color);
      }

      nav {
        align-items: center;
        display: flex;
        justify-content: space-between;
        height: 3rem;
        margin: 0;
        padding: 0 var(--spacing-l);
        background-color: var(--background-color-primary);
      }

      button {
        color: #fff;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-family: Arial, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
        font-size: 13px;
        line-height: normal;
        padding: 8px 16px;
        text-align: center;
      }

      .red {
        background-color: rgb(192, 19, 19);

        &:hover {
          background-color: rgb(152, 3, 3);
          transition: background-color 0.1s ease-in-out;
        }
      }

      button:focus-visible {
        outline: 1px solid var(--border-color);
      }

      #d3-container {
        margin: 0;
        padding: 0;
        background-color: var(--background-color-primary);
      }

      .dialog-content {
        max-height: 60vh;
        overflow: auto;
        padding: 0 1rem 1rem;
        margin: 0;
      }

      .level-0 > .node {
        top: 0;
        z-index: 10;
      }

      .level-1 > .node {
        top: 2rem;
        z-index: 9;
      }

      .level-2 > .node {
        top: 4rem;
        z-index: 8;
      }

      .level-3 > .node {
        top: 6rem;
        z-index: 7;
      }

      .level-4 > .node {
        top: 8rem;
        z-index: 6;
      }

      .level-5 > .node {
        top: 10rem;
        z-index: 5;
      }

      .node {
        position: sticky;
        background-color: var(--background-color-primary);
        margin-bottom: 0;
        margin-top: 0;
        padding-bottom: 0.5rem;
        padding-top: 0.5rem;
      }

      .list {
        list-style-type: none;
        margin: 0;
        padding: 0;

        & > li {
          display: flex;
          justify-content: space-between;
          align-items: center;

          & > .key {
            font-weight: bold;
            cursor: pointer;
            color: var(--link-color);

            &:hover {
              text-decoration: underline;
            }
          }

          & > .value {
            margin-left: 1rem;
            text-align: right;
          }
        }
      }

      .inherit {
        font-size: 0.9em;
        color: var(--secondary-text-color);
      }
    `;
  }

  override render() {
    return html`
      <dialog closedby="any" id="selectMenu">
      </dialog>
      <nav>
        <div>
          <h1>Repository Graph</h1>
        </div>
        <div>
          <label for="query" style="flex: 0 0 60px;">Filter:</label>
          <input type="text" id="query" name="query" placeholder="Filter projects" style="flex: 1;">
        </div>
      </nav>
      <div id="d3-container"></div>
    `;
  }

  override async firstUpdated() {
    this.selectMenu.addEventListener("close", () => {
      this.setHashVariable("open", null);
    })

    this.queryInput.addEventListener("change", () => {
      if (!!this.queryInput.value) {
        this.setHashVariable("query", this.queryInput.value);
      }
      if (this.getHashVariable("config") != null) {
        this.setHashVariable("config", this.getHashVariable("config"));
      }

      this.getDataAndRender();
    })

    await this.getDataAndRender();
  }

  async getDataAndRender() {
    const plugin = (this as any).plugin;

    let args = []
    if (!!this.getHashVariable("query")) {
      args.push(`query=${this.getHashVariable("query")}`);
      this.queryInput.value = this.getHashVariable("query") ?? "";
    }

    const query = args.length > 0 ? `?${args.join("&")}` : "";
    this.data = await plugin.restApi().send('GET', `/config/server/zenith~tree${query}`);
    if (this.data != undefined) this.renderTree(this.data);
  }

  getHashVariable(variable: string): string | null {
    const params = new URLSearchParams(window.location.hash.substring(1));
    const val = params.get(variable)
    return val != null ? decodeURI(val) : null;
  }

  setHashVariable(key: string, val: string|null) {
    const params = new URLSearchParams(window.location.hash.substring(1));
    if (val == null) params.delete(key)
    else params.set(key, encodeURI(val));
    window.location.hash = params.toString();
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
    const chosenConfig = this.getHashVariable("config") ?? "parent"
    const chosenProject = this.getHashVariable("open");
    const container = this.renderRoot.querySelector('#d3-container');
    if (!container) return;
    container.innerHTML = '';

    const color = d3.scaleOrdinal(d3.quantize(d3.interpolateRainbow, this.countUniqueValues(data)+1));

    const root = d3.hierarchy(data);

    const dx = 30;
    const dy = Math.max((container.clientWidth-100) / (root.height + 1), 250);
    const tree = d3.tree<Project>().nodeSize([dx, dy]);
    tree(root);

    let x0 = Infinity;
    let x1 = -Infinity;
    let y0 = Infinity;
    let y1 = -Infinity;
    root.each(d => {
      if (d.x != undefined && d.x > x1) x1 = d.x;
      if (d.x != undefined && d.x < x0) x0 = d.x;
      if (d.y != undefined && d.y > y1) y1 = d.y;
      if (d.y != undefined && d.y < y0) y0 = d.y;
      if (!this.selectMenu.open && d.data.name == chosenProject) this.showNodeDialog(d.data)
    });

    const height = x1 - x0 + dx * 2;
    const width = y1 - y0 + dy;

    const svg = d3.create("svg")
        .attr("viewBox", [-dy / 2, x0 - dx, width, height])
        .attr("width", width)
        .attr("height", height)
        .attr("style", "height: auto; font: 16px sans-serif; color: var(--primary-text-color, black);");

    svg.append("g")
        .attr("fill", "none")
        .attr("stroke", "var(--secondary-text-color)")
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
        .selectAll("g")
        .data(root.descendants())
        .join("g")
        .style("cursor", "pointer")
        .attr("transform", (d: any) => `translate(${d.y},${d.x})`)
        .on("click", (event: MouseEvent, d: any) => {
          event.preventDefault();
          this.showNodeDialog(d.data);
        });

    node.append("circle")
        .attr("fill", (d: any) => color(this.unwrap(d.data.values[chosenConfig]).value))
        .attr("r", 6);

    node.append("title")
        .text((d: any) => {
          const node = this.unwrap(d.data.values[chosenConfig]);
          return node.is_inherited ?
              `${d.data.name}: ${node.value} (INHERIT)` :
              `${d.data.name}: ${node.value}`
        });

    node.append("text")
        .attr("dy", "0.32em")
        .attr("x", 10)
        .attr("text-anchor", "start")
        .attr("paint-order", "stroke")
        .attr("fill", "var(--primary-text-color, black)")
        .text((d: any) => {
          const node = this.unwrap(d.data.values[chosenConfig]);
          return node.is_inherited ?
              `${(node.value?.length <= 20 ? node.value : node.value.substring(0, 17) + '...')} (INHERIT)` :
              `${(node.value?.length <= 20 ? node.value : node.value.substring(0, 17) + '...')}`
        });

    node.append("text")
        .attr("dy", "0.32em")
        .attr("x", -10)
        .attr("text-anchor", "end")
        .attr("paint-order", "stroke")
        .attr("fill", "var(--primary-text-color, black)")
        .text((d: any) => d.data.name.split('/').pop());

    container.appendChild(svg.node() as Node);
  }

  showNodeDialog(nodeData: Project) {
    this.selectMenu.innerHTML = '';

    const navBar = document.createElement('nav');

    const atitle = document.createElement('a');
    atitle.href = `/admin/repos/${nodeData.name},access`;
    atitle.target = '_blank';

    const title = document.createElement('h1');
    title.textContent = nodeData.name;
    atitle.appendChild(title);

    const closeButton = document.createElement('button');
    closeButton.id = 'close';
    closeButton.textContent = 'X';
    closeButton.className = 'red';
    closeButton.addEventListener('click', () => {
      this.selectMenu.close();
    });

    navBar.appendChild(atitle);
    navBar.appendChild(closeButton);
    this.selectMenu.appendChild(navBar);

    const container = document.createElement('div');
    container.className = 'dialog-content';
    this.buildAndRenderDialogTree(container, nodeData);
    this.selectMenu.appendChild(container);

    this.selectMenu.showModal();
    this.setHashVariable("open", nodeData.name)
  }

  buildAndRenderDialogTree(parent: HTMLElement, nodeData: Project) {
    const elementMap = new Map<string, HTMLElement>();
    elementMap.set('', parent);

    for (const key of Object.keys(nodeData.values)) {
      const parts = key.split(' ');
      let path = '';

      for (let i = 0; i < parts.length; i++) {
        const part = parts[i];
        const previousPath = path;
        path += (path ? '-' : '') + part;

        if (i === parts.length - 1) {
          // leaf
          const parentEl = elementMap.get(previousPath)!;

          let ul = parentEl.querySelector(':scope > ul.list') as HTMLElement;
          if (!ul) {
            ul = document.createElement('ul');
            ul.className = 'leaf list';
            parentEl.appendChild(ul);
          }

          const li = document.createElement('li');
          // @ts-ignore
          const valueData = this.unwrap(nodeData.values[key]);

          const keySpan = document.createElement('span');
          keySpan.className = 'leaf key';
          keySpan.textContent = part;
          keySpan.addEventListener("click", () => {
            this.setHashVariable("config", key);
            if (this.data != undefined) this.renderTree(this.data);
          });

          const valueSpan = document.createElement('span');
          valueSpan.className = 'leaf value';
          valueSpan.innerHTML = valueData.is_inherited
              ? `${valueData.value} <span class="inherit">(INHERIT)</span>`
              : valueData.value;

          li.appendChild(keySpan);
          li.appendChild(valueSpan);
          ul.appendChild(li);
        } else {
          // node
          if (!elementMap.has(path)) {
            const section = document.createElement('div');
            section.className = `group level-${i}`;
            section.style.paddingLeft = `${i * 16}px`;

            const header = document.createElement('h3');
            header.textContent = part;
            header.className = "node";
            section.appendChild(header);

            elementMap.set(path, section);
            elementMap.get(previousPath)!.appendChild(section);
          }
        }
      }
    }
  }

  unwrap(val: Value | null) : Value {
    if (val == null || !val.value) {
      return {value: 'NOT_AVAILABLE', is_inherited: false} as Value;
    }
    return val as Value;
  }

  countUniqueValues(project: Project | null): number {
    const uniqueValues = new Set<string>();
    const chosenConfig = this.getHashVariable("config") ?? "parent"

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
