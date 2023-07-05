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
  <el-container style="height: 100%">
    <el-header>
      <view-menu subject="analysisResult"
                 :file="file"
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
        <div style="left: 0%;top: 60px; position: fixed; z-index: 100; width: 10%; height: 100%; font-size: 16px">
          <el-card :header="$t('jifa.threadDump.navigation')" style="height: 100%;">
            <div class="nav-item"><a href="#navTop">{{ $t('jifa.threadDump.navToTop') }}</a></div>
            <el-divider/>
            <div class="nav-item"><a href="#overview">{{ $t('jifa.threadDump.basicInfo') }}</a></div>
            <div class="nav-item"><a href="#threadSummary">{{ $t('jifa.threadDump.threadSummary') }}</a></div>
            <div class="nav-item"><a href="#threadGroupSummary">{{ $t('jifa.threadDump.threadGroupSummary') }}</a></div>
            <div class="nav-item"><a href="#monitors">{{ $t('jifa.threadDump.monitors') }}</a></div>
            <div class="nav-item"><a href="#callSiteTree">{{ $t('jifa.threadDump.callSiteTree') }}</a></div>
            <el-divider/>
            <div class="nav-item"><a href="#fileContent" @click="showDetail('fileContent')">{{ $t('jifa.threadDump.fileContent') }}</a></div>
          </el-card>
        </div>
      
        <el-main style="padding: 5px; height: 100%;">
          <main-page :file="file" style="width: 90%; left: 10%;" id="navTop"/>
        </el-main>
      </el-container>
    </el-main>
  </el-container>
</template>

<script>
import ViewMenu from "../menu/ViewMenu";
import MainPage from "./Main";
import axios from "axios";
import {threadDumpService} from "@/util";

export default {
  props: ['file'],
  components: {
    ViewMenu, MainPage
  },
  data() {
    return {
      analysisState: 'NOT_STARTED',
      progressState: 'info',
      message: '',
      progress: 0,
      pollingInternal: 500,
    }
  },
  methods: {
    pollProgressOfAnalysis() {
      let self = this;
      if (!self || self._isDestroyed) {
        return
      }
      axios.get(threadDumpService(this.file, 'progressOfAnalysis')).then(resp => {
        let state = resp.data.state;
        let percent = resp.data.percent;
        if (resp.data.message) {
          this.message = resp.data.message.replace(/\\n/gm, "<b/>")
        }
        if (state === 'IN_PROGRESS') {
          if (percent >= 1) {
            this.progress = 99
          } else {
            this.progress = percent * 100
          }
          setTimeout(this.pollProgressOfAnalysis, this.pollingInternal)
        } else if (state === 'SUCCESS') {
          this.progress = 100;
          this.progressState = 'success';
          this.$notify({
            title: this.$t("jifa.goToOverViewPrompt"),
            position: 'top-right',
            type: "success",
            offset: 200, // changed
            duration: 1000,
            showClose: true,
            onClose: () => {
              this.analysisState = "SUCCESS"
            }
          })
        } else {
          this.progressState = 'danger';
          this.analysisState = "ERROR";
          axios.post(threadDumpService(this.file, 'release'))
        }
      })
    },
    analyzeThreadDump() {
      axios.post(threadDumpService(this.file, 'analyze')).then(() => {
        this.analysisState = "IN_PROGRESS";
        this.pollProgressOfAnalysis();
      })
    },
    showDetail(id) {
       //TODO: expand this
    }
  },
  mounted() {
    this.analyzeThreadDump();
  }
}
</script>
