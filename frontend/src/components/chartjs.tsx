'use client';

import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    BarElement,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend,
    ChartOptions,
    ChartData,
    LineController,
    BarController,
} from 'chart.js';
import { Chart } from 'react-chartjs-2';
import React from 'react';
import { Typography } from '@mui/material';

ChartJS.register(
    CategoryScale,
    LinearScale,
    BarElement,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend,
    LineController,
    BarController
);

export type RawWeeklyData = {
    startWeek: string;
    cost: number;
};

export type ChartInput = {
    type: 'line' | 'bar';
    label: string;
    // Accepts either array of numbers or a mapping for weekly
    data: (number | null)[] | RawWeeklyData[];
    yAxisID?: 'left' | 'right';
    color: string;
    labelType?: 'weekly';
};

export type LegendProp = {
    display: boolean;
    position: 'top' | 'left' | 'bottom' | 'right' | 'chartArea';
};

interface GenericChartProps {
    title?: string;
    labels: string[];
    datasets: ChartInput[];
    width?: number;
    height?: number;
    leftAxisLabel?: string;
    rightAxisLabel?: string;
    legend?: LegendProp;
}

export default function GenericChart({
    title,
    labels,
    datasets,
    width,
    height,
    leftAxisLabel,
    rightAxisLabel,
    legend,
}: GenericChartProps) {
    const chartData: ChartData<'bar' | 'line', number[], string> = {
        labels,
        datasets: datasets.map(ds => {
            let processedData: (number | null)[] = [];

            if (ds.labelType === 'weekly') {
                const raw = ds.data as RawWeeklyData[];
                const dataMap = new Map(
                    raw.map(entry => [
                        formatDateToLabel(entry.startWeek),
                        entry.cost,
                    ])
                );

                processedData = labels.map(label => dataMap.get(label) ?? null);
            } else {
                processedData = ds.data as number[]; // fallback to direct numeric data
            }

            const base: any = {
                type: ds.type,
                label: ds.label,
                data: processedData,
                yAxisID: ds.yAxisID === 'right' ? 'yRight' : 'y',
            };

            if (ds.type === 'line') {
                return {
                    ...base,
                    borderColor: ds.color,
                    tension: 0,
                    fill: false,
                    pointBackgroundColor: ds.color,
                    segment: {
                        borderColor: (ctx: any) =>
                            ctx.p1.parsed.y < 0 ? 'red' : ds.color,
                    },
                };
            }

            if (ds.type === 'bar') {
                return {
                    ...base,
                    backgroundColor: ds.color,
                    borderColor: ds.color,
                    borderWidth: 1,
                };
            }

            return base;
        }),
    };

    const options: ChartOptions<'bar' | 'line'> = {
        responsive: true,
        interaction: { mode: 'index', intersect: false },
        scales: {
            y: {
                position: 'left',
                beginAtZero: true,
                title: { display: !!leftAxisLabel, text: leftAxisLabel },
            },
            yRight: {
                position: 'right',
                beginAtZero: true,
                grid: { drawOnChartArea: false }, // avoid overlapping grids
                title: { display: !!rightAxisLabel, text: rightAxisLabel },
            },
        },
        plugins: {
            legend: {
                display: legend ? legend.display : false,
                position: legend ? legend.position : 'bottom',
            },
        },
    };

    return (
        <div style={{ width: width || '100%', height: height || '400px' }}>
            {title ? (
                <Typography variant="h6" fontWeight="bold" align="center">
                    {title}
                </Typography>
            ) : (
                ''
            )}
            <Chart type="bar" data={chartData} options={options} />
        </div>
    );
}

export function formatDateToLabel(dateStr: string): string {
    const date = new Date(dateStr);
    const day = String(date.getUTCDate()).padStart(2, '0');
    const month = String(date.getUTCMonth() + 1).padStart(2, '0');
    const year = date.getUTCFullYear();
    return `${day}/${month}/${year}`;
}
