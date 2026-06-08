import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import ReactECharts from 'echarts-for-react';
import { GetClimateHistoryGranularityEnum, RoomControllerApi } from '../generated-skeleton-api';
import { DashboardCalendar } from './Calendar';
import { Toast } from 'primereact/toast';
import { Dropdown } from 'primereact/dropdown';
import '../styles/TimeFilter.css';
import '../styles/ClimateHistoryChart.css';
import { useTemperature } from '../hooks/useTemperature';
import { useTimeFormat } from '../hooks/useTimeFormat';

const COLORS = {
    temperature: '#e05252',
    humidity:    '#26a69a',
    airQuality:  '#d4891a',
} as const;

type Metric     = 'All' | 'Temperature' | 'Humidity' | 'Air Quality';
type TimeFilter = 'Day' | 'Week' | 'Month' | 'Custom';

interface DataPoint {
    label:       string;
    temperature: number;
    humidity:    number;
    airQuality:  number;
    /** True when there was no sensor data for this time bucket (gap in the DB). */
    noData?:     boolean;
}

interface Limits {
    tempMin: number;
    tempMax: number;
    humMin:  number;
    humMax:  number;
    co2Max:  number;
}

interface RawPoint {
    timestamp:   string;
    temperature: number;
    humidity:    number;
    airQuality:  number;
}

interface AggPoint {
    date:           string;
    avgTemperature: number;
    avgHumidity:    number;
    avgAirQuality:  number;
}

interface Props {
    roomId: string;
    hideDayView?: boolean;
}

// ── Bucket sizes for the Day-view progressive zoom ──────────────────────────
const MS_HOUR   = 60 * 60 * 1000;
const MS_MINUTE = 60 * 1000;
const MS_SECOND = 1000;

const useIsMobile = () => {
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 899);
    useEffect(() => {
        const handler = () => setIsMobile(window.innerWidth <= 899);
        window.addEventListener('resize', handler);
        return () => window.removeEventListener('resize', handler);
    }, []);
    return isMobile;
};

const mean = (nums: number[]) =>
    nums.length ? nums.reduce((a, b) => a + b, 0) / nums.length : 0;

/**
 * Aggregate raw points into DataPoints by bucketing every `bucketMs` milliseconds.
 * - bucketMs = MS_HOUR   → hourly averages,  label "HH:00"
 * - bucketMs = MS_MINUTE → minute averages,  label "HH:MM"
 * - bucketMs = MS_SECOND → 1-second buckets, label "HH:MM:SS"
 *
 * When `rangeStart` and `rangeEnd` are provided (Unix ms), the full time range is
 * iterated and any bucket with no matching raw points is emitted as a `noData: true`
 * placeholder — this is how we detect and visualise real sensor outage gaps.
 *
 * Pass range only for hourly / minutely resolution; omit for second-level zoom
 * (86 400 empty-second buckets would be too many to render efficiently).
 */
function aggregateByMs(
    raw: RawPoint[],
    bucketMs: number,
    rangeStart?: number,
    rangeEnd?: number,
): DataPoint[] {
    if (!raw.length && rangeStart == null) return [];

    const p2 = (n: number) => String(n).padStart(2, '0');
    const groups = new Map<number, RawPoint[]>();
    for (const p of raw) {
        const t           = new Date(p.timestamp).getTime();
        const bucketStart = Math.floor(t / bucketMs) * bucketMs;
        const arr         = groups.get(bucketStart);
        if (arr) arr.push(p);
        else     groups.set(bucketStart, [p]);
    }

    // Determine the first / last bucket to iterate
    let firstBucket: number;
    let lastBucket:  number;
    if (rangeStart != null && rangeEnd != null) {
        firstBucket = Math.floor(rangeStart / bucketMs) * bucketMs;
        lastBucket  = Math.floor(rangeEnd   / bucketMs) * bucketMs;
    } else {
        const sortedKeys = [...groups.keys()].sort((a, b) => a - b);
        if (!sortedKeys.length) return [];
        firstBucket = sortedKeys[0];
        lastBucket  = sortedKeys[sortedKeys.length - 1];
    }

    const result: DataPoint[] = [];
    for (let bucket = firstBucket; bucket <= lastBucket; bucket += bucketMs) {
        const d = new Date(bucket);
        let label: string;
        if (bucketMs >= MS_HOUR) {
            label = `${p2(d.getHours())}:00`;
        } else if (bucketMs >= MS_MINUTE) {
            label = `${p2(d.getHours())}:${p2(d.getMinutes())}`;
        } else {
            label = `${p2(d.getHours())}:${p2(d.getMinutes())}:${p2(d.getSeconds())}`;
        }
        const pts = groups.get(bucket);
        if (pts && pts.length > 0) {
            result.push({
                label,
                temperature: mean(pts.map(p => p.temperature)),
                humidity:    mean(pts.map(p => p.humidity)),
                airQuality:  mean(pts.map(p => p.airQuality)),
            });
        } else {
            // No sensor data for this bucket — mark as gap
            result.push({ label, temperature: 0, humidity: 0, airQuality: 0, noData: true });
        }
    }
    return result;
}

