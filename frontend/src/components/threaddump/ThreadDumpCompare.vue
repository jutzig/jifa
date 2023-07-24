<!--
    Copyright (c) 2022 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->

 <template>
    <el-container  style="height: 100%">
        <el-container>
          <el-header>
            <view-menu subject="analysisResult"
                       :file="files"
                       :analysisState="analysisState"
                       type="THREAD_DUMP"/>
          </el-header>
      
          <el-main style="padding-top: 0">
      
            <div style="padding-top: 20px" v-if="analysisState === 'IN_PROGRESS' || analysisState === 'ERROR'">
              <b-progress height="2rem" show-progress :precision="2"
                          :value="progress"
                          :variant="progressState"
                          striped
                          :animated="progress < 100"></b-progress>
              <b-card class="mt-3" bg-variant="dark" text-variant="white" v-if="message">
                <b-card-text style="white-space: pre-line;">{{ message }}</b-card-text>
                <div class="d-flex justify-content-center mb-3" v-if="progressState === 'info'">
                  <b-spinner></b-spinner>
                </div>
              </b-card>
            </div>
      
            <el-container v-if="analysisState === 'SUCCESS'" style="height: 100%">
              <el-aside width="200px">
                <div style="font-size: 16px; height: 100%;">
                <el-card :header="$t('jifa.threadDump.navigation')" style="height: 100%;">
                  <div class="nav-item"><a href="#navTop">{{ $t('jifa.threadDump.navToTop') }}</a></div>
                  <el-divider/>
                  <div class="nav-item"><a href="#overview">{{ $t('jifa.threadDump.basicInfo') }}</a></div>
                  <div class="nav-item"><a href="#blockedThreads">{{ $t('jifa.threadDump.blockedThreadsLabel') }}</a></div>
                  <div class="nav-item"><a href="#threadSummary">{{ $t('jifa.threadDump.threadSummary') }}</a></div>
                  <div class="nav-item"><a href="#threadGroupSummary">{{ $t('jifa.threadDump.threadGroupSummary') }}</a></div>
                  <div class="nav-item"><a href="#monitors">{{ $t('jifa.threadDump.monitors') }}</a></div>
                  <div class="nav-item"><a href="#callSiteTree">{{ $t('jifa.threadDump.callSiteTree') }}</a></div>
                </el-card>
              </div>
             </el-aside>
              <el-main style="padding: 20px; height: 100%;">
                <el-container>
                  <el-card header="Thread State Compare" style="width: 100%;">
                    <div>
                      <el-row :gutter="10">
                        <el-col :span="10">
                          <table style="width: 100%;" class="thread-state-table">
                            <thead>
                              <th>Thread State</th>
                              <th v-for="(fileInfo, index) in comparison.fileInfos" ><a :href='"../threadDump?file=" + fileInfo.name' target="_blank" rel="noopener">{{fileInfo.originalName}}</a></th>
                            </thead>
                            <tbody>
                                <tr v-for="(state, stateIndex) in comparison.overviews[0].javaStates" >
                                    <td>{{state}}</td>
                                    <td v-for="(overview, index) in comparison.overviews" style="text-align: right;" >
                                      <span v-if="index>0 && computeThreadCountDiff(comparison, index, stateIndex) < 0" class="data-negative">( {{ computeThreadCountDiff(comparison, index, stateIndex) }} ) </span>
                                      <span v-if="index>0 && computeThreadCountDiff(comparison, index, stateIndex) > 0" class="data-positive">( +{{ computeThreadCountDiff(comparison, index, stateIndex) }} ) </span>                                      
                                      <span>{{overview.javaThreadStat.javaCounts[stateIndex]}}</span>
                                    </td>
                                </tr>
                            </tbody>
                          </table>
                        </el-col>
                        <el-col :span="12"><line-chart :chart-data="threadsChartData.data" :options="threadsChartOptions" :width='800' :height='400' /></el-col>
                      </el-row>
                    </div>
                  </el-card>
                </el-container>
              </el-main>
            </el-container>
          </el-main>
        </el-container>
    </el-container> 
  </template>
  
  <script>
  import ViewMenu from "../menu/ViewMenu";
  import axios from "axios";
  import {threadDumpService} from "@/util";
  import {threadDumpBase} from "@/util";
  import LineChart from '../charts/LineChart'
    

  export default {
    components: {
      LineChart,
      ViewMenu
    },
    data() {
      return {
        color: [
        '#003f5c',
        '#2f4b7c',
        '#665191',
        '#a05195',
        '#d45087',
        '#f95d6a',
        '#ff7c43',
        '#ffa600',
        '#488f31',
        '#8aa1b4'
        ],
        analysisState: 'NOT_STARTED',
        progressState: 'info',
        loading: true,
        message: '',
        progress: 0,
        pollingInternal: 500,
        comparison: null,
        files: [],
        threadsChartData: {
          type: 'line',
          data: {
            datasets: {}
          }
        },
        threadsChartOptions: {
          responsive: false,
          maintainAspectRatio: false,
          plugins: {
            title: {
              display: true,
              text: "test title"
            },
            tooltip: {
              mode: 'index'
            },
          },
          legend: {
            position: 'bottom'
          },
          interaction: {
            mode: 'nearest',
            axis: 'x',
            intersect: false
          },
          scales: {
            yAxes: [{
              stacked: true,
            }]
          }
        },
 
      }
    },
    methods: {
      analyzeThreadDump() {
        let self = this;
        this.analysisState = "IN_PROGRESS";

        let params = '?';
        for (let i = 0; i < this.files.length; i++) {
          params = params + "files=" + this.files[i] + "&"
        }
        axios.get(threadDumpBase() + 'compare/summary' + params).then(resp => {
          let summary = resp.data
          self.comparison = summary
        
          //create chart data
          this.createThreadStateChartData(summary)
          this.loading = false
          this.analysisState = "SUCCESS"
        })
        
      },
      createThreadStateChartData(summary) {
        let self = this;
        self.threadsChartData.data.labels = []
        summary.fileInfos.forEach(fileInfo => {
          self.threadsChartData.data.labels.push(fileInfo.originalName)
        })
        self.threadsChartData.data.datasets = []
        //one dataset per thread state kind, one data point per dump
        let states = summary.overviews[0].javaStates
        for (let i = 0; i < states.length; i++) {
          let stateData = []
          summary.overviews.forEach(overview => {
            stateData.push({
              y: overview.javaThreadStat.javaCounts[i],
            })
          })
          self.threadsChartData.data.datasets.push({
            data: stateData,
            backgroundColor: self.color[i],
            label: states[i],
          })
        }
      },

      computeThreadCountDiff(comparison, overviewIndex, stateIndex) {
        let value = comparison.overviews[overviewIndex].javaThreadStat.javaCounts[stateIndex]
        if (overviewIndex==0)
          return value
        let baseValue = comparison.overviews[0].javaThreadStat.javaCounts[stateIndex]
        let diff = value - baseValue
        return diff
      },
    },
    
    mounted() {
      this.files = this.$route.query.files 
      this.analyzeThreadDump();
    }
  }
  </script>
  <style>
  .data-positive {
    color: green;
    font-weight: bold;
  }

  .data-negative {
    color: red;
    font-weight: bold;
  }

  .thread-state-table {
    font-size: 0.9rem;
  }

  .thread-state-table tr{
    font-size: 0.9rem;
    border: 1px solid black;
	  padding: 3px;
  }

  .thread-state-table th{
    font-size: 0.9rem;
    border: 1px solid black;
	  padding: 3px;
    text-align: center;
  }

  .thread-state-table td{
    font-size: 0.9rem;
    border: 1px solid black;
	  padding: 3px;
  }

  thead {
	  background-color: #d2d7e2;
	  font-size: 1rem;
  }

  </style>