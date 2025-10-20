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
  value: Value;
  children: Project[];
}

interface Value {
  value: string;
  is_inherited: boolean;
}

@customElement('zenith-page')
export class ZenithPage extends LitElement {

  @query('#selectMenu') selectMenu!: HTMLDialogElement;
  @query('#open') openBtn!: HTMLButtonElement;
  @query('#close') closeBtn!: HTMLButtonElement;
  @query('#apply') applyBtn!: HTMLButtonElement;
  @query('#query') queryInput!: HTMLInputElement;
  @query('#config') configInput!: HTMLInputElement;

  static override get styles() {
    return css`
      dialog {
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

      select {
        background-color: var(--select-background-color);
        color: var(--primary-text-color);
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

      .blue {
        background-color: rgb(26, 108, 232);

        &:hover {
          background-color: rgb(2, 71, 161);
          transition: background-color 0.1s ease-in-out;
        }
      }

      button:focus-visible {
        outline: 1px solid var(--border-color);
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
      <dialog closedby="any" id="selectMenu">
        <div style="display: flex; flex-direction: column; gap: 12px; padding: 16px; min-width: 320px;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <h1>Filters:</h1>
          </div>
          
          <div style="display: flex; align-items: center; gap: 8px;">
            <label for="query" style="flex: 0 0 60px;">Query:</label>
            <input type="text" id="query" name="query" placeholder="Filter projects" style="flex: 1;">
          </div>

          <div style="display: flex; align-items: center; gap: 8px;">
            <label for="config" style="flex: 0 0 60px;">Config:</label>
            <select name="config" id="config" style="flex: 1;">
              <option selected value="parent">Parent</option>
              <option value="use_contributor_agreements">Contributor Agreements</option>
              <option value="use_content_merge">Content Merge</option>
              <option value="use_signed_off_by">Signed-off-by</option>
              <option value="create_new_change_for_all_not_in_target">Create New Change for Non-target</option>
              <option value="require_change_id">Require Change-Id</option>
              <option value="enable_signed_push">Enable Signed Push</option>
              <option value="require_signed_push">Require Signed Push</option>
              <option value="reject_implicit_merges">Reject Implicit Merges</option>
              <option value="private_by_default">Private by Default</option>
              <option value="work_in_progress_by_default">Work in Progress by Default</option>
              <option value="enable_reviewer_by_email">Reviewer by Email</option>
              <option value="match_author_to_committer_date">Match Author to Committer Date</option>
              <option value="reject_empty_commit">Reject Empty Commit</option>
              <option value="skip_adding_author_and_committer_as_reviewers">Skip Auto-Add Author/Committer</option>
              <option value="default_submit_type">Default Submit Type</option>
              <option value="max_object_size_limit">Max Object Size</option>
              <option value="project_state">Project State</option>
            </select>
          </div>

          <div style="display: flex; justify-content: flex-end; gap: 12px;">
            <button class="red" id="close">Close</button>
            <button class="blue" id="apply" type="submit">Apply</button>
          </div>
        </div>
      </dialog>

      <nav>
        <div>
          <h1>Repository Graph</h1>
        </div>
        <div>
          <button id="open" class="blue">Filter</button>
        </div>
      </nav>
      <main>
        <div id="d3-container"></div>
      </main>
    `;
  }

  override async firstUpdated() {
    this.openBtn.addEventListener("click", () => {
      this.selectMenu.showModal();
    });

    this.closeBtn.addEventListener("click", () => {
      this.selectMenu.close();
    });

    this.applyBtn.addEventListener("click", () => {
      this.selectMenu.close();

      let args = []
      if (!!this.queryInput.value) {
        args.push(`query=${this.queryInput.value}`);
      }
      if (!!this.configInput.value) {
        args.push(`config=${this.configInput.value}`);
      }
      window.history.pushState({}, "", `${args.length > 0 ? `?${args.join("&")}` : ""}`);

      this.getDataAndRender();
    });

    await this.getDataAndRender();
  }

  async getDataAndRender() {
    const plugin = (this as any).plugin;

    let args = []
    if (!!this.getQueryVariable("query")) {
      args.push(`query=${this.getQueryVariable("query")}`);
      this.queryInput.value = this.getQueryVariable("query");
    }
    if (!!this.getQueryVariable("config")) {
      args.push(`config=${this.getQueryVariable("config")}`);
      this.configInput.value = this.getQueryVariable("config");
    }

    const query = args.length > 0 ? `?${args.join("&")}` : "";
    const data: Project = await plugin.restApi().send('GET', `/config/server/zenith~tree${query}`);

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
        .attr("fill", (d: any) => color(d.data.value.value))
        .attr("r", 6);

    node.append("title")
        .text((d: any) => d.data.value.is_inherited ?
            `${d.data.name}: ${d.data.value.value} (INHERIT)` :
            `${d.data.name}: ${d.data.value.value}`);

    node.append("text")
        .attr("dy", "0.32em")
        .attr("x", 10)
        .attr("text-anchor", "start")
        .attr("paint-order", "stroke")
        .attr("stroke", "#fff")
        .attr("stroke-width", 2)
        .text((d: any) => d.data.value.is_inherited ?
            `${d.data.value.value} (INHERIT)` :
            `${d.data.value.value}`);

    node.append("text")
        .attr("dy", "0.32em")
        .attr("x", -10)
        .attr("text-anchor", "end")
        .attr("paint-order", "stroke")
        .attr("stroke", "#fff")
        .attr("stroke-width", 2)
        .text((d: any) => d.data.name);

    container.appendChild(svg.node() as Node);
  }

  countUniqueValues(project: Project | null): number {
    const uniqueValues = new Set<string>();

    function traverse(node: Project | null): void {
      if (!node) return;

      uniqueValues.add(node.value.value);

      for (const child of node.children) {
        traverse(child);
      }
    }

    traverse(project);
    return uniqueValues.size;
  }
}