function round2(n: number) { return Math.round(n * 100) / 100; }

function findNoDataZones(pts: DataPoint[]): Array<[{ xAxis: string }, { xAxis: string }]> {
    const out: Array<[{ xAxis: string }, { xAxis: string }]> = [];
    let start: string | null = null;
    for (let i = 0; i < pts.length; i++) {
        const pt = pts[i];
        if (pt.noData && start == null) {
            // Anchor the grey zone at the PREVIOUS data point so there is no
            // empty white strip between the line's last rendered segment and the
            // shaded area.  The reading at that label remains visible because
            // the overlay is semi-transparent (opacity 0.18).
            start = i > 0 ? pts[i - 1].label : pt.label;
        } else if (!pt.noData && start != null) {
            out.push([{ xAxis: start }, { xAxis: pt.label }]);
            start = null;
        }
    }
    if (start != null && pts.length > 0) {
        out.push([{ xAxis: start }, { xAxis: pts[pts.length - 1].label }]);
    }
    return out;
}

const pad2 = (n: number) => String(n).padStart(2, '0');
const fmtDate = (d: Date) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
const fmtTime = (d: Date) => `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;

/** Convert an "HH:MM" or "HH:MM:SS" label to 12-hour format. */
function toTimeLabel12(label: string): string {
    const parts = label.split(':');
    if (parts.length < 2) return label;
    const h    = parseInt(parts[0], 10);
    const ampm = h >= 12 ? 'PM' : 'AM';
    const h12  = h % 12 || 12;
    return parts.length === 3
        ? `${h12}:${parts[1]}:${parts[2]} ${ampm}`
        : `${h12}:${parts[1]} ${ampm}`;
}

/**
 * Convert a chart x-axis label for display respecting time/date format preferences.
 * Handles: "HH:MM", "HH:MM:SS", "YYYY-MM-DD"
 */
function convertChartLabel(label: string, is12Hour: boolean, dateFormat: string): string {
    // Time labels
    if (/^\d{2}:\d{2}(:\d{2})?$/.test(label)) {
        return is12Hour ? toTimeLabel12(label) : label;
    }
    // Date labels
    const dm = label.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (dm) {
        const [, yyyy, mm, dd] = dm;
        if (dateFormat === 'MM_DD_YYYY') return `${mm}/${dd}/${yyyy}`;
        if (dateFormat === 'YYYY_MM_DD') return label;
        return `${dd}.${mm}.${yyyy}`;   // DD_MM_YYYY default
    }
    return label;
}

export const ClimateHistoryChart: React.FC<Props> = ({ roomId, hideDayView = false }) => {
    const toastRef = useRef<Toast>(null);
    const { convert: convertTemp, unit: tempUnit } = useTemperature();
    const { is12Hour, dateFormat } = useTimeFormat();
    const isMobile = useIsMobile();

    const [timeFilter, setTimeFilter] = useState<TimeFilter>(hideDayView ? 'Week' : 'Day');
    const [dateRange,  setDateRange]  = useState<Date[] | null>(null);
    const [metric,     setMetric]     = useState<Metric>('All');
    const [data,       setData]       = useState<DataPoint[]>([]);   // Week / Month / Custom
    const [limits,     setLimits]     = useState<Limits | null>(null);
    const [loading,    setLoading]    = useState(false);

    // ── Day-view progressive zoom state ──────────────────────────────────────
    // All raw points fetched from the API for the current day
    const [dayRawPoints, setDayRawPoints] = useState<RawPoint[]>([]);
    // ECharts dataZoom position (percentages 0–100); tracked so we can persist
    // them across option re-renders and use them to pick aggregation granularity
    const [zoomStart, setZoomStart] = useState(0);
    const [zoomEnd,   setZoomEnd]   = useState(100);

    // Reset zoom to full range whenever the time filter changes
    useEffect(() => {
        setZoomStart(0);
        setZoomEnd(100);
    }, [timeFilter]);

    /**
     * Choose the bucket size based on how many raw Day-view points fall in the
     * currently visible window:
     *   > 180 visible → hourly   (smooth overview of the full day)
     *   10–180        → minutely (trend within a few hours)
     *   < 10          → 1-second (individual readings)
     */
    const dayBucketMs = useMemo(() => {
        if (timeFilter !== 'Day' || !dayRawPoints.length) return MS_HOUR;
        const visibleFraction = (zoomEnd - zoomStart) / 100;
        const visibleCount    = dayRawPoints.length * visibleFraction;
        if (visibleCount > 180) return MS_HOUR;
        if (visibleCount > 10)  return MS_MINUTE;
        return MS_SECOND;
    }, [timeFilter, dayRawPoints.length, zoomStart, zoomEnd]);

    /** Aggregated Day-view data; recomputed whenever zoom level or raw data changes.
     *
     * For hourly and minutely resolution we pass the full 24-hour window so that
     * time buckets with no sensor readings are filled in as `noData: true` —
     * this is what makes the outage gap visible as a grey zone on the chart.
     * Second-level resolution is only active when very few (<10) points are
     * visible, so the bucket count would be huge; skip range-filling there.
     */
    const dayData = useMemo(() => {
        if (timeFilter !== 'Day') return [];
        let rangeStart: number | undefined;
        let rangeEnd:   number | undefined;
        // Only fill the full range once we have actual data — avoids a flash of
        // all-grey buckets before the initial fetch completes.
        if (dayBucketMs >= MS_MINUTE && dayRawPoints.length > 0) {
            // Match the exact window used by the fetch (last 24 h, truncated to hour)
            const end = new Date();
            end.setMinutes(0, 0, 0);
            rangeEnd   = end.getTime();
            rangeStart = rangeEnd - 24 * 60 * 60 * 1000;
        }
        return aggregateByMs(dayRawPoints, dayBucketMs, rangeStart, rangeEnd);
    }, [timeFilter, dayRawPoints, dayBucketMs]);

    /**
     * The data actually fed to the chart.
     * Day view uses zoom-reactive aggregation; all other views use static data.
     */
    const effectiveData = timeFilter === 'Day' ? dayData : data;

    /** Labels converted for display (12-h time / date-format preference). */
    const displayData = useMemo(
        () => effectiveData.map(d => ({ ...d, label: convertChartLabel(d.label, is12Hour, dateFormat) })),
        [effectiveData, is12Hour, dateFormat],
    );

    const metricOptions = [
        { label: 'All metrics', value: 'All' },
        { label: 'Temperature', value: 'Temperature' },
        { label: 'Humidity',    value: 'Humidity' },
        { label: 'Air Quality', value: 'Air Quality' },
    ];

    useEffect(() => {
        new RoomControllerApi().getLimitsForRoom({ roomId })
            .then(r => setLimits(r.data as any))
            .catch(() => {});
    }, [roomId]);

    const fetchData = useCallback(() => {
        setLoading(true);

        // ── Day view: fetch all raw points; aggregation is done reactively ──
        if (timeFilter === 'Day') {
            const now = new Date();
            const end = new Date(now);
            end.setMinutes(0, 0, 0);
            const start = new Date(end.getTime() - 24 * 60 * 60 * 1000);
            new RoomControllerApi().getOvertimeClimateData({
                roomId,
                startDate: fmtDate(start),
                endDate:   fmtDate(end),
                startTime: fmtTime(start),
                endTime:   fmtTime(end),
            })
            .then(r => setDayRawPoints(r.data as RawPoint[]))
            .catch(() => setDayRawPoints([]))
            .finally(() => setLoading(false));
            return; // Day view is self-contained — skip the shared handler below
        }

        // ── Week / Month / Custom: aggregated data from getClimateHistory ──
        let req: Promise<DataPoint[]>;

        if (timeFilter === 'Custom' && dateRange?.[0] && dateRange?.[1]) {
            const startDate = dateRange[0].toISOString().slice(0, 10);
            const endDate   = dateRange[1].toISOString().slice(0, 10);
            const diffDays  = Math.round(
                (dateRange[1].getTime() - dateRange[0].getTime()) / 86_400_000,
            );

            if (diffDays < 3) {
                setLoading(false);
                toastRef.current?.show({
                    severity: 'warn',
                    summary:  'Range too short',
                    detail:   'Please select a range of at least 3 days.',
                    life:     4000,
                });
                setTimeFilter('Week');
                setDateRange(null);
                return;
            }

            const granularity = diffDays <= 4
                ? 'HOUR'
                : diffDays < 45 ? 'DAY'
                : 'WEEK';

            req = new RoomControllerApi().getClimateHistory({
                    roomId, startDate, endDate,
                    granularity: granularity as GetClimateHistoryGranularityEnum,
                })
                .then(r => (r.data as AggPoint[]).map(d => ({
                    label:       d.date,
                    temperature: d.avgTemperature,
                    humidity:    d.avgHumidity,
                    airQuality:  d.avgAirQuality,
                })));
        } else {
            const startDate = fmtDate(new Date(
                Date.now() - 1000 * 60 * 60 * 24 * (timeFilter === 'Month' ? 31 : 7),
            ));
            req = new RoomControllerApi().getClimateHistory({
                    roomId,
                    startDate,
                    endDate: fmtDate(new Date()),
                    granularity: GetClimateHistoryGranularityEnum.DAY,
                })
                .then(r => (r.data as AggPoint[]).map(d => ({
                    label:       d.date,
                    temperature: d.avgTemperature,
                    humidity:    d.avgHumidity,
                    airQuality:  d.avgAirQuality,
                })));
        }

        req.then(setData).catch(() => setData([])).finally(() => setLoading(false));
    }, [roomId, timeFilter, dateRange]);

    useEffect(() => {
        if (timeFilter === 'Custom' && (!dateRange?.[0] || !dateRange?.[1])) return;
        fetchData();
    }, [fetchData, timeFilter, dateRange]);

    // ── ECharts event: track zoom so we can switch aggregation granularity ──
    const onEvents = useMemo(() => ({
        datazoom: (params: any) => {
            if (timeFilter !== 'Day') return;
            // ECharts may fire with a batch array (inside zoom) or flat params (slider)
            const ev     = Array.isArray(params.batch) ? params.batch[0] : params;
            const s: number | undefined = ev?.start;
            const e: number | undefined = ev?.end;
            if (s != null && e != null) {
                setZoomStart(s);
                setZoomEnd(e);
            }
        },
    }), [timeFilter]);

    const showTemp = metric === 'All' || metric === 'Temperature';
    const showHum  = metric === 'All' || metric === 'Humidity';
    const showAQ   = metric === 'All' || metric === 'Air Quality';

    const L = limits;

    // Contiguous ranges where a metric violates its limits
    function violationRanges(
        pts: DataPoint[],
        getValue: (d: DataPoint) => number,
        min: number | null,
        max: number | null,
    ): Array<[{ xAxis: string }, { xAxis: string }]> {
        if (!pts.length || (min == null && max == null)) return [];
        const out: Array<[{ xAxis: string }, { xAxis: string }]> = [];
        let start: string | null = null;
        for (const pt of pts) {
            // A gap in the sensor data must close any open violation range — we
            // cannot declare a violation where there are no readings at all.
            if (pt.noData) {
                if (start != null) { out.push([{ xAxis: start }, { xAxis: pt.label }]); start = null; }
                continue;
            }
            const v   = getValue(pt);
            const bad = (min != null && v < min) || (max != null && v > max);
            if (bad && start == null)  { start = pt.label; }
            else if (!bad && start != null) { out.push([{ xAxis: start }, { xAxis: pt.label }]); start = null; }
        }
        if (start != null) out.push([{ xAxis: start }, { xAxis: pts[pts.length - 1].label }]);
        return out;
    }

    function mkLimitLines(entries: Array<{ yAxis: number; name: string }>, color: string) {
        const valid = entries.filter(e => e.yAxis != null && Number.isFinite(e.yAxis));
        if (!valid.length) return undefined;
        return {
            silent:    true,
            symbol:    ['none', 'none'] as const,
            animation: false,
            lineStyle: { color, type: 'dashed' as const, width: 1.5, opacity: 0.8 },
            label:     { show: true, fontSize: 9, color, position: 'insideEndTop' as const, formatter: '{b}' },
            data:      valid,
        };
    }

    function mkViolationArea(
        ranges: Array<[{ xAxis: string }, { xAxis: string }]>,
        color:  string,
    ) {
        if (!ranges.length) return undefined;
        return {
            silent:    true,
            itemStyle: { color: color + '33', borderWidth: 0 },
            label:     { show: false },
            data:      ranges,
        };
    }

    const noDataZones = findNoDataZones(displayData);

    const series = [
        // Phantom series — renders no-data grey zones across all metrics
        {
            name:      '__nodata__',
            type:      'line' as const,
            data:      [],
            lineStyle: { opacity: 0 },
            itemStyle: { opacity: 0 },
            markArea:  noDataZones.length > 0 ? {
                silent:    true,
                itemStyle: { color: 'rgba(148, 163, 184, 0.18)', borderWidth: 0 },
                label:     { show: false },
                data: noDataZones,
            } : undefined,
        },
        showTemp && {
            name:      `Temperature (${tempUnit})`,
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.temperature, width: 2 },
            itemStyle: { color: COLORS.temperature },
            data:      displayData.map(d => d.noData ? null : round2(convertTemp(d.temperature))),
            markLine:  L && metric === 'Temperature' ? mkLimitLines([
                { yAxis: round2(convertTemp(L.tempMin)), name: `T ${round2(convertTemp(L.tempMin))}${tempUnit}` },
                { yAxis: round2(convertTemp(L.tempMax)), name: `T ${round2(convertTemp(L.tempMax))}${tempUnit}` },
            ], COLORS.temperature) : undefined,
            markArea: L ? mkViolationArea(
                violationRanges(displayData, d => d.temperature, L.tempMin ?? null, L.tempMax ?? null),
                COLORS.temperature,
            ) : undefined,
        },
        showHum && {
            name:      'Humidity (%)',
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.humidity, width: 2 },
            itemStyle: { color: COLORS.humidity },
            data:      displayData.map(d => d.noData ? null : round2(d.humidity)),
            markLine:  L && metric === 'Humidity' ? mkLimitLines([
                { yAxis: L.humMin, name: `H ${L.humMin}%` },
                { yAxis: L.humMax, name: `H ${L.humMax}%` },
            ], COLORS.humidity) : undefined,
            markArea: L ? mkViolationArea(
                violationRanges(displayData, d => d.humidity, L.humMin ?? null, L.humMax ?? null),
                COLORS.temperature,
            ) : undefined,
        },
        showAQ && {
            name:      'Air Quality (ppm)',
            type:      'line',
            smooth:    true,
            symbol:    'none',
            lineStyle: { color: COLORS.airQuality, width: 2 },
            itemStyle: { color: COLORS.airQuality },
            data:      displayData.map(d => d.noData ? null : round2(d.airQuality)),
            markLine:  L && metric === 'Air Quality' ? mkLimitLines([
                { yAxis: L.co2Max, name: `CO₂ ${L.co2Max}` },
            ], COLORS.airQuality) : undefined,
            markArea: L ? mkViolationArea(
                violationRanges(displayData, d => d.airQuality, null, L.co2Max ?? null),
                COLORS.temperature,
            ) : undefined,
        },
    ].filter(Boolean);

    const rotateLabels = timeFilter === 'Day' || timeFilter === 'Custom';
    const showLegend   = metric === 'All';

    // Rotated labels need ~54 px; legend needs ~46 px; reduced on mobile
    const gridBottom = isMobile
        ? (rotateLabels ? 40 : 22) + (showLegend ? 32 : 0)
        : (rotateLabels ? 54 : 28) + (showLegend ? 46 : 0);

    // Exclude no-data placeholders from min/max so gap zeros don't distort the y-axis
    const validData = effectiveData.filter(d => !d.noData);

    const yAxisMax: number | undefined = (() => {
        if (L == null || !validData.length) return undefined;
        if (metric === 'Temperature') {
            const topC = Math.max(validData.reduce((m, d) => Math.max(m, d.temperature), -Infinity), L.tempMax);
            const top  = convertTemp(topC);
            return Math.ceil(top + Math.abs(top) * 0.1);
        }
        if (metric === 'Humidity') {
            const top = Math.max(validData.reduce((m, d) => Math.max(m, d.humidity), 0), L.humMax);
            return Math.ceil(top * 1.1);
        }
        if (metric === 'Air Quality') {
            const top = Math.max(validData.reduce((m, d) => Math.max(m, d.airQuality), 0), L.co2Max);
            return Math.ceil(top * 1.1);
        }
        return undefined;
    })();

    const yAxisMin: number | undefined = (() => {
        if (L == null || !validData.length) return undefined;
        if (metric === 'Temperature') {
            const bottomC = Math.min(validData.reduce((m, d) => Math.min(m, d.temperature), Infinity), L.tempMin);
            const bottom  = convertTemp(bottomC);
            return Math.floor(bottom - Math.abs(bottom) * 0.1);
        }
        if (metric === 'Humidity') {
            const bottom = Math.min(validData.reduce((m, d) => Math.min(m, d.humidity), Infinity), L.humMin);
            return Math.max(0, Math.floor(bottom * 0.9));
        }
        return undefined;
    })();

    const chartTextColor = getComputedStyle(document.documentElement)
        .getPropertyValue('--chart-text-color').trim() || '#475569';

    const option = {
        tooltip: {
            trigger:     'axis' as const,
            axisPointer: { type: 'cross' as const },
            confine:     true,
            textStyle:   { fontSize: isMobile ? 11 : 12 },
            formatter: (params: any[]) => {
                if (!params.length) return '';
                const label = params[0]?.name ?? '';
                // Check if every real series has a null value → sensor gap
                const realParams = params.filter(p => p.seriesName !== '__nodata__');
                const isGap = realParams.length > 0 && realParams.every(p => p.value == null);
                if (isGap) {
                    return `<div style="padding:2px 0">
                        <div style="margin-bottom:4px;font-weight:600">${label}</div>
                        <div style="display:flex;align-items:center;gap:6px;margin-top:3px">
                            <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#94a3b8"></span>
                            <span style="color:#94a3b8;font-style:italic">No sensor data for this period</span>
                        </div>
                    </div>`;
                }
                let out = `<div style="margin-bottom:4px;font-weight:600">${label}</div>`;
                for (const p of params) {
                    if (p.seriesName === '__nodata__') continue;
                    if (p.value == null) continue;
                    const v = p.value as number;
                    let violated = false;
                    if (L) {
                        if (p.seriesName === `Temperature (${tempUnit})`)
                            violated = (L.tempMin != null && v < round2(convertTemp(L.tempMin))) ||
                                       (L.tempMax != null && v > round2(convertTemp(L.tempMax)));
                        else if (p.seriesName === 'Humidity (%)')
                            violated = (L.humMin != null && v < L.humMin) || (L.humMax != null && v > L.humMax);
                        else if (p.seriesName === 'Air Quality (ppm)')
                            violated = L.co2Max != null && v > L.co2Max;
                    }
                    const color = violated ? '#ef4444' : 'inherit';
                    out += `<div style="display:flex;align-items:center;gap:6px;margin-top:3px">
                        <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color}"></span>
                        <span style="color:${color}">${p.seriesName}:&nbsp;<b>${v}</b></span>
                    </div>`;
                }
                return out;
            },
        },
        legend: {
            show:   showLegend,
            bottom: 0,
            data:   [`Temperature (${tempUnit})`, 'Humidity (%)', 'Air Quality (ppm)'],
            formatter: isMobile
                ? (name: string) => {
                    if (name.includes('Temperature')) return `Temp (${tempUnit})`;
                    if (name === 'Humidity (%)')       return 'Hum (%)';
                    if (name === 'Air Quality (ppm)')  return 'CO₂';
                    return name;
                }
                : undefined,
            textStyle: { fontSize: isMobile ? 10 : 11, color: chartTextColor },
        },
        grid: {
            left:         isMobile ? 36 : 48,
            right:        isMobile ? 14 : 24,
            top:          isMobile ? 10 : 16,
            bottom:       gridBottom,
            containLabel: false,
        },
        xAxis: {
            type:        'category' as const,
            boundaryGap: false,
            data:        displayData.map(d => d.label),
            axisLabel: {
                rotate:   rotateLabels ? (isMobile ? 20 : 35) : 0,
                fontSize: isMobile ? 10 : 11,
                interval: 'auto' as const,
                align:    rotateLabels ? 'right' : 'center',
                color:    chartTextColor,
            },
            axisLine: { lineStyle: { color: '#e2e8f0' } },
        },
        yAxis: {
            type:      'value' as const,
            max:       yAxisMax,
            min:       yAxisMin,
            nice:      true,
            splitLine: { lineStyle: { type: 'dashed' as const, color: '#f1f5f9' } },
            axisLabel: { fontSize: isMobile ? 10 : 11, color: chartTextColor },
        },
        dataZoom: [{
            type:          'inside' as const,
            xAxisIndex:    0,
            filterMode:    'none' as const,
            // Persist the zoom position across re-renders (needed when aggregation
            // level changes and xAxis.data length changes)
            start:         zoomStart,
            end:           zoomEnd,
            // Never allow zooming past 2 visible data points
            minValueSpan:  1,
        }],
        series,
    };

    return (
        <div className="chart-card">
            <Toast ref={toastRef} position="top-right" />
            <div className="chart-header">
                <div className="time-filters">
                    {(['Day', 'Week', 'Month'] as TimeFilter[])
                        .filter(f => !(hideDayView && f === 'Day'))
                        .map(f => (
                            <button
                                key={f}
                                className={`time-filter-btn ${timeFilter === f ? 'active' : ''}`}
                                onClick={() => { setTimeFilter(f); setDateRange(null); }}
                            >
                                {f}
                            </button>
                        ))}
                    <DashboardCalendar
                        dateRange={dateRange}
                        setDateRange={setDateRange}
                        isActive={timeFilter === 'Custom'}
                        onActivate={() => setTimeFilter('Custom')}
                    />
                </div>

                <Dropdown
                    value={metric}
                    options={metricOptions}
                    onChange={e => setMetric(e.value as Metric)}
                    className="metric-select"
                    panelClassName="metric-select-panel"
                />
            </div>

            {loading ? (
                <div className="chart-loading">Loading…</div>
            ) : effectiveData.length === 0 ? (
                <div className="chart-loading">No data available</div>
            ) : (
                <div className="chart-echarts-wrapper">
                    <ReactECharts
                        option={option}
                        style={{ height: '100%', width: '100%' }}
                        notMerge
                        onEvents={onEvents}
                    />
                </div>
            )}
        </div>
    );
};
