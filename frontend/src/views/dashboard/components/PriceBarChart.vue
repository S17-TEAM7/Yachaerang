<template>
  <div class="h-40 md:h-52">
    <canvas ref="barCanvas"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from "vue";
import Chart from "chart.js/auto";

const props = defineProps({
  labels: Array,
  diffData: Array,
});

const barCanvas = ref(null);
let barChart = null;

const crosshairPlugin = {
  id: "crosshair",
  z: 10,
  afterEvent: (chart, args) => {
    const { event, inChartArea } = args;
    chart.crosshairX = inChartArea ? event.x : null;
  },
  afterDraw: (chart) => {
    if (chart.crosshairX === null || chart.crosshairX === undefined) return;

    const ctx = chart.ctx;
    const chartArea = chart.chartArea;

    ctx.save();
    ctx.strokeStyle = "rgba(148, 163, 184, 0.5)";
    ctx.lineWidth = 1;
    ctx.setLineDash([5, 5]);
    ctx.beginPath();
    ctx.moveTo(chart.crosshairX, chartArea.top);
    ctx.lineTo(chart.crosshairX, chartArea.bottom);
    ctx.stroke();
    ctx.restore();
  },
};

const initChart = () => {
  if (!barCanvas.value) return;
  if (barChart) barChart.destroy();

  const validDiffs = props.diffData.map((d) => d.diff).filter((d) => d !== null && d !== undefined);
  const minDiff = validDiffs.length > 0 ? Math.min(...validDiffs) : 0;
  const maxDiff = validDiffs.length > 0 ? Math.max(...validDiffs) : 0;

  barChart = new Chart(barCanvas.value.getContext("2d"), {
    type: "bar",
    data: {
      labels: props.labels,
      datasets: [
        {
          data: props.diffData.map((d) => (d.diff !== null && d.diff !== undefined ? d.diff : null)),
          backgroundColor: props.diffData.map((d) => {
            if (d.diff === null || d.diff === undefined) return "rgba(200, 200, 200, 0.3)";
            return d.diff >= 0 ? "#fda4af" : "#93c5fd";
          }),
          hoverBackgroundColor: props.diffData.map((d) => {
            if (d.diff === null || d.diff === undefined) return "rgba(200, 200, 200, 0.5)";
            return d.diff >= 0 ? "#f43f5e" : "#3b82f6";
          }),
          borderRadius: 2,
          barPercentage: 0.8,
        },
      ],
    },
    plugins: [crosshairPlugin],
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: "index",
        intersect: false,
        axis: "x",
      },
      onHover: (event, activeElements) => {
        const chart = event.chart;
        if (activeElements.length > 0) {
          chart.canvas.style.cursor = "crosshair";
        } else {
          chart.canvas.style.cursor = "default";
        }
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: "rgba(30, 41, 59, 0.95)",
          titleColor: "#fff",
          bodyColor: "#fff",
          titleFont: { size: 13, weight: "bold" },
          bodyFont: { size: 12, weight: "500" },
          padding: 14,
          displayColors: false,
          cornerRadius: 8,
          titleSpacing: 6,
          bodySpacing: 4,
          z: 100,
          callbacks: {
            title: (context) => {
              const label = props.labels[context[0].dataIndex];
              return label ? `날짜: ${label}` : "";
            },
            label: (ctx) => {
              const item = props.diffData[ctx.dataIndex];
              if (item.diff === null || item.diff === undefined || item.val === null || item.val === undefined) {
                return ["값 없음"];
              }
              const prefix = item.diff >= 0 ? "▲" : "▼";
              const sign = item.diff >= 0 ? "+" : "";
              return [
                `실거래가: ${item.val.toLocaleString()}원`,
                `평균대비: ${prefix} ${Math.abs(Math.floor(item.diff)).toLocaleString()}원 (${sign}${item.pct !== null && item.pct !== undefined ? item.pct.toFixed(1) : "0.0"}%)`,
              ];
            },
          },
        },
      },
      scales: {
        x: { display: false },
        y: {
          suggestedMin: minDiff * 1.5,
          suggestedMax: maxDiff * 1.5,
          border: {
            display: true,
            color: "#cbd5e1",
            width: 2,
          },
          grid: {
            color: (ctx) => (ctx.tick.value === 0 ? "#94a3b8" : "#e2e8f0"),
            lineWidth: (ctx) => (ctx.tick.value === 0 ? 2 : 1),
            drawBorder: true,
          },
          ticks: {
            display: true,
            values: [0],
            callback: (val) => (val === 0 ? "AVG" : ""),
            font: { size: 12, weight: "bold" },
            color: "#64748b",
          },
        },
      },
    },
  });
};

onMounted(() => {
  initChart();
  if (barCanvas.value) {
    barCanvas.value.addEventListener("mousemove", (e) => {
      if (barChart) {
        const rect = barCanvas.value.getBoundingClientRect();
        const x = e.clientX - rect.left;
        barChart.crosshairX = x;
        barChart.update("none");
      }
    });
    barCanvas.value.addEventListener("mouseleave", () => {
      if (barChart) {
        barChart.crosshairX = null;
        barChart.update("none");
      }
    });
  }
});
watch(() => props.diffData, initChart, { deep: true });
</script>
